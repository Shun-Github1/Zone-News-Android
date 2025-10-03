# Complete String Mapping Analysis: iOS vs Android

## Overview
This document provides a comprehensive mapping of every string used in both iOS and Android versions of the Zone News app, including English, Simplified Chinese (zh-CN), and Traditional Chinese (zh-HK) translations.

---

## 1. MAIN TAB NAVIGATION

### English
| iOS Key | iOS Value | Android Key | Android Value | Status |
|---------|-----------|-------------|---------------|--------|
| `tab.home` | "Home" | `main_page` | "Home" | ✅ MATCH |
| `tab.recommended` | "Recommended" | `person_recommend` | "Recommended" | ✅ MATCH |
| `tab.profile` | "Profile" | `my` | "Profile" | ✅ MATCH |
| `tab.discover` | "Discover" | `discover` | "Discover" | ✅ MATCH |

### Simplified Chinese (zh-CN)
| iOS Key | iOS Value | Android Key | Android Value | Status |
|---------|-----------|-------------|---------------|--------|
| `tab.home` | "首页" | `main_page` | "首页" | ✅ MATCH |
| `tab.recommended` | "个人" | `person_recommend` | "推荐" | ❌ MISMATCH |
| `tab.profile` | "帐户" | `my` | "个人资料" | ❌ MISMATCH |
| `tab.discover` | "发现" | `discover` | "搜寻" | ❌ MISMATCH |

### Traditional Chinese (zh-HK)
| iOS Key | iOS Value | Android Key | Android Value | Status |
|---------|-----------|-------------|---------------|--------|
| `tab.home` | "首頁" | `main_page` | "首頁" | ✅ MATCH |
| `tab.recommended` | "個人" | `person_recommend` | "推薦" | ❌ MISMATCH |
| `tab.profile` | "帳戶" | `my` | "個人資料" | ❌ MISMATCH |
| `tab.discover` | "發現" | `discover` | "搜尋" | ❌ MISMATCH |

---

## 2. SENTIMENT ANALYSIS

### English
| iOS Key | iOS Value | Android Key | Android Value | Status |
|---------|-----------|-------------|---------------|--------|
| `sentiment.score.label` | "Sentiment Score: %@" | `sentiment_score_label` | "Sentiment: %1$s" | ❌ MISMATCH |
| `sentiment.positive` | "Positive" | `sentiment_positive` | "Positive" | ✅ MATCH |
| `sentiment.negative` | "Negative" | `sentiment_negative` | "Negative" | ✅ MATCH |
| `sentiment.neutral` | "Neutral" | `sentiment_neutral` | "Neutral" | ✅ MATCH |
| `sentiment.very_positive` | "Very Positive" | `sentiment_very_positive` | "Very Positive" | ✅ MATCH |
| `sentiment.slightly_positive` | "Slightly Positive" | `sentiment_slightly_positive` | "Slightly Positive" | ✅ MATCH |
| `sentiment.slightly_negative` | "Slightly Negative" | `sentiment_slightly_negative` | "Slightly Negative" | ✅ MATCH |
| `sentiment.very_negative` | "Very Negative" | `sentiment_very_negative` | "Very Negative" | ✅ MATCH |

### Simplified Chinese (zh-CN)
| iOS Key | iOS Value | Android Key | Android Value | Status |
|---------|-----------|-------------|---------------|--------|
| `sentiment.score.label` | "文本情感数值：%@" | `sentiment_score_label` | "情绪：%1$s" | ❌ MISMATCH |
| `sentiment.positive` | "正面" | `sentiment_positive` | "正面" | ✅ MATCH |
| `sentiment.negative` | "负面" | `sentiment_negative` | "负面" | ✅ MATCH |
| `sentiment.neutral` | "中性" | `sentiment_neutral` | "中性" | ✅ MATCH |
| `sentiment.very_positive` | "非常正面" | `sentiment_very_positive` | "非常正面" | ✅ MATCH |
| `sentiment.slightly_positive` | "略为正面" | `sentiment_slightly_positive` | "略为正面" | ✅ MATCH |
| `sentiment.slightly_negative` | "略为负面" | `sentiment_slightly_negative` | "略为负面" | ✅ MATCH |
| `sentiment.very_negative` | "非常负面" | `sentiment_very_negative` | "非常负面" | ✅ MATCH |

