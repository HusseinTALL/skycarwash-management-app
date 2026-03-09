package com.skycarwash.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Notification service — currently logs alerts to console.
 * Replace log calls with Firebase Admin SDK FCM calls in production
 * once FCM service-account credentials are configured.
 *
 * FCM integration steps (S5–S6 follow-up):
 *   1. Add firebase-admin dependency to pom.xml
 *   2. Set GOOGLE_APPLICATION_CREDENTIALS env-var to service-account JSON path
 *   3. Replace the log.warn calls below with FirebaseMessaging.getInstance().send(...)
 */
@Slf4j
@Service
public class NotificationService {

    public void sendExpiryAlert(String clientName, String phone, long daysLeft) {
        log.warn("[ALERT] Abonnement expirant bientôt — client='{}' phone={} dans {} jours",
                clientName, phone, daysLeft);
        // TODO: FCM push to MANAGER device topic
    }

    public void sendLowBalanceAlert(String clientName, String phone, int remainingPassages) {
        log.warn("[ALERT] Solde faible — client='{}' phone={} passages restants={}",
                clientName, phone, remainingPassages);
        // TODO: FCM push to MANAGER device topic
    }

    public void sendStockAlert(String productName, String stock, String unit) {
        log.warn("[ALERT] Stock bas — produit='{}' stock={} {}",
                productName, stock, unit);
        // TODO: FCM push to MANAGER device topic
    }
}
