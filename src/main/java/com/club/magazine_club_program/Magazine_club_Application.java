package com.club.magazine_club_program;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.club.magazine_club_program.Mapper")
@EnableScheduling
public class Magazine_club_Application {
    public static void main(String[] args) {
        SpringApplication.run(Magazine_club_Application.class, args);
    }
}


