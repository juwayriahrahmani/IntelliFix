package com.loganalyzer.service;

import com.loganalyzer.dto.LogStatisticsResponse;
import com.loganalyzer.entity.LogEntry;
import com.loganalyzer.entity.LogEntry.LogLevel;
import com.loganalyzer.repository.LogEntryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class LogService {
    private static final Logger logger = LoggerFactory.getLogger(LogService.class);
    
    @Autowired
    private LogEntryRepository logEntryRepository;
    
    private boolean realtimeGenerationActive = false;
    
    // Sample log messages for different levels
    private final Map<LogLevel, List<String>> sampleMessages = Map.of(
        LogLevel.INFO, Arrays.asList(
            "Application started successfully",
            "User login successful",
            "Data processing completed",
            "Cache refreshed",
            "Scheduled task executed",
            "Configuration loaded",
            "Service initialized",
            "Request processed successfully"
        ),
        LogLevel.WARN, Arrays.asList(
            "High memory usage detected",
            "Slow database query detected",
            "Deprecated API usage",
            "Configuration value missing, using default",
            "Connection pool nearly full",
            "Rate limit approaching",
            "Disk space running low",
            "Cache miss rate high"
        ),
        LogLevel.ERROR, Arrays.asList(
            "Database connection failed",
            "File not found",
            "Invalid user credentials",
            "Network timeout occurred",
            "Parsing error in configuration file",
            "Service unavailable",
            "Authentication failed",
            "Data validation error"
        ),
        LogLevel.DEBUG, Arrays.asList(
            "Entering method processData()",
            "Variable value: count=42",
            "SQL query executed: SELECT * FROM users",
            "Cache hit for key: user_123",
            "HTTP request received: GET /api/users",
            "Processing item 15 of 100",
            "Validation passed for input data",
            "Method execution time: 125ms"
        ),
        LogLevel.TRACE, Arrays.asList(
            "Method entry: calculateTotal()",
            "Loop iteration: i=5",
            "Condition check: isValid=true",
            "Variable assignment: result=success",
            "Function call: validateInput()",
            "Object state: initialized",
            "Parameter received: userId=12345",
            "Return value: status=OK"
        ),
        LogLevel.FATAL, Arrays.asList(
            "Critical system failure",
            "Database corruption detected",
            "Out of memory error",
            "Security breach detected",
            "System shutdown initiated",
            "Critical service failure",
            "Data integrity compromised",
            "Emergency stop triggered"
        )
    );
    
    private final List<String> loggerNames = Arrays.asList(
        "com.loganalyzer.service.UserService",
        "com.loganalyzer.controller.AuthController",
        "com.loganalyzer.security.JwtUtils",
        "com.loganalyzer.service.LogService",
        "org.springframework.web.servlet.DispatcherServlet",
        "org.hibernate.SQL",
        "com.loganalyzer.config.SecurityConfig",
        "org.springframework.security.web.FilterChainProxy"
    );
    
    public Page<LogEntry> getAllLogs(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return logEntryRepository.findAll(pageable);
    }
    
    public Page<LogEntry> getLogsByLevel(LogLevel level, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        return logEntryRepository.findByLevel(level, pageable);
    }
    
    public List<LogEntry> getRecentLogs(int minutes) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(minutes);
        return logEntryRepository.findRecentLogs(since);
    }
    
    public Page<LogEntry> searchLogs(String query, LogLevel level, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        
        if (level != null && query != null && !query.trim().isEmpty()) {
            return logEntryRepository.searchByLevelAndMessage(level, query.trim(), pageable);
        } else if (query != null && !query.trim().isEmpty()) {
            return logEntryRepository.searchByMessage(query.trim(), pageable);
        } else if (level != null) {
            return logEntryRepository.findByLevel(level, pageable);
        } else {
            return logEntryRepository.findAll(pageable);
        }
    }
    
    public LogStatisticsResponse getLogStatistics() {
        LogStatisticsResponse response = new LogStatisticsResponse();
        
        // Get total count
        long totalLogs = logEntryRepository.count();
        response.setTotalLogs(totalLogs);
        
        // Get recent logs count (last 24 hours)
        LocalDateTime yesterday = LocalDateTime.now().minusHours(24);
        long recentCount = logEntryRepository.countLogsSince(yesterday);
        response.setRecentLogsCount(recentCount);
        
        // Get log level statistics
        List<Object[]> levelStats = logEntryRepository.getLogLevelStatistics();
        Map<String, Long> levelCounts = new HashMap<>();
        
        for (Object[] stat : levelStats) {
            LogLevel level = (LogLevel) stat[0];
            Long count = (Long) stat[1];
            levelCounts.put(level.name(), count);
            
            // Set individual counts
            switch (level) {
                case ERROR -> response.setErrorCount(count);
                case WARN -> response.setWarningCount(count);
                case INFO -> response.setInfoCount(count);
                case DEBUG -> response.setDebugCount(count);
                case TRACE -> response.setTraceCount(count);
                case FATAL -> response.setFatalCount(count);
            }
        }
        
        response.setLogLevelCounts(levelCounts);
        return response;
    }
    
    public void generateSampleLogs(int count) {
        logger.info("Generating {} sample log entries", count);
        
        for (int i = 0; i < count; i++) {
            LogEntry logEntry = createRandomLogEntry();
            logEntryRepository.save(logEntry);
        }
        
        logger.info("Successfully generated {} sample log entries", count);
    }
    
    @Async
    public void startRealtimeLogGeneration() {
        realtimeGenerationActive = true;
        logger.info("Starting real-time log generation");
        
        while (realtimeGenerationActive) {
            try {
                // Generate 1-3 logs every 5-15 seconds
                int logCount = ThreadLocalRandom.current().nextInt(1, 4);
                for (int i = 0; i < logCount; i++) {
                    LogEntry logEntry = createRandomLogEntry();
                    logEntryRepository.save(logEntry);
                }
                
                // Wait 5-15 seconds
                int waitTime = ThreadLocalRandom.current().nextInt(5000, 15000);
                Thread.sleep(waitTime);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.error("Error in real-time log generation", e);
            }
        }
        
        logger.info("Real-time log generation stopped");
    }
    
    public void stopRealtimeLogGeneration() {
        realtimeGenerationActive = false;
        logger.info("Stopping real-time log generation");
    }
    
    public boolean isRealtimeGenerationActive() {
        return realtimeGenerationActive;
    }
    
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void generatePeriodicLogs() {
        if (!realtimeGenerationActive) {
            // Generate 1-2 logs periodically when real-time generation is not active
            int count = ThreadLocalRandom.current().nextInt(1, 3);
            for (int i = 0; i < count; i++) {
                LogEntry logEntry = createRandomLogEntry();
                logEntryRepository.save(logEntry);
            }
        }
    }
    
    private LogEntry createRandomLogEntry() {
        LogLevel[] levels = LogLevel.values();
        LogLevel randomLevel = levels[ThreadLocalRandom.current().nextInt(levels.length)];
        
        // Weight the probability towards INFO and DEBUG logs
        int rand = ThreadLocalRandom.current().nextInt(100);
        if (rand < 40) randomLevel = LogLevel.INFO;
        else if (rand < 65) randomLevel = LogLevel.DEBUG;
        else if (rand < 80) randomLevel = LogLevel.WARN;
        else if (rand < 95) randomLevel = LogLevel.ERROR;
        else if (rand < 98) randomLevel = LogLevel.TRACE;
        else randomLevel = LogLevel.FATAL;
        
        List<String> messages = sampleMessages.get(randomLevel);
        String randomMessage = messages.get(ThreadLocalRandom.current().nextInt(messages.size()));
        String randomLogger = loggerNames.get(ThreadLocalRandom.current().nextInt(loggerNames.size()));
        
        LogEntry logEntry = new LogEntry(randomLevel, randomMessage, randomLogger);
        
        // Add some random variation to timestamp (within last 24 hours)
        LocalDateTime baseTime = LocalDateTime.now();
        if (ThreadLocalRandom.current().nextBoolean()) {
            baseTime = baseTime.minusMinutes(ThreadLocalRandom.current().nextInt(1440)); // Up to 24 hours ago
        }
        logEntry.setTimestamp(baseTime);
        
        // Occasionally add stack traces for ERROR and FATAL logs
        if ((randomLevel == LogLevel.ERROR || randomLevel == LogLevel.FATAL) && 
            ThreadLocalRandom.current().nextInt(100) < 30) {
            logEntry.setStackTrace(generateSampleStackTrace());
        }
        
        // Add some technical details
        logEntry.setThreadName("thread-" + ThreadLocalRandom.current().nextInt(1, 10));
        logEntry.setClassName(randomLogger);
        logEntry.setMethodName(generateRandomMethodName());
        logEntry.setLineNumber(ThreadLocalRandom.current().nextInt(1, 500));
        logEntry.setSource("application");
        
        return logEntry;
    }
    
    private String generateSampleStackTrace() {
        return """
            java.lang.RuntimeException: Sample error for demonstration
                at com.loganalyzer.service.LogService.processData(LogService.java:245)
                at com.loganalyzer.controller.LogController.handleRequest(LogController.java:89)
                at java.base/java.lang.reflect.Method.invoke(Method.java:566)
                at org.springframework.web.method.support.InvocableHandlerMethod.doInvoke(InvocableHandlerMethod.java:190)
                at org.springframework.web.servlet.mvc.method.annotation.ServletInvocableHandlerMethod.invokeAndHandle(ServletInvocableHandlerMethod.java:105)
            """;
    }
    
    private String generateRandomMethodName() {
        List<String> methodNames = Arrays.asList(
            "processData", "validateInput", "saveEntity", "loadConfiguration",
            "handleRequest", "executeQuery", "parseResponse", "initializeService",
            "cleanup", "authenticate", "authorize", "encrypt", "decrypt", "transform"
        );
        return methodNames.get(ThreadLocalRandom.current().nextInt(methodNames.size()));
    }
    
    public List<LogEntry> getRelatedLogs(Long logId) {
        LogEntry logEntry = logEntryRepository.findById(logId).orElse(null);
        if (logEntry == null) {
            return new ArrayList<>();
        }
        
        // Find logs within ±5 minutes of the given log
        LocalDateTime startTime = logEntry.getTimestamp().minusMinutes(5);
        LocalDateTime endTime = logEntry.getTimestamp().plusMinutes(5);
        
        return logEntryRepository.findRelatedLogs(startTime, endTime, logId);
    }
    
    public List<LogEntry> getLogsWithStackTrace() {
        return logEntryRepository.findLogsWithStackTrace();
    }
    
    // Initialize with some sample data
    public void initializeSampleData() {
        if (logEntryRepository.count() == 0) {
            logger.info("Initializing with sample log data");
            generateSampleLogs(50);
        }
    }
}