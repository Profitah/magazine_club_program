package com.club.magazine_club_program.Mapper;

import com.club.magazine_club_program.DTO.OAuth2TokenDTO;
import org.apache.ibatis.annotations.*;

@Mapper
public interface OAuth2TokenMapper {

    // Refresh token만 저장 (INSERT 또는 UPDATE)
    // Access token은 메모리에만 저장 (짧은 만료 시간)
    @Insert("""
        INSERT INTO OAuth2Token (principal_name, registration_id,
                                 refresh_token_value, refresh_token_issued_at, refresh_token_expires_at)
        VALUES (#{principalName}, #{registrationId},
                #{refreshTokenValue}, #{refreshTokenIssuedAt}, #{refreshTokenExpiresAt})
        ON DUPLICATE KEY UPDATE
            refresh_token_value = #{refreshTokenValue},
            refresh_token_issued_at = #{refreshTokenIssuedAt},
            refresh_token_expires_at = #{refreshTokenExpiresAt}
    """)
    int save(OAuth2TokenDTO token);

    // Refresh token 조회
    @Select("""
        SELECT id, principal_name, registration_id,
               refresh_token_value, refresh_token_issued_at, refresh_token_expires_at
        FROM OAuth2Token
        WHERE principal_name = #{principalName} AND registration_id = #{registrationId}
    """)
    OAuth2TokenDTO findByPrincipalNameAndRegistrationId(
            @Param("principalName") String principalName,
            @Param("registrationId") String registrationId
    );

    // 토큰 삭제
    @Delete("""
        DELETE FROM OAuth2Token
        WHERE principal_name = #{principalName} AND registration_id = #{registrationId}
    """)
    int remove(@Param("principalName") String principalName, @Param("registrationId") String registrationId);

    // 만료된 refresh token 삭제
    @Delete("""
        DELETE FROM OAuth2Token
        WHERE refresh_token_expires_at < NOW()
    """)
    int removeExpiredTokens();
}