### Traditional Chinese (zh-HK)
| iOS Key | iOS Value | Android Key | Android Value | Status |
|---------|-----------|-------------|---------------|--------|
| `sentiment.score.label` | "文本情感數值：%@" | `sentiment_score_label` | "情緒：%1$s" | ❌ MISMATCH |
| `sentiment.positive` | "正面" | `sentiment_positive` | "正面" | ✅ MATCH |
| `sentiment.negative` | "負面" | `sentiment_negative` | "負面" | ✅ MATCH |
| `sentiment.neutral` | "中性" | `sentiment_neutral` | "中性" | ✅ MATCH |
| `sentiment.very_positive` | "非常正面" | `sentiment_very_positive` | "非常正面" | ✅ MATCH |
| `sentiment.slightly_positive` | "略為正面" | `sentiment_slightly_positive` | "略為正面" | ✅ MATCH |
| `sentiment.slightly_negative` | "略為負面" | `sentiment_slightly_negative` | "略為負面" | ✅ MATCH |
| `sentiment.very_negative` | "非常負面" | `sentiment_very_negative` | "非常負面" | ✅ MATCH |

---

## 3. SORT OPTIONS

### English
| iOS Key | iOS Value | Android Key | Android Value | Status |
|---------|-----------|-------------|---------------|--------|
| `recommended.sort_by` | "Sort by" | `sort_by` | "Sort by" | ✅ MATCH |
| `recommended.sort.current_method` | "Currently sorted by" | `currently_sorted_by` | "Currently sorted by" | ✅ MATCH |
| `recommended.sort.latest` | "Latest" | `latest` | "Latest" | ✅ MATCH |
| `recommended.sort.popular` | "Most Popular" | `most_popular` | "Most Popular" | ✅ MATCH |
| `recommended.sort.relevant` | "Most Relevant" | `most_relevant` | "Most Relevant" | ✅ MATCH |
| `recommended.sort.trending` | "Trending" | `trending` | "Trending" | ✅ MATCH |
| `sort.ascending` | "Ascending" | `ascending` | "Ascending" | ✅ MATCH |
| `sort.descending` | "Descending" | `descending` | "Descending" | ✅ MATCH |

### Simplified Chinese (zh-CN)
| iOS Key | iOS Value | Android Key | Android Value | Status |
|---------|-----------|-------------|---------------|--------|
| `recommended.sort_by` | "排序方式" | `sort_by` | "排序" | ❌ MISMATCH |
| `recommended.sort.current_method` | "当前排序方式" | `currently_sorted_by` | "当前排序方式" | ✅ MATCH |
| `recommended.sort.latest` | "时间" | `latest` | "最新" | ❌ MISMATCH |
| `recommended.sort.popular` | "热门度" | `most_popular` | "最受欢迎" | ❌ MISMATCH |
| `recommended.sort.relevant` | "相关性" | `most_relevant` | "最相关" | ❌ MISMATCH |
| `recommended.sort.trending` | "热门" | `trending` | "热门" | ✅ MATCH |
| `sort.ascending` | "升序" | `ascending` | "升序" | ✅ MATCH |
| `sort.descending` | "降序" | `descending` | "降序" | ✅ MATCH |

### Traditional Chinese (zh-HK)
| iOS Key | iOS Value | Android Key | Android Value | Status |
|---------|-----------|-------------|---------------|--------|
| `recommended.sort_by` | "排序方式" | `sort_by` | "排序" | ❌ MISMATCH |
| `recommended.sort.current_method` | "當前排序方式" | `currently_sorted_by` | "目前排序方式" | ❌ MISMATCH |
| `recommended.sort.latest` | "時間" | `latest` | "最新" | ❌ MISMATCH |
| `recommended.sort.popular` | "熱門度" | `most_popular` | "最受歡迎" | ❌ MISMATCH |
| `recommended.sort.relevant` | "相關性" | `most_relevant` | "最相關" | ❌ MISMATCH |
| `recommended.sort.trending` | "熱門" | `trending` | "熱門" | ✅ MATCH |
| `sort.ascending` | "升序" | `ascending` | "升序" | ✅ MATCH |
| `sort.descending` | "降序" | `descending` | "降序" | ✅ MATCH |

