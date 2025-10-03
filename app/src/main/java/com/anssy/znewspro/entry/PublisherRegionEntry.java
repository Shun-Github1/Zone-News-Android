package com.anssy.znewspro.entry;

import java.util.List;

/**
 * Publisher Region Entry for API responses
 * @Author yulu
 * @CreateTime 2025年01月15日 10:00:00
 */

public class PublisherRegionEntry {

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
        private List<RegionDTO> regions;
        private List<String> selected;

        public List<RegionDTO> getRegions() {
            return regions;
        }

        public void setRegions(List<RegionDTO> regions) {
            this.regions = regions;
        }

        public List<String> getSelected() {
            return selected;
        }

        public void setSelected(List<String> selected) {
            this.selected = selected;
        }

        public static class RegionDTO {
            private String tag;
            private String displayName;

            public String getTag() {
                return tag;
            }

            public void setTag(String tag) {
                this.tag = tag;
            }

            public String getDisplayName() {
                return displayName;
            }

            public void setDisplayName(String displayName) {
                this.displayName = displayName;
            }
        }
    }
}
