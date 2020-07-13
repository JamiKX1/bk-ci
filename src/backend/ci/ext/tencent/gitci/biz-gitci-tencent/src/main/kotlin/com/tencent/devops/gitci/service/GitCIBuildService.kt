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

package com.tencent.devops.gitci.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.ci.OBJECT_KIND_MANUAL
import com.tencent.devops.common.ci.OBJECT_KIND_MERGE_REQUEST
import com.tencent.devops.common.ci.OBJECT_KIND_PUSH
import com.tencent.devops.common.ci.OBJECT_KIND_TAG_PUSH
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.BuildFormPropertyType
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.enums.VMBaseOS
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.ManualTriggerElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.pipeline.type.gitci.GitCIDispatchType
import com.tencent.devops.gitci.dao.GitCISettingDao
import com.tencent.devops.gitci.dao.GitProjectPipelineDao
import com.tencent.devops.gitci.dao.GitRequestEventBuildDao
import com.tencent.devops.gitci.dao.GitCIServicesConfDao
import com.tencent.devops.common.ci.task.DockerRunDevCloudTask
import com.tencent.devops.common.ci.task.GitCiCodeRepoInput
import com.tencent.devops.common.ci.task.GitCiCodeRepoTask
import com.tencent.devops.common.ci.yaml.CIBuildYaml
import com.tencent.devops.common.ci.image.Credential
import com.tencent.devops.common.ci.image.Pool
import com.tencent.devops.common.ci.CiBuildConfig
import com.tencent.devops.common.ci.NORMAL_JOB
import com.tencent.devops.common.ci.VM_JOB
import com.tencent.devops.common.ci.image.MacOS
import com.tencent.devops.common.ci.task.ServiceJobDevCloudTask
import com.tencent.devops.common.ci.yaml.Job
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.enums.CodePullStrategy
import com.tencent.devops.common.pipeline.enums.GitPullModeType
import com.tencent.devops.common.pipeline.type.macos.MacOSDispatchType
import com.tencent.devops.gitci.client.ScmClient
import com.tencent.devops.gitci.pojo.*
import com.tencent.devops.gitci.pojo.git.GitEvent
import com.tencent.devops.gitci.pojo.git.GitMergeRequestEvent
import com.tencent.devops.gitci.pojo.git.GitPushEvent
import com.tencent.devops.gitci.pojo.git.GitTagPushEvent
import com.tencent.devops.gitci.utils.GitCIParameterUtils
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.api.service.ServicePipelineResource
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.scm.api.ServiceGitResource
import com.tencent.devops.store.api.atom.ServiceMarketAtomResource
import com.tencent.devops.store.pojo.atom.InstallAtomReq
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class GitCIBuildService @Autowired constructor(
    private val client: Client,
    private val scmClient: ScmClient,
    private val dslContext: DSLContext,
    private val gitProjectPipelineDao: GitProjectPipelineDao,
    private val gitCISettingDao: GitCISettingDao,
    private val gitRequestEventBuildDao: GitRequestEventBuildDao,
    private val gitServicesConfDao: GitCIServicesConfDao,
    private val buildConfig: BuildConfig,
    private val objectMapper: ObjectMapper,
    private val gitCIParameterUtils: GitCIParameterUtils
) {
    companion object {
        private val logger = LoggerFactory.getLogger(GitCIBuildService::class.java)
    }

    private val channelCode = ChannelCode.GIT

    fun gitCIBuild(event: GitRequestEvent, yaml: CIBuildYaml): BuildId? {
        logger.info("Git request event: $event, yaml: $yaml")

        // create or refresh pipeline
        val gitProjectConf = gitCISettingDao.getSetting(dslContext, event.gitProjectId) ?: throw OperationException("git ci projectCode not exist")
        var pipelineId = gitProjectPipelineDao.get(dslContext, gitProjectConf.gitProjectId)?.pipelineId
        if (pipelineId.isNullOrBlank()) {
            // create pipeline
            val model = createPipelineModel(event, gitProjectConf, yaml)
            pipelineId = client.get(ServicePipelineResource::class).create(event.userId, gitProjectConf.projectCode!!, model, channelCode).data!!.id
            gitProjectPipelineDao.save(dslContext, gitProjectConf.gitProjectId, gitProjectConf.projectCode!!, pipelineId)
        } else {
            // update pipeline
            var needReCreate = false
            try {
                val pipeline = client.get(ServicePipelineResource::class).get(event.userId, gitProjectConf.projectCode!!, pipelineId!!, channelCode)
                if (pipeline.isNotOk()) {
                    logger.error("get pipeline failed, msg: ${pipeline.message}")
                    needReCreate = true
                }
            } catch (e: Exception) {
                logger.error("get pipeline failed, pipelineId: $pipelineId, projectCode: ${gitProjectConf.projectCode}, error msg: ${e.message}")
                needReCreate = true
            }

            if (needReCreate) {
                val model = createPipelineModel(event, gitProjectConf, yaml)
                pipelineId = client.get(ServicePipelineResource::class).create(event.userId, gitProjectConf.projectCode!!, model, channelCode).data!!.id
                gitProjectPipelineDao.update(dslContext, gitProjectConf.gitProjectId, gitProjectConf.projectCode!!, pipelineId)
            } else {
                val model = createPipelineModel(event, gitProjectConf, yaml)
                client.get(ServicePipelineResource::class).edit(event.userId, gitProjectConf.projectCode!!, pipelineId!!, model, channelCode)
            }
        }

        logger.info("pipelineId: $pipelineId")

            // 启动构建
            val buildId = client.get(ServiceBuildResource::class).manualStartup(
            userId = event.userId,
            projectId = gitProjectConf.projectCode!!,
            pipelineId = pipelineId,
            values = mapOf(),
            channelCode = channelCode
        ).data!!.id
        gitRequestEventBuildDao.update(dslContext, event.id!!, pipelineId, buildId)
        logger.info("buildId: $buildId")

        // 推送启动构建消息,当人工触发时不推送构建消息
        if (event.objectKind != OBJECT_KIND_MANUAL) {
            scmClient.pushCommitCheck(
                event.commitId,
                event.description ?: "",
                event.mergeRequestId ?: 0L,
                buildId,
                event.userId,
                "pending",
                yaml.pipelineName ?: "",
                gitProjectConf
            )
        }

        return BuildId(buildId)
    }

    private fun createPipelineModel(event: GitRequestEvent, gitProjectConf: GitRepositoryConf, yaml: CIBuildYaml): Model {
        // 先安装插件市场的插件
        installMarketAtom(gitProjectConf, event.userId, GitCiCodeRepoTask.atomCode)
        installMarketAtom(gitProjectConf, event.userId, DockerRunDevCloudTask.atomCode)
        installMarketAtom(gitProjectConf, event.userId, ServiceJobDevCloudTask.atomCode)

        val stageList = mutableListOf<Stage>()

        // 第一个stage，触发类
        val manualTriggerElement = ManualTriggerElement("手动触发", "T-1-1-1")
        val params = createPipelineParams(gitProjectConf, yaml, event)
        val triggerContainer = TriggerContainer("0", "构建触发", listOf(manualTriggerElement), null, null, null, null, params)
        val stage1 = Stage(listOf(triggerContainer), "stage-1")
        stageList.add(stage1)

        // 第二个stage，services初始化
        addServicesStage(yaml, stageList)

        // 其他的stage
        yaml.stages!!.forEachIndexed { stageIndex, stage ->
            val containerList = mutableListOf<Container>()
            stage.stage.forEachIndexed { jobIndex, job ->
                val elementList = mutableListOf<Element>()
                // 根据job类型创建构建容器或者无构建环境容器，默认vmBuild
                if (job.job.type == null || job.job.type == VM_JOB) {
                    // 构建环境容器每个job的第一个插件都是拉代码
                    elementList.add(createGitCodeElement(event, gitProjectConf))
                    makeElementList(job, elementList, gitProjectConf, event.userId)
                    addVmBuildContainer(job, elementList, containerList, jobIndex)
                } else if (job.job.type == NORMAL_JOB) {
                    makeElementList(job, elementList, gitProjectConf, event.userId)
                    addNormalContainer(elementList, containerList)
                }
            }

            stageList.add(Stage(containerList, "stage-$stageIndex"))
        }
        return Model("git_" + gitProjectConf.gitProjectId + "_" + System.currentTimeMillis(), "", stageList, emptyList(), false, event.userId)
    }

    private fun addNormalContainer(elementList: List<Element>, containerList: MutableList<Container>) {
        containerList.add(NormalContainer(
            containerId = null,
            id = null,
            name = "无编译环境",
            elements = elementList,
            status = null,
            startEpoch = null,
            systemElapsed = null,
            elementElapsed = null,
            enableSkip = false,
            conditions = null,
            canRetry = false,
            jobControlOption = null,
            mutexGroup = null
        ))
    }

    private fun addVmBuildContainer(job: Job, elementList: List<Element>, containerList: MutableList<Container>, jobIndex: Int) {
        var osType = VMBaseOS.LINUX
        val containerPool =
            when {
                // 有container配置时优先使用
                job.job.pool?.container != null -> {
                    Pool(
                        container = job.job.pool!!.container,
                        credential = Credential(
                            user = job.job.pool!!.credential?.user ?: "",
                            password = job.job.pool!!.credential?.password ?: ""
                        ),
                        macOS = null
                    )
                }

                // 没有container配置时，优先使用macOS配置
                job.job.pool?.macOS != null -> {
                    osType = VMBaseOS.MACOS
                    Pool(
                        container = null,
                        credential = null,
                        macOS = MacOS(
                            systemVersion = job.job.pool!!.macOS?.systemVersion ?: "",
                            xcodeVersion = job.job.pool!!.macOS?.xcodeVersion ?: ""
                        )
                    )
                }

                // 假设都没有配置，使用默认镜像
                else -> {
                    Pool(buildConfig.registryImage, Credential("", ""), null)
                }
            }

        val vmContainer = VMBuildContainer(
            id = null,
            name = "Job_${jobIndex + 1} " + (job.job.name ?: ""),
            elements = elementList,
            status = null,
            startEpoch = null,
            systemElapsed = null,
            elementElapsed = null,
            baseOS = osType,
            vmNames = setOf(),
            maxQueueMinutes = 60,
            maxRunningMinutes = 900,
            buildEnv = null,
            customBuildEnv = null,
            thirdPartyAgentId = null,
            thirdPartyAgentEnvId = null,
            thirdPartyWorkspace = null,
            dockerBuildVersion = null,
            tstackAgentId = null,
            dispatchType = if (containerPool.macOS != null) {
                MacOSDispatchType(
                    macOSEvn = containerPool.macOS!!.systemVersion!! + ":" + containerPool.macOS!!.xcodeVersion!!,
                    systemVersion = containerPool.macOS!!.systemVersion!!,
                    xcodeVersion = containerPool.macOS!!.xcodeVersion!!
                )
            } else {
                GitCIDispatchType(objectMapper.writeValueAsString(containerPool))
            }
        )
        containerList.add(vmContainer)
    }

    private fun makeElementList(job: Job, elementList: MutableList<Element>, gitProjectConf: GitRepositoryConf, userId: String) {
        job.job.steps.forEach {
            val element = it.covertToElement(getCiBuildConf(buildConfig))
            elementList.add(element)
            if (element is MarketBuildAtomElement) {
                logger.info("install market atom: ${element.getAtomCode()}")
                installMarketAtom(gitProjectConf, userId, element.getAtomCode())
            }
        }
    }

    private fun installMarketAtom(gitProjectConf: GitRepositoryConf, userId: String, atomCode: String) {
        val projectCodes = ArrayList<String>()
        projectCodes.add(gitProjectConf.projectCode!!)
        try {
            client.get(ServiceMarketAtomResource::class).installAtom(
                    userId,
                    channelCode,
                    InstallAtomReq(projectCodes, atomCode))
        } catch (e: Throwable) {
            logger.error("install atom($atomCode) failed, exception:", e)
            // 可能之前安装过，继续执行不退出
        }
    }

    private fun createGitCodeElement(event: GitRequestEvent, gitProjectConf: GitRepositoryConf): Element {
        val gitToken = client.getScm(ServiceGitResource::class).getToken(gitProjectConf.gitProjectId).data!!
        logger.info("get token from scm success, gitToken: $gitToken")
        val gitCiCodeRepoInput = when (event.objectKind) {
            OBJECT_KIND_PUSH -> {
                GitCiCodeRepoInput(
                    repositoryName = gitProjectConf.name,
                    repositoryUrl = gitProjectConf.gitHttpUrl,
                    oauthToken = gitToken.accessToken,
                    localPath = null,
                    strategy = CodePullStrategy.REVERT_UPDATE,
                    pullType = GitPullModeType.COMMIT_ID,
                    refName = event.commitId
                )
            }
            OBJECT_KIND_TAG_PUSH -> {
                GitCiCodeRepoInput(
                    repositoryName = gitProjectConf.name,
                    repositoryUrl = gitProjectConf.gitHttpUrl,
                    oauthToken = gitToken.accessToken,
                    localPath = null,
                    strategy = CodePullStrategy.REVERT_UPDATE,
                    pullType = GitPullModeType.TAG,
                    refName = event.branch.removePrefix("refs/tags/")
                )
            }
            OBJECT_KIND_MERGE_REQUEST -> {
                GitCiCodeRepoInput(
                    repositoryName = gitProjectConf.name,
                    repositoryUrl = gitProjectConf.gitHttpUrl,
                    oauthToken = gitToken.accessToken,
                    localPath = null,
                    strategy = CodePullStrategy.REVERT_UPDATE,
                    pullType = GitPullModeType.BRANCH,
                    refName = "",
                    pipelineStartType = StartType.WEB_HOOK,
                    hookEventType = CodeEventType.MERGE_REQUEST.name,
                    hookSourceBranch = event.branch,
                    hookTargetBranch = event.targetBranch,
                    hookSourceUrl = gitProjectConf.gitHttpUrl,
                    hookTargetUrl = gitProjectConf.gitHttpUrl
                )
            }
            OBJECT_KIND_MANUAL -> {
                GitCiCodeRepoInput(
                    repositoryName = gitProjectConf.name,
                    repositoryUrl = gitProjectConf.gitHttpUrl,
                    oauthToken = gitToken.accessToken,
                    localPath = null,
                    strategy = CodePullStrategy.REVERT_UPDATE,
                    pullType = GitPullModeType.BRANCH,
                    refName = event.branch.removePrefix("refs/heads/")
                )
            }
            else -> {
                logger.error("event.objectKind invalid")
                null
            }
        }

        return MarketBuildAtomElement(
            name = "拉代码",
            id = null,
            status = null,
            atomCode = GitCiCodeRepoTask.atomCode,
            version = "1.*",
            data = mapOf("input" to gitCiCodeRepoInput!!)
        )
    }

    private fun addServicesStage(yaml: CIBuildYaml, stageList: MutableList<Stage>) {
        if (yaml.services == null || yaml.services!!.isEmpty()) {
            return
        }
        yaml.services!!.forEachIndexed { index, it ->
            // 判断镜像格式是否合法
            val (imageName, imageTag) = it.parseImage()
            val record = gitServicesConfDao.get(dslContext, imageName, imageTag) ?: throw RuntimeException("Git CI没有此镜像版本记录. ${it.image}")
            if (!record.enable) {
                throw RuntimeException("镜像版本不可用")
            }
            val serviceJobDevCloudInput = it.getServiceInput(record.repoUrl, record.repoUsername, record.repoPwd, record.env)

            val servicesElement = MarketBuildAtomElement(
                "创建${it.getType()}服务",
                null,
                null,
                ServiceJobDevCloudTask.atomCode,
                "1.*",
                mapOf("input" to serviceJobDevCloudInput, "namespace" to it.getServiceParamNameSpace())
            )

            val servicesContainer = NormalContainer(
                containerId = null,
                id = null,
                name = "创建${it.getType()}服务",
                elements = listOf(servicesElement),
                status = null,
                startEpoch = null,
                systemElapsed = null,
                elementElapsed = null,
                enableSkip = false,
                conditions = null,
                canRetry = true,
                jobControlOption = null,
                mutexGroup = null
            )
            val stage = Stage(listOf(servicesContainer), "stage-service-job-$index")
            stageList.add(stage)
        }
    }

    private fun createPipelineParams(gitProjectConf: GitRepositoryConf, yaml: CIBuildYaml, event: GitRequestEvent): MutableList<BuildFormProperty> {
        val result = mutableListOf<BuildFormProperty>()
        gitProjectConf.env?.forEach {
            val value = gitCIParameterUtils.encrypt(it.value)
            result.add(
                BuildFormProperty(
                    id = it.name,
                    required = false,
                    type = BuildFormPropertyType.PASSWORD,
                    defaultValue = value,
                    options = null,
                    desc = null,
                    repoHashId = null,
                    relativePath = null,
                    scmType = null,
                    containerType = null,
                    glob = null,
                    properties = null
                )
            )
        }

        val startParams = mutableMapOf<String, String>()

        // 通用参数
        startParams[CI_STATUS] = "true"
        startParams[CI_EVENT_TYPE] = event.objectKind
        startParams[CI_BRANCH] = event.branch
        startParams[CI_BUILD_USER] = event.userId
        startParams[CI_COMMIT_ID] = event.commitId
        startParams[CI_REPOSITORY_NAME] = gitProjectConf.name
        startParams[CI_BUILD_WEB_URL] = gitProjectConf.url
        startParams[CI_COMMIT_MESSAGE] = event.commitMsg.toString()

        // 写入WEBHOOK触发环境变量
        val originEvent = try {
            objectMapper.readValue<GitEvent>(event.event)
        } catch (e: Exception) {
            logger.warn("Fail to parse the git web hook commit event, errMsg: ${e.message}")
        }

        when (originEvent) {
            is GitPushEvent -> {
                startParams[CI_BUILD_USER] = originEvent.user_name
                startParams[CI_PUSH_BEFORE_COMMIT] = originEvent.before
                startParams[CI_PUSH_AFTER_COMMIT] = originEvent.after
                startParams[CI_PUSH_TOTAL_COMMIT] = originEvent.total_commits_count.toString()
                startParams[CI_PUSH_OPERATION_KIND] = originEvent.operation_kind
                startParams[CI_REF] = originEvent.ref
            }
            is GitTagPushEvent -> {
                startParams[CI_TAG_NAME] = event.branch
                startParams[CI_TAG_OPERATION] = originEvent.operation_kind ?: ""
                startParams[CI_PUSH_TOTAL_COMMIT] = originEvent.total_commits_count.toString()
                startParams[CI_TAG_USERNAME] = event.userId
                startParams[CI_REF] = originEvent.ref
            }
            is GitMergeRequestEvent -> {
                startParams[CI_MR_ACTION] = originEvent.object_attributes.action
                startParams[CI_MR_AUTHOR] = originEvent.user.username
                startParams[CI_MR_TARGET_BRANCH] = originEvent.object_attributes.target_branch
                startParams[CI_MR_SOURCE_BRANCH] = originEvent.object_attributes.source_branch
                startParams[CI_MR_TARGET_REPOSITORY] = originEvent.object_attributes.target.name
                startParams[CI_MR_SOURCE_REPOSITORY] = originEvent.object_attributes.source.name
                startParams[CI_MR_CREATE_TIME] = originEvent.object_attributes.created_at
                startParams[CI_MR_CREATE_TIME_TIMESTAMP] =
                    DateTimeUtil.zoneDateToTimestamp(originEvent.object_attributes.created_at).toString()
                startParams[CI_MR_UPDATE_TIME] = originEvent.object_attributes.updated_at
                startParams[CI_MR_UPDATE_TIME_TIMESTAMP] =
                    DateTimeUtil.zoneDateToTimestamp(originEvent.object_attributes.updated_at).toString()
                startParams[CI_MR_ID] = originEvent.object_attributes.iid.toString()
                startParams[CI_MR_TITLE] = originEvent.object_attributes.title
                startParams[CI_MR_URL] = originEvent.object_attributes.url
                startParams[CI_MR_NUMBER] = originEvent.object_attributes.id.toString()
                startParams[CI_MR_DESC] = originEvent.object_attributes.description
                startParams[CI_MR_ASSIGNEE] = originEvent.object_attributes.assignee_id.toString()
            }
        }

        // 用户自定义变量
        startParams.putAll(yaml.variables ?: mapOf())

        startParams.forEach {
            result.add(BuildFormProperty(
                id = it.key,
                required = false,
                type = BuildFormPropertyType.STRING,
                defaultValue = it.value,
                options = null,
                desc = null,
                repoHashId = null,
                relativePath = null,
                scmType = null,
                containerType = null,
                glob = null,
                properties = null
            ))
        }

        return result
    }

    private fun getCiBuildConf(buildConf: BuildConfig): CiBuildConfig {
        return CiBuildConfig(
                buildConf.codeCCSofwareClientImage,
                buildConf.codeCCSofwarePath,
                buildConf.registryHost,
                buildConf.registryUserName,
                buildConf.registryPassword,
                buildConf.registryImage,
                buildConf.cpu,
                buildConf.memory,
                buildConf.disk,
                buildConf.volume,
                buildConf.activeDeadlineSeconds,
                buildConf.devCloudAppId,
                buildConf.devCloudToken,
                buildConf.devCloudUrl
        )
    }
}
