package com.lufax.dashboard.util;

import org.junit.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link HashUtils}.
 * Covers: SHA-256, SHA-1, metric version computation, null/edge cases.
 */
public class HashUtilsTest {

    // ========== SHA-256 Tests ==========

    @Test
    public void testSha256_BasicInput() {
        String hash = HashUtils.sha256("hello");
        assertNotNull(hash);
        assertEquals(64, hash.length());
        // Known SHA-256 for "hello"
        assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824", hash);
    }

    @Test
    public void testSha256_EmptyString() {
        String hash = HashUtils.sha256("");
        assertNotNull(hash);
        assertEquals(64, hash.length());
        // Known SHA-256 of empty string
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", hash);
    }

    @Test
    public void testSha256_NullInput() {
        assertEquals("", HashUtils.sha256(null));
    }

    @Test
    public void testSha256_Unicode() {
        String hash = HashUtils.sha256("中文测试");
        assertNotNull(hash);
        assertEquals(64, hash.length());
    }

    @Test
    public void testSha256_DifferentInputsProduceDifferentHashes() {
        String hash1 = HashUtils.sha256("input_a");
        String hash2 = HashUtils.sha256("input_b");
        assertNotEquals(hash1, hash2);
    }

    @Test
    public void testSha256_SameInputSameHash() {
        String hash1 = HashUtils.sha256("consistent_input");
        String hash2 = HashUtils.sha256("consistent_input");
        assertEquals(hash1, hash2);
    }

    // ========== SHA-1 Tests ==========

    @Test
    public void testSha1_BasicInput() {
        String hash = HashUtils.sha1("hello");
        assertNotNull(hash);
        assertEquals(40, hash.length());
        // Known SHA-1 for "hello"
        assertEquals("aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d", hash);
    }

    @Test
    public void testSha1_NullInput() {
        assertEquals("", HashUtils.sha1(null));
    }

    @Test
    public void testSha1_EmptyString() {
        String hash = HashUtils.sha1("");
        assertNotNull(hash);
        assertEquals(40, hash.length());
    }

    // ========== Instance method tests (delegate to static) ==========

    @Test
    public void testComputeSha256_DelegatesToStatic() {
        HashUtils utils = new HashUtils();
        assertEquals(HashUtils.sha256("test"), utils.computeSha256("test"));
    }

    @Test
    public void testComputeSha1_DelegatesToStatic() {
        HashUtils utils = new HashUtils();
        assertEquals(HashUtils.sha1("test"), utils.computeSha1("test"));
    }

    // ========== computeMetricVersion Tests ==========

    @Test
    public void testComputeMetricVersion_ValidData() {
        String version = HashUtils.computeMetricVersion("{\"score\": 72.5}");
        assertNotNull(version);
        assertFalse(version.equals("unknown"));
        assertTrue(version.length() >= 8);
        // Should be first 8 chars of SHA-1 hex digest
        assertEquals(8, version.length());
    }

    @Test
    public void testComputeMetricVersion_NullData() {
        assertEquals("unknown", HashUtils.computeMetricVersion(null));
    }

    @Test
    public void testComputeMetricVersion_Consistency() {
        String v1 = HashUtils.computeMetricVersion("{\"score\": 70.0}");
        String v2 = HashUtils.computeMetricVersion("{\"score\": 70.0}");
        assertEquals(v1, v2);
    }

    @Test
    public void testComputeMetricVersion_DifferentDataDifferentVersion() {
        String v1 = HashUtils.computeMetricVersion("{\"score\": 70.0}");
        String v2 = HashUtils.computeMetricVersion("{\"score\": 71.0}");
        assertNotEquals(v1, v2);
    }

    @Test
    public void testComputeMetricVersion_EmptyObject() {
        String version = HashUtils.computeMetricVersion("{}");
        assertNotNull(version);
        assertNotEquals("unknown", version);
    }
}
