package rw.bnr.api_gateway.config;

import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@Slf4j
public class KafkaConfig
{
    @Bean
    public ProducerFactory<String, String> producerFactory()
    {
        log.info("Creating producer factory");

        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
//        configProps.put(ProducerConfig.RETRIES_CONFIG, retries);
//        configProps.put(ProducerConfig.ACKS_CONFIG, acks);
//        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, batchSize);
//        configProps.put(ProducerConfig.LINGER_MS_CONFIG, lingerMs);
//        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, bufferMemory);

        log.info("Producer factory should be created");
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate()
    {
        return new KafkaTemplate<>(producerFactory());
    }
}