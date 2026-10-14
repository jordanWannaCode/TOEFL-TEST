package com.toeflsimulator.examination;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.*;
@Component
@EnableScheduling
public class ExpirationScheduler {
    private final ExamSessions sessions;
    public ExpirationScheduler(ExamSessions sessions) { this.sessions=sessions; }
    @Scheduled(fixedDelayString="${EXAM_EXPIRATION_SCAN_MS:1000}") public void finalizeExpiredSessions() { sessions.expireDueSessions(); }
}
