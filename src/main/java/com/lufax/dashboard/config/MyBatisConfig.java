package com.lufax.dashboard.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.lufax.dashboard.repository")
public class MyBatisConfig {
}