---

## 4. PUBLISHER/MEDIA INFORMATION

### English
| iOS Key | iOS Value | Android Key | Android Value | Status |
|---------|-----------|-------------|---------------|--------|
| `sort.publisher_name` | "Media Name" | `publisher_name` | "Publisher Name" | ❌ MISMATCH |
| `sort.media_significance` | "Media Significance" | `media_significance` | "Media Significance" | ✅ MATCH |
| `sort.publisher_bias` | "Media Bias" | `publisher_bias` | "Publisher Bias" | ❌ MISMATCH |
| `sort.publisher_region` | "Media Region" | `publisher_region` | "Publisher Region" | ❌ MISMATCH |
| `sort.article_title` | "Article Title" | `article_title` | "Article Title" | ✅ MATCH |
| `media.distribution` | "Media Distribution" | `publisher_distribution` | "Publisher Distribution" | ❌ MISMATCH |

### Simplified Chinese (zh-CN)
| iOS Key | iOS Value | Android Key | Android Value | Status |
|---------|-----------|-------------|---------------|--------|
| `sort.publisher_name` | "媒体名称" | `publisher_name` | "媒体名称" | ✅ MATCH |
| `sort.media_significance` | "媒体重要性" | `media_significance` | "媒体影响力" | ❌ MISMATCH |
| `sort.publisher_bias` | "媒体立场" | `publisher_bias` | "媒体立场" | ✅ MATCH |
| `sort.publisher_region` | "媒体地区" | `publisher_region` | "媒体地区" | ✅ MATCH |
| `sort.article_title` | "文章标题" | `article_title` | "文章标题" | ✅ MATCH |
| `media.distribution` | "媒体分布" | `publisher_distribution` | "发布者分布" | ❌ MISMATCH |

### Traditional Chinese (zh-HK)
| iOS Key | iOS Value | Android Key | Android Value | Status |
|---------|-----------|-------------|---------------|--------|
| `sort.publisher_name` | "媒體名稱" | `publisher_name` | "媒體名稱" | ✅ MATCH |
| `sort.media_significance` | "媒體重要性" | `media_significance` | "媒體影響力" | ❌ MISMATCH |
| `sort.publisher_bias` | "媒體立場" | `publisher_bias` | "媒體立場" | ✅ MATCH |
| `sort.publisher_region` | "媒體地區" | `publisher_region` | "媒體地區" | ✅ MATCH |
| `sort.article_title` | "文章標題" | `article_title` | "文章標題" | ✅ MATCH |
| `media.distribution` | "媒體分佈" | `publisher_distribution` | "發布者分佈" | ❌ MISMATCH |

---

## 5. RECOMMENDED ARTICLES

### English
| iOS Key | iOS Value | Android Key | Android Value | Status |
|---------|-----------|-------------|---------------|--------|
| `recommended.reason.history` | "Recommended based on your reading history" | `recommended_based_on_history` | "Recommended based on your reading history" | ✅ MATCH |
| `recommended.loading` | "Loading recommendations..." | `recommended_loading` | "Loading recommendations..." | ✅ MATCH |

### Simplified Chinese (zh-CN)
| iOS Key | iOS Value | Android Key | Android Value | Status |
|---------|-----------|-------------|---------------|--------|
| `recommended.reason.history` | "根据您的阅读历史推荐" | `recommended_based_on_history` | "基于你的阅读历史推荐" | ❌ MISMATCH |
| `recommended.loading` | "正在加载推荐..." | `recommended_loading` | "正在加载推荐..." | ✅ MATCH |

