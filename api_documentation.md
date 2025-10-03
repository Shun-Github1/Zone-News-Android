# ZoneNews API Documentation

**Base URL**  
```
https://api.zonenews.io/dev/
```

**Note:** All backend dates use the format:  
```
YYYY-MM-DD HH:MM:SS
```

**Language Support:**  
All endpoints that return text content now support localization through an optional `lang` query parameter:
- `en-UK` (default) - English (UK)
- `zh-CN` - Simplified Chinese 
- `zh-HK` - Traditional Chinese (Hong Kong)

Language fallback:
- `en-US` → `en-UK`
- `zh-TW` → `zh-HK`

---

## Authentication

### Register
`POST /auth/register`

**Request Body**
```json
{
    "email": "user@example.com",
    "username": "username",
    "password": "password"
}
```

**Responses**
- **200 OK** – Registration successful, returns JWT Cookie  
- **401 Unauthorized** – Registration failed  
```json
{
    "msg": "Reason for failure"
}
```

---

### Login with Password
`POST /auth/login`

**Request Body**
```json
{
    "username": "username",
    "password": "password"
}
```

**Responses**
- **200 OK** – Login successful, returns JWT Cookie  
- **401 Unauthorized** – Login failed  

---

### Login with Google
`POST /auth/google-login`

### Login with Apple
`POST /auth/apple-login` 

### Logout
`POST /auth/logout`

### Refresh JWT Token
`GET /auth/refresh-token`

---

Note:
Anything beyond this point (everything except `/auth/` endpoints) will return data in the format:
```json
{
    code: 200/400/...,
    msg: "Request response message",
    data: {...} // different for each endpoint
}
```

## User Profile

### Get Browsing History / Saved Articles
`GET /profile/history`  
`GET /profile/saved`

**Query Parameters**
| Name | Type   | Required | Description                    |
|------|--------|----------|--------------------------------|
| lang | string | No       | Language code (en-UK, zh-CN, zh-HK) |

**Response**
```json
{
    "articles": [
        {
            "title": "Title",
            "pictureURL": "https://example.com/image.jpg",
            "date": "2025-08-09 14:00:00",
            "articleURL": "https://example.com/article",
            "articleID": "12345"
        }
    ]
}
```

---

### Save an Article
`POST /profile/saveadd`

**URL Parameters**
```
articleID=12345
```

---

### Delete Browsing History / Saved Article
`POST /profile/history/delete`  
`POST /profile/saved/delete`

**URL Parameters**
```
articleID=12345
```

---

### Get Personal Topics List
`GET /profile/topics`

**Query Parameters**
| Name | Type   | Required | Description                    |
|------|--------|----------|--------------------------------|
| lang | string | No       | Language code (en-UK, zh-CN, zh-HK) |

**Response**
```json
{
    "topics": [
        {"tag": "politics", "displayName": "Politics"},
        {"tag": "economics", "displayName": "Economics"},
        {"tag": "conflict", "displayName": "Conflict"}
    ]
}
```

---

### Get All Topics List
`GET /profile/listtopics`

**Query Parameters**
| Name | Type   | Required | Description                    |
|------|--------|----------|--------------------------------|
| lang | string | No       | Language code (en-UK, zh-CN, zh-HK) |

**Response**
```json
{
    "topics": [
        {"tag": "politics", "displayName": "Politics"},
        {"tag": "economics", "displayName": "Economics"},
        {"tag": "conflict", "displayName": "Conflict"},
        {"tag": "diplomacy", "displayName": "Diplomacy"},
        {"tag": "culture", "displayName": "Culture"},
        {"tag": "science", "displayName": "Science"},
        {"tag": "sports", "displayName": "Sports"},
        {"tag": "technology", "displayName": "Technology"},
        {"tag": "entertainment", "displayName": "Entertainment"}
    ]
}
```

---

### Edit Topics List
`GET /profile/edittopic`

