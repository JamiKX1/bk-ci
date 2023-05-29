package com.tencent.devops.experience.pojo.group

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("版本体验-体验组信息-V2")
data class GroupV2(
    @ApiModelProperty("体验组HashID", required = true)
    val groupHashId: String,
    @ApiModelProperty("体验组名称", required = true)
    val name: String,
    @ApiModelProperty("内部人员")
    val innerUsers: Set<String>,
    @ApiModelProperty("外部人员")
    val outerUsers: Set<String>,
    @ApiModelProperty("描述")
    val remark: String?
)
