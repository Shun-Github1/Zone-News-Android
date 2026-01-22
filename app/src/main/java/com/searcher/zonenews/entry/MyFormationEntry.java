package com.searcher.zonenews.entry;

/**
 * @Description TODO
 * @Author yulu
 * @CreateTime 2025年07月07日 14:45:00
 */

public class MyFormationEntry {

    private Integer code;
    private DataDTO data;
    private String msg;

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public DataDTO getData() {
        return data;
    }

    public void setData(DataDTO data) {
        this.data = data;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public static class DataDTO {
        private String authMethod;
        private String email;
        private Boolean isPro;
        private String language;
        private String profileIcon;
        private String username;

        public String getAuthMethod() {
            return authMethod;
        }

        public void setAuthMethod(String authMethod) {
            this.authMethod = authMethod;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public Boolean getIsPro() {
            return isPro;
        }

        public void setIsPro(Boolean isPro) {
            this.isPro = isPro;
        }

        public String getLanguage() {
            return language;
        }

        public void setLanguage(String language) {
            this.language = language;
        }

        public String getProfileIcon() {
            return profileIcon;
        }

        public void setProfileIcon(String profileIcon) {
            this.profileIcon = profileIcon;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }
    }
}
