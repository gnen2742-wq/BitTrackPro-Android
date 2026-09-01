# BitTrack Pro

Profesyonel, abonelik tabanlı Bitcoin takip uygulaması için Android başlangıç projesi.

## İlk sürüm
- BTC/EUR ve BTC/USD piyasa ekranı
- Portföy ekranı
- Fiyat alarmı ekranı
- Premium aylık/yıllık abonelik ekranı
- Google Play Billing entegrasyon iskeleti
- Firebase Auth / Firestore / Messaging bağımlılıkları
- GitHub Actions ile APK derleme

## Yayına almadan önce gerekli gerçek bağlantılar
1. `app/google-services.json` ekleyin ve `com.google.gms.google-services` eklentisini aktif edin.
2. Firebase Authentication ve Firestore'u yapılandırın.
3. Lisanslı/güvenilir BTC fiyat API'si bağlayın.
4. Google Play Console'da abonelik ürünleri oluşturun (ör. `premium_monthly`, `premium_yearly`).
5. Play Billing satın alımlarını sunucu tarafında doğrulayın.
6. Gizlilik politikası, kullanım şartları ve finansal bilgi uyarılarını ekleyin.

Bu sürüm kripto para saklamaz, kullanıcı fonlarına erişmez ve alım-satım emri göndermez.
