package com.club.magazine_club_program.DTO;

public class OAuth2UserInfo {
    private String id;
    private String email;
    private String nickname;

    public OAuth2UserInfo() {}

    public OAuth2UserInfo(String id, String email, String nickname) {
        this.id = id;
        this.email = email;
        this.nickname = nickname;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public static OAuth2UserInfoBuilder builder() {
        return new OAuth2UserInfoBuilder();
    }

    public static class OAuth2UserInfoBuilder {
        private String id;
        private String email;
        private String nickname;

        public OAuth2UserInfoBuilder id(String id) {
            this.id = id;
            return this;
        }

        public OAuth2UserInfoBuilder email(String email) {
            this.email = email;
            return this;
        }

        public OAuth2UserInfoBuilder nickname(String nickname) {
            this.nickname = nickname;
            return this;
        }

        public OAuth2UserInfo build() {
            return new OAuth2UserInfo(id, email, nickname);
        }
    }
}

