package com.club.magazine_club_program.Config;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.ICredentialRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class AuthenticatorConfig {

    /**
     * Google Authenticator 빈 설정
     * ICredentialRepository 구현체를 제공하여 라이브러리 요구사항 충족
     */
    @Bean
    public GoogleAuthenticator googleAuthenticator() {
        GoogleAuthenticator googleAuthenticator = new GoogleAuthenticator();
        googleAuthenticator.setCredentialRepository(new SimpleCredentialRepository());
        // 시간 윈도우는 라이브러리 기본값 사용 (일반적으로 ±1.5분 허용)
        return googleAuthenticator;
    }

    /**
     * 간단한 ICredentialRepository 구현체
     * 우리는 DB에 직접 Secret을 저장하므로 여기서는 빈 구현만 제공
     */
    private static class SimpleCredentialRepository implements ICredentialRepository {
        private final Map<String, String> secrets = new HashMap<>();

        @Override
        public String getSecretKey(String userName) {
            return secrets.get(userName);
        }

        @Override
        public void saveUserCredentials(String userName, String secretKey, int validationCode, List<Integer> scratchCodes) {
            secrets.put(userName, secretKey);
        }
    }
}

