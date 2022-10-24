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

package com.tencent.devops.stream.trigger.mq.streamMrConflict

import com.tencent.devops.common.event.dispatcher.SampleEventDispatcher
import com.tencent.devops.stream.trigger.StreamTriggerRequestService
import com.tencent.devops.stream.trigger.actions.EventActionFactory
import com.tencent.devops.stream.trigger.actions.tgit.TGitMrActionGit
import com.tencent.devops.stream.trigger.exception.handler.StreamTriggerExceptionHandler
import com.tencent.devops.stream.trigger.parsers.MergeConflictCheck
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.stereotype.Service

@Service
class StreamMrConflictCheckListener @Autowired
constructor(
    private val mergeConflictCheck: MergeConflictCheck,
    private val streamTriggerRequestService: StreamTriggerRequestService,
    private val eventDispatcher: SampleEventDispatcher,
    private val exHandler: StreamTriggerExceptionHandler,
    private val actionFactory: EventActionFactory
) {

    fun listenGitCIRequestTriggerEvent(checkEvent: StreamMrConflictCheckEvent) {
        try {
            val action = actionFactory.loadByData(
                checkEvent.eventStr,
                checkEvent.actionCommonData,
                checkEvent.actionContext,
                checkEvent.actionSetting
            )
            if (action == null) {
                logger.warn("StreamMrConflictCheckListener|listenGitCIRequestTriggerEvent|$checkEvent")
                return
            }

            action as TGitMrActionGit
            val (isFinish, isTrigger) = with(checkEvent) {
                mergeConflictCheck.checkMrConflictByListener(
                    action = action,
                    path2PipelineExists = path2PipelineExists,
                    isEndCheck = retryTime == 1,
                    notBuildRecordId = notBuildRecordId
                )
            }
            // 未检查完成，继续进入延时队列
            if (!isFinish && checkEvent.retryTime > 0) {
                logger.warn(
                    "Retry to check gitci mr request conflict " +
                        "event [${action.data.eventCommon}|${checkEvent.retryTime}]"
                )
                checkEvent.retryTime--
                eventDispatcher.dispatch(checkEvent)
            } else {
                if (isTrigger) {
                    exHandler.handle(action) {
                        streamTriggerRequestService.matchAndTriggerPipeline(
                            action = action,
                            path2PipelineExists = checkEvent.path2PipelineExists
                        )
                    }
                }
            }
        } catch (e: Throwable) {
            logger.warn("StreamMrConflictCheckListener|listenGitCIRequestTriggerEvent|error", e)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(StreamMrConflictCheckListener::class.java)
    }
}
