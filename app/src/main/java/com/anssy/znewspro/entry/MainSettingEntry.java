package com.anssy.znewspro.entry;

import java.io.Serializable;

/**
 * @Description 主页设置
 * @Author yulu
 * @CreateTime 2025年08月04日 17:18:17
 */

public class MainSettingEntry implements Serializable {
    private String name;
    private int checkId;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCheckId() {
        return checkId;
    }

    public void setCheckId(int checkId) {
        this.checkId = checkId;
    }
}
