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

package com.tencent.devops.store.resources.template

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.store.api.template.ServiceTemplateResource
import com.tencent.devops.store.pojo.template.InstallTemplateReq
import com.tencent.devops.store.pojo.template.MarketTemplateResp
import com.tencent.devops.store.service.template.MarketTemplateService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceTemplateResourceImpl @Autowired constructor(
    private val marketTemplateService: MarketTemplateService
) : ServiceTemplateResource {
    override fun installTemplate(userId: String, installTemplateReq: InstallTemplateReq): Result<Boolean> {
        // 可见与可安装鉴权在marketTemplateService中实现
        return marketTemplateService.installTemplate(
            userId = userId,
            channelCode = ChannelCode.BS,
            installTemplateReq = installTemplateReq
        )
    }

    override fun list(userId: String): Result<MarketTemplateResp> {
        return Result(
            marketTemplateService.list(
                userId = userId.trim(),
                keyword = null,
                classifyCode = null,
                category = null,
                labelCode = null,
                score = null,
                rdType = null,
                sortType = null,
                projectCode = null,
                page = null,
                pageSize = 1
            )
        )
    }

    override fun validateUserTemplateComponentVisibleDept(
        userId: String,
        templateCode: String,
        projectCode: String
    ): Result<Boolean> {
        return marketTemplateService.validateUserTemplateComponentVisibleDept(
            userId = userId,
            templateCode = templateCode,
            projectCodeList = arrayListOf(projectCode)
        )
    }

    override fun validateModelComponentVisibleDept(
        userId: String,
        stageList: List<Stage>,
        projectCode: String
    ): Result<Boolean> {
        return marketTemplateService.verificationModelComponentVisibleDept(userId, stageList, projectCode)
    }
}
