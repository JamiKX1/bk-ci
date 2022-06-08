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

import com.tencent.devops.trigger.pojo.EventBusSource
import com.tencent.devops.model.trigger.tables.TEventBusSource
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class EventBusSourceDao {

    fun create(dslContext: DSLContext, eventBusSource: EventBusSource) {
        val now = LocalDateTime.now()
        with(TEventBusSource.T_EVENT_BUS_SOURCE) {
            dslContext.insertInto(
                this,
                BUS_ID,
                PROJECT_ID,
                NAME,
                DESC,
                CREATE_TIME,
                CREATOR,
                UPDATE_TIME,
                UPDATER
            )
                .values(
                    eventBusSource.busId,
                    eventBusSource.projectId,
                    eventBusSource.name,
                    eventBusSource.desc,
                    now,
                    eventBusSource.creator,
                    now,
                    eventBusSource.updater
                ).onDuplicateKeyIgnore()
                .execute()
        }
    }

    fun batchCreate(dslContext: DSLContext, eventBusSources: List<EventBusSource>) {
        eventBusSources.forEach { eventBusSource ->
            create(dslContext = dslContext, eventBusSource)
        }
    }

    fun deleteByBusId(dslContext: DSLContext, busId: String, projectId: String) {
        with(TEventBusSource.T_EVENT_BUS_SOURCE) {
            dslContext.deleteFrom(this)
                .where(BUS_ID.eq(busId))
                .and(PROJECT_ID.eq(projectId))
                .execute()
        }
    }
}