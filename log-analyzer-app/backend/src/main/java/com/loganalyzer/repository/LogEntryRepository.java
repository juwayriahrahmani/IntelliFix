package com.loganalyzer.repository;

import com.loganalyzer.entity.LogEntry;
import com.loganalyzer.entity.LogEntry.LogLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LogEntryRepository extends JpaRepository<LogEntry, Long> {
    
    // Find logs by level
    List<LogEntry> findByLevel(LogLevel level);
    Page<LogEntry> findByLevel(LogLevel level, Pageable pageable);
    
    // Find logs by time range
    List<LogEntry> findByTimestampBetween(LocalDateTime start, LocalDateTime end);
    Page<LogEntry> findByTimestampBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);
    
    // Find recent logs
    @Query("SELECT l FROM LogEntry l WHERE l.timestamp >= :since ORDER BY l.timestamp DESC")
    List<LogEntry> findRecentLogs(@Param("since") LocalDateTime since);
    
    @Query("SELECT l FROM LogEntry l WHERE l.timestamp >= :since ORDER BY l.timestamp DESC")
    Page<LogEntry> findRecentLogs(@Param("since") LocalDateTime since, Pageable pageable);
    
    // Search logs by message content
    @Query("SELECT l FROM LogEntry l WHERE LOWER(l.message) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY l.timestamp DESC")
    List<LogEntry> searchByMessage(@Param("query") String query);
    
    @Query("SELECT l FROM LogEntry l WHERE LOWER(l.message) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY l.timestamp DESC")
    Page<LogEntry> searchByMessage(@Param("query") String query, Pageable pageable);
    
    // Combined search with level and message
    @Query("SELECT l FROM LogEntry l WHERE l.level = :level AND LOWER(l.message) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY l.timestamp DESC")
    Page<LogEntry> searchByLevelAndMessage(@Param("level") LogLevel level, @Param("query") String query, Pageable pageable);
    
    // Find logs by logger name
    List<LogEntry> findByLoggerNameContainingIgnoreCase(String loggerName);
    
    // Statistics queries
    @Query("SELECT COUNT(l) FROM LogEntry l WHERE l.level = :level")
    long countByLevel(@Param("level") LogLevel level);
    
    @Query("SELECT l.level, COUNT(l) FROM LogEntry l GROUP BY l.level")
    List<Object[]> getLogLevelStatistics();
    
    @Query("SELECT COUNT(l) FROM LogEntry l WHERE l.timestamp >= :since")
    long countLogsSince(@Param("since") LocalDateTime since);
    
    @Query("SELECT l.level, COUNT(l) FROM LogEntry l WHERE l.timestamp >= :since GROUP BY l.level")
    List<Object[]> getLogLevelStatisticsSince(@Param("since") LocalDateTime since);
    
    // Find logs with stack traces (errors)
    @Query("SELECT l FROM LogEntry l WHERE l.stackTrace IS NOT NULL ORDER BY l.timestamp DESC")
    List<LogEntry> findLogsWithStackTrace();
    
    // Find related logs (same time window)
    @Query("SELECT l FROM LogEntry l WHERE l.timestamp BETWEEN :startTime AND :endTime AND l.id != :excludeId ORDER BY l.timestamp ASC")
    List<LogEntry> findRelatedLogs(@Param("startTime") LocalDateTime startTime, 
                                   @Param("endTime") LocalDateTime endTime, 
                                   @Param("excludeId") Long excludeId);
    
    // Get latest logs
    @Query("SELECT l FROM LogEntry l ORDER BY l.timestamp DESC")
    Page<LogEntry> findLatestLogs(Pageable pageable);
    
    // Delete old logs
    @Query("DELETE FROM LogEntry l WHERE l.timestamp < :before")
    void deleteLogsBefore(@Param("before") LocalDateTime before);
}