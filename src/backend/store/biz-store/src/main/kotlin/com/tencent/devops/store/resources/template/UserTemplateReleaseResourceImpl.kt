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
package com.tencent.devops.store.resources.template

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.template.UserTemplateReleaseResource
import com.tencent.devops.store.pojo.common.StoreProcessInfo
import com.tencent.devops.store.pojo.template.MarketTemplateRelRequest
import com.tencent.devops.store.pojo.template.MarketTemplateUpdateRequest
import com.tencent.devops.store.service.template.TemplateReleaseService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserTemplateReleaseResourceImpl @Autowired constructor(
    private val templateReleaseService: TemplateReleaseService
) : UserTemplateReleaseResource {

    override fun addMarketTemplate(
        userId: String,
        templateCode: String,
        marketTemplateRelRequest: MarketTemplateRelRequest
    ): Result<Boolean> {
        return templateReleaseService.addMarketTemplate(userId, templateCode, marketTemplateRelRequest)
    }

    override fun updateMarketTemplate(
        userId: String,
        marketTemplateUpdateRequest: MarketTemplateUpdateRequest
    ): Result<String?> {
        return templateReleaseService.updateMarketTemplate(userId, marketTemplateUpdateRequest)
    }

    override fun getProcessInfo(userId: String, templateId: String): Result<StoreProcessInfo> {
        return templateReleaseService.getProcessInfo(userId, templateId)
    }

    override fun cancelRelease(userId: String, templateId: String): Result<Boolean> {
        return templateReleaseService.cancelRelease(userId, templateId)
    }

    override fun offlineTemplate(userId: String, templateCode: String, version: String?, reason: String?): Result<Boolean> {
        return templateReleaseService.offlineTemplate(userId, templateCode, version, reason)
    }
}