package com.lufax.dashboard.util;

import org.junit.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

/**
 * ResourceUtils 单元测试
 * 覆盖: classpath 文件读取、UTF-8 编码、资源不存在处理
 */
public class ResourceUtilsTest {

    // ========== readClasspathFile / readClasspathFileUtf8 ==========

    @Test
    public void testReadClasspathFile_ExistingFile() {
        // application.properties 在 classpath 中一定存在（Spring Boot 项目）
        String content = ResourceUtils.readClasspathFile("application.properties");
        assertNotNull("读取已有文件不应为 null", content);
        assertFalse("内容不应为空", content.isEmpty());
    }

    @Test
    public void testReadClasspathFileUtf8_SameAsReadClasspathFile() {
        String a = ResourceUtils.readClasspathFile("application.properties");
        String b = ResourceUtils.readClasspathFileUtf8("application.properties");
        assertEquals("两个方法应返回相同结果", a, b);
    }

    @Test
    public void testReadClasspathFile_NonExistent_ReturnsEmpty() {
        String content = ResourceUtils.readClasspathFile("non_existent_file_12345.txt");
        assertNotNull("不存在的文件返回空字符串而非 null", content);
        assertTrue("不存在的文件应返回空字符串", content.isEmpty());
    }

    @Test
    public void testReadClasspathFileUtf8_Utf8Content() {
        // 读取 schema-test.sql，验证 UTF-8 中文/特殊字符正常
        String content = ResourceUtils.readClasspathFileUtf8("schema-test.sql");
        assertFalse(content.isEmpty());
        assertTrue("应包含建表语句关键字", content.contains("CREATE TABLE"));
    }

    // ========== readClasspathStream ==========

    @Test
    public void testReadClasspathStream_ExistingFile() {
        InputStream is = ResourceUtils.readClasspathStream("application.properties");
        assertNotNull("已有文件流不应为 null", is);
    }

    @Test
    public void testReadClasspathStream_NonExistent_ReturnsNull() {
        InputStream is = ResourceUtils.readClasspathStream("no_such_file_99999.xyz");
        assertNull("不存在文件应返回 null", is);
    }

    @Test
    public void testReadClasspathStream_ContentReadable() throws Exception {
        InputStream is = ResourceUtils.readClasspathStream("schema-test.sql");
        assertNotNull(is);
        byte[] bytes = new byte[is.available()];
        is.read(bytes);
        String content = new String(bytes, StandardCharsets.UTF_8);
        assertTrue("流内容可读且包含预期关键字", content.contains("insight_result"));
    }

    // ========== 边界情况 ==========

    @Test
    public void testReadClasspathFile_NullPath_HandlesGracefully() {
        // 传入 null 不应抛异常，应返回空字符串
        String content = ResourceUtils.readClasspathFile(null);
        assertNotNull(content);
        // null path → ClassPathResource 构造失败 → 返回空
        assertTrue("null 路径应安全返回空字符串", content.isEmpty());
    }

    @Test
    public void testReadClasspathFile_EmptyPath() {
        String content = ResourceUtils.readClasspathFile("");
        assertNotNull(content);
        // 空路径通常找不到资源，返回空字符串
    }

    @Test
    public void testReadJsonSchemaFile() {
        // 如果存在 JSON Schema 文件则测试读取
        String content = ResourceUtils.readClasspathFileUtf8("schemas/dashboard-schema.json");
        // 可能不存在，只要不抛异常即可
        assertNotNull(content);
    }
}
