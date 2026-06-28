package com.company.daizhang;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 代账系统应用入口
 */
@SpringBootApplication
@MapperScan("com.company.daizhang.module.*.mapper")
@EnableScheduling
public class DaizhangApplication {

    public static void main(String[] args) {
        SpringApplication.run(DaizhangApplication.class, args);
    }
}
