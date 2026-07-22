package com.company.daizhang;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 代账系统应用入口
 */
@SpringBootApplication
// 扫描 mapper 接口:
//   module.*.mapper        匹配一级子包(如 module.voucher.mapper)
//   module.*.*.mapper      匹配二级子包(如 module.system.totp.mapper / module.system.security.mapper / module.system.backup.mapper)
@MapperScan({
        "com.company.daizhang.module.*.mapper",
        "com.company.daizhang.module.*.*.mapper"
})
@EnableScheduling
public class DaizhangApplication {

    public static void main(String[] args) {
        SpringApplication.run(DaizhangApplication.class, args);
    }
}
