package com.searcher.zonenews.entry;

import java.util.List;

/**
 * @Description TODO
 * @Author yulu
 * @CreateTime 2025年07月04日 17:21:35
 */

public class ArticleDetailEntry {

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
        private String articleID;
        private List<ArticlesDTO> articles;
        private CoverageDTO coverage;
        private String date;
        private DescriptionDTO description;
        private MetricsDTO metrics;
        private Integer nSources;
        private String articleURL;
        private String pictureURL;
        private String shareURL;
        private String region;
        private List<RelatedArticlesDTO> relatedArticles;
        private List<RelatedTopicDTO> relatedTopics;
        private String sector;
        private String title;

        public String getArticleID() {
            return articleID;
        }

        public void setArticleID(String articleID) {
            this.articleID = articleID;
        }

        public List<ArticlesDTO> getArticles() {
            return articles;
        }

        public void setArticles(List<ArticlesDTO> articles) {
            this.articles = articles;
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

        public DescriptionDTO getDescription() {
            return description;
        }

        public void setDescription(DescriptionDTO description) {
            this.description = description;
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

        public String getShareURL() {
            return shareURL;
        }

        public void setShareURL(String shareURL) {
            this.shareURL = shareURL;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public List<RelatedArticlesDTO> getRelatedArticles() {
            return relatedArticles;
        }

        public void setRelatedArticles(List<RelatedArticlesDTO> relatedArticles) {
            this.relatedArticles = relatedArticles;
        }

        public List<RelatedTopicDTO> getRelatedTopics() {
            return relatedTopics;
        }

        public void setRelatedTopics(List<RelatedTopicDTO> relatedTopics) {
            this.relatedTopics = relatedTopics;
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

        public String getArticleURL() {
            return articleURL;
        }

        public void setArticleURL(String articleURL) {
            this.articleURL = articleURL;
        }

        public static class CoverageDTO {
            private IconsDTO icons;
            private PercentageDTO percentage;

            public IconsDTO getIcons() {
                return icons;
            }

            public void setIcons(IconsDTO icons) {
                this.icons = icons;
            }

            public PercentageDTO getPercentage() {
                return percentage;
            }

            public void setPercentage(PercentageDTO percentage) {
                this.percentage = percentage;
            }

            public static class IconsDTO {
                private List<IconDTO> centric;
                private List<IconDTO> progressive;

                public List<IconDTO> getCentric() {
                    return centric;
                }

                public void setCentric(List<IconDTO> centric) {
                    this.centric = centric;
                }

                public List<IconDTO> getProgressive() {
                    return progressive;
                }

                public void setProgressive(List<IconDTO> progressive) {
                    this.progressive = progressive;
                }

                public static class IconDTO {
                    private Double size;
                    private Double rx;
                    private Double ry;
                    private String logo;

                    public Double getSize() {
                        return size;
                    }

                    public void setSize(Double size) {
                        this.size = size;
                    }

                    public Double getRx() {
                        return rx;
                    }

                    public void setRx(Double rx) {
                        this.rx = rx;
                    }

                    public Double getRy() {
                        return ry;
                    }

                    public void setRy(Double ry) {
                        this.ry = ry;
                    }

                    public String getLogo() {
                        return logo;
                    }

                    public void setLogo(String logo) {
                        this.logo = logo;
                    }
                }
            }

            public static class PercentageDTO {
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

        public static class ArticlesDTO {
            private String articleURL;
            private String description;
            private String title;
            private String publisherIcon;
            private String publisherName;
            private String publisherRegion;
            private PublisherStanceDTO publisherStance;
            private Integer bias;
            private Integer mediaSignificance;
            private Integer publisherID;

            public String getArticleURL() {
                return articleURL;
            }

            public void setArticleURL(String articleURL) {
                this.articleURL = articleURL;
            }

            public String getDescription() {
                return description;
            }

            public void setDescription(String description) {
                this.description = description;
            }

            public String getTitle() {
                return title;
            }

            public void setTitle(String title) {
                this.title = title;
            }

            public String getPublisherIcon() {
                return publisherIcon;
            }

            public void setPublisherIcon(String publisherIcon) {
                this.publisherIcon = publisherIcon;
            }

            public String getPublisherName() {
                return publisherName;
            }

            public void setPublisherName(String publisherName) {
                this.publisherName = publisherName;
            }

            public String getPublisherRegion() {
                return publisherRegion;
            }

            public void setPublisherRegion(String publisherRegion) {
                this.publisherRegion = publisherRegion;
            }

            public PublisherStanceDTO getPublisherStance() {
                return publisherStance;
            }

            public void setPublisherStance(PublisherStanceDTO publisherStance) {
                this.publisherStance = publisherStance;
            }

            public Integer getBias() {
                return bias;
            }

            public void setBias(Integer bias) {
                this.bias = bias;
            }

            public Integer getMediaSignificance() {
                return mediaSignificance;
            }

            public void setMediaSignificance(Integer mediaSignificance) {
                this.mediaSignificance = mediaSignificance;
            }

            public Integer getPublisherID() {
                return publisherID;
            }

            public void setPublisherID(Integer publisherID) {
                this.publisherID = publisherID;
            }
        }

        public static class PublisherStanceDTO {
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

        public static class RelatedArticlesDTO {
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
            private List<RelatedTopicDTO> topics;

            public List<RelatedTopicDTO> getTopics() {
                return topics;
            }

            public void setTopics(List<RelatedTopicDTO> topics) {
                this.topics = topics;
            }

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
        }

        public static class DescriptionDTO {
            private String synopsis;
            private String implications;

            public String getSynopsis() {
                return synopsis;
            }

            public void setSynopsis(String synopsis) {
                this.synopsis = synopsis;
            }

            public String getImplications() {
                return implications;
            }

            public void setImplications(String implications) {
                this.implications = implications;
            }
        }

        public static class RelatedTopicDTO {
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

        private List<QuoteEntry> quotes;

        public List<QuoteEntry> getQuotes() {
            return quotes;
        }

        public void setQuotes(List<QuoteEntry> quotes) {
            this.quotes = quotes;
        }
    }
}
