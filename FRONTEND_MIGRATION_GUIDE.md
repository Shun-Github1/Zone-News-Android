# Frontend Migration Guide

## Overview

The ZoneNews API has been updated to use a standardized error response format. This guide explains the changes and provides migration examples for Android (Kotlin), iOS (Swift), and Web (JavaScript) developers.

---

## What Changed?

### Response Format Changes

#### **Error Responses (4xx, 5xx)**

**Before:**
```json
{
  "code": 400,
  "msg": "Email already registered",
  "data": {}
}
```

**After:**
```json
{
  "code": 400,
  "msg": "Error",
  "data": {
    "msg": "Email already registered",
    "code": "AUE06"
  }
}
```

#### **Success Responses (200, 201)**

**Before:**
```json
{
  "code": 200,
  "msg": "Login successful",
  "data": {
    "csrf_token": "..."
  }
}
```

**After:**
```json
{
  "code": 200,
  "msg": "Success",
  "data": {
    "csrf_token": "..."
  }
}
```

### Key Changes

1. **Root-level `msg` field**: Now always either `"Error"` or `"Success"` - **do not** use this for displaying user messages
2. **Error messages moved**: Actual error messages are now in `data.msg` for errors
3. **Error codes added**: Each error now has a unique code in `data.code` (e.g., `"AUE06"`)
4. **Success responses unchanged**: Data structure remains the same for successful responses

---

## Migration Steps

### 1. Update Error Handling

**Before**, you checked `response.msg` for the error message:
- ❌ `response.msg` → "Email already registered"

**Now**, check `response.data.code` for error type and `response.data.msg` for the message:
- ✅ `response.data.code` → "AUE06"
- ✅ `response.data.msg` → "Email already registered"

### 2. Update Success Detection

**Before**: Check if `response.msg` contains "success" (case-insensitive)

**After**: Check if `response.code === 200` (or other 2xx status codes)

**Note**: Always use HTTP status code for success detection - it's computationally faster than string comparison.

### 3. Use Error Codes for Conditional Logic

**Before**: String matching on error messages
```javascript
if (response.msg.includes("already registered")) {
  // Handle duplicate email
}
```

**After**: Check error codes
```javascript
if (response.data.code === "AUE06") {
  // Handle duplicate resource (email/username)
}
```

---

## Platform-Specific Examples

### Android (Kotlin)

#### Define Response Models

```kotlin
// Updated response models
data class ApiResponse<T>(
    val code: Int,
    val msg: String,  // Always "Success" or "Error"
    val data: T
)

// For error responses
data class ErrorData(
    val msg: String,  // Actual error message
    val code: String  // Error code like "AUE06"
)

// For successful data responses (example)
data class LoginData(
    val csrf_token: String
)
```

#### Update Network Layer

```kotlin
// Before
fun handleResponse(response: ApiResponse<Any>) {
    if (response.code == 200) {
        showToast(response.msg)  // ❌ Shows "Login successful"
    } else {
        showError(response.msg)  // ❌ Shows "Email already registered"
    }
}

// After
fun handleResponse(response: ApiResponse<Any>) {
    if (response.code == 200) {  // ✅ Use HTTP status code
        // Success - data is in response.data
        // For user messages, use localized strings based on context
        showToast(R.string.login_success)
    } else {
        // Error - actual message is in response.data
        val errorData = Gson().fromJson(
            Gson().toJson(response.data),
            ErrorData::class.java
        )
        handleError(errorData)
    }
}

fun handleError(error: ErrorData) {
    // Option 1: Display the error message from API
    showError(error.msg)

    // Option 2: Use error code for localized messages (Recommended)
    // Check the last 2 characters for consistent error types
    val localizedMessage = when (error.code.takeLast(2)) {
        "00" -> getString(R.string.error_invalid_format)      // Invalid data format
        "01" -> getString(R.string.error_missing_parameter)   // Missing required parameter
        "02" -> getString(R.string.error_not_authenticated)   // Not authenticated
        "03" -> getString(R.string.error_server_error)        // Database/server error
        "04" -> getString(R.string.error_not_found)           // Resource not found
        "05" -> getString(R.string.error_invalid_value)       // Invalid parameter value
        "06" -> getString(R.string.error_already_exists)      // Resource already exists
        "07" -> getString(R.string.error_auth_failed)         // Authentication failed
        "0A" -> getString(R.string.error_session_expired)     // Invalid token
        "0B" -> getString(R.string.error_weak_password)       // Password requirements not met
        else -> error.msg  // Fallback to API message
    }

    // Display: "[CODE] Localized message" for easier debugging
    showError("[${error.code}] $localizedMessage")
}
```

