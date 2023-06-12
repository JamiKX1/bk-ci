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

package com.tencent.devops.store.service.common.impl

import com.tencent.devops.artifactory.api.service.ServiceArtifactoryResource
import com.tencent.devops.artifactory.constant.BKREPO_DEFAULT_USER
import com.tencent.devops.artifactory.constant.BKREPO_STORE_PROJECT_ID
import com.tencent.devops.artifactory.constant.REPO_NAME_PLUGIN
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.repository.api.ServiceGitRepositoryResource
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.net.URLEncoder

@Service
class TxStoreI18nMessageServiceImpl : StoreI18nMessageServiceImpl() {

    companion object {
        private val logger = LoggerFactory.getLogger(TxStoreI18nMessageServiceImpl::class.java)
    }

    override fun getPropertiesFileStr(
        projectCode: String,
        fileDir: String,
        i18nDir: String,
        fileName: String,
        repositoryHashId: String?,
        branch: String?
    ): String? {
        return if (!repositoryHashId.isNullOrBlank()) {
            // 从工蜂拉取文件
            try {
                client.get(ServiceGitRepositoryResource::class).getFileContent(
                    repoId = repositoryHashId,
                    filePath = "$i18nDir/$fileName",
                    reversion = null,
                    branch = branch,
                    repositoryType = null
                ).data
            } catch (ignored: Throwable) {
                logger.warn("getPropertiesFileStr fileName:$fileName,branch:$branch error", ignored)
                null
            }
        } else {
            // 直接从仓库拉取文件
            val filePath =
                URLEncoder.encode("$projectCode/$fileDir/$i18nDir/$fileName", Charsets.UTF_8.name())
            return client.get(ServiceArtifactoryResource::class).getFileContent(
                userId = BKREPO_DEFAULT_USER,
                projectId = BKREPO_STORE_PROJECT_ID,
                repoName = REPO_NAME_PLUGIN,
                filePath = filePath
            ).data
        }
    }

    override fun getPropertiesFileNames(
        projectCode: String,
        fileDir: String,
        i18nDir: String,
        repositoryHashId: String?,
        branch: String?
    ): List<String>? {
        return if (!repositoryHashId.isNullOrBlank()) {
            val gitRepositoryDirItems = client.get(ServiceGitRepositoryResource::class).getGitRepositoryTreeInfo(
                userId = AUTH_HEADER_USER_ID_DEFAULT_VALUE,
                repoId = repositoryHashId,
                refName = branch,
                path = i18nDir,
                tokenType = TokenTypeEnum.PRIVATE_KEY
            ).data
            gitRepositoryDirItems?.filter { it.type != "tree" }?.map { it.name }
        } else {
            val filePath = URLEncoder.encode("$projectCode/$fileDir/$i18nDir", Charsets.UTF_8.name())
            client.get(ServiceArtifactoryResource::class).listFileNamesByPath(
                userId = BKREPO_DEFAULT_USER,
                projectId = BKREPO_STORE_PROJECT_ID,
                repoName = REPO_NAME_PLUGIN,
                filePath = filePath
            ).data
        }
    }
}
