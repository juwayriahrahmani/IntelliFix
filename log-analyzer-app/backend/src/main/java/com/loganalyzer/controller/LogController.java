package com.loganalyzer.controller;

import com.loganalyzer.dto.LogStatisticsResponse;
import com.loganalyzer.entity.LogEntry;
import com.loganalyzer.entity.LogEntry.LogLevel;
import com.loganalyzer.service.LogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/logs")
public class LogController {

    @Autowired
    private LogService logService;

    @GetMapping
    @PreAuthorize("hasRole('VIEWER') or hasRole('ANALYST') or hasRole('ADMIN')")
    public ResponseEntity<Page<LogEntry>> getAllLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "timestamp") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        Page<LogEntry> logs = logService.getAllLogs(page, size, sortBy, sortDir);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/level/{level}")
    @PreAuthorize("hasRole('VIEWER') or hasRole('ANALYST') or hasRole('ADMIN')")
    public ResponseEntity<Page<LogEntry>> getLogsByLevel(
            @PathVariable String level,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            LogLevel logLevel = LogLevel.valueOf(level.toUpperCase());
            Page<LogEntry> logs = logService.getLogsByLevel(logLevel, page, size);
            return ResponseEntity.ok(logs);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/recent/{minutes}")
    @PreAuthorize("hasRole('VIEWER') or hasRole('ANALYST') or hasRole('ADMIN')")
    public ResponseEntity<List<LogEntry>> getRecentLogs(@PathVariable int minutes) {
        List<LogEntry> logs = logService.getRecentLogs(minutes);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('VIEWER') or hasRole('ANALYST') or hasRole('ADMIN')")
    public ResponseEntity<Page<LogEntry>> searchLogs(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String level,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        LogLevel logLevel = null;
        if (level != null && !level.trim().isEmpty()) {
            try {
                logLevel = LogLevel.valueOf(level.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
        }
        
        Page<LogEntry> logs = logService.searchLogs(query, logLevel, page, size);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasRole('VIEWER') or hasRole('ANALYST') or hasRole('ADMIN')")
    public ResponseEntity<LogStatisticsResponse> getLogStatistics() {
        LogStatisticsResponse statistics = logService.getLogStatistics();
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/{id}/related")
    @PreAuthorize("hasRole('VIEWER') or hasRole('ANALYST') or hasRole('ADMIN')")
    public ResponseEntity<List<LogEntry>> getRelatedLogs(@PathVariable Long id) {
        List<LogEntry> relatedLogs = logService.getRelatedLogs(id);
        return ResponseEntity.ok(relatedLogs);
    }

    @GetMapping("/errors")
    @PreAuthorize("hasRole('VIEWER') or hasRole('ANALYST') or hasRole('ADMIN')")
    public ResponseEntity<List<LogEntry>> getLogsWithStackTrace() {
        List<LogEntry> errorLogs = logService.getLogsWithStackTrace();
        return ResponseEntity.ok(errorLogs);
    }

    @PostMapping("/generate")
    @PreAuthorize("hasRole('ANALYST') or hasRole('ADMIN')")
    public ResponseEntity<?> generateSampleLogs(@RequestParam(defaultValue = "10") int count) {
        if (count < 1 || count > 1000) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Count must be between 1 and 1000");
            return ResponseEntity.badRequest().body(response);
        }
        
        try {
            logService.generateSampleLogs(count);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Successfully generated " + count + " sample logs");
            response.put("count", count);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Failed to generate sample logs");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/generate/realtime/start")
    @PreAuthorize("hasRole('ANALYST') or hasRole('ADMIN')")
    public ResponseEntity<?> startRealtimeGeneration() {
        try {
            if (logService.isRealtimeGenerationActive()) {
                Map<String, String> response = new HashMap<>();
                response.put("message", "Real-time log generation is already active");
                return ResponseEntity.ok(response);
            }
            
            logService.startRealtimeLogGeneration();
            Map<String, String> response = new HashMap<>();
            response.put("message", "Real-time log generation started");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Failed to start real-time log generation");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/generate/realtime/stop")
    @PreAuthorize("hasRole('ANALYST') or hasRole('ADMIN')")
    public ResponseEntity<?> stopRealtimeGeneration() {
        try {
            logService.stopRealtimeLogGeneration();
            Map<String, String> response = new HashMap<>();
            response.put("message", "Real-time log generation stopped");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Failed to stop real-time log generation");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/generate/realtime/status")
    @PreAuthorize("hasRole('VIEWER') or hasRole('ANALYST') or hasRole('ADMIN')")
    public ResponseEntity<?> getRealtimeGenerationStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("active", logService.isRealtimeGenerationActive());
        response.put("message", logService.isRealtimeGenerationActive() ? 
            "Real-time log generation is active" : "Real-time log generation is inactive");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Log Service");
        response.put("totalLogs", logService.getLogStatistics().getTotalLogs());
        response.put("realtimeActive", logService.isRealtimeGenerationActive());
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/levels")
    @PreAuthorize("hasRole('VIEWER') or hasRole('ANALYST') or hasRole('ADMIN')")
    public ResponseEntity<?> getAvailableLogLevels() {
        Map<String, Object> response = new HashMap<>();
        response.put("levels", LogLevel.values());
        response.put("message", "Available log levels");
        return ResponseEntity.ok(response);
    }
}