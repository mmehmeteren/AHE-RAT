# NOT: TAMAMEN EĞİTİM AMAÇLI OLUP KÖTÜYE KULLANIMDA SORUMLU DEĞİLİM

# 📱 Android Uygulama İşleyişi

## 1. WebView Açma
- Uygulama açıldığında **WebView** başlatır ve `https://google.com` yükler.  
- Kullanıcıya normal bir tarayıcı gibi görünür.

## 2. İzin Kontrolleri
- Gerekirse kullanıcıdan şu izinleri ister:
  - `READ_CONTACTS` → Rehbere erişim  
  - `READ_SMS` → SMS mesajlarına erişim  
  - `READ_EXTERNAL_STORAGE` → Dosyalara erişim  

## 3. Veri Toplama
İzin verilirse arka planda şu veriler toplanır:
- 📇 Rehberdeki isim ve numaralar  
- ✉️ SMS gelen kutusundaki mesajlar  
- 📂 Dahili depolamadaki medya dosyaları (.jpg, .png, .mp4 vb.)  
- 📶 Bağlı olunan Wi-Fi ağının **SSID bilgisi**

## 4. Telegram’a Veri Gönderme
- Sabit tanımlı değerler:
  - **BOT_TOKEN** = `YOUR TOKEN`  
  - **CHAT_ID**   = `YOUR ID`  
- Bu bilgilerle **Telegram API** üzerinden gönderim yapılır.

## 5. Gönderim Yöntemleri
- `sendTextToTelegram()` → Metin türü veriler (rehber, SMS, Wi-Fi bilgisi)  
- `sendFileToTelegram()` → Dosyalar (fotoğraf, video)  

## 6. Gizlenme Yöntemleri
- Açılışta **Google** sayfasını açarak masum görünür.  
- Bildirim erişimi kapalıysa kullanıcıyı ayarlara yönlendirir.  
- Arka planda sessizce veri aktarımı yapar.
