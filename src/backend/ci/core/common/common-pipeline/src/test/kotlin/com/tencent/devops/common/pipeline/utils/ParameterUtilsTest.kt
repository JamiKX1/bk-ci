package com.tencent.devops.common.pipeline.utils

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildLessAtomElement
import org.junit.Assert
import org.junit.Test

/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
class ParameterUtilsTest {
    @Test
    fun parameterSizeCheck() {
        val objectMapper = ObjectMapper()
        val data1 = mutableMapOf<String, Any>()
        data1["key1"] = "value1"
        val element1 = MarketBuildLessAtomElement(
            name = "test",
            id = "e-xxx",
            status = "running",
            atomCode = "testAtom",
            version = "1.0",
            data = data1
        )
        Assert.assertTrue(ParameterUtils.parameterSizeCheck(element1, objectMapper))

        var sb = StringBuffer()
        while (sb.length < 65534) {
            sb.append("this is too long value,")
        }
        data1["key2"] = sb.toString()
        val element2 = MarketBuildLessAtomElement(
            name = "test2",
            id = "e-xxx",
            status = "running",
            atomCode = "testAtom2",
            version = "1.0",
            data = data1
        )
        Assert.assertFalse(ParameterUtils.parameterSizeCheck(element2, objectMapper))
    }
}
