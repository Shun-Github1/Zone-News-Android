package com.anssy.znewspro.entry;

import java.util.List;

/**
 * @Description TODO
 * @Author yulu
 * @CreateTime 2025年07月07日 10:44:47
 */

public class TopicListEntry {
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
        private List<TopicDTO> topics;
        private List<TopicDTO> sectors;
        private List<TopicDTO> regions;

        public List<TopicDTO> getTopics() {
            return topics;
        }

        public void setTopics(List<TopicDTO> topics) {
            this.topics = topics;
        }
        
        public List<TopicDTO> getSectors() {
            return sectors;
        }

        public void setSectors(List<TopicDTO> sectors) {
            this.sectors = sectors;
        }
        
        public List<TopicDTO> getRegions() {
            return regions;
        }

        public void setRegions(List<TopicDTO> regions) {
            this.regions = regions;
        }
        
        // Backward compatibility method
        public List<String> getTopicsAsStrings() {
            if (topics == null) return null;
            return topics.stream()
                .map(TopicDTO::getDisplayName)
                .collect(java.util.stream.Collectors.toList());
        }
    }
    
    public static class TopicDTO {
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
