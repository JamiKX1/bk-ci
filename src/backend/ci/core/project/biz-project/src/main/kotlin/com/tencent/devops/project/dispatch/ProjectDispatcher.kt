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

package com.tencent.devops.project.dispatch

import com.tencent.devops.common.event.annotation.Event
import com.tencent.devops.common.event.dispatcher.EventDispatcher
import com.tencent.devops.common.event.dispatcher.mq.MQEventDispatcher
import com.tencent.devops.common.event.pojo.pipeline.IPipelineEvent
import com.tencent.devops.common.event.pojo.pipeline.IPipelineRoutableEvent
import com.tencent.devops.project.pojo.mq.ProjectBroadCastEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.stereotype.Component
import java.lang.Exception

@Component
class ProjectDispatcher @Autowired constructor(
    private val streamBridge: StreamBridge
) : EventDispatcher<ProjectBroadCastEvent> {

    companion object {
        private val logger = LoggerFactory.getLogger(ProjectDispatcher::class.java)
    }

    override fun dispatch(vararg events: ProjectBroadCastEvent) {
        events.forEach { event ->
            try {
                val eventType = event::class.java.annotations.find { s -> s is Event } as Event
                val destination = // 根据 routeKey+后缀 实现动态变换路由Key
                    // TODO 定向路由
                    if (event is IPipelineRoutableEvent && !event.routeKeySuffix.isNullOrBlank()) {
                        eventType.destination + event.routeKeySuffix
                    } else {
                        eventType.destination
                    }
                event.sendTo(bridge = streamBridge)
            } catch (ignored: Exception) {
                logger.error("[ENGINE_MQ_SEVERE] Fail to dispatch the event($event)", ignored)
            }
        }
    }
}
