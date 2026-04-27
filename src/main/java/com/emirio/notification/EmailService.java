package com.emirio.notification;

public interface EmailService {
    void sendNewArticleNotification(String articleName, String articleDescription, String articleImageUrl);
}