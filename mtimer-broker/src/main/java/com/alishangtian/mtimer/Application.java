package com.alishangtian.mtimer;

import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @Desc Application
 * @Time 2020/08/30
 * @Author alishangtian
 */
@SpringBootApplication(scanBasePackages = "com.alishangtian.mtimer")
@Log4j2
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
        log.info("server started");
    }
}
