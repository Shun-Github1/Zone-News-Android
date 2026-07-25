# ZoneNews App-Backend Integration Documentation

This document describes how the ZoneNews Android app communicates with the backend APIs. It outlines the core components, authentication management, and details the implementation for four core functionalities: **Login**, **Default Feed Retrieval**, **Personalized Feed Retrieval**, and **Tagging/Topics Management**.

---

## 1. Architecture Overview & Core Components
The network layer of the app is built using **Retrofit 2**, **OkHttp 3**, and **Dagger Hilt** for dependency injection.

### Key API & Network Files
*   **[Constants.kt](file:///c:/Users/ShunKwok/Downloads/Zonenews/app/src/main/java/com/searcher/zonenews/utils/Constants.kt)**:
    *   Defines the base API URL: `COMMON_URL = "https://api.zonenews.io/dev/"`.
    *   Specifies other constant values like query params (`ADD`/`DELETE` tags, `latest`/`popular`/`relevant` sorting, and locale constants like `en-UK`, `zh-CN`, `zh-HK`).
*   **[AppHttpService.kt](file:///c:/Users/ShunKwok/Downloads/Zonenews/app/src/main/java/com/searcher/zonenews/net/AppHttpService.kt)**:
    *   The primary Retrofit interface listing all API endpoints, parameter mappings (using `@Path`, `@Query`, `@Body`), and response return models.
*   **[NetworkModule.kt](file:///c:/Users/ShunKwok/Downloads/Zonenews/app/src/main/java/com/searcher/zonenews/utils/network/NetworkModule.kt)**:
    *   Dagger Hilt Module providing singleton instances of `OkHttpClient`, `Retrofit`, and `AppHttpService`.
    *   Configures OkHttp timeouts, SSL trust (accepts all for dev environment), and interceptors (logging, cookie/session management, and CSRF token handling).
*   **[PersistentCookieJar.kt](file:///c:/Users/ShunKwok/Downloads/Zonenews/app/src/main/java/com/searcher/zonenews/utils/network/PersistentCookieJar.kt)**:
    *   Custom implementation of OkHttp's `CookieJar` interface.
    *   Intercepts cookies from response headers and persists them in local `SharedPreferences` (`"cookie_prefs"`).
    *   Handles retrieving valid session cookies and extracting the CSRF token.
*   **[NetworkResponse.kt](file:///c:/Users/ShunKwok/Downloads/Zonenews/app/src/main/java/com/searcher/zonenews/utils/network/exception/NetworkResponse.kt)**:
    *   Defines a sealed class `NetworkResponse<out T : Any, out U : Any>` wrapping responses into `Success`, `NetError`, and `UnknownError` states.
*   **[GenericResponse.kt](file:///c:/Users/ShunKwok/Downloads/Zonenews/app/src/main/java/com/searcher/zonenews/utils/network/exception/GenericResponse.kt)**:
    *   Defines the typealias `typealias GenericResponse<S> = NetworkResponse<S, HttpError>` used as a return wrapper in Retrofit endpoints.

### CSRF & Session Management
The backend uses cookie-based sessions (typically via Flask-JWT-Extended) and requires a CSRF token for any modifying requests (`POST`, `PUT`, `DELETE`).
1.  **Session Cookies**: During login, response `Set-Cookie` headers are saved automatically by [PersistentCookieJar.kt](file:///c:/Users/ShunKwok/Downloads/Zonenews/app/src/main/java/com/searcher/zonenews/utils/network/PersistentCookieJar.kt). OkHttp automatically attaches these cookies in subsequent requests to the API domain.
2.  **CSRF Headers**: The interceptor inside `provideUserOkHttpClient` checks if the request method is modifying (`POST`, `PUT`, `DELETE`). If so, it extracts the CSRF token from the persisted cookies using `cookieJar.getCsrfToken()` and appends it to the outgoing headers as `X-CSRF-Token`.

---

## 2. Core Functionalities Details

### A. Authentication & Login
The application supports manual registration, username/password login, and Firebase authentication (for Google, Facebook, Apple).

#### 1. Manual Registration
*   **Endpoint**: `POST auth/register`
*   **Request Body**:
    ```json
    {
        "email": "user@example.com",
        "username": "username",
        "password": "password"
    }
    ```
*   **Repository Implementation**: `registerApp(email, userName, passWord)` in [LoginRepository.kt](file:///c:/Users/ShunKwok/Downloads/Zonenews/app/src/main/java/com/searcher/zonenews/repository/LoginRepository.kt) creates a JSON body and calls `appHttpService.registerApp(...)`. Returns a `GenericResponse<CommonResponseEntry>`.

#### 2. Manual Login
*   **Endpoint**: `POST auth/login`
*   **Request Body**:
    ```json
    {
        "username": "username",
        "password": "password"
    }
    ```
*   **Repository Implementation**: `loginApp(name, pass)` in [LoginRepository.kt](file:///c:/Users/ShunKwok/Downloads/Zonenews/app/src/main/java/com/searcher/zonenews/repository/LoginRepository.kt) triggers `appHttpService.loginApp(requestBody)`. Returns `GenericResponse<LoginEntry>`.

#### 3. Firebase Social Login
*   **Endpoint**: `POST auth/firebase`
*   **Request Body**:
    ```json
    {
        "idToken": "firebase_id_token"
    }
    ```
*   **Repository Implementation**: `loginWithFirebase(idToken)` in [LoginRepository.kt](file:///c:/Users/ShunKwok/Downloads/Zonenews/app/src/main/java/com/searcher/zonenews/repository/LoginRepository.kt). Returns `GenericResponse<LoginEntry>`.

#### 4. Mandatory Client Requirement: Firebase Provider Linking
When authentication fails because an email is registered with a different Firebase credential, Firebase triggers: `auth/account-exists-with-different-credential`. Clients must handle this dynamically:
1.  Intercept the error and fetch existing sign-in methods using `fetchSignInMethodsForEmail(auth, email)`.
2.  Authenticate with the existing credential.
3.  Call `linkWithCredential(user, pendingCredential)` to merge accounts.
4.  Acquire a fresh Firebase ID Token and send it via `POST auth/firebase`.

#### 5. Logout & Session Invalidation
*   **Endpoint**: `POST auth/logout`
*   **Implementation**: `outLoginApp()` in [LoginRepository.kt](file:///c:/Users/ShunKwok/Downloads/Zonenews/app/src/main/java/com/searcher/zonenews/repository/LoginRepository.kt) sends the logout request and calls `cookieJar.clearCookies()` to wipe all in-memory and persisted `cookie_prefs` in `SharedPreferences`.

---

### B. Default Feed Retrieval
Fetches news articles for the default/home feed based on tab filtering.

1.  **Endpoint**:
    *   `GET feed`
2.  **Parameters**:
    *   `tag` (String, Optional): Filter by page tab ("today", "hk", "china").
    *   `offset` (Int, Optional): Number of articles to skip.
    *   `limit` (Int, Optional): Max number of articles to return.
    *   `lang` (String, Optional): Localization language (e.g., `en-UK`, `zh-CN`, `zh-HK`).
3.  **Repository Calls ([HomeRepository.kt](file:///c:/Users/ShunKwok/Downloads/Zonenews/app/src/main/java/com/searcher/zonenews/repository/HomeRepository.kt))**:
    *   `queryHomeData(tag, pageNo, pageSize, language)`: Calculates zero-based offset: `(pageNo - 1) * pageSize` and triggers `appHttpService.getHomeData(...)`.
4.  **ViewModel Handling ([HomeModel.kt](file:///c:/Users/ShunKwok/Downloads/Zonenews/app/src/main/java/com/searcher/zonenews/model/HomeModel.kt))**:
    *   Calls `getHomeDataList(tag, pageNo, pageSize)`.
    *   Translates UI tab names (localized variations like "Today", "香港", "中國") to the standard API tags using `convertTabNameToApiTag`.
    *   Exposes a `LiveData<HomeDataListEntry>` observed by [HomeChildFrag.kt](file:///c:/Users/ShunKwok/Downloads/Zonenews/app/src/main/java/com/searcher/zonenews/ui/mainfrag/homechild/HomeChildFrag.kt).
5.  **Data Models**:
    *   **[HomeDataListEntry.java](file:///c:/Users/ShunKwok/Downloads/Zonenews/app/src/main/java/com/searcher/zonenews/entry/HomeDataListEntry.java)**: Contains a `DataDTO` with lists of `articles` and `headlines` (banner news). Each article entity has metadata like `centric`/`progressive` stance metrics, `sentiment`/`subjectivity` values, and `publisherRegion`/`sector`.

---

### C. "Personal" Page Content Retrieval
Retrieves personalized news feed recommendations based on user interests.

1.  **Endpoint**:
    *   `GET feed/personal`
2.  **Parameters**:
    *   `offset` (Int): Calculated pagination offset `(pageNo - 1) * pageSize`.
    *   `limit` (Int): Size of pages.
    *   `lang` (String, Optional): Current locale.
    *   `sortby` (String, Optional): Sort order (`latest`, `popular`, or `relevant`).
3.  **Repository & ViewModel Calls**:
    *   **[PersonRecommendRepository.kt](file:///c:/Users/ShunKwok/Downloads/Zonenews/app/src/main/java/com/searcher/zonenews/repository/PersonRecommendRepository.kt)**: `queryRecommendList(pageNo, pageSize, language, sortBy)`.
    *   **[PersonRecommendModel.kt](file:///c:/Users/ShunKwok/Downloads/Zonenews/app/src/main/java/com/searcher/zonenews/model/PersonRecommendModel.kt)**: Exposes `recommendListEntry: LiveData<SearchListEntry>`. Exposes error states under code `1000`.
4.  **UI Observation**:
    *   Observed by **[YourFeedFragment.kt](file:///c:/Users/ShunKwok/Downloads/Zonenews/app/src/main/java/com/searcher/zonenews/ui/mainfrag/YourFeedFragment.kt)** to build the user's primary personalized feed screen.
5.  **Data Models**:
    *   **[SearchListEntry.java](file:///c:/Users/ShunKwok/Downloads/Zonenews/app/src/main/java/com/searcher/zonenews/entry/SearchListEntry.java)**: Contains lists of personal recommendation articles along with pagination metadata (`page`, `limit`, `total`, `totalPages`).

---

### D. Tagging & Topics Management
Users can customize topics of interest, which dynamically affects their personalized feed. Additionally, users can retrieve feeds specific to individual topic tags.

#### 1. Topic Preferences Configuration
*   **Get user followed topics**: `GET profile/topics` (Returns followed topics).
*   **Get all available topics**: `GET profile/listtopics` (Returns system topics).
*   **Modify followed topics**: `POST profile/edittopic` with parameters `action` ("ADD" or "DELETE") and `topic` (tag ID).

#### 2. Optimistic UI Updates
To provide an instantaneous experience, `TopicModel.kt` executes `updateMyTopicsOptimistically(...)` which updates followed topics in memory. The UI immediately reflects the new selection state (following/not following) before the backend API call completes.

#### 3. Topic-Specific Feed Retrieval
When a user taps an individual topic tag in the feed list, the app opens a topic-specific feed overlay (**[TagNewsBottomSheetFragment.kt](file:///c:/Users/ShunKwok/Downloads/Zonenews/app/src/main/java/com/searcher/zonenews/ui/newsdetail/TagNewsBottomSheetFragment.kt)**).
*   **Endpoint**: `GET feed/topic/{topic}`
*   **Repository Implementation**: `queryFeedByTopic(topic, pageNo, pageSize, language)` in [HomeRepository.kt](file:///c:/Users/ShunKwok/Downloads/Zonenews/app/src/main/java/com/searcher/zonenews/repository/HomeRepository.kt).
*   **ViewModel Invocation**: `homeModel.getDataByTopicTag(tagApiId, pageNo, pageSize)` in [HomeModel.kt](file:///c:/Users/ShunKwok/Downloads/Zonenews/app/src/main/java/com/searcher/zonenews/model/HomeModel.kt).
*   **Server Fallback Prevention**: If the requested tag returns a payload containing `headlines`, the server has likely defaulted back to the generic "Today" feed because the topic tag did not match or was empty. The `HomeModel` intercepts this by checking `!response.data?.headlines.isNullOrEmpty()` and replaces it with an empty feed, preventing generic content replacement.

#### 4. Data Models**:
*   **[TopicListEntry.java](file:///c:/Users/ShunKwok/Downloads/Zonenews/app/src/main/java/com/searcher/zonenews/entry/TopicListEntry.java)**: Encapsulates list of `TopicDTO` items, containing `tag` and `displayName`.
*   **[CommonResponseEntry.java](file:///c:/Users/ShunKwok/Downloads/Zonenews/app/src/main/java/com/searcher/zonenews/entry/CommonResponseEntry.java)**: Holds standard success/error return messages from mutating operations.

---

## 3. Existing API & Network Files Cheat Sheet
Below are the primary files governing the API integration within the Android codebase:

| Component | Absolute File Path | Description |
| :--- | :--- | :--- |
| **API Endpoints Interface** | [AppHttpService.kt](file:///c:/Users/ShunKwok/Downloads/Zonenews/app/src/main/java/com/searcher/zonenews/net/AppHttpService.kt) | Retrofit API method signatures and parameter mappings. |
| **Network Client Module** | [NetworkModule.kt](file:///c:/Users/ShunKwok/Downloads/Zonenews/app/src/main/java/com/searcher/zonenews/utils/network/NetworkModule.kt) | Setup for `OkHttpClient`, SSL Trust, CSRF headers, and Retrofit. |
| **Session Cookie Store** | [PersistentCookieJar.kt](file:///c:/Users/ShunKwok/Downloads/Zonenews/app/src/main/java/com/searcher/zonenews/utils/network/PersistentCookieJar.kt) | Manages cookie storage in `SharedPreferences` and CSRF extraction. |
| **General Constants** | [Constants.kt](file:///c:/Users/ShunKwok/Downloads/Zonenews/app/src/main/java/com/searcher/zonenews/utils/Constants.kt) | Host URL (`COMMON_URL`), codes, types, and language tag lists. |
| **Response Envelope Wrapper** | [GenericResponse.kt](file:///c:/Users/ShunKwok/Downloads/Zonenews/app/src/main/java/com/searcher/zonenews/utils/network/exception/GenericResponse.kt) | Wraps calls using `NetworkResponse` and `HttpError`. |
| **Login Repository** | [LoginRepository.kt](file:///c:/Users/ShunKwok/Downloads/Zonenews/app/src/main/java/com/searcher/zonenews/repository/LoginRepository.kt) | Handles manual/Firebase logins, registration, and logouts. |
| **Home/Feed Repository** | [HomeRepository.kt](file:///c:/Users/ShunKwok/Downloads/Zonenews/app/src/main/java/com/searcher/zonenews/repository/HomeRepository.kt) | Handles offset calculation, home feed, and tag-specific feeds. |
| **Recommend Repository** | [PersonRecommendRepository.kt](file:///c:/Users/ShunKwok/Downloads/Zonenews/app/src/main/java/com/searcher/zonenews/repository/PersonRecommendRepository.kt) | Intermediary for personalized feed recommendation retrieval. |
| **Topic/Tag Repository** | [TopicRepository.kt](file:///c:/Users/ShunKwok/Downloads/Zonenews/app/src/main/java/com/searcher/zonenews/repository/TopicRepository.kt) | Fetches and manages followed/system topic tags. |
| **Entry Serialization Models** | [entry directory](file:///c:/Users/ShunKwok/Downloads/Zonenews/app/src/main/java/com/searcher/zonenews/entry) | Contains model serialization entries like `LoginEntry`, `HomeDataListEntry`, `SearchListEntry`, `TopicListEntry`. |
