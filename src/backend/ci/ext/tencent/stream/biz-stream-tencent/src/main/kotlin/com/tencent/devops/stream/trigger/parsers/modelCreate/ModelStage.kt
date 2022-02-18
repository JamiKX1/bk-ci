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

package com.tencent.devops.stream.trigger.parsers.modelCreate

import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.ci.v2.JobRunsOnType
import com.tencent.devops.common.ci.v2.Resources
import com.tencent.devops.common.ci.v2.stageCheck.ReviewVariable
import com.tencent.devops.common.ci.v2.stageCheck.StageCheck
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.enums.StageRunCondition
import com.tencent.devops.common.pipeline.option.StageControlOption
import com.tencent.devops.common.pipeline.pojo.StagePauseCheck
import com.tencent.devops.common.pipeline.pojo.StageReviewGroup
import com.tencent.devops.common.pipeline.pojo.element.atom.ManualReviewParam
import com.tencent.devops.common.pipeline.pojo.element.atom.ManualReviewParamPair
import com.tencent.devops.common.pipeline.pojo.element.atom.ManualReviewParamType
import com.tencent.devops.common.quality.pojo.enums.QualityOperation
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.pojo.BuildTemplateAcrossInfo
import com.tencent.devops.stream.common.exception.QualityRulesException
import com.tencent.devops.stream.pojo.GitProjectPipeline
import com.tencent.devops.stream.pojo.GitRequestEvent
import com.tencent.devops.stream.pojo.enums.GitCINotifyType
import com.tencent.devops.stream.pojo.v2.GitCIBasicSetting
import com.tencent.devops.quality.api.v2.pojo.ControlPointPosition
import com.tencent.devops.quality.api.v3.ServiceQualityRuleResource
import com.tencent.devops.quality.api.v3.pojo.request.RuleCreateRequestV3
import com.tencent.devops.quality.pojo.enum.RuleOperation
import com.tencent.devops.stream.trigger.parsers.triggerMatch.matchUtils.PathMatchUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import com.tencent.devops.common.ci.v2.Stage as GitCIV2Stage
import com.tencent.devops.stream.pojo.v2.QualityElementInfo
import org.springframework.beans.factory.annotation.Value
import org.springframework.util.AntPathMatcher