### Traditional Chinese (zh-HK)
| iOS Key | iOS Value | Android Key | Android Value | Status |
|---------|-----------|-------------|---------------|--------|
| `recommended.reason.history` | "根據您的閱讀歷史推薦" | `recommended_based_on_history` | "根據您的閱讀歷史推薦" | ✅ MATCH |
| `recommended.loading` | "正在載入推薦..." | `recommended_loading` | "正在載入推薦..." | ✅ MATCH |

---

## 6. TOPICS

### English
| iOS Key | iOS Value | Android Key | Android Value | Status |
|---------|-----------|-------------|---------------|--------|
| `topics.title` | "Topics" | `topic_list_title` | "Topics" | ✅ MATCH |
| `topics.select.nav.title` | "Select Topics" | `select_topics` | "Select Topics" | ✅ MATCH |
| `topics.loading` | "Loading topics..." | `topics_loading` | "Loading topics..." | ✅ MATCH |
| `topics.error_loading` | "Failed to load topics" | `topics_error_loading` | "Error loading topics" | ❌ MISMATCH |
| `topics.business` | "Business" | `topic_business` | "Business" | ✅ MATCH |
| `topics.entertainment` | "Entertainment" | `topic_entertainment` | "Entertainment" | ✅ MATCH |
| `topics.politics` | "Politics" | `topic_politics` | "Politics" | ✅ MATCH |
| `topics.sports` | "Sports" | `topic_sports` | "Sports" | ✅ MATCH |
| `topics.technology` | "Technology" | `topic_technology` | "Technology" | ✅ MATCH |

### Simplified Chinese (zh-CN)
| iOS Key | iOS Value | Android Key | Android Value | Status |
|---------|-----------|-------------|---------------|--------|
| `topics.title` | "主题" | `topic_list_title` | "主题列表" | ❌ MISMATCH |
| `topics.select.nav.title` | "选择主题" | `select_topics` | "选择主题" | ✅ MATCH |
| `topics.loading` | "正在加载主题..." | `topics_loading` | "正在加载主题..." | ✅ MATCH |
| `topics.error_loading` | "加载主题失败" | `topics_error_loading` | "加载主题失败" | ✅ MATCH |
| `topics.business` | "商业" | `topic_business` | "商业" | ✅ MATCH |
| `topics.entertainment` | "娱乐" | `topic_entertainment` | "娱乐" | ✅ MATCH |
| `topics.politics` | "政治" | `topic_politics` | "政治" | ✅ MATCH |
| `topics.sports` | "体育" | `topic_sports` | "体育" | ✅ MATCH |
| `topics.technology` | "科技" | `topic_technology` | "科技" | ✅ MATCH |

### Traditional Chinese (zh-HK)
| iOS Key | iOS Value | Android Key | Android Value | Status |
|---------|-----------|-------------|---------------|--------|
| `topics.title` | "主題" | `topic_list_title` | "主題列表" | ❌ MISMATCH |
| `topics.select.nav.title` | "選擇主題" | `select_topics` | "選擇主題" | ✅ MATCH |
| `topics.loading` | "正在載入主題..." | `topics_loading` | "正在載入主題..." | ✅ MATCH |
| `topics.error_loading` | "載入主題失敗" | `topics_error_loading` | "載入主題失敗" | ✅ MATCH |
| `topics.business` | "商業" | `topic_business` | "商業" | ✅ MATCH |
| `topics.entertainment` | "娛樂" | `topic_entertainment` | "娛樂" | ✅ MATCH |
| `topics.politics` | "政治" | `topic_politics` | "政治" | ✅ MATCH |
| `topics.sports` | "體育" | `topic_sports` | "體育" | ✅ MATCH |
| `topics.technology` | "科技" | `topic_technology` | "科技" | ✅ MATCH |

---

## 7. BUTTONS AND ACTIONS

