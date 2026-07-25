# API Update: Quotes Field in Article Endpoint

**Date**: 2026-02-14

## Summary

The `GET /article/{id}` endpoint now returns a `quotes` array containing notable quotes associated with the article group, localized to the requested language.

## What Changed

A new `quotes` field has been added to the article response object. It appears alongside existing fields like `articles`, `description`, and `relatedArticles`.

## Response Schema

The `quotes` field is an array of quote objects. It will always be present in the response (never `null`), but may be empty (`[]`) if no quotes exist for the article group.

```json
{
    "quotes": [
        {
            "text": "We remain committed to maintaining stability.",
            "entityType": "person",
            "name": "John Smith",
            "role": "Chief Executive Officer",
            "sourceURL": "https://example.com/source-article",
            "category": "policy",
            "background": "Statement made during annual press conference"
        }
    ]
}
```

### Quote Object Fields

| Field        | Type   | Always Present | Description                                        |
|--------------|--------|----------------|----------------------------------------------------|
| `text`       | string | Yes            | The quote text                                     |
| `entityType` | string | No             | Type of entity (e.g., `"person"`, `"organisation"`) |
| `name`       | string | No             | Name of the quoted entity                          |
| `role`       | string | No             | Role or title of the quoted entity                 |
| `sourceURL`  | string | No             | URL of the source article for the quote            |
| `category`   | string | No             | Category of the quote                              |
| `background` | string | No             | Background context for the quote                   |

**Note**: Only `text` is guaranteed. All other fields are omitted (not present in the object) when their value is `null` in the database.

## Language Support

Quotes are filtered by the `lang` query parameter, same as other localized content:

- `en-UK` (default)
- `zh-HK`
- `zh-CN`

Each language returns a separate set of quotes. If no quotes exist for the requested language, the array will be empty.

## Front-End Action Required

1. Add an optional `quotes` array to your article type/interface
2. Render quotes in the article detail view if the array is non-empty
3. Handle the case where optional fields (`entityType`, `name`, `role`, `sourceURL`, `category`, `background`) may be absent from individual quote objects

### TypeScript Example

```typescript
interface Quote {
    text: string;
    entityType?: string;
    name?: string;
    role?: string;
    sourceURL?: string;
    category?: string;
    background?: string;
}

interface Article {
    // ... existing fields ...
    quotes: Quote[];
    // ... existing fields ...
}
```

### Swift Example (iOS)

```swift
struct Quote: Codable {
    let text: String
    let entityType: String?
    let name: String?
    let role: String?
    let sourceURL: String?
    let category: String?
    let background: String?
}

struct Article: Codable {
    // ... existing fields ...
    let quotes: [Quote]
    // ... existing fields ...
}
```

### Kotlin Example (Android)

```kotlin
@Serializable
data class Quote(
    val text: String,
    val entityType: String? = null,
    val name: String? = null,
    val role: String? = null,
    val sourceURL: String? = null,
    val category: String? = null,
    val background: String? = null
)

@Serializable
data class Article(
    // ... existing fields ...
    val quotes: List<Quote>,
    // ... existing fields ...
)
```

## Backward Compatibility

This is a **non-breaking** additive change. The `quotes` field is new and does not modify any existing fields. Front-end code that does not reference `quotes` will continue to work without changes.
