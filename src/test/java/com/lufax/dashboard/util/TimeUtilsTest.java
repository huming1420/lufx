package com.lufax.dashboard.util;

import org.junit.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.Assert.*;

/**
 * TimeUtils 单元测试
 * 覆盖: ISO 时间格式化/解析、时间差计算、null/空值边界
 */
public class TimeUtilsTest {

    private static final String TEST_ISO_TIME = "2025-06-15T10:30:00Z";
    private static final String TEST_ISO_TIME_2 = "2025-06-15T10:35:30Z";

    // ========== nowIsoUtc / getCurrentTimeIso8601 ==========

    @Test
    public void testNowIsoUtc_ReturnsNonEmpty() {
        String result = TimeUtils.nowIsoUtc();
        assertNotNull("当前时间不应为 null", result);
        assertFalse("当前时间不应为空", result.isEmpty());
    }

    @Test
    public void testNowIsoUtc_ContainsTAndZ() {
        String result = TimeUtils.nowIsoUtc();
        assertTrue("ISO 格式应包含 T 分隔符", result.contains("T"));
        assertTrue("UTC 时间应以 Z 结尾或包含偏移量",
                result.endsWith("Z") || result.contains("+") || result.contains("-"));
    }

    @Test
    public void testGetCurrentTimeIso8601_SameAsNowIsoUtc() {
        String a = TimeUtils.nowIsoUtc();
        String b = TimeUtils.getCurrentTimeIso8601();
        // 两个方法委托到同一实现，时间戳差异应在 1 秒内
        long diff = Math.abs(TimeUtils.secondsBetween(a, b));
        assertTrue("两方法结果差异应 < 2秒", diff < 2);
    }

    // ========== secondsBetween ==========

    @Test
    public void testSecondsBetween_KnownValues() {
        // 2025-06-15T10:30:00Z 和 2025-06-15T10:35:30Z 差 330 秒
        long diff = TimeUtils.secondsBetween(TEST_ISO_TIME, TEST_ISO_TIME_2);
        assertEquals("5分30秒 = 330秒", 330L, diff);
    }

    @Test
    public void testSecondsBetween_SameTime_ReturnsZero() {
        long diff = TimeUtils.secondsBetween(TEST_ISO_TIME, TEST_ISO_TIME);
        assertEquals("相同时间差为 0", 0L, diff);
    }

    @Test
    public void testSecondsBetween_OrderIndependent() {
        long ab = TimeUtils.secondsBetween(TEST_ISO_TIME, TEST_ISO_TIME_2);
        long ba = TimeUtils.secondsBetween(TEST_ISO_TIME_2, TEST_ISO_TIME);
        assertEquals("时间差与顺序无关", ab, ba);
    }

    @Test
    public void testSecondsBetween_NullInput_ReturnsZero() {
        assertEquals("第一个参数为 null 返回 0",
                0L, TimeUtils.secondsBetween(null, TEST_ISO_TIME));
        assertEquals("第二个参数为 null 返回 0",
                0L, TimeUtils.secondsBetween(TEST_ISO_TIME, null));
        assertEquals("两个都为 null 返回 0",
                0L, TimeUtils.secondsBetween(null, null));
    }

    @Test
    public void testSecondsBetween_EmptyString_ReturnsZero() {
        assertEquals(0L, TimeUtils.secondsBetween("", TEST_ISO_TIME));
        assertEquals(0L, TimeUtils.secondsBetween(TEST_ISO_TIME, "   "));
    }

    @Test
    public void testSecondsBetween_InvalidFormat_ReturnsZero() {
        assertEquals("非法格式返回 0",
                0L, TimeUtils.secondsBetween("not-a-date", TEST_ISO_TIME));
    }

    // ========== secondsSince ==========

    @Test
    public void testSecondsSince_PastTime_ReturnsPositive() {
        // 用一个过去的时间点
        String past = Instant.now().minus(60, ChronoUnit.SECONDS)
                .toString().replace("+00:00", "Z");
        long since = TimeUtils.secondsSince(past);
        assertTrue("过去的时间应返回正值", since >= 59 && since <= 65); // 允许误差
    }

    @Test
    public void testSecondsSince_NullInput_ReturnsZero() {
        assertEquals(0L, TimeUtils.secondsSince(null));
    }

    @Test
    public void testSecondsSince_EmptyInput_ReturnsZero() {
        assertEquals(0L, TimeUtils.secondsSince(""));
        assertEquals(0L, TimeUtils.secondsSince("   "));
    }

    // ========== formatIso ==========

    @Test
    public void testFormatIso_ValidInstant() {
        Instant instant = Instant.parse(TEST_ISO_TIME);
        String formatted = TimeUtils.formatIso(instant);
        assertNotNull(formatted);
        assertTrue("格式化结果应包含 T", formatted.contains("T"));
    }

    @Test
    public void testFormatIso_NullInput_ReturnsNull() {
        assertNull(TimeUtils.formatIso(null));
    }

    @Test
    public void testFormatIso_RoundTripConsistency() {
        // formatIso(parse) 应保持一致
        String original = "2025-03-21T12:00:00Z";
        Instant instant = Instant.parse(original);
        String formatted = TimeUtils.formatIso(instant);
        assertNotNull(formatted);
        assertTrue("往返转换后时间应一致或等效",
                formatted.startsWith("2025-03-21T12:00"));
    }

    // ========== parseIso 内部逻辑 (通过公开方法间接测试) ==========

    @Test
    public void testParseIso_IsoFormatWithOffset() {
        // 带偏移量的格式
        long diff = TimeUtils.secondsBetween(
                "2025-01-01T00:00:00+08:00",
                "2025-01-01T00:00:08+08:00"
        );
        assertEquals("带偏移量的时间差计算正确", 8L, diff);
    }

    @Test
    public void testParseiso_InvalidFormat_SecondsBetweenZero() {
        long diff = TimeUtils.secondsBetween("abc", "def");
        assertEquals("完全非法的日期字符串返回 0", 0L, diff);
    }
}
