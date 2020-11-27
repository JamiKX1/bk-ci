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

package com.tencent.devops.gitci.pojo

import com.tencent.devops.process.pojo.pipeline.ModelDetail
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("蓝盾工蜂项目配置")
data class GitProjectPipeline(
    @ApiModelProperty("工蜂项目ID")
    val gitProjectId: Long,
    @ApiModelProperty("蓝盾项目Code")
    val projectCode: String,
    @ApiModelProperty("蓝盾流水线ID")
    val pipelineId: String,
    @ApiModelProperty("所在分支", required = true)
    val branch: String,
    @ApiModelProperty("文件路径", required = true)
    val filePath: String,
    @ApiModelProperty("流水线名称", required = true)
    val displayName: String,
    @ApiModelProperty("是否启用", required = true)
    val enabled: Boolean,
    @ApiModelProperty("创建人", required = false)
    val creator: String?,
    @ApiModelProperty("最近一次构建详情", required = false)
    val latestBuildDetail: ModelDetail?,
    @ApiModelProperty("创建时间")
    val createTime: Long?,
    @ApiModelProperty("修改时间")
    val updateTime: Long?
)
