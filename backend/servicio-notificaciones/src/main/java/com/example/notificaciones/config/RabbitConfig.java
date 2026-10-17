package com.example.notificaciones.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;

@Configuration
public class RabbitConfig {
    public static final String EXCHANGE = "pad.eventos";
    public static final String QUEUE = "pad.notificaciones";
    public static final String DLQ = "pad.notificaciones.dlq";
    public static final String ROUTING_KEY = "notificacion.entregar";

    @Bean DirectExchange eventosExchange() { return new DirectExchange(EXCHANGE, true, false); }
    @Bean Queue notificacionesQueue() {
        return QueueBuilder.durable(QUEUE)
                .deadLetterExchange("")
                .deadLetterRoutingKey(DLQ)
                .build();
    }
    @Bean Queue notificacionesDlq() { return QueueBuilder.durable(DLQ).build(); }
    @Bean Binding notificacionesBinding(@Qualifier("notificacionesQueue") Queue notificacionesQueue,
                                        DirectExchange eventosExchange) {
        return BindingBuilder.bind(notificacionesQueue).to(eventosExchange).with(ROUTING_KEY);
    }
    @Bean Jackson2JsonMessageConverter messageConverter() { return new Jackson2JsonMessageConverter(); }
    @Bean RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, Jackson2JsonMessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        template.setMandatory(true);
        return template;
    }
}
