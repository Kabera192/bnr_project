package rw.bnr.audit_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import rw.bnr.api_gateway.dto.RequestLogDto;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
@Slf4j
public class LogFileService
{
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService rotationScheduler;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    @Value("${audit.log.directory:logs}")
    private String logDirectory;

    @Value("${audit.log.filename.prefix:audit-log}")
    private String logFilePrefix;

    @Value("${audit.log.rotation.hours:48}")
    private int rotationHours;

    private BufferedWriter currentWriter;
    private Path currentLogFile;
    @Getter
    private LocalDateTime lastRotation;

    public LogFileService()
    {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.rotationScheduler = Executors.newSingleThreadScheduledExecutor();
    }

    @PostConstruct
    public void initialize() throws IOException
    {
        // Create log directory if it doesn't exist
        Path logDir = Paths.get(logDirectory);
        if (!Files.exists(logDir))
        {
            Files.createDirectories(logDir);
            log.info("Created log directory: {}", logDir.toAbsolutePath());
        }

        // Initialize the first log file
        rotateLogFile();

        // Schedule automatic rotation every specified hours
        rotationScheduler.scheduleAtFixedRate(
                this::scheduleRotation,
                rotationHours,
                rotationHours,
                TimeUnit.HOURS
        );

        log.info("LogFileService initialized with rotation every {} hours", rotationHours);
    }

    public void writeLog(RequestLogDto logDto)
    {
        lock.readLock().lock();
        try
        {
            if (currentWriter == null)
            {
                log.warn("No current writer available, skipping log entry");
                return;
            }

            String logLine = formatLogEntry(logDto);
            currentWriter.write(logLine);
            currentWriter.newLine();
            currentWriter.flush(); // Ensure immediate write

            log.debug("Written log entry: {}", logLine);

        }
        catch (IOException e)
        {
            log.error("Error writing log entry: {}", e.getMessage(), e);
        }
        finally
        {
            lock.readLock().unlock();
        }
    }

    private String formatLogEntry(RequestLogDto logDto) throws IOException
    {
        // Create a structured log entry in JSON format
        return objectMapper.writeValueAsString(logDto);
    }

    private void scheduleRotation()
    {
        try
        {
            rotateLogFile();
        }
        catch (IOException e)
        {
            log.error("Failed to rotate log file: {}", e.getMessage(), e);
        }
    }

    public void rotateLogFile() throws IOException
    {
        lock.writeLock().lock();
        try
        {
            // Close current writer if exists
            if (currentWriter != null) {
                currentWriter.flush();
                currentWriter.close();
                log.info("Closed previous log file: {}", currentLogFile);
            }

            // Generate new log file name with timestamp
            LocalDateTime now = LocalDateTime.now();
            String fileName = String.format("%s-%s.log",
                    logFilePrefix,
                    now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")));

            currentLogFile = Paths.get(logDirectory, fileName);

            // Create new writer
            currentWriter = Files.newBufferedWriter(
                    currentLogFile,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.APPEND
            );

            lastRotation = now;

            log.info("Rotated to new log file: {}", currentLogFile.toAbsolutePath());

            // Write rotation marker
            writeRotationMarker();

        }
        finally
        {
            lock.writeLock().unlock();
        }
    }

    private void writeRotationMarker()
    {
        try
        {
            String marker = String.format(
                    "=== LOG ROTATION - %s ===",
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            );
            currentWriter.write(marker);
            currentWriter.newLine();
            currentWriter.flush();
        }
        catch (IOException e)
        {
            log.error("Error writing rotation marker: {}", e.getMessage());
        }
    }

    public void forceRotation() throws IOException
    {
        log.info("Forcing log rotation...");
        rotateLogFile();
    }

    public String getCurrentLogFile()
    {
        return currentLogFile != null ? currentLogFile.toString() : "No current log file";
    }

    public long getLogsSinceRotation()
    {
        if (currentLogFile == null || !Files.exists(currentLogFile))
        {
            return 0;
        }

        try
        {
            return Files.lines(currentLogFile)
                    .filter(line -> !line.startsWith("===")) // Exclude rotation markers
                    .count();
        }
        catch (IOException e)
        {
            log.error("Error counting lines in log file: {}", e.getMessage());
            return -1;
        }
    }

    @PreDestroy
    public void cleanup()
    {
        log.info("Shutting down LogFileService...");

        // Shutdown rotation scheduler
        rotationScheduler.shutdown();
        try
        {
            if (!rotationScheduler.awaitTermination(5, TimeUnit.SECONDS))
            {
                rotationScheduler.shutdownNow();
            }
        }
        catch (InterruptedException e)
        {
            rotationScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // Close current writer
        lock.writeLock().lock();
        try
        {
            if (currentWriter != null)
            {
                currentWriter.flush();
                currentWriter.close();
                log.info("Closed log file: {}", currentLogFile);
            }
        }
        catch (IOException e)
        {
            log.error("Error closing log file: {}", e.getMessage());
        }
        finally
        {
            lock.writeLock().unlock();
        }
    }
}