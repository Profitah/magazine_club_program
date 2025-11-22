package com.club.magazine_club_program.Controller.Admin;

import jakarta.servlet.http.HttpSession;

/*관리자 인증 유틸리티 클래스*/
public class AdminAuthUtil {
    
    /* 관리자 인증 확인 */
    public static boolean isAdminAuthenticated(HttpSession session) {
        Boolean authenticated = (Boolean) session.getAttribute("authenticated");
        return authenticated != null && authenticated;
    }
    
    /**관리자 이메일 조회 */
    public static String getAdminEmail(HttpSession session) {
        return (String) session.getAttribute("adminEmail");
    }
}