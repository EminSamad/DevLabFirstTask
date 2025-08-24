package com.example.devlabweek1task1;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableScheduling
@EnableRetry
public class DevLabWeek1Task1Application {

    public static void main(String[] args) {
        SpringApplication.run(DevLabWeek1Task1Application.class, args);
    }

}
