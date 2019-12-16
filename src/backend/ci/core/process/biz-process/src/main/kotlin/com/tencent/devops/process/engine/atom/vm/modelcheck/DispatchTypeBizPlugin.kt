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

package com.tencent.devops.process.engine.atom.vm.modelcheck

import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.type.StoreDispatchType
import com.tencent.devops.common.pipeline.type.docker.ImageType
import com.tencent.devops.process.plugin.ContainerBizPlugin
import com.tencent.devops.process.plugin.annotation.ContainerBiz

/**
 * @Description
 * @Date 2019/12/15
 * @Version 1.0
 */
@ContainerBiz
class DispatchTypeBizPlugin : ContainerBizPlugin<VMBuildContainer> {
    override fun containerClass(): Class<VMBuildContainer> {
        return VMBuildContainer::class.java
    }

    override fun afterCreate(container: VMBuildContainer, projectId: String, pipelineId: String, pipelineName: String, userId: String, channelCode: ChannelCode) {
    }

    override fun beforeDelete(container: VMBuildContainer, userId: String, pipelineId: String?) {
    }

    override fun check(container: VMBuildContainer, appearedCnt: Int) {
        val dispatchType = container.dispatchType
        if (dispatchType is StoreDispatchType) {
            if (dispatchType.imageType == ImageType.BKSTORE) {
                // BKSTORE的镜像确保code与version不为空
                if (dispatchType.imageCode.isNullOrBlank()) {
                    throw IllegalArgumentException("从研发商店选择的镜像code不可为空")
                }
                if (dispatchType.imageVersion.isNullOrBlank()) {
                    throw IllegalArgumentException("从研发商店选择的镜像version不可为空")
                }
            } else {
                // 其余类型的镜像确保value不为空
                if (dispatchType.value.isBlank()) {
                    throw IllegalArgumentException("非商店蓝盾源/第三方源的镜像value不可为空")
                }
            }
        }
    }
}
