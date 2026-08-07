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

    @Bean
    public Queue posterQueue() {
        return new Queue(properties.posterQueue(), true);
    }

    @Bean
    public Binding posterBinding() {
        return BindingBuilder
                .bind(posterQueue())
                .to(jobsExchange())
                .with(properties.posterRoutingKey());
    }

    @Bean
    public Queue metadataQueue() {
        return new Queue(properties.metadataQueue(), true);
    }

    @Bean
    public Binding metadataBinding() {
        return BindingBuilder
                .bind(metadataQueue())
                .to(jobsExchange())
                .with(properties.metadataRoutingKey());
    }

    @Bean
    public Queue transcodeQueue() {
        return new Queue(properties.transcodeQueue(), true);
    }

    @Bean
    public Binding transcodeBinding() {
        return BindingBuilder.bind(transcodeQueue()).to(jobsExchange())
                .with(properties.transcodeRoutingKey());
    }

    @Bean
    public Queue previewQueue() {
        return new Queue(properties.previewQueue(), true);
    }

    @Bean
    public Binding previewBinding() {
        return BindingBuilder.bind(previewQueue()).to(jobsExchange())
                .with(properties.previewRoutingKey());
    }

    @Bean
    public Queue waveformQueue() {
        return new Queue(properties.waveformQueue(), true);
    }

    @Bean
    public Binding waveformBinding() {
        return BindingBuilder.bind(waveformQueue()).to(jobsExchange())
                .with(properties.waveformRoutingKey());
    }

    @Bean
    public Queue audioTranscodeQueue() {
        return new Queue(properties.audioTranscodeQueue(), true);
    }

    @Bean
    public Binding audioTranscodeBinding() {
        return BindingBuilder.bind(audioTranscodeQueue()).to(jobsExchange())
                .with(properties.audioTranscodeRoutingKey());
    }
}
