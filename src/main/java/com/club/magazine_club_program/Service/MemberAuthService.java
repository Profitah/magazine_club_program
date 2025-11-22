package com.club.magazine_club_program.Service;

import com.club.magazine_club_program.DTO.MemberAuthDTO;
import com.club.magazine_club_program.DTO.RegisterRequest;
import com.club.magazine_club_program.Mapper.MemberAuthMapper;
import com.club.magazine_club_program.Util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 일반 로그인 회원 인증 서비스
 * 새로운 파일로 생성하여 기존 코드와 분리
 */
@Service
public class MemberAuthService {

    private static final Logger log = LoggerFactory.getLogger(MemberAuthService.class);
    private final MemberAuthMapper memberAuthMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final BannedEmailService bannedEmailService;

    public MemberAuthService(
            MemberAuthMapper memberAuthMapper,
            BCryptPasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            BannedEmailService bannedEmailService) {
        this.memberAuthMapper = memberAuthMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.bannedEmailService = bannedEmailService;
    }

    /**
     * 일반 로그인 처리
     * @param email 이메일
     * @param password 평문 비밀번호
     * @return 로그인 결과 (JWT 토큰 포함)
     */
    public Map<String, Object> login(String email, String password) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 이메일 차단 확인
            if (bannedEmailService.isEmailBanned(email)) {
                response.put("success", false);
                response.put("message", "차단된 계정입니다");
                return response;
            }

            // 회원 조회
            MemberAuthDTO member = memberAuthMapper.findByEmail(email);
            
            if (member == null) {
                response.put("success", false);
                response.put("message", "이메일 또는 비밀번호가 올바르지 않습니다");
                return response;
            }

            // 회원 자격 일시정지 확인
            if (member.isSuspended()) {
                response.put("success", false);
                response.put("message", "일시정지된 계정입니다");
                return response;
            }

            // 비밀번호 검증
            if (member.getPassword() == null || !passwordEncoder.matches(password, member.getPassword())) {
                response.put("success", false);
                response.put("message", "이메일 또는 비밀번호가 올바르지 않습니다");
                log.warn("로그인 실패: 잘못된 비밀번호 - email={}", email);
                return response;
            }

            // JWT 토큰 발급
            Map<String, Object> tokenInfo = jwtUtil.generateTokenPair(
                    member.getId(), 
                    member.getEmail(), 
                    member.getName()
            );

            // 응답 구성
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("memberId", member.getId());
            userInfo.put("email", member.getEmail());
            userInfo.put("name", member.getName());

            response.put("success", true);
            response.put("message", "로그인 성공");
            response.put("user", userInfo);
            response.put("token", tokenInfo);

            log.info("일반 로그인 성공: memberId={}, email={}", member.getId(), email);
            
        } catch (Exception e) {
            log.error("로그인 처리 중 에러 발생: email={}", email, e);
            response.put("success", false);
            response.put("message", "로그인 처리 중 오류가 발생했습니다");
        }
        
        return response;
    }

    /**
     * 회원가입 처리
     * @param request 회원가입 요청
     * @return 회원가입 결과
     */
    public Map<String, Object> register(RegisterRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String email = request.getEmail();
            
            // 이메일 차단 확인
            if (bannedEmailService.isEmailBanned(email)) {
                response.put("success", false);
                response.put("message", "가입할 수 없는 이메일입니다");
                return response;
            }

            // 이메일 중복 확인
            if (memberAuthMapper.existsByEmail(email)) {
                response.put("success", false);
                response.put("message", "이미 등록된 이메일입니다");
                return response;
            }

            // 비밀번호 암호화
            String encodedPassword = passwordEncoder.encode(request.getPassword());

            // 회원 생성
            MemberAuthDTO newMember = new MemberAuthDTO();
            newMember.setName(request.getName());
            newMember.setEmail(email);
            newMember.setPassword(encodedPassword);

            int result = memberAuthMapper.registerMember(newMember);
            
            if (result > 0 && newMember.getId() > 0) {
                response.put("success", true);
                response.put("message", "회원가입 성공");
                response.put("memberId", newMember.getId());
                log.info("회원가입 성공: memberId={}, email={}, name={}", 
                        newMember.getId(), email, request.getName());
            } else {
                response.put("success", false);
                response.put("message", "회원가입 실패");
            }
            
        } catch (Exception e) {
            log.error("회원가입 처리 중 에러 발생: email={}", request.getEmail(), e);
            response.put("success", false);
            response.put("message", "회원가입 처리 중 오류가 발생했습니다");
        }
        
        return response;
    }
}

