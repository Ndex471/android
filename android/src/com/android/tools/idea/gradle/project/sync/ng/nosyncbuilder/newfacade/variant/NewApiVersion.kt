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
package com.android.tools.idea.gradle.project.sync.ng.nosyncbuilder.newfacade.variant

import com.android.tools.idea.gradle.project.sync.ng.nosyncbuilder.interfaces.variant.ApiVersion
import com.android.tools.idea.gradle.project.sync.ng.nosyncbuilder.misc.OldApiVersion
import com.android.tools.idea.gradle.project.sync.ng.nosyncbuilder.proto.VariantProto

data class NewApiVersion(
  override val apiLevel: Int,
  override val codename: String?,
  override val apiString: String) : ApiVersion {
  constructor(oldApiVersion: OldApiVersion) : this(
    oldApiVersion.apiLevel,
    oldApiVersion.codename,
    oldApiVersion.apiString
  )

  constructor(proto: VariantProto.ApiVersion) : this(
    proto.apiLevel,
    if (proto.hasCodename()) proto.codename else null,
    proto.apiString
  )
}
