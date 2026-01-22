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
- **409 Conflict** – Registration failed  
```json
{
    "msg": "Email already registered or Username already taken"
}
```

*comments*
- verify user email (handled by backend)
- password should have a minimum length and contain numbers 
  and/or special characters (checked by backend or frontend?)
- desktop site has a 'confirm password' that's checked by frontend

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
- **200 OK** – Login successful, returns csrf token
- **400 BAD REQUEST** - Missing username or password
- **401 Unauthorized** – Login failed (incorrect username or password)

---

### Login with Firebase (includes Google, Facebook and Apple)
`POST /auth/firebase`

**Request Body**
```json
{
    "idToken": "firebase_id_token"
}

```

**Responses**
- **200 OK** – Login successful, returns csrf token  
```
{
    "msg": "Login successful",
    "code": 200,
    "data": {
        "csrf_token": "csrf_token_value"
    }
}
```
- **401 Unauthorized** – Login failed due to an invalid or expired Firebase token. 
```
{
    "msg": "Invalid Firebase token",
    "code": 401
}
```
- **409 Conflict** - email is already associated with a manual (username + password) account.
```
{
    "msg": "An account already exists with this email. Please log in with your username and password.",
    "code": 409
}
```
❌ OLD behavior (removed)

Reject login if the same email was used with a different Firebase provider

✅ NEW behavior (current)

Multiple Firebase providers are automatically linked

Users can log in with Google, Facebook, or Apple interchangeably

The same account and data are preserved

👉 Clients must handle Firebase provider linking
when Firebase raises:

auth/account-exists-with-different-credential

🔧 Client-side requirement (MANDATORY)

When receiving auth/account-exists-with-different-credential from Firebase:

Fetch existing sign-in methods:

fetchSignInMethodsForEmail(auth, email)


Sign in with the existing provider

Call:

linkWithCredential(user, pendingCredential)


Obtain a fresh idToken

Send it to POST /auth/firebase

❗ Do NOT block login or show a permanent error in this case.

---

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

### Get Profile Information
`GET /profile`

**Response**
```json
{
    "authMethod": "account",
    "email": "test001@zonenews.io",
    "isPro": false,
    "language": "zh-CN",
    "profileIcon": "https://api.zonenews.io/dev/img/icon?fn=0",
    "username": "test001"
}
```

*comments*
- authMethod is either 'account' or 'firebase'

---

### Change language
`PUT /profile/language`

**Request Body**
```json
{
    "language": "chosen language",
}
```

**Responses**
- **200 OK** – Language updated
- **400 BAD REQUEST** - Language not in *(en-UK, zh-HK or zh-CN)*
- **500 Internal Server Error** – Database error

---

### Redeem Code for Zone News Pro
`POST /profile/redeem`

**Request Body**
```json
{
    "code": "dgH9pxAz2LmCEA3l",
}
```

**Responses**
- **200 OK** – Account Type updated
- **400 BAD REQUEST** - Invalid code
- **500 Internal Server Error** – Database error

---

### Cancel Pro Subscription
`POST /profile/cancelsubscription`

**Responses**
- **200 OK** – Pro Subscription cancelled
- **500 Internal Server Error** – Database error

---

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

### Append article to reading history
`POST /profile/reading-history`

**Request Body**
```json
{
    "article_id": "articleID",
}
```

*comments*
- date & time of reading the article will be logged automatically
- return reading history sorted by most recently opened first

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
`POST /profile/edittopic`

**Query Parameters**
| Name   | Type   | Required | Description                           |
|--------|--------|----------|---------------------------------------|
| action | string | Yes      | Action to perform (ADD or DELETE)     |
| topic  | string | Yes      | Topic tag (e.g., "politics", "sports") |
| lang   | string | No       | Language code (en-UK, zh-CN, zh-HK)  |

**Example:**
```
POST /profile/edittopic?action=ADD&topic=politics&lang=en-UK
```

**Response**
- **200 OK** – Topic list updated
- **400 Bad Request** – Invalid topic tag or action  

---

### Get All Sectors List
`GET /profile/sectors`

**Query Parameters**
| Name | Type   | Required | Description                    |
|------|--------|----------|--------------------------------|
| lang | string | No       | Language code (en-UK, zh-CN, zh-HK) |