@Component
class ModelStage @Autowired constructor(
    private val client: Client,
    private val modelContainer: ModelContainer,
    private val modelElement: ModelElement
) {

    @Value("\${stream.marketRun.atomCode:#{null}}")
    private val runPlugInAtomCode: String? = null

    companion object {
        private val logger = LoggerFactory.getLogger(ModelStage::class.java)
    }

    private val matcher = AntPathMatcher()

    // 根据顺序，先匹配 <= 和 >= 在匹配 = > <因为 >= 包含 > 和 =
    val operations = mapOf(
        QualityOperation.convertToSymbol(QualityOperation.GE) to QualityOperation.GE,
        QualityOperation.convertToSymbol(QualityOperation.LE) to QualityOperation.LE,
        QualityOperation.convertToSymbol(QualityOperation.GT) to QualityOperation.GT,
        QualityOperation.convertToSymbol(QualityOperation.LT) to QualityOperation.LT,
        QualityOperation.convertToSymbol(QualityOperation.EQ) to QualityOperation.EQ
    )

    fun createStage(
        stage: GitCIV2Stage,
        event: GitRequestEvent,
        gitBasicSetting: GitCIBasicSetting,
        stageIndex: Int,
        finalStage: Boolean = false,
        resources: Resources? = null,
        changeSet: Set<String>? = null,
        pipeline: GitProjectPipeline,
        jobBuildTemplateAcrossInfos: Map<String, BuildTemplateAcrossInfo>?,
        elementNames: MutableList<QualityElementInfo>?
    ): Stage {
        val containerList = mutableListOf<Container>()
        val stageEnable = PathMatchUtils.isIncludePathMatch(stage.ifModify, changeSet)

        stage.jobs.forEachIndexed { jobIndex, job ->
            val jobEnable = stageEnable && PathMatchUtils.isIncludePathMatch(job.ifModify, changeSet)
            val elementList = modelElement.makeElementList(
                job = job,
                gitBasicSetting = gitBasicSetting,
                changeSet = changeSet,
                jobEnable = jobEnable,
                event = event
            )

            if (job.runsOn.poolName == JobRunsOnType.AGENT_LESS.type) {
                modelContainer.addNormalContainer(
                    job = job,
                    elementList = elementList,
                    containerList = containerList,
                    jobIndex = jobIndex,
                    jobEnable = jobEnable,
                    finalStage = finalStage
                )
            } else {
                modelContainer.addVmBuildContainer(
                    job = job,
                    elementList = elementList,
                    containerList = containerList,
                    jobIndex = jobIndex,
                    projectCode = gitBasicSetting.projectCode!!,
                    finalStage = finalStage,
                    jobEnable = jobEnable,
                    resources = resources,
                    buildTemplateAcrossInfo = jobBuildTemplateAcrossInfos?.get(job.id)
                )
            }

            // 添加红线指标判断需要的数据
            elementList.forEach { ele ->
                elementNames?.add(QualityElementInfo(ele.name, ele.getAtomCode().let {
                    // 替换 run 插件使其不管使用什么具体插件，在红线那边都是 run
                    if (it == runPlugInAtomCode) {
                        "run"
                    } else {
                        it
                    }
                }))
            }
        }

        // 根据if设置stageController
        var stageControlOption = StageControlOption()
        if (!finalStage && !stage.ifField.isNullOrBlank()) {
            stageControlOption = StageControlOption(
                runCondition = StageRunCondition.CUSTOM_CONDITION_MATCH,
                customCondition = stage.ifField.toString()
            )
        }
        stageControlOption = stageControlOption.copy(enable = stageEnable)

        val stageId = VMUtils.genStageId(stageIndex)
        return Stage(
            id = stageId,
            name = stage.name ?: if (finalStage) {
                "Final"
            } else {
                VMUtils.genStageId(stageIndex - 1)
            },
            tag = stage.label,
            fastKill = stage.fastKill,
            stageControlOption = stageControlOption,
            containers = containerList,
            finally = finalStage,
            checkIn = createStagePauseCheck(
                stageCheck = stage.checkIn,
                position = ControlPointPosition.BEFORE_POSITION,
                event = event,
                pipeline = pipeline,
                stageId = stageId,
                elementNames = elementNames
            ),
            checkOut = createStagePauseCheck(
                stageCheck = stage.checkOut,
                position = ControlPointPosition.AFTER_POSITION,
                event = event,
                pipeline = pipeline,
                stageId = stageId,
                elementNames = elementNames
            )
        )
    }

    private fun createStagePauseCheck(
        stageCheck: StageCheck?,
        position: String,
        event: GitRequestEvent,
        pipeline: GitProjectPipeline,
        stageId: String,
        elementNames: MutableList<QualityElementInfo>?
    ): StagePauseCheck? {
        if (stageCheck == null) return null
        val check = StagePauseCheck()
        check.timeout = stageCheck.timeoutHours
        if (stageCheck.reviews?.flows?.isNotEmpty() == true) {
            check.manualTrigger = true
            check.reviewDesc = stageCheck.reviews?.description
            check.reviewParams = createReviewParams(stageCheck.reviews?.variables)
            check.reviewGroups = stageCheck.reviews?.flows?.map { it ->
                StageReviewGroup(name = it.name, reviewers = it.reviewers)
            }?.toMutableList()
        }
        if (stageCheck.gates?.isNotEmpty() == true) {
            check.ruleIds = createRules(
                stageCheck = stageCheck,
                event = event,
                position = position,
                pipeline = pipeline,
                stageId = stageId,
                elementNames = elementNames
            )
        }
        return check
    }

    private fun createReviewParams(variables: Map<String, ReviewVariable>?): List<ManualReviewParam>? {
        if (variables.isNullOrEmpty()) return null
        val params = mutableListOf<ManualReviewParam>()
        variables.forEach { (key, variable) ->
            params.add(ManualReviewParam(
                key = "variables.$key",
                value = variable.default,
                required = true,
                valueType = when (variable.type) {
                    "TEXTAREA" -> ManualReviewParamType.TEXTAREA
                    "SELECTOR" -> ManualReviewParamType.ENUM
                    "SELECTOR-MULTIPLE" -> ManualReviewParamType.MULTIPLE
                    "BOOL" -> ManualReviewParamType.BOOLEAN
                    else -> ManualReviewParamType.STRING
                },
                chineseName = variable.label,
                desc = variable.description,
                options = variable.values?.map { ManualReviewParamPair(it, it) }
            ))
        }
        return params
    }

    /**
     * 根据规则创建红线
     * 规则实例： CodeccCheckAtomDebug.coverity_serious_defect <= 2
     */
    @Suppress("ComplexMethod")
    private fun createRules(
        stageCheck: StageCheck,
        event: GitRequestEvent,
        position: String,
        pipeline: GitProjectPipeline,
        stageId: String,
        elementNames: MutableList<QualityElementInfo>?
    ): List<String>? {
        val ruleList: MutableList<RuleCreateRequestV3> = mutableListOf()
        val taskSteps: MutableList<RuleCreateRequestV3.CreateRequestTask> = mutableListOf()
        stageCheck.gates?.forEach GateEach@{ gate ->
            val indicators = gate.rule.map { rule ->
                // threshold可能包含小数，所以把最后的一部分都取出来在分割
                var (atomCode, stepName, mid) = getAtomCodeAndOther(rule, operations)
                var op = ""
                run breaking@{
                    operations.keys.forEach {
                        if (mid.contains(it)) {
                            op = it
                            return@breaking
                        }
                    }
                }
                if (op.isBlank()) {
                    logger.warn("GitProject: ${event.gitProjectId} event: ${event.id} rule: $rule not find operations")
                    return@GateEach
                }
                val enNameAndThreshold = mid.split(op)

                // 步骤不为空时添加步骤参数
                if (stepName != null) {
                    atomCode = checkAndGetRealStepName(stepName, elementNames) ?: atomCode
                    taskSteps.add(
                        RuleCreateRequestV3.CreateRequestTask(
                            taskName = stepName.removeSuffix("*"),
                            indicatorEnName = enNameAndThreshold.first().trim()
                        )
                    )
                }

                RuleCreateRequestV3.CreateRequestIndicator(
                    atomCode = atomCode,
                    enName = enNameAndThreshold.first().trim(),
                    operation = operations[op]!!.name,
                    threshold = enNameAndThreshold.last().trim()
                )
            }
            val opList = mutableListOf<RuleCreateRequestV3.CreateRequestOp>()
            gate.notifyOnFail.forEach NotifyEach@{ notify ->
                val type = GitCINotifyType.getNotifyByYaml(notify.type) ?: return@NotifyEach
                opList.add(
                    RuleCreateRequestV3.CreateRequestOp(
                        operation = RuleOperation.END,
                        notifyTypeList = listOf(type),
                        // 通知接受人未填缺省触发人
                        notifyUserList = if (notify.receivers.isNullOrEmpty()) {
                            listOf(event.userId)
                        } else {
                            notify.receivers?.toList()
                        },
                        notifyGroupList = null,
                        auditUserList = null,
                        auditTimeoutMinutes = null
                    )
                )
            }
            ruleList.add(
                RuleCreateRequestV3(
                    name = gate.name,
                    desc = "",
                    indicators = indicators,
                    position = position,
                    range = listOf(pipeline.pipelineId),
                    templateRange = null,
                    gatewayId = null,
                    opList = opList,
                    stageId = stageId,
                    gateKeepers = gate.continueOnFail?.gatekeepers,
                    taskSteps = taskSteps
                )
            )
        }
        logger.info("GitProject: ${event.gitProjectId} event: ${event.id} ruleList: $ruleList create gates")
        try {
            val resultList = client.get(ServiceQualityRuleResource::class).create(
                userId = event.userId,
                projectId = "git_${event.gitProjectId}",
                pipelineId = pipeline.pipelineId,
                ruleList = ruleList
            ).data
            if (!resultList.isNullOrEmpty()) return resultList.map { it.ruleBuildId }
        } catch (ignore: Throwable) {
            logger.warn("Failed to save quality rules with error: ", ignore.message)
            if (ignore is RemoteServiceException) {
                throw QualityRulesException(ignore.errorMessage, ignore.errorCode.toString())
            } else {
                throw QualityRulesException(ignore.message ?: "")
            }
        }
        return null
    }

    // 1、 <插件code>.<指标名><操作符><阈值>
    // 2、 <插件code>.<步骤名称>.<指标名><操作符><阈值>
    fun getAtomCodeAndOther(rule: String, operations: Map<String, QualityOperation>): Triple<String, String?, String> {
        var op = ""
        operations.keys.forEach {
            if (rule.contains(it)) {
                op = it
            }
        }
        if (op.isBlank()) {
            throw QualityRulesException("gates rules format error: no quality operations")
        }
        return when (rule.split(op).first().toCharArray()
            .filter { it == '.' }
            .groupBy { it.toString() }
            .ifEmpty { null }?.get(".")?.count()
        ) {
            1 -> {
                val index = rule.indexOfFirst { it == '.' }
                return Triple(
                    rule.substring(0 until index),
                    null,
                    rule.substring((index + 1) until rule.length)
                )
            }
            2 -> {
                val firstIndex = rule.indexOfFirst { it == '.' }
                val first = rule.substring(0 until firstIndex)
                val second = rule.removePrefix("$first.").indexOfFirst { it == '.' } + first.length + 1
                return Triple(
                    first,
                    rule.substring((firstIndex + 1) until second),
                    rule.substring((second + 1) until rule.length)
                )
            }
            else -> {
                throw QualityRulesException("gates rules format error: '.' number is wrong")
            }
        }
    }

    // 校验 当相同插件相同步骤名称匹配到多个时，系统无法判断以谁为准，将判定为失败
    fun checkAndGetRealStepName(stepName: String, elementNames: MutableList<QualityElementInfo>?): String? {
        if (elementNames.isNullOrEmpty()) {
            return null
        }
        var matchTimes = 0
        var atomCode: String? = null
        elementNames.forEach {
            if (matcher.match(stepName.replace("*", "**"), it.elementName)) {
                matchTimes++
                atomCode = it.atomCode
            }
            if (matchTimes >= 2) {
                throw QualityRulesException(
                    "there are multiple matches with the same step name $stepName of the same plug-in ${it.elementName}"
                )
            }
        }
        if (matchTimes < 1) {
            throw QualityRulesException("there none matches with the step name $stepName")
        }
        return atomCode
    }
}
