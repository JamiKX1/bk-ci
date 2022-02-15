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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.stream.trigger.template

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.ci.v2.PreTemplateScriptBuildYaml
import com.tencent.devops.common.ci.v2.utils.ScriptYmlUtils
import com.tencent.devops.common.ci.v2.utils.YamlCommonUtils
import com.tencent.devops.stream.trigger.template.pojo.GetTemplateParam
import com.tencent.devops.stream.trigger.template.pojo.TemplateProjectData
import org.junit.Test

import org.springframework.core.io.ClassPathResource
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.StringReader

class YamlTemplateTest {

    val sampleDir = "samples"

    @Test
    fun testAllTemplate() {
        val dir = "all"
        check("$sampleDir/$dir/all.yml")
    }

    @Test
    fun testStagesTemplate() {
        val dir = "stages"
        check("$sampleDir/$dir/stages.yml")
    }

    @Test
    fun testJobsTemplate() {
        val dir = "jobs"
        check("$sampleDir/$dir/jobs.yml")
    }

    @Test
    fun testStepsTemplate() {
        val dir = "steps"
        check("$sampleDir/$dir/stepss.yml")
    }

    @Test
    fun testExtendsTemplate() {
        val dir = "extends"
        check("$sampleDir/$dir/extends.yml")
    }

    @Test
    fun testSpecialsTemplate() {
        val dir = "specials"
        check("$sampleDir/$dir/longParametersTest.yml")

        check("$sampleDir/$dir/user.yml")
    }

    private fun check(file: String) {
        var flag = true
        val sample = BufferedReader(
            StringReader(
                replace(file)
            )
        )
        val compared = BufferedReader(
            StringReader(
                getStrFromResource("compared/${file.removePrefix("samples")}")
            )
        )
        var line = sample.readLine()
        var lineCompare = compared.readLine()
        while (line != null) {
            // 随机生成的id不计入比较
            if (line.trim().startsWith("id") || line.trim().startsWith("- id")) {
                line = sample.readLine()
                lineCompare = compared.readLine()
                continue
            }
            if (line != lineCompare) {
                println("$line != $lineCompare")
                flag = false
                break
            }
            line = sample.readLine()
            lineCompare = compared.readLine()
        }
        assert(flag)
    }

    private fun replace(testYaml: String): String {
        val sb = getStrFromResource(testYaml)

        val yaml = ScriptYmlUtils.formatYaml(sb)
        val preTemplateYamlObject = YamlUtil.getObjectMapper().readValue(yaml, PreTemplateScriptBuildYaml::class.java)
        val preScriptBuildYaml = YamlTemplate(
            filePath = testYaml,
            yamlObject = preTemplateYamlObject,
            projectData = TemplateProjectData(
                gitRequestEventId = 1,
                triggerUserId = "ruotiantang",
                triggerProjectId = 580280,
                triggerToken = "",
                triggerRef = "master",
                sourceProjectId = 580280,
                changeSet = null,
                event = null,
                forkGitToken = null
            ),
            getTemplateMethod = ::getTestTemplate,
            nowRepo = null,
            repo = null
        ).replace()
        val (normalOb, trans) = ScriptYmlUtils.normalizeGitCiYaml(preScriptBuildYaml, "")
        val yamls = YamlUtil.toYaml(normalOb)
        println(YamlCommonUtils.toYamlNotNull(preScriptBuildYaml))
        println("------------------------")
        println(JsonUtil.toJson(trans))
        println("------------------------")
        println(yamls)
        return yamls
    }

    private fun getTestTemplate(
        param: GetTemplateParam
    ): String {
        val newPath = if (param.targetRepo == null) {
            "templates/${param.fileName}"
        } else {
            "templates/${param.targetRepo}/templates/${param.fileName}"
        }
        val sb = getStrFromResource(newPath)
        return ScriptYmlUtils.formatYaml(sb)
    }

    private fun getStrFromResource(testYaml: String): String {
        val classPathResource = ClassPathResource(testYaml)
        val inputStream: InputStream = classPathResource.inputStream
        val isReader = InputStreamReader(inputStream)

        val reader = BufferedReader(isReader)
        val sb = StringBuffer()
        var str: String?
        while (reader.readLine().also { str = it } != null) {
            sb.append(str).append("\n")
        }
        inputStream.close()
        return sb.toString()
    }
}