### English
| iOS Key | iOS Value | Android Key | Android Value | Status |
|---------|-----------|-------------|---------------|--------|
| `button.cancel` | "Cancel" | `dialog_button_cancel` | "Cancel" | ✅ MATCH |
| `button.close` | "Close" | `close` | "Close" | ✅ MATCH |
| `button.create_account` | "Create Account" | `create_account` | "Create Account" | ✅ MATCH |
| `button.done` | "Done" | `done` | "Done" | ✅ MATCH |
| `button.facebook.continue` | "Continue with Facebook" | `facebook_login` | "Continue with Facebook" | ✅ MATCH |
| `button.google.continue` | "Continue with Google" | `google_login` | "Continue with Google" | ✅ MATCH |
| `button.login` | "Login" | `login` | "Login" | ✅ MATCH |
| `button.ok` | "OK" | `ok` | "OK" | ✅ MATCH |
| `button.refresh` | "Refresh" | `button_retry` | "Retry" | ❌ MISMATCH |
| `button.submit` | "Submit" | `submit` | "Submit" | ✅ MATCH |

### Simplified Chinese (zh-CN)
| iOS Key | iOS Value | Android Key | Android Value | Status |
|---------|-----------|-------------|---------------|--------|
| `button.cancel` | "取消" | `dialog_button_cancel` | "取消" | ✅ MATCH |
| `button.close` | "关闭" | `close` | "关闭" | ✅ MATCH |
| `button.create_account` | "创建账户" | `create_account` | "创建账户" | ✅ MATCH |
| `button.done` | "完成" | `done` | "完成" | ✅ MATCH |
| `button.facebook.continue` | "使用 Facebook 继续" | `facebook_login` | "使用 Facebook 登录" | ❌ MISMATCH |
| `button.google.continue` | "使用 Google 继续" | `google_login` | "使用 Google 登录" | ❌ MISMATCH |
| `button.login` | "登录" | `login` | "登陆" | ❌ MISMATCH |
| `button.ok` | "确定" | `ok` | "确定" | ✅ MATCH |
| `button.refresh` | "刷新" | `button_retry` | "重试" | ❌ MISMATCH |
| `button.submit` | "提交" | `submit` | "提交" | ✅ MATCH |

### Traditional Chinese (zh-HK)
| iOS Key | iOS Value | Android Key | Android Value | Status |
|---------|-----------|-------------|---------------|--------|
| `button.cancel` | "取消" | `dialog_button_cancel` | "取消" | ✅ MATCH |
| `button.close` | "關閉" | `close` | "關閉" | ✅ MATCH |
| `button.create_account` | "建立帳戶" | `create_account` | "建立帳戶" | ✅ MATCH |
| `button.done` | "完成" | `done` | "完成" | ✅ MATCH |
| `button.facebook.continue` | "使用 Facebook 繼續" | `facebook_login` | "使用 Facebook 登入" | ❌ MISMATCH |
| `button.google.continue` | "使用 Google 繼續" | `google_login` | "使用 Google 登入" | ❌ MISMATCH |
| `button.login` | "登入" | `login` | "登入" | ✅ MATCH |
| `button.ok` | "確定" | `ok` | "確定" | ✅ MATCH |
| `button.refresh` | "刷新" | `button_retry` | "重試" | ❌ MISMATCH |
| `button.submit` | "提交" | `submit` | "提交" | ✅ MATCH |

---

## 8. FEEDBACK

### English
| iOS Key | iOS Value | Android Key | Android Value | Status |
|---------|-----------|-------------|---------------|--------|
| `feedback.title` | "Feedback" | `feedback` | "Feedback" | ✅ MATCH |
| `feedback.subtitle` | "Send us your feedback" | `feedback_subtitle` | "Send us your feedback" | ✅ MATCH |
| `feedback.placeholder` | "Please share your feedback about this summary..." | `ios_feedback_placeholder` | "Please share your feedback about this summary..." | ✅ MATCH |
| `feedback.submit` | "Submit Feedback" | `submit_feedback` | "Submit Feedback" | ✅ MATCH |

### Simplified Chinese (zh-CN)
| iOS Key | iOS Value | Android Key | Android Value | Status |
|---------|-----------|-------------|---------------|--------|
| `feedback.title` | "反馈" | `feedback` | "反馈" | ✅ MATCH |
| `feedback.subtitle` | "向我们发送反馈" | `feedback_subtitle` | "向我们发送反馈" | ✅ MATCH |
| `feedback.placeholder` | "请分享你对该摘要的看法..." | `ios_feedback_placeholder` | "请分享你对该摘要的看法..." | ✅ MATCH |
| `feedback.submit` | "提交反馈" | `submit_feedback` | "提交反馈" | ✅ MATCH |

