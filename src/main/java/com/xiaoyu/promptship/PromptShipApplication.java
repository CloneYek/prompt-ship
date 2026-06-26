package com.xiaoyu.promptship;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 启动类
 */
@SpringBootApplication
@MapperScan("com.xiaoyu.promptship.mapper")
public class PromptShipApplication {

    public static void main(String[] args) {
        SpringApplication.run(PromptShipApplication.class, args);
    }

}
