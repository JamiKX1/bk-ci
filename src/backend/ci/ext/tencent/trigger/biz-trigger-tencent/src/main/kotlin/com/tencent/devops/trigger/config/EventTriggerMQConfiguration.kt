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

package com.tencent.devops.trigger.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.trigger.constant.MQ
import com.tencent.devops.trigger.listener.EventWebhookRequestListener
import com.tencent.devops.trigger.listener.EventTargetRunListener
import com.tencent.devops.common.event.dispatcher.pipeline.mq.Tools
import com.tencent.devops.trigger.listener.EventAppWebhookRequestListener
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.DirectExchange
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class EventTriggerMQConfiguration {

    @Bean
    fun rabbitAdmin(connectionFactory: ConnectionFactory): RabbitAdmin {
        return RabbitAdmin(connectionFactory)
    }

    @Bean
    fun messageConverter(objectMapper: ObjectMapper) = Jackson2JsonMessageConverter(objectMapper)

    @Bean
    fun eventTriggerExchange(): DirectExchange {
        val directExchange = DirectExchange(MQ.EXCHANGE_EVENT_TRIGGER_LISTENER_DIRECT, true, false)
        directExchange.isDelayed = true
        return directExchange
    }

    @Bean
    fun eventWebhookRequestQueue() = Queue(MQ.QUEUE_EVENT_WEBHOOK_REQUEST)

    @Bean
    fun eventWebhookRequestQueueBind(
        @Autowired eventWebhookRequestQueue: Queue,
        @Autowired eventTriggerExchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(eventWebhookRequestQueue).to(eventTriggerExchange)
            .with(MQ.ROUTE_EVENT_WEBHOOK_REQUEST)
    }

    @Bean
    fun eventWebhookRequestListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired eventWebhookRequestQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired eventWebhookRequestListener: EventWebhookRequestListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val adapter = MessageListenerAdapter(
            eventWebhookRequestListener,
            eventWebhookRequestListener::execute.name
        )
        adapter.setMessageConverter(messageConverter)
        return Tools.createSimpleMessageListenerContainerByAdapter(
            connectionFactory = connectionFactory,
            queue = eventWebhookRequestQueue,
            rabbitAdmin = rabbitAdmin,
            startConsumerMinInterval = 10000,
            consecutiveActiveTrigger = 5,
            concurrency = 10,
            maxConcurrency = 50,
            adapter = adapter
        )
    }

    @Bean
    fun eventAppWebhookRequestQueue() = Queue(MQ.QUEUE_EVENT_APP_WEBHOOK_REQUEST)

    @Bean
    fun eventAppWebhookRequestQueueBind(
        @Autowired eventAppWebhookRequestQueue: Queue,
        @Autowired eventTriggerExchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(eventAppWebhookRequestQueue).to(eventTriggerExchange)
            .with(MQ.ROUTE_EVENT_APP_WEBHOOK_REQUEST)
    }

    @Bean
    fun eventAppWebhookRequestListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired eventAppWebhookRequestQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired eventAppWebhookRequestListener: EventAppWebhookRequestListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val adapter = MessageListenerAdapter(
            eventAppWebhookRequestListener,
            eventAppWebhookRequestListener::execute.name
        )
        adapter.setMessageConverter(messageConverter)
        return Tools.createSimpleMessageListenerContainerByAdapter(
            connectionFactory = connectionFactory,
            queue = eventAppWebhookRequestQueue,
            rabbitAdmin = rabbitAdmin,
            startConsumerMinInterval = 10000,
            consecutiveActiveTrigger = 5,
            concurrency = 10,
            maxConcurrency = 50,
            adapter = adapter
        )
    }

    @Bean
    fun eventTargetRunQueue() = Queue(MQ.QUEUE_EVENT_TARGET_RUN)

    @Bean
    fun eventTargetRunQueueBind(
        @Autowired eventTargetRunQueue: Queue,
        @Autowired eventTriggerExchange: DirectExchange
    ): Binding {
        return BindingBuilder.bind(eventTargetRunQueue).to(eventTriggerExchange)
            .with(MQ.ROUTE_EVENT_TARGET_RUN)
    }

    @Bean
    fun eventTargetRunListenerContainer(
        @Autowired connectionFactory: ConnectionFactory,
        @Autowired eventTargetRunQueue: Queue,
        @Autowired rabbitAdmin: RabbitAdmin,
        @Autowired eventTargetRunListener: EventTargetRunListener,
        @Autowired messageConverter: Jackson2JsonMessageConverter
    ): SimpleMessageListenerContainer {
        val adapter = MessageListenerAdapter(
            eventTargetRunListener,
            eventTargetRunListener::execute.name
        )
        adapter.setMessageConverter(messageConverter)
        return Tools.createSimpleMessageListenerContainerByAdapter(
            connectionFactory = connectionFactory,
            queue = eventTargetRunQueue,
            rabbitAdmin = rabbitAdmin,
            startConsumerMinInterval = 10000,
            consecutiveActiveTrigger = 5,
            concurrency = 10,
            maxConcurrency = 50,
            adapter = adapter
        )
    }
}
