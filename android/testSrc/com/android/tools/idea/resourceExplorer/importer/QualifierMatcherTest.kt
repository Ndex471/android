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
package com.android.tools.idea.resourceExplorer.importer

import com.android.ide.common.resources.configuration.DensityQualifier
import com.android.ide.common.resources.configuration.NightModeQualifier
import com.android.ide.common.resources.configuration.ResourceQualifier
import com.android.resources.Density
import com.android.resources.NightMode
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test


class QualifierMatcherTest {

  @Test
  fun parsePathWithNoMapper() {
    val qualifierLexer = QualifierMatcher()
    val (resourceName, qualifiers) = qualifierLexer.parsePath("/test/Path/file.png")
    assertEquals("_test_Path_file", resourceName)
    assertEquals(0, qualifiers.size)
  }

  @Test
  fun parsePathWithFileNameMappers() {
    val mappers = setOf(
        EnumBasedMapper("", "", DensityQualifier::class.java,
            mapOf(
                "@2x" to Density.XHIGH,
                "@3x" to Density.XXHIGH
            ), Density.MEDIUM))
    val qualifierLexer = QualifierMatcher(mappers)
    checkResult(qualifierLexer.parsePath("icon@2x.png"), "icon", DensityQualifier(Density.XHIGH))
    checkResult(qualifierLexer.parsePath("icon@3x.png"), "icon", DensityQualifier(Density.XXHIGH))
    checkResult(qualifierLexer.parsePath("icon.png"), "icon", DensityQualifier(Density.MEDIUM))
  }

  @Test
  fun parsePathWithIncompleteMapper() {
    val mappers = setOf(
        EnumBasedMapper("", "", DensityQualifier::class.java,
            mapOf(
                "@2x" to Density.XHIGH,
                "@3x" to Density.XXHIGH
            ), Density.MEDIUM),
        EnumBasedMapper("", "", NightModeQualifier::class.java, mapOf(
            "_dark" to NightMode.NIGHT
        )))
    val qualifierLexer = QualifierMatcher(mappers)
    checkResult(qualifierLexer.parsePath("icon@2x_dark.png"), "icon", DensityQualifier(Density.XHIGH), NightModeQualifier(NightMode.NIGHT))
    checkResult(qualifierLexer.parsePath("icon_dark@2x.png"), "icon_dark", DensityQualifier(Density.XHIGH))
    checkResult(qualifierLexer.parsePath("icon_dark.png"), "icon", NightModeQualifier(NightMode.NIGHT), DensityQualifier(Density.MEDIUM))
  }

  @Test
  fun parsePathEmptyStringMapper() {
    val mappers = setOf(
        EnumBasedMapper("", "", DensityQualifier::class.java,
            mapOf(
                "@2x" to Density.XHIGH,
                "@3x" to Density.XXHIGH,
                "" to Density.MEDIUM)),
        EnumBasedMapper("", "", NightModeQualifier::class.java, mapOf(
            "_dark" to NightMode.NIGHT
        )))
    val qualifierLexer = QualifierMatcher(mappers)
    checkResult(qualifierLexer.parsePath("icon@2x_dark.png"), "icon", DensityQualifier(Density.XHIGH), NightModeQualifier(NightMode.NIGHT))
    checkResult(qualifierLexer.parsePath("icon_dark@2x.png"), "icon_dark", DensityQualifier(Density.XHIGH))
    checkResult(qualifierLexer.parsePath("icon_dark.png"), "icon", NightModeQualifier(NightMode.NIGHT), DensityQualifier(Density.MEDIUM))
  }

  private fun checkResult(result: QualifierMatcher.Result, name: String, vararg qualifiers: ResourceQualifier) {
    val (resourceName, resultQualifiers) = result
    assertEquals(name, resourceName)
    assertArrayEquals(resultQualifiers.toTypedArray(), qualifiers)
  }
}