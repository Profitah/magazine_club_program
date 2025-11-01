package com.club.magazine_club_program.Config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        // 테이블이 없을 경우에 테이블 생성
        try {
            String url = jdbcTemplate.getDataSource().getConnection().getMetaData().getURL();
            System.out.println("=== Database URL: " + url + " ===");
            
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS Admin (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    email VARCHAR(255) NOT NULL UNIQUE,
                    totp_secret VARCHAR(255),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
            System.out.println("Admin 테이블 확인/생성 완료");
            
        } catch (Exception e) {
            System.err.println("데이터베이스 초기화 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