#### Example: Login Flow

```kotlin
// Before
suspend fun login(username: String, password: String): Result<LoginData> {
    return try {
        val response = api.login(LoginRequest(username, password))
        if (response.code == 200) {
            Result.success(response.data as LoginData)
        } else {
            Result.failure(Exception(response.msg))  // ❌
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}

// After
suspend fun login(username: String, password: String): Result<LoginData> {
    return try {
        val response = api.login(LoginRequest(username, password))
        if (response.code == 200) {  // ✅ Use HTTP status code
            Result.success(response.data as LoginData)
        } else {
            val errorData = Gson().fromJson(
                Gson().toJson(response.data),
                ErrorData::class.java
            )
            Result.failure(ApiException(errorData.code, errorData.msg))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}

// Custom exception class
class ApiException(val errorCode: String, message: String) : Exception(message)
```

---

### iOS (Swift)

#### Define Response Models

```swift
// Updated response models
struct ApiResponse<T: Codable>: Codable {
    let code: Int
    let msg: String  // Always "Success" or "Error"
    let data: T
}

// For error responses
struct ErrorData: Codable {
    let msg: String  // Actual error message
    let code: String  // Error code like "AUE06"
}

// For successful data responses (example)
struct LoginData: Codable {
    let csrf_token: String
}
```

#### Update Network Layer

```swift
// Before
func handleResponse(_ response: ApiResponse<Any>) {
    if response.code == 200 {
        showToast(response.msg)  // ❌ Shows "Login successful"
    } else {
        showError(response.msg)  // ❌ Shows "Email already registered"
    }
}

// After
func handleResponse<T>(_ response: ApiResponse<T>) {
    if response.code == 200 {  // ✅ Use HTTP status code
        // Success - data is in response.data
        showToast(NSLocalizedString("login_success", comment: ""))
    } else {
        // Error - parse error data
        if let errorData = try? JSONDecoder().decode(
            ErrorData.self,
            from: JSONEncoder().encode(response.data)
        ) {
            handleError(errorData)
        }
    }
}

func handleError(_ error: ErrorData) {
    // Option 1: Display the error message from API
    showError(error.msg)

    // Option 2: Use error code for localized messages (Recommended)
    // Check the last 2 characters for consistent error types
    let errorType = String(error.code.suffix(2))
    let localizedMessage: String

    switch errorType {
    case "00":
        localizedMessage = NSLocalizedString("error_invalid_format", comment: "")      // Invalid data format
    case "01":
        localizedMessage = NSLocalizedString("error_missing_parameter", comment: "")   // Missing required parameter
    case "02":
        localizedMessage = NSLocalizedString("error_not_authenticated", comment: "")   // Not authenticated
    case "03":
        localizedMessage = NSLocalizedString("error_server_error", comment: "")        // Database/server error
    case "04":
        localizedMessage = NSLocalizedString("error_not_found", comment: "")           // Resource not found
    case "05":
        localizedMessage = NSLocalizedString("error_invalid_value", comment: "")       // Invalid parameter value
    case "06":
        localizedMessage = NSLocalizedString("error_already_exists", comment: "")      // Resource already exists
    case "07":
        localizedMessage = NSLocalizedString("error_auth_failed", comment: "")         // Authentication failed
    case "0A":
        localizedMessage = NSLocalizedString("error_session_expired", comment: "")     // Invalid token
    case "0B":
        localizedMessage = NSLocalizedString("error_weak_password", comment: "")       // Password requirements not met
    default:
        localizedMessage = error.msg  // Fallback to API message
    }

    // Display: "[CODE] Localized message" for easier debugging
    showError("[\(error.code)] \(localizedMessage)")
}
```

