package com.studyagent.main;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IdleStartTest {

    @Test
    void idleStartIsNowMinusIdleMillis() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 4, 12, 1, 5);
        long idleMillis = 60_000;
        LocalDateTime expected = LocalDateTime.of(2026, 9, 4, 12, 0, 5);
        assertEquals(expected, Application.computeIdleStart(now, idleMillis));
    }

    @Test
    void idleStartWithLargerIdleTime() {
        // 已空闲 2 分钟，开始时间应回退 2 分钟
        LocalDateTime now = LocalDateTime.of(2026, 9, 4, 12, 3, 0);
        LocalDateTime expected = LocalDateTime.of(2026, 9, 4, 12, 1, 0);
        assertEquals(expected, Application.computeIdleStart(now, 120_000));
    }

    @Test
    void zeroIdleMeansNow() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 4, 12, 0, 0);
        assertEquals(now, Application.computeIdleStart(now, 0));
    }
}