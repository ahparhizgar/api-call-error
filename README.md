# API Call Error

A Kotlin Multiplatform library that provides structured error handling for API calls. 
It defines a comprehensive hierarchy of error types that can be thrown by API clients, making error
handling consistent, type-safe, and easier to maintain across your application.

## When should you use it?

Use this library when:

- You're building a Kotlin Multiplatform project that communicates with REST APIs
- You want type-safe error handling instead of parsing HTTP status codes manually
- You need to extract and display server-provided error messages to users
- You want to differentiate between network failures, server issues, and client-side problems

## Error Hierarchy

```
ApiCallError (base class)
├─ InvalidDataError
├─ NetworkError           // Connection issues, timeouts
└─ HttpError              // HTTP status code errors
    ├─ ServerError        // 5xx errors
    └─ ClientError        // 4xx errors
        ├─ BadRequest     // 400
        ├─ Unauthorized   // 401
        ├─ Forbidden      // 403
        ├─ NotFound       // 404
        ├─ RateLimitReached // 429
        └─ OtherClientError // Other 4xx codes
```

## Setup

### Adding the dependency via Maven Central

Add the dependency to your `build.gradle.kts`:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            // For core library only:
            implementation("io.github.ahparhizgar:api-call-error:0.1.0")
            // For Ktor Client plugin:
            implementation("io.github.ahparhizgar:api-call-error-ktor:0.1.0")
        }
    }
}
```

## Usage

### Option 1: Using the Ktor Plugin (Recommended)

The Ktor plugin automatically converts HTTP errors, network failures, and serialization issues into typed `ApiCallError` exceptions.

#### Basic Setup

```kotlin
val client = HttpClient {
    install(ApiCallErrorPlugin)
    // ...
}
```

#### Extracting Error Information from Server Responses

If your API returns structured error responses, you can extract them using the `extractPayload` configuration:

```kotlin
@Serializable
data class ErrorResponse(
    val errorKey: String,
    val message: String,
    val details: Map<String, String>? = null
)

val client = HttpClient {
    install(ContentNegotiation) {
        json()
    }
    install(ApiCallErrorPlugin) {
        extractPayload { response ->
            // Only called for 4xx and 5xx responses
            val errorResponse = try {
                response.body<ErrorResponse>()
            } catch (e: Exception) {
                return@extractPayload null
            }
            
            ClientErrorExtras(
                userMessage = errorResponse.message,
                errorKey = errorResponse.errorKey,
                payload = errorResponse.details
            )
        }
    }
}
```

#### Handling Errors

```kotlin
suspend fun fetchUser(userId: Int): User {
    return try {
        client.get("https://api.example.com/users/$userId").body()
    } catch (e: NotFound) {
        // User doesn't exist
        // show proper message or take action
    } catch (e: ApiCallError) {
        // Extract this when into a function to reuse general error handling logic
        when (e) {
            is Unauthorized -> {
                // Token expired or invalid
                refreshTokenAndRetry()
            }
            is NetworkError -> {
                // Network connectivity issue
                showOfflineMessage()
            }
            is InvalidDataError -> {
                // Response couldn't be parsed
                logParsingError(e)
            }
            is ServerError -> {
                // Server is having issues (5xx)
                showServerErrorMessage()
            }
            is ClientError -> {
                // Other 4xx error
                showError(e.userMessage ?: "Request failed")
            }
        }
    }
}
```

### Option 2: Using Plain Error Classes

If you're not using Ktor or want to manually create errors, you can use the error classes directly.

#### Data Validation Helpers

The library provides convenient functions for validating response data:

```kotlin
fun parseUserResponse(userDto: dto): User {
    requireData(dto.id != null) {
        "Missing required field: id"
    }
    
    val age = dto.age?.takeIf { it >= 0 }
        ?: invalidData("Field 'age' must be non negative")
    
    return User(dto.id, age)
}
```

#### Custom Error Types

You can extend the error hierarchy for your specific needs:

```kotlin
class CustomClientError(
    override val message: String? = null,
    override val cause: Throwable? = null,
    override val payload: Any? = null
) : ClientError()
```

## Requirements

- For Ktor plugin: Ktor Client 3.0.0 or higher