#### Example: Login Flow

```swift
// Before
func login(username: String, password: String) async throws -> LoginData {
    let response = try await api.login(username: username, password: password)
    if response.code == 200 {
        return response.data as! LoginData
    } else {
        throw NSError(domain: "API", code: response.code,
                      userInfo: [NSLocalizedDescriptionKey: response.msg])  // ❌
    }
}

// After
func login(username: String, password: String) async throws -> LoginData {
    let response: ApiResponse<LoginData> = try await api.login(
        username: username,
        password: password
    )

    if response.code == 200 {  // ✅ Use HTTP status code
        return response.data
    } else {
        // Parse error data
        let errorData = try JSONDecoder().decode(
            ErrorData.self,
            from: JSONEncoder().encode(response.data)
        )
        throw ApiError(code: errorData.code, message: errorData.msg)
    }
}

// Custom error class
struct ApiError: Error, LocalizedError {
    let code: String
    let message: String

    var errorDescription: String? { message }
}
```

---

### Web (JavaScript/TypeScript)

#### Define Type Interfaces (TypeScript)

```typescript
// Updated response types
interface ApiResponse<T> {
  code: number;
  msg: 'Success' | 'Error';  // Always one of these two
  data: T;
}

// For error responses
interface ErrorData {
  msg: string;  // Actual error message
  code: string;  // Error code like "AUE06"
}

// For successful data responses (example)
interface LoginData {
  csrf_token: string;
}
```

#### Update API Client

```typescript
// Before
async function handleResponse(response: ApiResponse<any>) {
  if (response.code === 200) {
    showToast(response.msg);  // ❌ Shows "Login successful"
  } else {
    showError(response.msg);  // ❌ Shows "Email already registered"
  }
}

// After
async function handleResponse<T>(response: ApiResponse<T>) {
  if (response.code === 200) {  // ✅ Use HTTP status code
    // Success - data is in response.data
    showToast('Operation successful');
  } else {
    // Error - actual message is in response.data
    const errorData = response.data as unknown as ErrorData;
    handleError(errorData);
  }
}

function handleError(error: ErrorData) {
  // Option 1: Display the error message from API
  showError(error.msg);

  // Option 2: Use error code for localized messages (Recommended)
  // Check the last 2 characters for consistent error types
  const errorType = error.code.slice(-2);
  let localizedMessage: string;

  switch (errorType) {
    case '00':
      localizedMessage = 'Invalid request. Please try again.';
      break;
    case '01':
      localizedMessage = 'Please fill in all required fields.';
      break;
    case '02':
      localizedMessage = 'Please log in to continue.';
      break;
    case '03':
      localizedMessage = 'Something went wrong. Please try again later.';
      break;
    case '04':
      localizedMessage = 'Requested item not found.';
      break;
    case '05':
      localizedMessage = 'Invalid input. Please check and try again.';
      break;
    case '06':
      localizedMessage = 'This item already exists.';
      break;
    case '07':
      localizedMessage = 'Invalid credentials. Please try again.';
      break;
    case '0A':
      localizedMessage = 'Session expired. Please log in again.';
      break;
    case '0B':
      localizedMessage = 'Password must be at least 8 characters with letters and numbers.';
      break;
    default:
      localizedMessage = error.msg;  // Fallback to API message
  }

  // Display: "[CODE] Localized message" for easier debugging
  showError(`[${error.code}] ${localizedMessage}`);
}
```

#### Example: Login Flow with Axios

