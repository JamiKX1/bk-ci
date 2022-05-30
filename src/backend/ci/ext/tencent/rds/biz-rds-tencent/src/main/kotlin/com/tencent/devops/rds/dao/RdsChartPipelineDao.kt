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

package com.tencent.devops.rds.dao

import com.tencent.devops.model.rds.tables.TRdsChartPipeline
import com.tencent.devops.rds.pojo.RdsChartPipelineInfo
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class RdsChartPipelineDao {

    fun createPipeline(
        dslContext: DSLContext,
        pipeline: RdsChartPipelineInfo,
        initPipeline: Boolean? = false
    ): Int {
        with(TRdsChartPipeline.T_RDS_CHART_PIPELINE) {
            return dslContext.insertInto(
                this,
                PIPELINE_ID,
                PRODUCT_CODE,
                FILE_PATH,
                PROJECT_NAME,
                SERVICE_NAME,
                ORIGIN_YAML,
                PARSED_YAML,
                INIT_PIPELINE,
                CREATE_TIME,
                UPDATE_TIME
            ).values(
                pipeline.pipelineId,
                pipeline.productCode,
                pipeline.filePath,
                pipeline.projectName,
                pipeline.serviceName,
                pipeline.originYaml,
                pipeline.parsedYaml,
                initPipeline,
                LocalDateTime.now(),
                LocalDateTime.now()
            ).execute()
        }
    }

    fun updatePipeline(
        dslContext: DSLContext,
        pipeline: RdsChartPipelineInfo
    ) {
        with(TRdsChartPipeline.T_RDS_CHART_PIPELINE) {
            dslContext.update(this)
                .set(ORIGIN_YAML, pipeline.originYaml)
                .set(PARSED_YAML, pipeline.parsedYaml)
                .set(PROJECT_NAME, pipeline.projectName)
                .set(SERVICE_NAME, pipeline.serviceName)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(PIPELINE_ID.eq(pipeline.pipelineId))
                .and(INIT_PIPELINE.eq(false))
                .execute()
        }
    }

    fun getChartPipelines(dslContext: DSLContext, productCode: String): List<RdsChartPipelineInfo> {
        with(TRdsChartPipeline.T_RDS_CHART_PIPELINE) {
            return dslContext.selectFrom(this)
                .where(PRODUCT_CODE.eq(productCode))
                .and(INIT_PIPELINE.eq(false))
                .fetch().map {
                    RdsChartPipelineInfo(
                        pipelineId = it.pipelineId,
                        productCode = it.productCode,
                        filePath = it.filePath,
                        projectName = it.projectName,
                        serviceName = it.serviceName,
                        originYaml = it.originYaml,
                        parsedYaml = it.parsedYaml
                    )
                }
        }
    }

    fun getPipelineById(
        dslContext: DSLContext,
        pipelineId: String
    ): RdsChartPipelineInfo? {
        with(TRdsChartPipeline.T_RDS_CHART_PIPELINE) {
            val record = dslContext.selectFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .fetchAny()
            return record?.let {
                RdsChartPipelineInfo(
                    pipelineId = it.pipelineId,
                    productCode = it.productCode,
                    filePath = it.filePath,
                    projectName = it.projectName,
                    serviceName = it.serviceName,
                    originYaml = it.originYaml,
                    parsedYaml = it.parsedYaml
                )
            }
        }
    }

    fun getInitPipelines(dslContext: DSLContext, productCode: String): RdsChartPipelineInfo? {
        with(TRdsChartPipeline.T_RDS_CHART_PIPELINE) {
            val record = dslContext.selectFrom(this)
                .where(PRODUCT_CODE.eq(productCode))
                .and(INIT_PIPELINE.eq(true))
                .fetchAny()
            return record?.let {
                RdsChartPipelineInfo(
                    pipelineId = it.pipelineId,
                    productCode = it.productCode,
                    filePath = it.filePath,
                    projectName = it.projectName,
                    serviceName = it.serviceName,
                    originYaml = it.originYaml,
                    parsedYaml = it.parsedYaml
                )
            }
        }
    }

    fun getProductPipelineByService(
        dslContext: DSLContext,
        productCode: String,
        filePath: String,
        projectName: String?,
        serviceName: String?
    ): RdsChartPipelineInfo? {
        with(TRdsChartPipeline.T_RDS_CHART_PIPELINE) {
            val select = dslContext.selectFrom(this)
                .where(
                    PRODUCT_CODE.eq(productCode)
                        .and(FILE_PATH.eq(filePath))
                )
            projectName?.let { select.and(PROJECT_NAME.eq(it)) }
            serviceName?.let { select.and(SERVICE_NAME.eq(it)) }
            return select.fetchAny()?.let {
                RdsChartPipelineInfo(
                    pipelineId = it.pipelineId,
                    productCode = it.productCode,
                    filePath = it.filePath,
                    projectName = it.projectName,
                    serviceName = it.serviceName,
                    originYaml = it.originYaml,
                    parsedYaml = it.parsedYaml
                )
            }
        }
    }
}
