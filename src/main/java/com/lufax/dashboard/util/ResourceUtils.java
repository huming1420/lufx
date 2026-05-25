package com.lufax.dashboard.util;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Component
public class ResourceUtils {

    public static String readClasspathFile(String path) {
        return readClasspathFileUtf8(path);
    }

    public static String readClasspathFileUtf8(String path) {
        InputStream is = readClasspathStream(path);
        if (is == null) {
            return "";
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString().trim();
        } catch (IOException e) {
            return "";
        }
    }

    public static InputStream readClasspathStream(String path) {
        try {
            Resource resource = new ClassPathResource(path);
            if (resource.exists()) {
                return resource.getInputStream();
            }
            return ResourceUtils.class.getClassLoader().getResourceAsStream(path);
        } catch (IOException e) {
            return null;
        }
    }

    public String readResource(String path) {
        return readClasspathFileUtf8(path);
    }
}