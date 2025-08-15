package com.anssy.znewspro.entry;

/**
 * @Description TODO
 * @Author yulu
 * @CreateTime 2025年07月05日 09:47:48
 */

public class CommonResponseEntry {
    private Integer code;
    private String msg;

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

    @Override
    public String toString() {
        return "CommonResponseEntry{" +
                "code=" + code +
                ", msg='" + msg + '\'' +
                '}';
    }
}
