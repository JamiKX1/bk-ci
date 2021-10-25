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

package com.tencent.devops.quality.service.v2

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.quality.pojo.enums.QualityOperation
import com.tencent.devops.common.quality.pojo.enums.RuleInterceptResult
import com.tencent.devops.model.quality.tables.records.TQualityRuleBuildHisRecord
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.pojo.StageQualityRequest
import com.tencent.devops.quality.api.v2.pojo.ControlPointPosition
import com.tencent.devops.quality.api.v2.pojo.QualityIndicator
import com.tencent.devops.quality.api.v2.pojo.QualityRule
import com.tencent.devops.quality.api.v2.pojo.enums.QualityDataType
import com.tencent.devops.quality.api.v2.pojo.request.RuleCreateRequest
import com.tencent.devops.quality.api.v3.pojo.request.BuildCheckParamsV3
import com.tencent.devops.quality.api.v3.pojo.request.RuleCreateRequestV3
import com.tencent.devops.quality.api.v3.pojo.response.RuleCreateResponseV3
import com.tencent.devops.quality.dao.HistoryDao
import com.tencent.devops.quality.dao.v2.QualityRuleBuildHisDao
import com.tencent.devops.quality.dao.v2.QualityRuleBuildHisOperationDao
import com.tencent.devops.quality.exception.QualityOpConfigException
import com.tencent.devops.quality.pojo.enum.RuleOperation
import org.apache.commons.lang3.math.NumberUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import javax.ws.rs.core.Response

