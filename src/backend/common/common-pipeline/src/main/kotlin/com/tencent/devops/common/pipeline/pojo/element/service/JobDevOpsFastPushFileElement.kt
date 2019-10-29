package com.tencent.devops.common.pipeline.pojo.element.service

import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("作业平台-构件分发", description = JobDevOpsFastPushFileElement.classType)
data class JobDevOpsFastPushFileElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "JOB快速执行脚本",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("源类型", required = false)
    val srcType: String = "",
    @ApiModelProperty("源路径", required = false)
    val srcPath: String = "",
    @ApiModelProperty("源节点id", required = false)
    val srcNodeId: String = "",
    @ApiModelProperty("源服务器账户", required = false)
    val srcAccount: String = "",
    @ApiModelProperty("目标路径", required = false)
    val targetPath: String = "",
    @ApiModelProperty("目标账户", required = false)
    val targetAccount: String = "",
    @ApiModelProperty("目标节点id列表", required = false)
    val targetNodeId: List<String>?,
    @ApiModelProperty("目标环境id列表", required = false)
    val targetEnvId: List<String>?,
    @ApiModelProperty("目标环境名称列表", required = false)
    val targetEnvName: List<String>?,
    @ApiModelProperty("目标环境类型", required = false)
    val targetEnvType: String = "",
    @ApiModelProperty("超时时间", required = true)
    val timeout: Int? = 600
) : Element(name, id, status) {
    companion object {
        const val classType = "jobDevOpsFastPushFile"
    }

    override fun getTaskAtom(): String {
        return "jobDevOpsFastPushFileTaskAtom"
    }

    override fun getClassType() = classType
}
