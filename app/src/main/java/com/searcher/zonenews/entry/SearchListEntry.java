package com.searcher.zonenews.entry;

import java.util.List;

/**
 * @Description TODO
 * @Author yulu
 * @CreateTime 2025年07月04日 16:25:16
 */

public class SearchListEntry {

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
        private List<ArticlesDTO> articles;
        private MetaDTO meta;

        public List<ArticlesDTO> getArticles() {
            return articles;
        }

        public void setArticles(List<ArticlesDTO> articles) {
            this.articles = articles;
        }

        public MetaDTO getMeta() {
            return meta;
        }

        public void setMeta(MetaDTO meta) {
            this.meta = meta;
        }

        public static class MetaDTO {
            private Integer page;
            private Integer limit;
            private Integer total;
            private Integer totalPages;

            public Integer getPage() {
                return page;
            }

            public void setPage(Integer page) {
                this.page = page;
            }

            public Integer getLimit() {
                return limit;
            }

            public void setLimit(Integer limit) {
                this.limit = limit;
            }

            public Integer getTotal() {
                return total;
            }

            public void setTotal(Integer total) {
                this.total = total;
            }

            public Integer getTotalPages() {
                return totalPages;
            }

            public void setTotalPages(Integer totalPages) {
                this.totalPages = totalPages;
            }
        }

        public static class ArticlesDTO {
            private String articleID;
            private String articleURL;
            private CoverageDTO coverage;
            private String date;
            private MetricsDTO metrics;
            private Integer nSources;
            private String pictureURL;
            private String region;
            private String sector;
            private String title;
            private List<TopicsDTO> topics;

            public String getArticleID() {
                return articleID;
            }

            public void setArticleID(String articleID) {
                this.articleID = articleID;
            }

            public String getArticleURL() {
                return articleURL;
            }

            public void setArticleURL(String articleURL) {
                this.articleURL = articleURL;
            }

            public CoverageDTO getCoverage() {
                return coverage;
            }

            public void setCoverage(CoverageDTO coverage) {
                this.coverage = coverage;
            }

            public String getDate() {
                return date;
            }

            public void setDate(String date) {
                this.date = date;
            }

            public MetricsDTO getMetrics() {
                return metrics;
            }

            public void setMetrics(MetricsDTO metrics) {
                this.metrics = metrics;
            }

            public Integer getNSources() {
                return nSources;
            }

            public void setNSources(Integer nSources) {
                this.nSources = nSources;
            }

            public String getPictureURL() {
                return pictureURL;
            }

            public void setPictureURL(String pictureURL) {
                this.pictureURL = pictureURL;
            }

            public String getRegion() {
                return region;
            }

            public void setRegion(String region) {
                this.region = region;
            }

            public String getSector() {
                return sector;
            }

            public void setSector(String sector) {
                this.sector = sector;
            }

            public String getTitle() {
                return title;
            }

            public void setTitle(String title) {
                this.title = title;
            }

            public List<TopicsDTO> getTopics() {
                return topics;
            }

            public void setTopics(List<TopicsDTO> topics) {
                this.topics = topics;
            }

            public static class CoverageDTO {
                private Double centric;
                private Double progressive;

                public Double getCentric() {
                    return centric;
                }

                public void setCentric(Double centric) {
                    this.centric = centric;
                }

                public Double getProgressive() {
                    return progressive;
                }

                public void setProgressive(Double progressive) {
                    this.progressive = progressive;
                }
            }

            public static class MetricsDTO {
                private Double sentiment;
                private Double subjectivity;

                public Double getSentiment() {
                    return sentiment;
                }

                public void setSentiment(Double sentiment) {
                    this.sentiment = sentiment;
                }

                public Double getSubjectivity() {
                    return subjectivity;
                }

                public void setSubjectivity(Double subjectivity) {
                    this.subjectivity = subjectivity;
                }
            }

            public static class TopicsDTO {
                private String displayName;
                private String tag;

                public String getDisplayName() {
                    return displayName;
                }

                public void setDisplayName(String displayName) {
                    this.displayName = displayName;
                }

                public String getTag() {
                    return tag;
                }

                public void setTag(String tag) {
                    this.tag = tag;
                }
            }
        }
    }
}