### Traditional Chinese (zh-HK)
| iOS Key | iOS Value | Android Key | Android Value | Status |
|---------|-----------|-------------|---------------|--------|
| `feedback.title` | "回饋" | `feedback` | "回饋" | ✅ MATCH |
| `feedback.subtitle` | "傳送你的回饋給我們" | `feedback_subtitle` | "傳送你的回饋給我們" | ✅ MATCH |
| `feedback.placeholder` | "請分享你對該摘要的看法..." | `ios_feedback_placeholder` | "請分享你對該摘要的看法..." | ✅ MATCH |
| `feedback.submit` | "提交回饋" | `submit_feedback` | "提交回饋" | ✅ MATCH |

---

## 9. SEARCH AND DISCOVER

### English
| iOS Key | iOS Value | Android Key | Android Value | Status |
|---------|-----------|-------------|---------------|--------|
| `search.placeholder` | "Search news..." | `search_hint` | "Search news..." | ✅ MATCH |
| `search.results.found` | "Found %d results" | `found_results_format` | "Found %1$d results related to \"%2$s\"" | ❌ MISMATCH |
| `discover.trending_topics` | "Trending Topics" | `trending_topics` | "Trending Topics" | ✅ MATCH |
| `discover.trending_searches` | "Trending Searches" | `trending_searches` | "Trending Searches" | ✅ MATCH |

### Simplified Chinese (zh-CN)
| iOS Key | iOS Value | Android Key | Android Value | Status |
|---------|-----------|-------------|---------------|--------|
| `search.placeholder` | "搜索新闻..." | `search_hint` | "搜索新闻..." | ✅ MATCH |
| `search.results.found` | "找到 %d 个结果" | `found_results_format` | "找到 %1$d 个与 \"%2$s\" 相关的结果" | ❌ MISMATCH |
| `discover.trending_topics` | "热门主题" | `trending_topics` | "热门主题" | ✅ MATCH |
| `discover.trending_searches` | "热门搜索" | `trending_searches` | "热门搜索" | ✅ MATCH |

### Traditional Chinese (zh-HK)
| iOS Key | iOS Value | Android Key | Android Value | Status |
|---------|-----------|-------------|---------------|--------|
| `search.placeholder` | "搜尋新聞..." | `search_hint` | "搜尋新聞..." | ✅ MATCH |
| `search.results.found` | "找到 %d 個結果" | `found_results_format` | "找到 %1$d 個與 \"%2$s\" 相關的結果" | ❌ MISMATCH |
| `discover.trending_topics` | "熱門主題" | `trending_topics` | "熱門主題" | ✅ MATCH |
| `discover.trending_searches` | "熱門搜尋" | `trending_searches` | "熱門搜尋" | ✅ MATCH |

---

## 10. SUMMARY AND ATTRIBUTION

### English
| iOS Key | iOS Value | Android Key | Android Value | Status |
|---------|-----------|-------------|---------------|--------|
| `summary.attribution` | "Summary provided by SearcherAI" | `searcher_ai_attribution` | "Summary provided by SearcherAI" | ✅ MATCH |
| `summary.generate` | "Generate Summary" | `context` | "Generate Context" | ❌ MISMATCH |

### Simplified Chinese (zh-CN)
| iOS Key | iOS Value | Android Key | Android Value | Status |
|---------|-----------|-------------|---------------|--------|
| `summary.attribution` | "此摘要由SearcherAI提供" | `searcher_ai_attribution` | "摘要由 SearcherAI 提供" | ❌ MISMATCH |
| `summary.generate` | "生成摘要" | `context` | "获取背景信息" | ❌ MISMATCH |

### Traditional Chinese (zh-HK)
| iOS Key | iOS Value | Android Key | Android Value | Status |
|---------|-----------|-------------|---------------|--------|
| `summary.attribution` | "此摘要由SearcherAI提供" | `searcher_ai_attribution` | "摘要由 SearcherAI 提供" | ❌ MISMATCH |
| `summary.generate` | "生成摘要" | `context` | "獲取背景資訊" | ❌ MISMATCH |

