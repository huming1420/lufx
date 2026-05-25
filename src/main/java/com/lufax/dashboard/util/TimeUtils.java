package com.lufax.dashboard.util;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Component
public class TimeUtils {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    public static String nowIsoUtc() {
        return Instant.now().atOffset(ZoneOffset.UTC).format(ISO_FORMATTER);
    }

    public static String getCurrentTimeIso8601() {
        return nowIsoUtc();
    }

    public static long secondsBetween(String isoTime1, String isoTime2) {
        Instant t1 = parseIso(isoTime1);
        Instant t2 = parseIso(isoTime2);
        if (t1 == null || t2 == null) {
            return 0;
        }
        return Math.abs(java.time.Duration.between(t1, t2).getSeconds());
    }

    public static long secondsSince(String isoTime) {
        if (isoTime == null || isoTime.trim().isEmpty()) {
            return 0;
        }
        Instant past = parseIso(isoTime);
        if (past == null) {
            return 0;
        }
        return java.time.Duration.between(past, Instant.now()).getSeconds();
    }

    public static String formatIso(Instant instant) {
        if (instant == null) {
            return null;
        }
        return instant.atOffset(ZoneOffset.UTC).format(ISO_FORMATTER);
    }

    private static Instant parseIso(String isoTime) {
        if (isoTime == null || isoTime.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(isoTime, ISO_FORMATTER).toInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException e) {
            try {
                return Instant.parse(isoTime);
            } catch (DateTimeParseException ex) {
                return null;
            }
        }
    }
}