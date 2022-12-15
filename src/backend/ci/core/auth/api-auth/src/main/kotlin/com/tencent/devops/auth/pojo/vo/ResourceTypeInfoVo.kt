package com.tencent.devops.auth.pojo.vo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("资源类型")
data class ResourceTypeInfoVo(
    @ApiModelProperty("资源类型")
    val resourceType: String,
    @ApiModelProperty("资源类型名")
    val name: String
)
