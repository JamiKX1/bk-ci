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
 *
 */

package com.tencent.devops.repository.dao

import com.tencent.devops.model.repository.tables.TRepositoryPipelineRef
import com.tencent.devops.model.repository.tables.records.TRepositoryPipelineRefRecord
import com.tencent.devops.repository.pojo.RepoPipelineRef
import com.tencent.devops.repository.pojo.RepoPipelineRefVo
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class RepoPipelineRefDao {

    fun batchAdd(
        dslContext: DSLContext,
        repoPipelineRefs: Collection<RepoPipelineRef>
    ) {
        if (repoPipelineRefs.isEmpty()) {
            return
        }
        val now = LocalDateTime.now()
        dslContext.batch(repoPipelineRefs.map {
            with(TRepositoryPipelineRef.T_REPOSITORY_PIPELINE_REF) {
                dslContext.insertInto(
                    this,
                    PROJECT_ID,
                    PIPELINE_ID,
                    PIPELINE_NAME,
                    REPOSITORY_ID,
                    TASK_ID,
                    TASK_NAME,
                    ATOM_CODE,
                    ATOM_CATEGORY,
                    CREATE_TIME,
                    UPDATE_TIME
                ).values(
                    it.projectId,
                    it.pipelineId,
                    it.pipelineName,
                    it.repositoryId,
                    it.taskId,
                    it.taskName,
                    it.atomCode,
                    it.atomCategory,
                    now,
                    now
                ).onDuplicateKeyUpdate()
                    .set(TASK_NAME, it.taskName)
                    .set(REPOSITORY_ID, it.repositoryId)
                    .set(UPDATE_TIME, now)
            }
        }).execute()
    }

    fun batchDelete(dslContext: DSLContext, ids: List<Long>) {
        if (ids.isEmpty()) {
            return
        }

        with(TRepositoryPipelineRef.T_REPOSITORY_PIPELINE_REF) {
            dslContext.deleteFrom(this)
                .where(ID.`in`(ids))
                .execute()
        }
    }


    fun listByPipeline(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String
    ): List<TRepositoryPipelineRefRecord> {
        return with(TRepositoryPipelineRef.T_REPOSITORY_PIPELINE_REF) {
            dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .fetch()
        }
    }

    fun listByRepo(
        dslContext: DSLContext,
        projectId: String,
        repositoryId: Long,
        limit: Int,
        offset: Int
    ): List<RepoPipelineRefVo> {
        return with(TRepositoryPipelineRef.T_REPOSITORY_PIPELINE_REF) {
            dslContext.select(PROJECT_ID, PIPELINE_ID, PIPELINE_NAME)
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(REPOSITORY_ID.eq(repositoryId))
                .groupBy(PROJECT_ID, PIPELINE_ID, PIPELINE_NAME)
                .orderBy(PIPELINE_NAME.desc())
                .limit(limit).offset(offset)
                .fetch {
                    RepoPipelineRefVo(
                        projectId = it.value1(),
                        pipelineId = it.value2(),
                        pipelineName = it.value3()
                    )
                }
        }
    }

    fun countByRepo(
        dslContext: DSLContext,
        projectId: String,
        repositoryId: Long
    ): Long {
        return with(TRepositoryPipelineRef.T_REPOSITORY_PIPELINE_REF) {
            dslContext.select()
                .from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(REPOSITORY_ID.eq(repositoryId))
                .groupBy(PROJECT_ID, PIPELINE_ID, PIPELINE_NAME)
                .fetchGroups(PIPELINE_ID).size.toLong()
        }
    }
}
