package com.anssy.znewspro.entry;

/**
 * @Description TODO
 * @Author yulu
 * @CreateTime 2025年06月30日 13:59:40
 */

public class HomeBannerEntry {
    private String bannerUrl;
    private String  title;
    private String desc;

    public HomeBannerEntry(String bannerUrl, String title, String desc) {
        this.bannerUrl = bannerUrl;
        this.title = title;
        this.desc = desc;
    }

    public String getBannerUrl() {
        return bannerUrl;
    }

    public void setBannerUrl(String bannerUrl) {
        this.bannerUrl = bannerUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}
