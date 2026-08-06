package com.mediaforge.common.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    private final RabbitProperties properties;

    public RabbitConfig(RabbitProperties properties){
        this.properties = properties;
    }

    @Bean
    public DirectExchange jobsExchange(){
        return new DirectExchange(properties.exchange());
    }

    @Bean
    public Queue thumbnailQueue(){
        return new Queue(properties.thumbnailQueue(),true);
    }

    @Bean
    public Binding thumbnailBinding(){
        return BindingBuilder
                .bind(thumbnailQueue())
                .to(jobsExchange())
                .with(properties.thumbnailRoutingKey());
    }

    @Bean
    public MessageConverter jsonMessageConverter(){
        return new JacksonJsonMessageConverter();
    }
}
