/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.tools.profilers.cpu

import com.android.testutils.TestUtils
import com.android.tools.adtui.model.FakeTimer
import com.android.tools.idea.transport.faketransport.FakeTransportService.FAKE_DEVICE_NAME
import com.android.tools.idea.transport.faketransport.FakeTransportService.FAKE_PROCESS_NAME
import com.android.tools.profiler.proto.Cpu.CpuTraceInfo
import com.android.tools.profiler.proto.Cpu.CpuTraceType
import com.android.tools.profiler.proto.CpuProfiler.CpuProfilingAppStartResponse
import com.android.tools.profiler.proto.CpuProfiler.CpuProfilingAppStopResponse
import com.android.tools.profiler.proto.CpuProfiler.GetTraceResponse
import com.android.tools.profiler.protobuf3jarjar.ByteString
import com.android.tools.profilers.FakeIdeProfilerServices
import com.android.tools.profilers.ProfilerClient
import com.android.tools.profilers.StudioProfilers
import com.google.common.truth.Truth.assertThat
import org.junit.rules.ExternalResource
import java.util.concurrent.TimeUnit

/**
 * A JUnit rule for simulating CPU profiler events.
 */
class FakeCpuProfiler(val grpcChannel: com.android.tools.idea.transport.faketransport.FakeGrpcChannel,
                      val cpuService: FakeCpuService) : ExternalResource() {

  lateinit var ideServices: FakeIdeProfilerServices
  lateinit var stage: CpuProfilerStage
  private lateinit var timer: FakeTimer

  override fun before() {
    ideServices = FakeIdeProfilerServices()
    timer = FakeTimer()

    val profilers = StudioProfilers(ProfilerClient(grpcChannel.name), ideServices, timer)
    // One second must be enough for new devices (and processes) to be picked up
    profilers.setPreferredProcess(FAKE_DEVICE_NAME, FAKE_PROCESS_NAME, null)
    timer.tick(FakeTimer.ONE_SECOND_IN_NS)

    stage = CpuProfilerStage(profilers)
    stage.studioProfilers.stage = stage
  }

  fun startCapturing(success: Boolean = true) {
    assertThat(stage.captureState).isEqualTo(CpuProfilerStage.CaptureState.IDLE)

    cpuService.setStartProfilingStatus(
      when (success) {
        true -> CpuProfilingAppStartResponse.Status.SUCCESS
        false -> CpuProfilingAppStartResponse.Status.FAILURE
      }
    )

    stage.startCapturing()
    assertThat(stage.captureState).isEqualTo(CpuProfilerStage.CaptureState.CAPTURING)
  }

  fun stopCapturing(success: Boolean = true) {
    assertThat(stage.captureState).isEqualTo(CpuProfilerStage.CaptureState.CAPTURING)

    cpuService.setStopProfilingStatus(
      when (success) {
        true -> {
          cpuService.setValidTrace(true)
          CpuProfilingAppStopResponse.Status.SUCCESS
        }
        false -> CpuProfilingAppStopResponse.Status.STOP_COMMAND_FAILED
      }
    )

    stage.stopCapturing()
  }

  /**
   * Simulates capturing a trace.
   *
   * @param id ID of the trace
   * @param fromUs starting timestamp of the trace
   * @param toUs ending timestamp of the trace
   * @param traceType the profiler type of the trace
   */
  fun captureTrace(id: Long = 0,
                   fromUs: Long = 0,
                   toUs: Long = 0,
                   traceType: CpuTraceType = CpuTraceType.ART) {

    // Change the selected configuration, so that startCapturing() will use the correct one.
    selectConfig(traceType)
    startCapturing()

    cpuService.apply {
      setTraceId(id)
      this.traceType = traceType
    }
    stopCapturing()
    // In production, stopCapturing would insert the trace info for us.
    // However, in tests, it is useful to pass a fake trace info.
    cpuService.setGetTraceResponseStatus(GetTraceResponse.Status.SUCCESS)
    cpuService.addTraceInfo(CpuTraceInfo.newBuilder()
                              .setTraceId(id)
                              .setTraceType(traceType)
                              .setTraceFilePath("fake_path_$id.trace")
                              .setFromTimestamp(TimeUnit.MICROSECONDS.toNanos(fromUs))
                              .setToTimestamp(TimeUnit.MICROSECONDS.toNanos(toUs))
                              .build())
  }

  fun setTrace(trace: ByteString) {
    cpuService.setTrace(trace)
  }

  fun setTrace(path: String) {
    val file = TestUtils.getWorkspaceFile(path)
    setTrace(trace = CpuProfilerTestUtils.traceFileToByteString(file))
  }

  /**
   * Selects a configuration from [CpuProfilerStage] with the given profiler type.
   */
  private fun selectConfig(traceType: CpuTraceType) {
    when (traceType) {
      CpuTraceType.ATRACE -> ideServices.enableAtrace(true)
      else -> {
      }
    }

    stage.profilerConfigModel.apply {
      // We are manually updating configurations, as enabling a feature flag could introduce additional configs.
      updateProfilingConfigurations()
      profilingConfiguration = defaultProfilingConfigurations.first { it.traceType == traceType }
    }
  }
}