**Query Parameters**
| Name   | Type   | Required | Description                           |
|--------|--------|----------|---------------------------------------|
| action | string | Yes      | Action to perform (ADD or DELETE)     |
| topic  | string | Yes      | Topic tag (e.g., "politics", "sports") |
| lang   | string | No       | Language code (en-UK, zh-CN, zh-HK)  |

**Example:**
```
GET /profile/edittopic?action=ADD&topic=politics&lang=en-UK
```

**Response**
- **200 OK** – Topic list updated
- **400 Bad Request** – Invalid topic tag or action  

---

### Get Profile Information
`GET /profile`

**Response**
```json
{
    "profileID": "user_123",
    "profileIcon": "https://example.com/avatar.jpg"
}
```

---

### Publisher Region Management
`GET /profile/publisher-region`  
`POST /profile/publisher-region`

#### GET Request

**Query Parameters**
| Name | Type   | Required | Description                    |
|------|--------|----------|--------------------------------|
| lang | string | No       | Language code (en-UK, zh-CN, zh-HK) |

**Response**
```json
{
    "regions": [
        {"tag": "hk", "displayName": "Hong Kong SAR"},
        {"tag": "china", "displayName": "China"},
        {"tag": "uk", "displayName": "United Kingdom"},
        {"tag": "usa", "displayName": "United States of America"},
        {"tag": "asia-others", "displayName": "Asia (others)"},
        {"tag": "europe-others", "displayName": "Europe (others)"}
    ],
    "selected": ["hk", "china", "uk"]
}
```

#### POST Request

**Query Parameters**
| Name   | Type   | Required | Description                           |
|--------|--------|----------|---------------------------------------|
| action | string | Yes      | Action to perform (ADD or REMOVE)     |
| tag    | string | Yes      | Region tag (e.g., "hk", "china", "asia-others") |
| lang   | string | No       | Language code (en-UK, zh-CN, zh-HK)  |

**Example:**
```
POST /profile/publisher-region?action=ADD&tag=asia-others&lang=en-UK
```

**Response**
- **200 OK** – Region selection updated
- **400 Bad Request** – Invalid region tag or action

---

## Feeds

### Get Home Feed
`GET /feed`

**Query Parameters**
| Name   | Type    | Required | Description                |
|--------|--------|----------|----------------------------|
| tag    | string | No       | Filter by tag              |
| offset | int    | No       | Article offset (default 0) |
| limit  | int    | No       | Article limit (max 10)     |
| lang   | string | No       | Language code (en-UK, zh-CN, zh-HK) |

`tag` attribute should be the tab of the home page.
Today -> today
Hong Kong -> hk
China -> china

**Response**
```json
{
    "articles": [
        {
            "title": "Title",
            "pictureURL": "https://example.com/image.jpg",
            "date": "2025-08-09 14:00:00",
            "articleURL": "https://example.com/article",
            "articleID": "12345",
            "coverage": {
                "centric": 0.6,
                "progressive": 0.4
            },
            "metrics": {
                "sentiment": 0.5,
                "subjectivity": -0.8
            },
            "region": "US",
            "sector": "Politics",
            "nSources": 2
        }
    ],
    "headlines": [
        {
            "title": "Headline Title",
            "pictureURL": "https://example.com/image.jpg",
            "date": "2025-08-09 14:00:00",
            "articleURL": "https://example.com/article",
            "articleID": "67890",
            "description": "Short summary"
        }
    ]
}
```
Note: `"sentiment"` and `"subjectivity"` are decimal metrics that span from -1.0 to 1.0
The same is true for any mention of these variables in other endpoints.

---

### Get Personalized Feed
`GET /feed/personal`

**Query Parameters**
| Name   | Type    | Required | Description                |
|--------|--------|----------|----------------------------|
| offset | int    | No       | Article offset (default 0) |
| limit  | int    | No       | Article limit (default 10) |
| lang   | string | No       | Language code (en-UK, zh-CN, zh-HK) |
| sortby | string | No       | Sort order: `latest`, `popular`, or `relevant` |

**Example:**
```
GET /feed/personal?offset=0&limit=10&lang=en-UK&sortby=latest
```