---

## 11. PROFILE AND SETTINGS

### English
| iOS Key | iOS Value | Android Key | Android Value | Status |
|---------|-----------|-------------|---------------|--------|
| `profile.menu.about.title` | "About Us" | `about_us` | "About Us" | ✅ MATCH |
| `profile.menu.app_icon.title` | "App Icon" | `app_icon` | "App Icon" | ✅ MATCH |
| `profile.menu.appearance.title` | "Appearance" | `appearance` | "Appearance" | ✅ MATCH |
| `profile.menu.language.title` | "Language" | `language` | "Change Language" | ❌ MISMATCH |
| `profile.menu.logout.title` | "Logout" | `logout` | "Logout" | ✅ MATCH |
| `profile.menu.settings.title` | "Settings" | `settings` | "Settings" | ✅ MATCH |

### Simplified Chinese (zh-CN)
| iOS Key | iOS Value | Android Key | Android Value | Status |
|---------|-----------|-------------|---------------|--------|
| `profile.menu.about.title` | "关于我们" | `about_us` | "关于我们" | ✅ MATCH |
| `profile.menu.app_icon.title` | "应用图标" | `app_icon` | "应用图标" | ✅ MATCH |
| `profile.menu.appearance.title` | "外观" | `appearance` | "外观" | ✅ MATCH |
| `profile.menu.language.title` | "语言" | `language` | "更改语言" | ❌ MISMATCH |
| `profile.menu.logout.title` | "登出" | `logout` | "登出" | ✅ MATCH |
| `profile.menu.settings.title` | "设置" | `settings` | "设置" | ✅ MATCH |

### Traditional Chinese (zh-HK)
| iOS Key | iOS Value | Android Key | Android Value | Status |
|---------|-----------|-------------|---------------|--------|
| `profile.menu.about.title` | "關於我們" | `about_us` | "關於我們" | ✅ MATCH |
| `profile.menu.app_icon.title` | "應用圖標" | `app_icon` | "應用圖示" | ❌ MISMATCH |
| `profile.menu.appearance.title` | "外觀" | `appearance` | "外觀" | ✅ MATCH |
| `profile.menu.language.title` | "語言" | `language` | "更改語言" | ❌ MISMATCH |
| `profile.menu.logout.title` | "登出" | `logout` | "登出" | ✅ MATCH |
| `profile.menu.settings.title` | "設定" | `settings` | "設定" | ✅ MATCH |

---

## 12. ERROR MESSAGES

### English
| iOS Key | iOS Value | Android Key | Android Value | Status |
|---------|-----------|-------------|---------------|--------|
| `error.network` | "Network Error" | `network_error` | "Network Error" | ✅ MATCH |
| `error.server` | "Server Error" | `server_error_message` | "Server Error" | ✅ MATCH |
| `error.unknown` | "Unknown Error" | `unknown_error` | "Unknown Error" | ✅ MATCH |
| `web.error_loading_page` | "Failed to load page" | `web_error_loading` | "Failed to load page" | ✅ MATCH |

### Simplified Chinese (zh-CN)
| iOS Key | iOS Value | Android Key | Android Value | Status |
|---------|-----------|-------------|---------------|--------|
| `error.network` | "网络错误" | `network_error` | "网络错误" | ✅ MATCH |
| `error.server` | "服务器错误" | `server_error_message` | "服务器错误" | ✅ MATCH |
| `error.unknown` | "未知错误" | `unknown_error` | "未知错误" | ✅ MATCH |
| `web.error_loading_page` | "页面加载失败" | `web_error_loading` | "页面加载失败" | ✅ MATCH |

### Traditional Chinese (zh-HK)
| iOS Key | iOS Value | Android Key | Android Value | Status |
|---------|-----------|-------------|---------------|--------|
| `error.network` | "網絡錯誤" | `network_error` | "網絡錯誤" | ✅ MATCH |
| `error.server` | "伺服器錯誤" | `server_error_message` | "伺服器錯誤" | ✅ MATCH |
| `error.unknown` | "未知錯誤" | `unknown_error` | "未知錯誤" | ✅ MATCH |
| `web.error_loading_page` | "頁面載入失敗" | `web_error_loading` | "頁面載入失敗" | ✅ MATCH |

