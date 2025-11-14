package com.club.magazine_club_program.Mapper;

import com.club.magazine_club_program.DTO.OAuth2TokenDTO;
import org.apache.ibatis.annotations.*;

@Mapper
public interface OAuth2TokenMapper {

    // 토큰 저장 (INSERT 또는 UPDATE)
    @Insert("""
        INSERT INTO OAuth2Token (principal_name, registration_id, access_token_value, 
                                 access_token_issued_at, access_token_expires_at, access_token_type,
                                 refresh_token_value, refresh_token_issued_at, refresh_token_expires_at)
        VALUES (#{principalName}, #{registrationId}, #{accessTokenValue},
                #{accessTokenIssuedAt}, #{accessTokenExpiresAt}, #{accessTokenType},
                #{refreshTokenValue}, #{refreshTokenIssuedAt}, #{refreshTokenExpiresAt})
        ON DUPLICATE KEY UPDATE
            access_token_value = #{accessTokenValue},
            access_token_issued_at = #{accessTokenIssuedAt},
            access_token_expires_at = #{accessTokenExpiresAt},
            access_token_type = #{accessTokenType},
            refresh_token_value = #{refreshTokenValue},
            refresh_token_issued_at = #{refreshTokenIssuedAt},
            refresh_token_expires_at = #{refreshTokenExpiresAt}
    """)
    int save(OAuth2TokenDTO token);

    // 토큰 조회
    @Select("""
        SELECT id, principal_name, registration_id, access_token_value,
               access_token_issued_at, access_token_expires_at, access_token_type,
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

    // 만료된 토큰 삭제
    @Delete("""
        DELETE FROM OAuth2Token
        WHERE access_token_expires_at < NOW()
    """)
    int removeExpiredTokens();
}

