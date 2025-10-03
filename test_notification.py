#!/usr/bin/env python3
"""
Firebase Cloud Messaging Test Script
Send test notifications to your app
"""

import requests
import json

# Replace with your server key from Firebase Console
SERVER_KEY = "YOUR_SERVER_KEY_HERE"

# Replace with your FCM token from app logs
FCM_TOKEN = "YOUR_FCM_TOKEN_HERE"

def send_notification(title, body, data=None):
    """Send a notification via FCM"""
    
    url = "https://fcm.googleapis.com/fcm/send"
    
    headers = {
        "Authorization": f"key={SERVER_KEY}",
        "Content-Type": "application/json"
    }
    
    payload = {
        "to": FCM_TOKEN,
        "notification": {
            "title": title,
            "body": body,
            "icon": "ic_notifications_24",
            "sound": "default"
        },
        "data": data or {}
    }
    
    response = requests.post(url, headers=headers, json=payload)
    
    if response.status_code == 200:
        print("✅ Notification sent successfully!")
        print(f"Response: {response.json()}")
    else:
        print(f"❌ Failed to send notification: {response.status_code}")
        print(f"Response: {response.text}")

def send_news_notification():
    """Send a news-specific notification"""
    send_notification(
        title="Breaking News",
        body="Check out the latest updates from Zone News",
        data={
            "type": "news",
            "article_id": "12345",
            "click_action": "OPEN_ARTICLE"
        }
    )

def send_general_notification():
    """Send a general notification"""
    send_notification(
        title="Welcome to Zone News",
        body="Stay updated with the latest news and updates"
    )

if __name__ == "__main__":
    print("Firebase Cloud Messaging Test Script")
    print("=" * 40)
    
    # Check if credentials are set
    if SERVER_KEY == "YOUR_SERVER_KEY_HERE" or FCM_TOKEN == "YOUR_FCM_TOKEN_HERE":
        print("❌ Please update SERVER_KEY and FCM_TOKEN in this script")
        print("\nTo get your credentials:")
        print("1. SERVER_KEY: Firebase Console > Project Settings > Cloud Messaging")
        print("2. FCM_TOKEN: Check app logs when running the app")
        exit(1)
    
    print("Sending test notifications...")
    print()
    
    # Send general notification
    print("1. Sending general notification...")
    send_general_notification()
    print()
    
    # Send news notification
    print("2. Sending news notification...")
    send_news_notification()
    print()
    
    print("Test completed!")





