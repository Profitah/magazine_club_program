package com.club.magazine_club_program.Mapper;

import com.club.magazine_club_program.DTO.InstagramImageDTO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface InstagramGalleryMapper {

    /**
     * 인스타그램 이미지 저장
     */
    @Insert("""
        INSERT INTO InstagramGallery (username, original_url, s3_url, crawled_at)
        VALUES (#{username}, #{originalUrl}, #{s3Url}, NOW())
    """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(InstagramImageDTO image);

    /**
     * 사용자명으로 이미지 목록 조회
     */
    @Select("""
        SELECT id, username, original_url, s3_url, crawled_at, created_at
        FROM InstagramGallery
        WHERE username = #{username}
        ORDER BY crawled_at DESC
    """)
    List<InstagramImageDTO> findByUsername(@Param("username") String username);

    /**
     * 사용자명으로 이미지 목록 조회 (페이지네이션)
     */
    @Select("""
        SELECT id, username, original_url, s3_url, crawled_at, created_at
        FROM InstagramGallery
        WHERE username = #{username}
        ORDER BY crawled_at DESC
        LIMIT #{limit} OFFSET #{offset}
    """)
    List<InstagramImageDTO> findByUsernameWithPagination(
            @Param("username") String username,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    /**
     * 사용자명으로 이미지 개수 조회
     */
    @Select("""
        SELECT COUNT(*)
        FROM InstagramGallery
        WHERE username = #{username}
    """)
    int countByUsername(@Param("username") String username);

    /**
     * 원본 URL로 이미지 존재 여부 확인
     */
    @Select("""
        SELECT COUNT(*) > 0
        FROM InstagramGallery
        WHERE original_url = #{originalUrl}
    """)
    boolean existsByOriginalUrl(@Param("originalUrl") String originalUrl);
}

