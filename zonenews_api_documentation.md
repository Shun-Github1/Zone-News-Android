# ZoneNews API Documentation

**Base URL**  
```
https://api.zonenews.io/dev/
```

**Note:** All backend dates use the format:  
```
YYYY-MM-DD HH:MM:SS
```

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

**Response**
```json
{
    "topics": ["Politics", "Technology"]
}
```

---

### Get All Topics List
`GET /profile/listtopics`  
(Same response format as `/profile/topics`)

---

### Edit Topics List
`GET /profile/edittopic`

**URL Parameters**
```
action=ADD|DELETE
topic="Topic Name"
```

**Response**
- **200 OK** – Topic list updated  

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

## Feeds

### Get Home Feed
`GET /feed`

**Query Parameters**
| Name   | Type    | Required | Description                |
|--------|--------|----------|----------------------------|
| tag    | string | No       | Filter by tag              |
| offset | int    | No       | Article offset (default 0) |
| limit  | int    | No       | Article limit (max 10)     |

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
```
offset=0
limit=0
```
(Same response format as `/feed`)

---

## Articles

### Get Article
`GET /article/{id OR title}`

**Response**
```json
{
    "title": "Article Title",
    "pictureURL": "https://example.com/image.jpg",
    "date": "2025-08-09 14:00:00",
    "articleID": "12345",
    "shareURL": "https://example.com/share",
    "description": "Summary",
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
            "publisherStance": "progressive",
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

**Response**
```json
{
    "content": "About us text..."
}
```
(Content may contain clickable URLs.)
