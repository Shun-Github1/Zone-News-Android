# API Changes - Article Description Structure Update

## Overview
The article description field in the `/article/{id}` endpoint has been changed from a simple string to a structured object with two keys: `synopsis` and `implications`.

## Change Details

### Before (Old Structure)
```json
{
    "description": "Single string containing the article summary and analysis combined"
}
```

### After (New Structure)
```json
{
    "description": {
        "synopsis": "Brief summary of the main events",
        "implications": "Analysis of potential consequences and broader impact"
    }
}
```

## Affected Endpoints
- `GET /article/{id}` - Article description field structure changed

## Required Client Changes

### Frontend/Mobile Apps
1. **Update JSON parsing**: Change code that expects `description` to be a string
2. **UI Components**: Update components that display article descriptions to handle the new structure
3. **Type definitions**: Update TypeScript interfaces or data models

**Example Migration:**

```typescript
// OLD
interface Article {
    description: string;
    // other fields...
}

// NEW
interface Article {
    description: {
        synopsis: string;
        implications: string;
    };
    // other fields...
}
```

```javascript
// OLD - Direct string usage
const descriptionText = article.description;

// NEW - Access structured fields
const synopsis = article.description.synopsis;
const implications = article.description.implications;
```

### Backend Integration
If you're consuming this API from another backend service, update your:
1. Data models/structs
2. JSON unmarshaling/parsing logic
3. Any business logic that processes descriptions

## Language Support
The new structure is available in all supported languages:
- `en-UK` (English)
- `zh-CN` (Simplified Chinese)
- `zh-HK` (Traditional Chinese/Hong Kong)

## Migration Timeline
- **Effective Date**: Immediately
- **Deprecation**: The old string format is no longer supported
- **Testing**: Update your test cases to expect the new structure

## Testing Your Integration
Use these sample article IDs to test the new format:
- Any existing article ID will return the new structure
- Example: `GET /article/12345?lang=en-UK`

## Need Help?
If you encounter issues migrating to the new structure, please contact the backend team or file an issue in the project repository.