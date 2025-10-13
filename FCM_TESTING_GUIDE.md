# Firebase Cloud Messaging Testing Guide

This guide provides multiple methods to test Firebase Cloud Messaging (FCM) notifications in your Zone News app.

## Prerequisites

1. **FCM Token**: Get your FCM token from app logs
2. **Server Key**: Get your server key from Firebase Console
3. **App Running**: Make sure the app is installed and running

## Method 1: Firebase Console (Recommended for Beginners)

### Step 1: Access Firebase Console
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project (`zone-news`)
3. Navigate to **Messaging** in the left sidebar

### Step 2: Send Test Notification
1. Click **"Send your first message"** or **"New campaign"**
2. Enter notification details:
   - **Title**: "Test Notification"
   - **Text**: "This is a test notification from Firebase Console"
3. Click **"Send test message"**
4. Enter your FCM token (see "Getting FCM Token" section below)
5. Click **"Test"**

## Method 2: Using Python Script

### Step 1: Install Dependencies
```bash
pip install requests
```

### Step 2: Update Credentials
1. Open `test_notification.py`
2. Replace `YOUR_SERVER_KEY_HERE` with your server key
3. Replace `YOUR_FCM_TOKEN_HERE` with your FCM token

### Step 3: Run the Script
```bash
python test_notification.py
```

## Method 3: Using cURL Script

### Step 1: Update Credentials
1. Open `test_notification.sh`
2. Replace `YOUR_SERVER_KEY_HERE` with your server key
3. Replace `YOUR_FCM_TOKEN_HERE` with your FCM token

### Step 2: Make Script Executable
```bash
chmod +x test_notification.sh
```

### Step 3: Run the Script
```bash
./test_notification.sh
```

## Method 4: Using Test Activity (In-App Testing)

### Step 1: Access Test Activity
Add this to your app's navigation or create a debug menu item:
```kotlin
val intent = Intent(this, NotificationTestActivity::class.java)
startActivity(intent)
```

### Step 2: Test Different Notification Types
- **General Notification**: Basic notification that opens the app
- **News Notification**: Notification that can open specific articles
- **Custom Notification**: Notification with custom data payload
- **Action Notification**: Notification with action buttons

## Getting Your Credentials

### FCM Token
1. Run your app
2. Check the logs for: `FCM Registration Token: YOUR_TOKEN_HERE`
3. Or add this to your app to display the token:
```kotlin
FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
    if (task.isSuccessful) {
        val token = task.result
        Log.d("FCM", "FCM Token: $token")
        // Display token in UI or copy to clipboard
    }
}
```

### Server Key
1. Go to Firebase Console
2. Select your project
3. Go to **Project Settings** (gear icon)
4. Click **Cloud Messaging** tab
5. Copy the **Server Key**

## Testing Different Notification Types

### 1. General Notification
```json
{
  "to": "FCM_TOKEN",
  "notification": {
    "title": "General Notification",
    "body": "This is a general notification"
  }
}
```

### 2. News Notification
```json
{
  "to": "FCM_TOKEN",
  "notification": {
    "title": "Breaking News",
    "body": "Check out the latest updates"
  },
  "data": {
    "type": "news",
    "article_id": "12345"
  }
}
```

### 3. Data-Only Notification
```json
{
  "to": "FCM_TOKEN",
  "data": {
    "type": "custom",
    "custom_field": "custom_value",
    "timestamp": "1234567890"
  }
}
```

## Troubleshooting

### Common Issues

1. **No notifications received**
   - Check if notification permissions are granted
   - Verify FCM token is correct
   - Check app logs for errors

2. **Notifications not showing**
   - Ensure notification channel is created
   - Check if notifications are enabled in device settings
   - Verify the app is not in battery optimization

3. **Token refresh issues**
   - FCM tokens refresh automatically
   - Implement token refresh handling in your app
   - Update your backend with new tokens

### Debug Steps

1. **Check App Logs**
   ```bash
   adb logcat | grep -E "(FCM|Firebase|Notification)"
   ```

2. **Verify Permissions**
   - Go to Settings > Apps > Zone News > Notifications
   - Ensure notifications are enabled

3. **Test with Firebase Console**
   - Use Firebase Console to send test notifications
   - This helps isolate if the issue is with your backend or FCM setup

## Advanced Testing

### Testing Notification Handling
1. Send notification when app is in foreground
2. Send notification when app is in background
3. Send notification when app is closed
4. Test different notification actions

### Testing Data Payloads
1. Test with different data types
2. Test with large payloads
3. Test with special characters
4. Test with different notification types

## Production Considerations

1. **Rate Limiting**: Implement rate limiting for notifications
2. **User Preferences**: Allow users to opt-out of notifications
3. **Analytics**: Track notification open rates
4. **A/B Testing**: Test different notification formats
5. **Error Handling**: Implement proper error handling for failed notifications

## Security Notes

1. **Server Key**: Keep your server key secure
2. **Token Validation**: Validate FCM tokens on your backend
3. **User Consent**: Ensure users have consented to notifications
4. **Data Privacy**: Be mindful of data in notification payloads

## Support

If you encounter issues:
1. Check Firebase Console for error messages
2. Review app logs for FCM-related errors
3. Test with Firebase Console first
4. Verify your FCM setup is correct






