package com.mediaforge.common.messaging;

import org.springframework.amqp.core.*;
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
        return workQueue(properties.thumbnailQueue());
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
        return workQueue(properties.posterQueue());
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
        return workQueue(properties.metadataQueue());
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
        return workQueue(properties.transcodeQueue());
    }

    @Bean
    public Binding transcodeBinding() {
        return BindingBuilder.bind(transcodeQueue()).to(jobsExchange())
                .with(properties.transcodeRoutingKey());
    }

    @Bean
    public Queue previewQueue() {
        return workQueue(properties.previewQueue());
    }

    @Bean
    public Binding previewBinding() {
        return BindingBuilder.bind(previewQueue()).to(jobsExchange())
                .with(properties.previewRoutingKey());
    }

    @Bean
    public Queue waveformQueue() {
        return workQueue(properties.waveformQueue());
    }

    @Bean
    public Binding waveformBinding() {
        return BindingBuilder.bind(waveformQueue()).to(jobsExchange())
                .with(properties.waveformRoutingKey());
    }

    @Bean
    public Queue audioTranscodeQueue() {
        return workQueue(properties.audioTranscodeQueue());
    }

    @Bean
    public Binding audioTranscodeBinding() {
        return BindingBuilder.bind(audioTranscodeQueue()).to(jobsExchange())
                .with(properties.audioTranscodeRoutingKey());
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(properties.deadLetterExchange());
    }

    @Bean
    public Queue deadLetterQueue() {
        return new Queue(properties.deadLetterQueue(), true);
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with(properties.deadLetterRoutingKey());
    }

    private Queue workQueue(String name) {
        return QueueBuilder.durable(name)
                .withArgument("x-dead-letter-exchange", properties.deadLetterExchange())
                .withArgument("x-dead-letter-routing-key", properties.deadLetterRoutingKey())
                .build();
    }

    private Queue waitQueue(String name, String returnRoutingKey) {
        return QueueBuilder.durable(name)
                .withArgument("x-message-ttl", (int) properties.retryDelayMs())
                .withArgument("x-dead-letter-exchange", properties.exchange())
                .withArgument("x-dead-letter-routing-key", returnRoutingKey)
                .build();
    }

    @Bean
    public Queue thumbnailWaitQueue() {
        return waitQueue(properties.thumbnailWaitQueue(), properties.thumbnailRoutingKey());
    }

    @Bean
    public Queue posterWaitQueue() {
        return waitQueue(properties.posterWaitQueue(), properties.posterRoutingKey());
    }

    @Bean
    public Queue transcodeWaitQueue() {
        return waitQueue(properties.transcodeWaitQueue(), properties.transcodeRoutingKey());
    }

    @Bean
    public Queue previewWaitQueue() {
        return waitQueue(properties.previewWaitQueue(), properties.previewRoutingKey());
    }

    @Bean
    public Queue metadataWaitQueue() {
        return waitQueue(properties.metadataWaitQueue(), properties.metadataRoutingKey());
    }

    @Bean
    public Queue waveformWaitQueue() {
        return waitQueue(properties.waveformWaitQueue(), properties.waveformRoutingKey());
    }

    @Bean
    public Queue audioTranscodeWaitQueue() {
        return waitQueue(properties.audioTranscodeWaitQueue(), properties.audioTranscodeRoutingKey());
    }

}
