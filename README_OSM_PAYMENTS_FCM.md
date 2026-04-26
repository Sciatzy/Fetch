# Fetch OSM + Pricing + Payments + FCM (Test Setup)

This document covers the current mobile-side integration for:

- OpenStreetMap task pinning (`osmdroid`)
- Route fee estimation through backend (`/v1/pricing/estimate`)
- PayMongo checkout initialization through backend (`/v1/payments/paymongo/checkout`)
- FCM token sync + notification handling

## Current flow

1. Customer opens `MapTaskComposerActivity` from dashboard.
2. Customer taps map to set pickup and drop-off pins.
3. App calls backend pricing endpoint to estimate distance/fee.
4. App creates Firestore task with route metadata.
5. App initializes PayMongo checkout via backend and opens checkout URL.
6. App registers geofence for drop-off radius.
7. FCM token is synced on authenticated home entry.

## Required app config (already wired in BuildConfig)

- `BACKEND_BASE_URL`
- `OPENROUTESERVICE_API_KEY`
- `PAYMONGO_SECRET_KEY` (test-only local setup)
- `PAYMONGO_PUBLIC_KEY`
- `PAYMONGO_WEBHOOK_URL`
- `PAYMONGO_WEBHOOK_SECRET`

## Backend endpoints expected by app

- `POST /v1/pricing/estimate`
- `POST /v1/payments/paymongo/checkout`
- `POST /v1/fcm/register`

## Build

```powershell
Set-Location "C:\Users\SCIATZY MARIE\Documents\Fetch"
.\gradlew.bat :app:assembleDebug --console=plain
```

## Notes

- For production, move secret credentials to backend env only.
- Webhook must update payment status in Firestore using `paymentCheckoutId` and task metadata.

