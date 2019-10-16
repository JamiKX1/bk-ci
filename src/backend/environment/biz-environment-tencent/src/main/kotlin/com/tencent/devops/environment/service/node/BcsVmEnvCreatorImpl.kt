package com.tencent.devops.environment.service.node

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.misc.client.BcsClient
import com.tencent.devops.environment.dao.EnvDao
import com.tencent.devops.environment.dao.EnvNodeDao
import com.tencent.devops.environment.dao.NodeDao
import com.tencent.devops.environment.dao.ProjectConfigDao
import com.tencent.devops.environment.permission.EnvironmentPermissionService
import com.tencent.devops.environment.pojo.EnvCreateInfo
import com.tencent.devops.environment.pojo.EnvironmentId
import com.tencent.devops.environment.pojo.enums.NodeSource
import com.tencent.devops.environment.pojo.enums.NodeType
import com.tencent.devops.environment.utils.BcsVmParamCheckUtils
import com.tencent.devops.environment.utils.NodeStringIdUtils
import com.tencent.devops.model.environment.tables.records.TEnvNodeRecord
import com.tencent.devops.model.environment.tables.records.TNodeRecord
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class BcsVmEnvCreatorImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val envDao: EnvDao,
    private val nodeDao: NodeDao,
    private val envNodeDao: EnvNodeDao,
    private val projectConfigDao: ProjectConfigDao,
    private val bcsClient: BcsClient,
    private val environmentPermissionService: EnvironmentPermissionService
) : EnvCreator {

    override fun id(): String {
        return NodeSource.CREATE.name
    }

    override fun createEnv(projectId: String, userId: String, envCreateInfo: EnvCreateInfo): EnvironmentId {

        val now = LocalDateTime.now()

        // 创建 BCSVM 节点
        val vmCreateInfoP = BcsVmParamCheckUtils.checkAndGetVmCreateParam(
            dslContext = dslContext,
            projectConfigDao = projectConfigDao,
            nodeDao = nodeDao,
            projectId = projectId,
            userId = userId,
            vmParam = envCreateInfo.bcsVmParam!!
        )
        val bcsVmList = bcsClient.createVM(
            clusterId = envCreateInfo.bcsVmParam!!.clusterId,
            namespace = projectId,
            instanceCount = envCreateInfo.bcsVmParam!!.instanceCount,
            image = vmCreateInfoP.first,
            resCpu = vmCreateInfoP.second,
            resMemory = vmCreateInfoP.third
        )
        val nodeList = bcsVmList.map {
            TNodeRecord(
                null,
                "",
                projectId,
                it.ip,
                it.name,
                it.status,
                NodeType.BCSVM.name,
                it.clusterId,
                projectId,
                userId,
                now,
                now.plusDays(envCreateInfo.bcsVmParam!!.validity.toLong()),
                it.osName,
                null,
                null,
                false,
                "",
                "",
                null,
                now,
                userId
            )
        }

        var envId = 0L
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)

            nodeDao.batchAddNode(context, nodeList)
            val insertedNodeList = nodeDao.listServerNodesByIps(
                dslContext = context,
                projectId = projectId,
                ips = bcsVmList.map { it.ip })

            insertedNodeList.forEach {
                environmentPermissionService.createNode(
                    userId = userId,
                    projectId = projectId,
                    nodeId = it.nodeId,
                    nodeName = "${NodeStringIdUtils.getNodeStringId(it)}(${it.nodeIp})"
                )
            }

            envId = envDao.create(
                dslContext = context,
                userId = userId,
                projectId = projectId,
                envName = envCreateInfo.name,
                envDesc = envCreateInfo.desc,
                envType = envCreateInfo.envType.name,
                envVars = ObjectMapper().writeValueAsString(envCreateInfo.envVars)
            )
            envNodeDao.batchStoreEnvNode(
                dslContext = context,
                envNodeList = insertedNodeList.map { TEnvNodeRecord(envId, it.nodeId, projectId) })

            environmentPermissionService.createEnv(
                userId = userId,
                projectId = projectId,
                envId = envId,
                envName = envCreateInfo.name
            )
        }

        return EnvironmentId(HashUtil.encodeLongId(envId))
    }
}