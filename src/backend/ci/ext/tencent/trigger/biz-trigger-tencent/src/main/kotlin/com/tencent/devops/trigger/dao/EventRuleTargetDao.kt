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

package com.tencent.devops.trigger.dao

import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.model.trigger.tables.TEventRuleTarget
import com.tencent.devops.model.trigger.tables.records.TEventRuleTargetRecord
import com.tencent.devops.trigger.pojo.EventRuleTarget
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class EventRuleTargetDao {

    fun create(dslContext: DSLContext, eventRuleTarget: EventRuleTarget) {
        val now = LocalDateTime.now()
        with(TEventRuleTarget.T_EVENT_RULE_TARGET) {
            dslContext.insertInto(
                this,
                TARGET_ID,
                PROJECT_ID,
                BUS_ID,
                RULE_ID,
                TARGET_NAME,
                PUSH_RETRY_STRATEGY,
                TARGET_PARAMS,
                DESC,
                CREATE_TIME,
                CREATOR,
                UPDATE_TIME,
                UPDATER
            ).values(
                eventRuleTarget.targetId,
                eventRuleTarget.projectId,
                eventRuleTarget.busId,
                eventRuleTarget.ruleId,
                eventRuleTarget.targetName,
                eventRuleTarget.pushRetryStrategy,
                eventRuleTarget.targetParams,
                eventRuleTarget.desc,
                now,
                eventRuleTarget.creator,
                now,
                eventRuleTarget.updater
            ).onDuplicateKeyUpdate()
                .set(TARGET_NAME,  eventRuleTarget.targetName)
                .set(PUSH_RETRY_STRATEGY, eventRuleTarget.pushRetryStrategy)
                .set(TARGET_PARAMS, eventRuleTarget.targetParams)
                .set(UPDATE_TIME, now)
                .set(UPDATER, eventRuleTarget.updater)
                .execute()
        }
    }

    fun batchCreate(dslContext: DSLContext, eventRuleTargets: List<EventRuleTarget>) {
        eventRuleTargets.forEach { eventRuleTarget ->
            create(
                dslContext = dslContext,
                eventRuleTarget = eventRuleTarget
            )
        }
    }

    fun listByRuleId(
        dslContext: DSLContext,
        projectId: String,
        ruleId: String
    ): List<EventRuleTarget> {
        return with(TEventRuleTarget.T_EVENT_RULE_TARGET) {
            dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(RULE_ID.eq(ruleId))
                .fetch()
        }.map { convert(it) }
    }

    fun getByTargetName(
        dslContext: DSLContext,
        projectId: String,
        ruleId: String,
        targetName: String
    ): EventRuleTarget? {
        val record = with(TEventRuleTarget.T_EVENT_RULE_TARGET) {
            dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(RULE_ID.eq(ruleId))
                .and(TARGET_NAME.eq(targetName))
                .fetchAny()
        } ?: return null
        return convert(record)
    }

    fun convert(record: TEventRuleTargetRecord): EventRuleTarget {
        return with(record) {
            EventRuleTarget(
                targetId = targetId,
                ruleId = ruleId,
                projectId = projectId,
                busId = busId,
                targetName = targetName,
                pushRetryStrategy = pushRetryStrategy,
                targetParams = targetParams,
                desc = desc,
                createTime = createTime.timestampmilli(),
                creator = creator,
                updateTime = updateTime.timestampmilli(),
                updater = updater
            )
        }
    }

    fun deleteByBusId(dslContext: DSLContext, busId: String, projectId: String) {
        with(TEventRuleTarget.T_EVENT_RULE_TARGET) {
            dslContext.deleteFrom(this)
                .where(BUS_ID.eq(busId))
                .and(PROJECT_ID.eq(projectId))
                .execute()
        }
    }
}