@Service
class QualityRuleBuildHisService constructor(
    private val qualityRuleBuildHisDao: QualityRuleBuildHisDao,
    private val qualityIndicatorService: QualityIndicatorService,
    private val indicatorService: QualityIndicatorService,
    private val historyDao: HistoryDao,
    private val qualityRuleBuildHisOperationDao: QualityRuleBuildHisOperationDao,
    private val dslContext: DSLContext,
    private val client: Client
) {

    private val logger = LoggerFactory.getLogger(QualityRuleBuildHisService::class.java)

    fun serviceCreate(
        userId: String,
        projectId: String,
        pipelineId: String,
        ruleRequestList: List<RuleCreateRequestV3>
    ): List<RuleCreateResponseV3> {
        checkRuleRequest(ruleRequestList)

        return ruleRequestList.map { ruleRequest ->
            logger.info("start to create rule: $projectId, $pipelineId, ${ruleRequest.name}")
            val indicatorIds = mutableListOf<RuleCreateRequest.CreateRequestIndicator>()

            ruleRequest.indicators.groupBy { it.atomCode }.forEach { (atomCode, indicators) ->
                val indicatorMap = indicators.map { it.enName to it }.toMap()
                indicatorService.serviceList(atomCode, indicators.map { it.enName }).forEach {
                    val requestIndicator = indicatorMap[it.enName]
                    checkThresholdType(requestIndicator!!, it)
                    indicatorIds.add(RuleCreateRequest.CreateRequestIndicator(
                        it.hashId,
                        requestIndicator.operation,
                        requestIndicator.threshold
                    ))
                }
            }

            logger.info("start to create rule snapshot: $projectId, $pipelineId, ${ruleRequest.name}")
            val id = qualityRuleBuildHisDao.create(dslContext, userId, projectId, pipelineId, ruleRequest, indicatorIds)

            RuleCreateResponseV3(ruleRequest.name, projectId, pipelineId, HashUtil.encodeLongId(id))
        }
    }

    @Suppress("ReturnCount")
    private fun checkThresholdType(
        requestIndicator: RuleCreateRequestV3.CreateRequestIndicator,
        indicator: QualityIndicator
    ) {
        when (indicator.thresholdType) {
            QualityDataType.INT -> {
                if (NumberUtils.isDigits(requestIndicator.threshold)) {
                    return
                }
            }
            QualityDataType.FLOAT -> {
                if (NumberUtils.isCreatable(requestIndicator.threshold)) {
                    return
                }
            }
            QualityDataType.BOOLEAN -> {
                if (requestIndicator.threshold == "true" || requestIndicator.threshold == "false") {
                    return
                }
            }
            else -> {
                if (NumberUtils.isDigits(requestIndicator.threshold)) {
                    return
                }
            }
        }
        throw OperationException("指标[${requestIndicator.enName}]值类型为[${indicator.thresholdType}]，" +
            "请修改红线阈值[${requestIndicator.threshold}]")
    }

    fun list(ruleBuildIds: Collection<Long>): List<QualityRule> {
        logger.info("start to check rule in his: $ruleBuildIds")
        val allRule = qualityRuleBuildHisDao.list(dslContext, ruleBuildIds)

        logger.info("start to check rule op list in his: ${allRule.size}")

        val allIndicatorIds = mutableSetOf<Long>()
        allRule.forEach {
            allIndicatorIds.addAll(it.indicatorIds.split(",").map { indicatorId -> indicatorId.toLong() })
        }

        logger.info("start to check rule indicator: ${allIndicatorIds.firstOrNull()}, ${allIndicatorIds.size}")
        val qualityIndicatorMap = qualityIndicatorService.serviceList(allIndicatorIds).map {
            HashUtil.decodeIdToLong(it.hashId).toString() to it
        }.toMap()
        return allRule.map {
            val thresholdList = it.indicatorThresholds.split(",")
            val opList = it.indicatorOperations.split(",")
            val ruleIndicatorIdMap = it.indicatorIds.split(",").mapIndexed { index, id ->
                id.toLong() to Pair(opList[index], thresholdList[index])
            }.toMap()

            val rule = QualityRule(
                hashId = HashUtil.encodeLongId(it.id),
                name = it.ruleName,
                desc = it.ruleDesc,
                indicators = it.indicatorIds.split(",").map INDICATOR@{ indicatorId ->
                    val indicator = qualityIndicatorMap[indicatorId]
                        ?: throw IllegalArgumentException("indicatorId not found: $indicatorId, $qualityIndicatorMap")

                    val indicatorCopy = indicator.clone()

                    val item = ruleIndicatorIdMap[indicatorId.toLong()]

                    indicatorCopy.operation = QualityOperation.valueOf(item?.first ?: indicator.operation.name)
                    indicatorCopy.threshold = item?.second ?: indicator.threshold

                    return@INDICATOR indicatorCopy
                },
                controlPoint = QualityRule.RuleControlPoint(
                    "", "", "", ControlPointPosition(ControlPointPosition.AFTER_POSITION), listOf()
                ),
                range = if (it.pipelineRange.isNullOrBlank()) {
                    listOf()
                } else {
                    it.pipelineRange.split(",")
                },
                templateRange = if (it.templateRange.isNullOrBlank()) {
                    listOf()
                } else {
                    it.templateRange.split(",")
                },
                operation = RuleOperation.END,
                notifyTypeList = null,
                notifyUserList = null,
                notifyGroupList = null,
                auditUserList = null,
                auditTimeoutMinutes = null,
                gatewayId = it.gatewayId,
                opList = if (it.operationList.isNullOrBlank()) {
                    listOf()
                } else {
                    JsonUtil.to(it.operationList, object : TypeReference<List<QualityRule.RuleOp>>() {})
                },
                status = if (!it.status.isNullOrBlank()) {
                    RuleInterceptResult.valueOf(it.status)
                } else {
                    null
                },
                gateKeepers = if (it.gateKeepers.isNullOrBlank()) {
                    listOf()
                } else {
                    it.gateKeepers.split(",")
                },
                stageId = it.stageId
            )
            rule
        }
    }

    @Suppress("NestedBlockDepth")
    private fun checkRuleRequest(ruleRequestList: List<RuleCreateRequestV3>) {
        ruleRequestList.forEach { request ->
            request.opList?.forEach { op ->
                if (op.operation == RuleOperation.END) {
                    if (op.notifyTypeList.isNullOrEmpty()) {
                        throw QualityOpConfigException("notify type is empty for operation notify")
                    }
                    if (op.notifyGroupList.isNullOrEmpty() && op.notifyUserList.isNullOrEmpty()) {
                        throw QualityOpConfigException("notifyGroupList and notifyUserList is empty for operation end")
                    }
                } else {
                    if (op.auditTimeoutMinutes == null) {
                        throw QualityOpConfigException("auditTimeoutMinutes is empty for operation audit")
                    }
                    if (op.auditUserList.isNullOrEmpty()) {
                        throw QualityOpConfigException("auditUserList is empty for operation audit")
                    }
                }

                if (request.indicators.isEmpty()) {
                    throw QualityOpConfigException("quality rule indicators is empty")
                }
            }
        }
    }

    fun updateBuildId(ruleBuildIds: Collection<Long>, buildId: String) {
        val count = qualityRuleBuildHisDao.updateBuildId(ruleBuildIds, buildId)
        logger.info("finish to update rule build his build id: $count")
    }

    fun updateStatus(ruleBuildId: Long, status: String): Int {
        val count = qualityRuleBuildHisDao.updateStatus(ruleBuildId, status)
        logger.info("finish to update rule his status: $count, $ruleBuildId, $status")
        return count
    }

    fun convertGateKeepers(ruleList: Collection<QualityRule>, buildCheckParamsV3: BuildCheckParamsV3): Int {
        var count = 0
        ruleList.forEach { it ->
            val gateKeepers = (it.gateKeepers ?: listOf()).map { user ->
                EnvUtils.parseEnv(user, buildCheckParamsV3.runtimeVariable ?: mapOf())
            }
            count = qualityRuleBuildHisDao.updateGateKeepers(HashUtil.decodeIdToLong(it.hashId),
                gateKeepers?.joinToString(","))
            logger.info("QUALITY|CONVERTGATEKEEPERS|$gateKeepers|COUNT|$count")
        }
        return count
    }

    fun updateStatusService(userId: String, ruleBuildId: Long, pass: Boolean): Boolean {
        var count = 0

        val rules = qualityRuleBuildHisDao.list(dslContext, listOf(ruleBuildId))
        logger.info("update rule for ruleId: $ruleBuildId")
        rules.forEach {
            if (it.gateKeepers != null) {
                if (it.gateKeepers!!.isEmpty() || !(it.gateKeepers!!.contains(userId))) {
                    throw ErrorCodeException(
                        statusCode = Response.Status.FORBIDDEN.statusCode,
                        errorCode = ProcessMessageCode.USER_NEED_PIPELINE_X_PERMISSION,
                        defaultMessage = "用户($userId)不在当前把关人名单中",
                        params = null
                    )
                }
                val ruleResult = if (pass) RuleInterceptResult.INTERCEPT_PASS.name
                            else RuleInterceptResult.INTERCEPT.name
                logger.info("rule $ruleBuildId update status: $ruleResult, $pass")

                if (checkReview(userId, it, pass)) {
                    count = updateStatus(ruleBuildId, ruleResult)
                    qualityRuleBuildHisOperationDao.create(dslContext, userId, ruleBuildId, it.stageId)
                }
            }
        }
        return count > 0
    }

    fun checkReview(userId: String, record: TQualityRuleBuildHisRecord, pass: Boolean): Boolean {
        val stageRules = qualityRuleBuildHisDao.listStageRules(dslContext, record.buildId, record.stageId)
        var passFlag = false
        var stageFinish = false

        if (stageRules.size == 1) {
            stageFinish = true
            passFlag = true
        } else {
            stageRules.filter { it.id != record.id }.map {
                if (it?.status != RuleInterceptResult.WAIT.name) {
                    stageFinish = true
                } else {
                    stageFinish = false
                    return@map
                }
            }
        }

        logger.info("stageFinish is $stageFinish")
        if (stageFinish) {
            stageRules.filter { it.id != record.id }.map {
                if (it?.status == RuleInterceptResult.INTERCEPT_PASS.name || it?.status == null) {
                    passFlag = true
                } else {
                    passFlag = false
                    return@map
                }
            }
            logger.info("passFlag is $passFlag. start to send stageRequest")
            val ruleHistory = historyDao.list(dslContext, record.projectId, record.pipelineId, null, null,
                null, null, null, null)
            return client.get(ServiceBuildResource::class).qualityTriggerStage(
                userId = userId,
                projectId = record.projectId,
                pipelineId = record.pipelineId,
                buildId = record.buildId,
                stageId = record.stageId,
                qualityRequest = StageQualityRequest(
                    position = record.rulePos,
                    pass = passFlag && pass,
                    checkTimes = ruleHistory.first()?.checkTimes ?: 1
                )
            ).data ?: false
        }
        return true
    }
}
