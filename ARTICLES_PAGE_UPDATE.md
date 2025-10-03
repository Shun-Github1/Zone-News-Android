# Articles Page API Updates

## Overview
Multiple changes have been made to the publisher information in API responses:

1. The `publisherStance` field now uses a structured format with `tag` and `displayName` attributes
2. New metrics `mediaSignificance` and `bias` have been added to publisher objects

## Changes Made

### Before
```json
{
  "publisherStance": "Progressive"
}
```

### After  
```json
{
  "publisherStance": {
    "tag": "p",
    "displayName": "Progressive"
  },
  "mediaSignificance": 4,
  "bias": 6
}
```

## New Metrics
- `mediaSignificance`: Integer value from 1-6 representing the significance/influence of the media outlet
- `bias`: Integer value from 1-8 representing the bias level of the publisher

## Tag Values  
- `p` = Progressive
- `c` = Conservative

## Affected Endpoints
This change affects the following API endpoints that return publisher information:

- `/article/{id}` - The `articles` array within the response
- Any endpoint that calls `generatePublishers()` function

## Localization
The `displayName` will be localized based on the requested language:

| Language | Progressive | Conservative |
|----------|-------------|--------------|
| en-UK    | Progressive | Conservative |
| zh-CN    | 进步        | 保守         |
| zh-HK    | 進步        | 保守         |

## Required Frontend Updates
Frontend parsers should be updated to:

1. Access the display text via `publisherStance.displayName` instead of directly using `publisherStance`
2. Use `publisherStance.tag` for any filtering, sorting, or programmatic logic
3. Update any existing code that expects `publisherStance` to be a string
4. Handle the new `mediaSignificance` and `bias` integer fields for publisher analytics/filtering

## Example Usage
```javascript
// Before
const stanceText = publisher.publisherStance;

// After  
const stanceText = publisher.publisherStance.displayName;
const stanceTag = publisher.publisherStance.tag;
const significance = publisher.mediaSignificance;  // 1-6
const biasLevel = publisher.bias;                  // 1-8

// For filtering
const progressivePublishers = publishers.filter(p => p.publisherStance.tag === 'p');
const highSignificancePublishers = publishers.filter(p => p.mediaSignificance >= 5);
const lowBiasPublishers = publishers.filter(p => p.bias <= 3);
```