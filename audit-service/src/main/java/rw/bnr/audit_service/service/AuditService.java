package rw.bnr.audit_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import rw.bnr.api_gateway.dto.RequestLogDto;

@Service
@RequiredArgsConstructor
@Slf4j
@EnableKafka
public class AuditService
{
    private final LogFileService logFileService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "request-logs",
            groupId = "audit-service-group"
    )
    public void processRequestLog(
            @Payload String message,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.debug("Received message from topic: {}, partition: {}, offset: {}", topic, partition, offset);

        try {
            // Parse the JSON message into RequestLogDto
            RequestLogDto logDto = objectMapper.readValue(message, RequestLogDto.class);

            // Write to log file
            logFileService.writeLog(logDto);

            log.debug("Successfully processed log entry for path: {} from IP: {}",
                    logDto.getPath(), logDto.getIp());

        } catch (Exception e) {
            log.error("Error processing Kafka message: {}. Message: {}", e.getMessage(), message, e);

            // Optionally, you could send failed messages to a dead letter queue
            // or implement retry logic here
        }
    }

    // Health check method
    public String getHealthStatus()
    {
        try {
            String currentFile = logFileService.getCurrentLogFile();
            long logCount = logFileService.getLogsSinceRotation();

            return String.format("HEALTHY - Current log file: %s, Logs since rotation: %d",
                    currentFile, logCount);
        } catch (Exception e) {
            return String.format("UNHEALTHY - Error: %s", e.getMessage());
        }
    }
}