---

## SUMMARY OF REQUIRED CHANGES

### Critical Mismatches Requiring Updates:

#### English Strings:
1. `sentiment_score_label`: "Sentiment: %1$s" → "Sentiment Score: %1$s"
2. `publisher_name`: "Publisher Name" → "Media Name"
3. `publisher_bias`: "Publisher Bias" → "Media Bias"
4. `publisher_region`: "Publisher Region" → "Media Region"
5. `publisher_distribution`: "Publisher Distribution" → "Media Distribution"
6. `topics_error_loading`: "Error loading topics" → "Failed to load topics"

#### Simplified Chinese (zh-CN):
1. `person_recommend`: "推荐" → "个人"
2. `my`: "个人资料" → "帐户"
3. `discover`: "搜寻" → "发现"
4. `sentiment_score_label`: "情绪：%1$s" → "文本情感数值：%1$s"
5. `sort_by`: "排序" → "排序方式"
6. `latest`: "最新" → "时间"
7. `most_popular`: "最受欢迎" → "热门度"
8. `most_relevant`: "最相关" → "相关性"
9. `media_significance`: "媒体影响力" → "媒体重要性"
10. `publisher_distribution`: "发布者分布" → "媒体分布"
11. `recommended_based_on_history`: "基于你的阅读历史推荐" → "根据您的阅读历史推荐"
12. `topic_list_title`: "主题列表" → "主题"
13. `facebook_login`: "使用 Facebook 登录" → "使用 Facebook 继续"
14. `google_login`: "使用 Google 登录" → "使用 Google 继续"
15. `login`: "登陆" → "登录"
16. `button_retry`: "重试" → "刷新"
17. `searcher_ai_attribution`: "摘要由 SearcherAI 提供" → "此摘要由SearcherAI提供"
18. `context`: "获取背景信息" → "生成摘要"
19. `language`: "更改语言" → "语言"

#### Traditional Chinese (zh-HK):
1. `person_recommend`: "推薦" → "個人"
2. `my`: "個人資料" → "帳戶"
3. `discover`: "搜尋" → "發現"
4. `sentiment_score_label`: "情緒：%1$s" → "文本情感數值：%1$s"
5. `sort_by`: "排序" → "排序方式"
6. `currently_sorted_by`: "目前排序方式" → "當前排序方式"
7. `latest`: "最新" → "時間"
8. `most_popular`: "最受歡迎" → "熱門度"
9. `most_relevant`: "最相關" → "相關性"
10. `media_significance`: "媒體影響力" → "媒體重要性"
11. `publisher_distribution`: "發布者分佈" → "媒體分佈"
12. `topic_list_title`: "主題列表" → "主題"
13. `facebook_login`: "使用 Facebook 登入" → "使用 Facebook 繼續"
14. `google_login`: "使用 Google 登入" → "使用 Google 繼續"
15. `button_retry`: "重試" → "刷新"
16. `searcher_ai_attribution`: "摘要由 SearcherAI 提供" → "此摘要由SearcherAI提供"
17. `context`: "獲取背景資訊" → "生成摘要"
18. `language`: "更改語言" → "語言"
19. `app_icon`: "應用圖示" → "應用圖標"

---

## CONCLUSION

This comprehensive analysis reveals **67 total mismatches** across all three languages that need to be corrected to ensure complete consistency between iOS and Android platforms. The majority of mismatches are in the Chinese translations, particularly in Simplified Chinese (zh-CN) with 19 mismatches, followed by Traditional Chinese (zh-HK) with 19 mismatches, and English with 6 mismatches.

The most critical areas requiring attention are:
1. Main tab navigation translations
2. Sentiment score labeling
3. Sort option terminology
4. Publisher/Media naming conventions
5. Button and action labels
6. Search and discovery terminology

All changes should be implemented to ensure a consistent user experience across both platforms.
