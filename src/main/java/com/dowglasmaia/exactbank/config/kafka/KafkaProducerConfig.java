package com.dowglasmaia.exactbank.config.kafka;

import com.dowglasmaia.provider.model.MobileRechargeRequestDTO;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.io.Serializable;
import java.util.HashMap;

import static org.apache.kafka.clients.producer.ProducerConfig.*;

@RequiredArgsConstructor
@Configuration
public class KafkaProducerConfig {

    private final KafkaProperties properties;

    @Bean
    public ProducerFactory JsonProducerFactory(){
        var props = new HashMap<String, Object>();
        props.put(BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServers());
        props.put(KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        return new DefaultKafkaProducerFactory(props, new StringSerializer(), new JsonSerializer());
    }

    @Bean
    public KafkaTemplate<String, MobileRechargeRequestDTO> mobileRechargeRequesKafkaTemplate(ProducerFactory JsonProducerFactory){
        return new KafkaTemplate<>(JsonProducerFactory);
    }
}