```typescript
// Before
async function login(username: string, password: string): Promise<LoginData> {
  try {
    const response = await axios.post<ApiResponse<LoginData>>('/auth/login', {
      username,
      password
    });

    if (response.data.code === 200) {
      return response.data.data;
    } else {
      throw new Error(response.data.msg);  // ❌
    }
  } catch (error) {
    throw error;
  }
}

// After
async function login(username: string, password: string): Promise<LoginData> {
  try {
    const response = await axios.post<ApiResponse<LoginData>>('/auth/login', {
      username,
      password
    });

    if (response.data.code === 200) {  // ✅ Use HTTP status code
      return response.data.data;
    } else {
      // Error response - data contains error info
      const errorData = response.data.data as unknown as ErrorData;
      throw new ApiError(errorData.code, errorData.msg);
    }
  } catch (error) {
    if (axios.isAxiosError(error) && error.response) {
      const errorData = error.response.data.data as ErrorData;
      throw new ApiError(errorData.code, errorData.msg);
    }
    throw error;
  }
}

// Custom error class
class ApiError extends Error {
  constructor(public code: string, message: string) {
    super(message);
    this.name = 'ApiError';
  }
}
```

#### Example: JavaScript (without TypeScript)

```javascript
// After - JavaScript version
async function login(username, password) {
  try {
    const response = await fetch('/dev/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password })
    });

    const data = await response.json();

    if (data.code === 200) {  // ✅ Use HTTP status code
      return data.data;
    } else {
      // Error response
      const error = data.data;
      throw new Error(`[${error.code}] ${error.msg}`);
    }
  } catch (error) {
    console.error('Login failed:', error);
    throw error;
  }
}
```

---

## Error Code Reference

### Understanding Error Codes

Error codes follow the pattern: **XXKNN**

- **XX**: Blueprint prefix (AU=Auth, FE=Feed, PR=Profile, etc.)
- **K**: Message type (E=Error, D=Data)
- **NN**: Error identifier (00-FF)

**💡 Important**: The last two characters (**NN**) are consistent across all blueprints. For example:
- `AUE00`, `FEE00`, `PRE00`, `ARE00` all represent **"Invalid data format"**
- `AUE01`, `FEE01`, `PRE01` all represent **"Missing required parameter"**

**This means you only need to check the last 2 characters to determine what error message to show.**

### Common Error Codes (All Platforms)

The following errors use the same last two digits across all endpoints:

| Last 2 Chars | Description | Recommended User Message |
|--------------|-------------|--------------------------|
| **E00** | Invalid data format | "Invalid request. Please try again." |
| **E01** | Missing required parameter | "Please fill in all required fields." |
| **E02** | Not authenticated | "Please log in to continue." |
| **E03** | Database error | "Something went wrong. Please try again later." |
| **E04** | Resource not found | "Requested item not found." |
| **E05** | Invalid parameter value | "Invalid input. Please check and try again." |
| **E06** | Resource already exists | "This item already exists." |
| **E07** | Authentication failed | "Invalid credentials. Please try again." |
| **E08** | Internal server error | "Server error. Please try again later." |
| **E09** | Forbidden | "You don't have permission to do this." |
| **E0A** | Invalid token | "Session expired. Please log in again." |
| **E0B** | Password requirements not met | "Password must be at least 8 characters with letters and numbers." |
| **E0C** | Verification error | "Verification failed. Please try again." |
| **E0D** | Unsupported operation | "This operation is not supported." |

**Example**: If you receive error code `PRE06`, check the last 2 characters (`06`) → "Resource already exists" → Show message: "This item already exists."

### Two Approaches to Error Handling

**Option 1: Display API Message Directly**
- **Best for**: Quick implementation, server-controlled messaging
- **Pros**: No need to maintain error message mappings
- **Cons**: Messages might not be localized or user-friendly

**Option 2: Check Last 2 Characters + Display Error Code (Recommended)**
- **Best for**: Consistent error handling with debugging support
- **Pros**:
  - One error code → one user message, works for all blueprints
  - Displaying error code (e.g., `[AUE06]`) helps with debugging and support
  - Users can report specific error codes to support team
- **Cons**: Requires maintaining localized error message mappings

**💡 Recommended Strategy**: Use **Option 2** (last 2 characters) with error code display. Format: `"[ERROR_CODE] Localized message"` (e.g., `"[PRE06] This item already exists"`). This gives users a friendly message while providing technical context for debugging.
