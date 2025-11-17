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
                    role VARCHAR(50) DEFAULT 'ADMIN',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_email (email),
                    INDEX idx_role (role)
                )
            """);
            System.out.println("Admin 테이블 확인/생성 완료");
            
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS InstagramGallery (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    username VARCHAR(255) NOT NULL,
                    original_url VARCHAR(500),
                    s3_url VARCHAR(500) NOT NULL,
                    crawled_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_username (username),
                    INDEX idx_crawled_at (crawled_at)
                )
            """);
            System.out.println("InstagramGallery 테이블 확인/생성 완료");
            
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS ClubPost (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    title VARCHAR(255) NOT NULL,
                    description TEXT,
                    created_by_admin_id INT NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    due_date TIMESTAMP NULL,
                    is_active BOOLEAN DEFAULT TRUE,
                    FOREIGN KEY (created_by_admin_id) REFERENCES Admin(id) ON DELETE RESTRICT,
                    INDEX idx_created_at (created_at),
                    INDEX idx_is_active (is_active),
                    INDEX idx_created_by_admin_id (created_by_admin_id)
                )
            """);
            System.out.println("ClubPost 테이블 확인/생성 완료");
            
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS PostPermission (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    post_id INT NOT NULL,
                    admin_id INT NOT NULL,
                    permission_type VARCHAR(50) NOT NULL,
                    granted_by_admin_id INT NOT NULL,
                    granted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (post_id) REFERENCES ClubPost(id) ON DELETE CASCADE,
                    FOREIGN KEY (admin_id) REFERENCES Admin(id) ON DELETE CASCADE,
                    FOREIGN KEY (granted_by_admin_id) REFERENCES Admin(id) ON DELETE RESTRICT,
                    INDEX idx_post_id (post_id),
                    INDEX idx_admin_id (admin_id),
                    UNIQUE KEY unique_post_admin (post_id, admin_id)
                )
            """);
            System.out.println("PostPermission 테이블 확인/생성 완료");
            
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS AssignmentSubmission (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    post_id INT NOT NULL,
                    member_id INT NOT NULL,
                    image_url VARCHAR(500),
                    title VARCHAR(255),
                    reflection TEXT,
                    captured_at TIMESTAMP NULL,
                    location VARCHAR(255),
                    submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (post_id) REFERENCES ClubPost(id) ON DELETE CASCADE,
                    FOREIGN KEY (member_id) REFERENCES MemberInfo(id) ON DELETE CASCADE,
                    INDEX idx_post_id (post_id),
                    INDEX idx_member_id (member_id),
                    INDEX idx_submitted_at (submitted_at)
                )
            """);
            System.out.println("AssignmentSubmission 테이블 확인/생성 완료");
            
        } catch (Exception e) {
            System.err.println("데이터베이스 초기화 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

