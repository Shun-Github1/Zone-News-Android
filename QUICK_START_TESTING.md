# Quick Start: Testing Firebase Notifications

## 🚀 Fastest Way to Test (Firebase Console)

### Step 1: Get Your FCM Token
1. Run your app on a device/emulator
2. Check the logs for: `FCM Registration Token: YOUR_TOKEN_HERE`
3. Copy this token

### Step 2: Send Test Notification
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your `zone-news` project
3. Click **Messaging** in the left menu
4. Click **"Send your first message"**
5. Enter:
   - **Title**: "Test Notification"
   - **Text**: "This is a test from Firebase Console"
6. Click **"Send test message"**
7. Paste your FCM token
8. Click **"Test"**

## 📱 In-App Testing (Easiest for Development)

### Step 1: Access Test Activity
Add this to your app's debug menu or temporarily add to MainActivity:
```kotlin
// Add this button click handler somewhere in your app
button.setOnClickListener {
    val intent = Intent(this, NotificationTestActivity::class.java)
    startActivity(intent)
}
```

### Step 2: Test Different Notifications
- **General Notification**: Basic notification
- **News Notification**: Opens specific article
- **Custom Notification**: With custom data
- **Action Notification**: With action buttons

## 🐍 Python Script Testing

### Step 1: Install Python
```bash
pip install requests
```

### Step 2: Update Credentials
1. Open `test_notification.py`
2. Replace `YOUR_SERVER_KEY_HERE` with your server key
3. Replace `YOUR_FCM_TOKEN_HERE` with your FCM token

### Step 3: Run
```bash
python test_notification.py
```

## 🔧 cURL Testing

### Step 1: Update Credentials
1. Open `test_notification.sh`
2. Replace `YOUR_SERVER_KEY_HERE` with your server key
3. Replace `YOUR_FCM_TOKEN_HERE` with your FCM token

### Step 2: Run
```bash
chmod +x test_notification.sh
./test_notification.sh
```

## 📋 Getting Your Credentials

### FCM Token
- **From Logs**: Check app logs for `FCM Registration Token:`
- **From Code**: Add this to display token in UI:
```kotlin
FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
    if (task.isSuccessful) {
        val token = task.result
        // Display token in UI or copy to clipboard
        Log.d("FCM", "Token: $token")
    }
}
```

### Server Key
1. Firebase Console → Project Settings → Cloud Messaging
2. Copy the **Server Key**

## 🎯 Test Scenarios

### 1. App in Foreground
- Send notification while app is open
- Should show notification in status bar
- Should trigger `onMessageReceived`

### 2. App in Background
- Send notification while app is minimized
- Should show notification in status bar
- Tapping should open app

### 3. App Closed
- Send notification while app is completely closed
- Should show notification in status bar
- Tapping should open app

## 🐛 Troubleshooting

### No Notifications Received
1. Check notification permissions are granted
2. Verify FCM token is correct
3. Check device notification settings
4. Look for errors in app logs

### Notifications Not Showing
1. Check if notification channel is created
2. Verify notifications are enabled in device settings
3. Check if app is in battery optimization

### Token Issues
1. FCM tokens refresh automatically
2. Check logs for new token
3. Update your backend with new token

## 📊 Expected Results

### Successful Test
- ✅ Notification appears in status bar
- ✅ App icon shows in notification
- ✅ Tapping notification opens app
- ✅ Logs show FCM token and message received

### Failed Test
- ❌ No notification appears
- ❌ Error messages in logs
- ❌ Permission denied messages

## 🔄 Next Steps

1. **Test All Scenarios**: Foreground, background, closed
2. **Test Different Types**: General, news, custom
3. **Test on Different Devices**: Various Android versions
4. **Integrate with Backend**: Send real notifications from your server
5. **Monitor Analytics**: Track notification open rates

## 📞 Need Help?

1. Check the detailed `FCM_TESTING_GUIDE.md`
2. Review Firebase Console for error messages
3. Check app logs for FCM-related errors
4. Test with Firebase Console first to isolate issues



