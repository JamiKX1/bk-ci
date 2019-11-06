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

package com.tencent.devops.environment.dao.thirdPartyAgent

import com.tencent.devops.common.service.utils.ByteUtils
import com.tencent.devops.model.environment.tables.TEnvironmentThirdpartyEnableProjects
import com.tencent.devops.model.environment.tables.records.TEnvironmentThirdpartyEnableProjectsRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ThirdPartyAgentEnableProjectsDao {

    fun enable(
        dslContext: DSLContext,
        projectId: String,
        enable: Boolean
    ) {
        with(TEnvironmentThirdpartyEnableProjects.T_ENVIRONMENT_THIRDPARTY_ENABLE_PROJECTS) {
            dslContext.transaction { configuration ->
                val context = DSL.using(configuration)
                val record = context.selectFrom(this)
                        .where(PROJECT_ID.eq(projectId))
                        .fetchOne()
                val now = LocalDateTime.now()
                if (record == null) {
                    context.insertInto(this,
                            PROJECT_ID,
                            ENALBE,
                            CREATED_TIME,
                            UPDATED_TIME)
                            .values(projectId,
                                    ByteUtils.bool2Byte(enable),
                                    now,
                                    now)
                            .execute()
                } else {
                    context.update(this)
                            .set(ENALBE, ByteUtils.bool2Byte(enable))
                            .set(UPDATED_TIME, now)
                            .where(PROJECT_ID.eq(projectId))
                            .execute()
                }
            }
        }
    }

    fun isEnable(dslContext: DSLContext, projectId: String): Boolean {
        with(TEnvironmentThirdpartyEnableProjects.T_ENVIRONMENT_THIRDPARTY_ENABLE_PROJECTS) {
            val record = dslContext.selectFrom(this)
                    .where(PROJECT_ID.eq(projectId))
                    .fetchOne() ?: return false
            return ByteUtils.byte2Bool(record.enalbe)
        }
    }

    fun list(dslContext: DSLContext): Result<TEnvironmentThirdpartyEnableProjectsRecord> {
        with(TEnvironmentThirdpartyEnableProjects.T_ENVIRONMENT_THIRDPARTY_ENABLE_PROJECTS) {
            return dslContext.selectFrom(this)
                    .fetch()
        }
    }
}