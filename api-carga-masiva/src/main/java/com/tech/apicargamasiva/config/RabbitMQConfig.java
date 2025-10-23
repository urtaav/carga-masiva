package com.tech.apicargamasiva.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class RabbitMQConfig {

    // Constantes de nombres
    public static final String EXCHANGE = "importacion.exchange";
    public static final String QUEUE = "importacion.chunk.queue";
    public static final String ROUTING_KEY = "importacion.chunk";

    // Dead Letter Queue
    public static final String DLX = "importacion.dlx";
    public static final String DLQ = "importacion.dlq";
    public static final String DLQ_ROUTING_KEY = "importacion.dlq";

    // Queue para notificaciones
    public static final String NOTIFICATION_QUEUE = "importacion.notification.queue";
    public static final String NOTIFICATION_ROUTING_KEY = "importacion.notification";

    @Value("${spring.rabbitmq.listener.simple.concurrency:5}")
    private int concurrency;

    @Value("${spring.rabbitmq.listener.simple.max-concurrency:10}")
    private int maxConcurrency;

    @Value("${spring.rabbitmq.listener.simple.prefetch:5}")
    private int prefetch;

    /**
     * Configuración del ObjectMapper para serialización JSON
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        return mapper;
    }

    /**
     * Conversor de mensajes a JSON
     */
    @Bean
    public MessageConverter messageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    /**
     * Template para enviar mensajes
     */
    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter) {

        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);

        // Confirmaciones de publicación (requiere configuración en application.yml)
        template.setMandatory(true);

        return template;
    }

    /**
     * Admin para crear colas dinámicamente
     */
    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        admin.setAutoStartup(true);
        return admin;
    }

    /**
     * Factory para listeners
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter) {

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setConcurrentConsumers(concurrency);
        factory.setMaxConcurrentConsumers(maxConcurrency);
        factory.setPrefetchCount(prefetch);

        // Reconocimiento automático
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);

        // Default requeue = true, será manejado por DLQ después de reintentos
        factory.setDefaultRequeueRejected(false);

        // Error handler
        factory.setErrorHandler(t -> {
            log.error("Error en listener de RabbitMQ: {}", t.getMessage(), t);
        });

        return factory;
    }

    // ==========================================
    // EXCHANGES
    // ==========================================

    @Bean
    public DirectExchange exchange() {
        return ExchangeBuilder
                .directExchange(EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public DirectExchange dlx() {
        return ExchangeBuilder
                .directExchange(DLX)
                .durable(true)
                .build();
    }

    // ==========================================
    // QUEUES
    // ==========================================

    /**
     * Cola principal de procesamiento con DLQ configurado
     */
    @Bean
    public Queue queue() {
        return QueueBuilder
                .durable(QUEUE)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .withArgument("x-message-ttl", 3600000) // 1 hora
                .withArgument("x-max-length", 100000) // Máximo 100k mensajes
                .build();
    }

    /**
     * Dead Letter Queue - mensajes que fallaron después de reintentos
     */
    @Bean
    public Queue dlq() {
        return QueueBuilder
                .durable(DLQ)
                .withArgument("x-message-ttl", 86400000) // 24 horas
                .build();
    }

    /**
     * Cola de notificaciones
     */
    @Bean
    public Queue notificationQueue() {
        return QueueBuilder
                .durable(NOTIFICATION_QUEUE)
                .build();
    }

    // ==========================================
    // BINDINGS
    // ==========================================

    @Bean
    public Binding binding(Queue queue, DirectExchange exchange) {
        return BindingBuilder
                .bind(queue)
                .to(exchange)
                .with(ROUTING_KEY);
    }

    @Bean
    public Binding dlqBinding(Queue dlq, DirectExchange dlx) {
        return BindingBuilder
                .bind(dlq)
                .to(dlx)
                .with(DLQ_ROUTING_KEY);
    }

    @Bean
    public Binding notificationBinding(Queue notificationQueue, DirectExchange exchange) {
        return BindingBuilder
                .bind(notificationQueue)
                .to(exchange)
                .with(NOTIFICATION_ROUTING_KEY);
    }

    /**
     * Inicializa las estructuras de RabbitMQ al arrancar
     */
    @Bean
    public org.springframework.boot.CommandLineRunner initRabbitMQ(RabbitAdmin rabbitAdmin) {
        return args -> {
            log.info("Inicializando estructuras de RabbitMQ...");

            rabbitAdmin.declareExchange(exchange());
            rabbitAdmin.declareExchange(dlx());

            rabbitAdmin.declareQueue(queue());
            rabbitAdmin.declareQueue(dlq());
            rabbitAdmin.declareQueue(notificationQueue());

            rabbitAdmin.declareBinding(binding(queue(), exchange()));
            rabbitAdmin.declareBinding(dlqBinding(dlq(), dlx()));
            rabbitAdmin.declareBinding(notificationBinding(notificationQueue(), exchange()));

            log.info("✅ Estructuras de RabbitMQ inicializadas correctamente");
            log.info("   - Exchange: {}", EXCHANGE);
            log.info("   - Queue: {}", QUEUE);
            log.info("   - DLQ: {}", DLQ);
            log.info("   - Notification Queue: {}", NOTIFICATION_QUEUE);
        };
    }
}