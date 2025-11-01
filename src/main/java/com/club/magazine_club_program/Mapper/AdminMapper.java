package com.club.magazine_club_program.Mapper;

import com.club.magazine_club_program.DTO.AdminDTO;
import org.apache.ibatis.annotations.*;

@Mapper
public interface AdminMapper {

    @Select("SELECT id, email, totp_secret as totpSecret FROM Admin WHERE email = #{email}")
    AdminDTO findByEmail(String email);

    @Update("UPDATE Admin SET totp_secret = #{totpSecret} WHERE email = #{email}")
    int updateTotpSecret(@Param("email") String email, @Param("totpSecret") String totpSecret);

    @Insert("INSERT INTO Admin (email, totp_secret) VALUES (#{email}, #{totpSecret})")
    int insertAdmin(@Param("email") String email, @Param("totpSecret") String totpSecret);
}

