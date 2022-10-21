package com.tencent.devops.process.engine.pojo.event

import com.tencent.devops.common.event.annotation.Event
import com.tencent.devops.common.event.dispatcher.pipeline.mq.MQ
import com.tencent.devops.common.event.pojo.pipeline.IPipelineEvent
import com.tencent.devops.common.stream.enums.ActionType

/**
 * 流水线开启stream事件
 *
 * @version 1.0
 */
@Event(MQ.QUEUE_PIPELINE_STREAM_ENABLED)
data class PipelineStreamEnabledEvent(
    override val source: String,
    override val projectId: String,
    override val pipelineId: String,
    override val userId: String,
    override var actionType: ActionType = ActionType.START,
    override var delayMills: Int = 0,
    val gitProjectId: Long,
    val gitProjectUrl: String,
    val enable: Boolean
) : IPipelineEvent(actionType, source, projectId, pipelineId, userId, delayMills)