**Response**
- **200 OK** – Feed retrieved successfully
- **400 Bad Request** – Invalid sortby parameter

(Same response format as `/feed`)

---

### Get Trending Topics
`GET /feed/trending-topics`

**Query Parameters**
| Name | Type   | Required | Description                    |
|------|--------|----------|--------------------------------|
| lang | string | No       | Language code (en-UK, zh-CN, zh-HK) |

**Response**
```json
{
    "topics": [
        {"tag": "politics", "displayName": "Politics"},
        {"tag": "technology", "displayName": "Technology"},
        {"tag": "sports", "displayName": "Sports"},
        {"tag": "culture", "displayName": "Culture"}
    ]
}
```

**Example:**
```
GET /feed/trending-topics?lang=zh-CN
```

Returns 3-6 randomly selected trending topics with localized display names.

---

## Articles

### Get Article
`GET /article/{id OR title}`

**Query Parameters**
| Name | Type   | Required | Description                    |
|------|--------|----------|--------------------------------|
| lang | string | No       | Language code (en-UK, zh-CN, zh-HK) |

**Response**
```json
{
    "title": "Article Title",
    "pictureURL": "https://example.com/image.jpg",
    "date": "2025-08-09 14:00:00",
    "articleID": "12345",
    "shareURL": "https://example.com/share",
    "description": {
        "synopsis": "Brief summary of the main events",
        "implications": "Analysis of potential consequences and broader impact"
    },
    "coverage": {
        "percentage": {
            "centric": 0.6,
            "progressive": 0.4
        },
        "icons": {
		  "centric": [
			{ "size": 0.5, "rx": 0.5, "ry": 0.8, "logo": "logo URL" }
		  ],
		  "progressive": [
			{ "size": 0.5, "rx": 0.2, "ry": 0.8, "logo": "logo URL" }
		  ]
		}
    },
    "metrics": {
        "sentiment": -0.5,
        "subjectivity": -0.8
    },
    "articles": [
        {
            "publisherName": "Publisher",
            "publisherIcon": "https://example.com/logo.png",
            "title": "Article Title",
            "articleURL": "https://example.com/article",
            "publisherStance": {
                "tag": "p",
                "displayName": "Progressive"
            },
            "mediaSignificance": 4,
            "bias": 6,
            "publisherRegion": "US"
        }
    ],
    "relatedTopics": ["Politics", "Economy"],
    "relatedArticles": [],
    "liked": true
}
```

---

### Article Feedback
`POST /article/{id OR title}/feedback`

**Request Body**
```json
{
    "content": "Feedback text"
}
```

---

## Search

### Search Articles
`GET /search?q={query}`

**Query Parameters**
| Name | Type   | Required | Description                    |
|------|--------|----------|--------------------------------|
| q    | string | Yes      | Search query                   |
| lang | string | No       | Language code (en-UK, zh-CN, zh-HK) |

**Response**
```json
{
    "articles": [
        {
            "title": "Search Result Title",
            "pictureURL": "https://example.com/image.jpg",
            "articleURL": "https://example.com/article"
        }
    ]
}
```

---

### Get Trending Search Articles
`GET /search/trending`

**Query Parameters**
| Name | Type   | Required | Description                    |
|------|--------|----------|--------------------------------|
| lang | string | No       | Language code (en-UK, zh-CN, zh-HK) |

(Same response format as `/search?q={}`)

---

## Other Endpoints

### Notifications
`POST /notifications`  
Uses Firebase Cloud Messaging (FCM) to send notifications.

---

### Share Tracking
`POST /track/action`

**Request Body**
```json
{
    "articleID": "12345"
}
```

---

### About Us
`GET /info/aboutus`

**Query Parameters**
| Name | Type   | Required | Description                    |
|------|--------|----------|--------------------------------|
| lang | string | No       | Language code (en-UK, zh-CN, zh-HK) |

**Response**
```json
{
    "content": "About us text..."
}
```
(Content may contain clickable URLs and will be returned in the requested language.)