**Response**
```json
{
    "sectors": [
        {"tag": "politics", "displayName": "Politics"},
        {"tag": "economics", "displayName": "Economics"},
        {"tag": "conflict", "displayName": "Conflict"},
        {"tag": "diplomacy", "displayName": "Diplomacy"},
        {"tag": "culture", "displayName": "Culture"},
        {"tag": "science", "displayName": "Science"},
        {"tag": "sports", "displayName": "Sports"},
        {"tag": "technology", "displayName": "Technology"},
        {"tag": "entertainment", "displayName": "Entertainment"},
        {"tag": "military", "displayName": "Military"},
        {"tag": "current-affairs", "displayName": "Current Affairs"}
    ]
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

### Account deletion
`POST /profile/delete-account`

**Request Body**
```json
{
    "password": "current_password"
}
OR 
{
    "idToken": "fresh_firebase_id_token"
}
(accounts and firebase)
```

**Response**
- **200 OK** – Account deleted successfully
- **400 BAD REQUEST** - missing password or firebase idToken 
- **401 Unauthorized** - missing CSRF, invalid JWT, wrong password, invalid idToken
- **500 Internal Server Error** – Database failure during detection

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
            "articleID": "7c3b1d9f-3b",
            "articleURL": "https://api.zonenews.io/dev/article/7c3b1d9f-3b",
            "date": "yyyy-mm-ddThh:mm:ss",
            "description": "Article description",
            "metrics": {
                "sentiment": 0.12
            },
            "nSources": 2,
            "pictureURL": "picturelink.com",
            "region": "Region",
            "sector": "Sector",
            "title": "Article title"
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
| limit  | int    | No       | Article limit (default 20) |
| lang   | string | No       | Language code (en-UK, zh-CN, zh-HK) |
| sortby | string | No       | Sort order: `latest`, `popular`, or `relevant` |

**Example:**
```
GET /feed/personal?offset=0&limit=20&lang=en-UK&sortby=latest
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
    "sector": "SomeSector",
    "region": "Region",
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
            "publisherID": 99,
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
    "relatedTopics": [
        {
            "displayName": "You Meinu",
            "tag": "you-meinu"
        },
        {
            "displayName": "Breach of Trust",
            "tag": "breach-of-trust"
        }
    ],
    "relatedArticles": []
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
| Name   | Type   | Required | Description                                              |
| ------ | ------ | -------- | -------------------------------------------------------- |
| q      | string | Yes      | Search query                                             |
| lang   | string | No       | Language code (`en-UK`, `zh-CN`, `zh-HK`)                |
| page   | int    | No       | Page number (default = `1`)                              |
| limit  | int    | No       | Results per page (default = `15`)                        |
| sortby | string | No       | Sort order (`latest` *(default)*, `popular`, `relevant`) |

*note: `latest` (default), `popular`, or `relevant`

**Response**
```json
{
    "articles": [
        {
            "title": "Search Result Title",
            "articleID": "articleID",
            "pictureURL": "https://example.com/image.jpg",
            "articleURL": "https://example.com/article",
            "date": "yyyy-mm-ddThh:mm:ss",
            "description": "Article description",
            "nSources": "number of sources",
            "region": "region",
            "sector": "sector",
            "coverage": {
                "centric": 0.0,
                "progressive": 1.0
            },
            "metrics": {
                "sentiment": 0.12,
                "subjectivity": 0.41
            }
        }
    ],
    "meta": {
        "page": 1,
        "limit": 15,
        "total": 128,
        "totalPages": 9
    }
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

## Info

### Publisher Information
`GET /info/publisher/{id}`

**Query Parameters**
| Name | Type   | Required | Description                    |
|------|--------|----------|--------------------------------|
| lang | string | No       | Language code (en-UK, zh-CN, zh-HK) |

**Reseponse**
```json
{
    "conglomerate": "Nikkei Inc.",
    "controller": "Nikkei Inc.",
    "id": 33,
    "intro": "London-based global financial newspaper, pro-globalization and pro-free-market.",
    "name": "Financial Times",
    "region": "United Kingdom",
    "stance": {
        "displayName": "Progressive",
        "tag": "p"
    },
    "type": "Corporate",
    "website": "www.ft.com"
},
```

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

*** Website Only ***

- /feed/topic/{topic}
- /feed/latest (region, sector, title, sentiment, dateTime, image)
- /feed/mostread (region, sector, title, sentiment)





