#!/bin/bash

# Firebase Cloud Messaging Test Script using cURL
# Replace these values with your actual credentials

SERVER_KEY="YOUR_SERVER_KEY_HERE"
FCM_TOKEN="YOUR_FCM_TOKEN_HERE"

echo "Firebase Cloud Messaging Test Script"
echo "===================================="

# Check if credentials are set
if [ "$SERVER_KEY" = "YOUR_SERVER_KEY_HERE" ] || [ "$FCM_TOKEN" = "YOUR_FCM_TOKEN_HERE" ]; then
    echo "❌ Please update SERVER_KEY and FCM_TOKEN in this script"
    echo ""
    echo "To get your credentials:"
    echo "1. SERVER_KEY: Firebase Console > Project Settings > Cloud Messaging"
    echo "2. FCM_TOKEN: Check app logs when running the app"
    exit 1
fi

echo "Sending test notifications..."
echo ""

# Test 1: General notification
echo "1. Sending general notification..."
curl -X POST https://fcm.googleapis.com/fcm/send \
  -H "Authorization: key=$SERVER_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "to": "'$FCM_TOKEN'",
    "notification": {
      "title": "Test Notification",
      "body": "This is a test notification from cURL",
      "icon": "ic_notifications_24",
      "sound": "default"
    },
    "data": {
      "type": "general"
    }
  }'

echo ""
echo ""

# Test 2: News notification
echo "2. Sending news notification..."
curl -X POST https://fcm.googleapis.com/fcm/send \
  -H "Authorization: key=$SERVER_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "to": "'$FCM_TOKEN'",
    "notification": {
      "title": "Breaking News",
      "body": "Check out the latest updates from Zone News",
      "icon": "ic_notifications_24",
      "sound": "default"
    },
    "data": {
      "type": "news",
      "article_id": "12345",
      "click_action": "OPEN_ARTICLE"
    }
  }'

echo ""
echo "Test completed!"












