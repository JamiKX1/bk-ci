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

package com.tencent.devops.repository.pojo

import io.swagger.annotations.ApiModelProperty
import io.swagger.annotations.ApiParam

data class RepoPipelineRef(
    val projectId: String,
    @ApiParam("流水线ID")
    val pipelineId: String,
    @ApiModelProperty("流水线名称")
    val pipelineName: String,
    @ApiModelProperty("代码库Id")
    val repositoryId: Long,
    @ApiModelProperty("插件ID")
    val taskId: String,
    @ApiModelProperty("插件名")
    val taskName: String,
    @ApiModelProperty("插件code")
    val atomCode: String,
    @ApiModelProperty("插件版本")
    val atomVersion: String? = null,
    @ApiModelProperty("插件类别")
    val atomCategory: String,
    @ApiModelProperty("插件参数")
    val taskParams: Map<String, Any>,
    @ApiModelProperty("插件配置的代码库类型")
    val taskRepoType: String,
    @ApiModelProperty("插件配置的代码库hashId")
    val taskRepoHashId: String?,
    @ApiModelProperty("插件配置的代码库别名")
    val taskRepoRepoName: String?,
    @ApiModelProperty("触发类型")
    val triggerType: String?,
    @ApiModelProperty("事件类型")
    val eventType: String?,
    @ApiModelProperty("触发条件")
    val triggerCondition: String?,
    @ApiModelProperty("触发条件md5")
    val triggerConditionMd5: String?
)
