package com.orders.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

import com.orders.common.events.InventoryReservationFailedEvent;
import com.orders.common.events.InventoryReservedEvent;

@Configuration 
public class kafkaInventoryConfig {


    @Value("${spring.kafka.bootstrap-servers:localhost:29092}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String group_id;

    private Map<String, Object> baseConsumerProperties() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, group_id);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
        return props;
    }

    // --- Consumer Factory for InventoryReservedEvent ---

    @Bean
    public ConsumerFactory<String, InventoryReservedEvent> inventoryReservedConsumerFactory() {

         JacksonJsonDeserializer<InventoryReservedEvent> 
                    jsonDeserializer = new JacksonJsonDeserializer<>(InventoryReservedEvent.class);

        jsonDeserializer.addTrustedPackages("com.orders.common.events.*");
        jsonDeserializer.setUseTypeHeaders(false); // Set to false if producer doesn't send Spring Kafka type headers

        return new DefaultKafkaConsumerFactory<>(
                baseConsumerProperties(),
                new StringDeserializer(),
                jsonDeserializer
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, InventoryReservedEvent> kafkaInventoryReservedListenerContainerFactory(
            ConsumerFactory<String, InventoryReservedEvent> inventoryReservedConsumerFactory) {

        ConcurrentKafkaListenerContainerFactory<String, InventoryReservedEvent> factory = 
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(inventoryReservedConsumerFactory);
        return factory;
    }

    // --- Consumer Factory for InventoryReservationFailedEvent ---

    @Bean
    public ConsumerFactory<String, InventoryReservationFailedEvent> inventoryReservationFailedEventConsumerFactory() {

         JacksonJsonDeserializer<InventoryReservationFailedEvent> 
                    jsonDeserializer = new JacksonJsonDeserializer<>(InventoryReservationFailedEvent.class);

        jsonDeserializer.addTrustedPackages("com.orders.common.events.*");
        jsonDeserializer.setUseTypeHeaders(false); // Set to false if producer doesn't send Spring Kafka type headers

        return new DefaultKafkaConsumerFactory<>(
                baseConsumerProperties(),
                new StringDeserializer(),
                jsonDeserializer
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, InventoryReservedEvent> kafkaInventoryReservationFailedListenerContainerFactory(
            ConsumerFactory<String, InventoryReservedEvent> inventoryReservationFailedEventConsumerFactory) {

        ConcurrentKafkaListenerContainerFactory<String, InventoryReservedEvent> factory = 
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(inventoryReservationFailedEventConsumerFactory);
        return factory;
    }


}
