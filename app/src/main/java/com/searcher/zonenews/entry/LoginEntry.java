package com.searcher.zonenews.entry;

/**
 * @Description TODO
 * @Author yulu
 * @CreateTime 2025年07月04日 11:29:16
 */

public class LoginEntry {

    private Integer code;
    private String msg;
    private DataDTO data;

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public DataDTO getData() {
        return data;
    }

    public void setData(DataDTO data) {
        this.data = data;
    }

    public static class DataDTO {
        private String access_token;
        private String csrf_token;

        public String getAccess_token() {
            return access_token;
        }

        public void setAccess_token(String access_token) {
            this.access_token = access_token;
        }

        public String getCsrf_token() {
            return csrf_token;
        }

        public void setCsrf_token(String csrf_token) {
            this.csrf_token = csrf_token;
        }

        /**
         * Get the token value, preferring csrf_token if available, falling back to access_token for backward compatibility
         */
        public String getToken() {
            return csrf_token != null ? csrf_token : access_token;
        }
    }
}
