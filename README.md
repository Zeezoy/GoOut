# Go Out — Pemantauan Kepadatan Koridor Kota Malang

Go Out adalah aplikasi Android native (Kotlin + Jetpack Compose) untuk memantau
kepadatan tiga koridor jalan utama Kota Malang berbasis **crowdsourcing**:
telemetri GPS pasif dan laporan aktif 1-ketukan dari warga. Terinspirasi SISRI
(Surabaya Integrated Smart Road Infrastructure), diadaptasi menjadi pendekatan
ringan berbasis smartphone tanpa kamera lalu lintas/ATCS.

Proyek akhir praktikum **Pengembangan Aplikasi Perangkat Bergerak**.

## Koridor Terpantau

1. Jl. Veteran (Bundaran UB → Simpang Matos)
2. Jl. Soekarno-Hatta (Simpang Suhat → Jembatan Brantas)
3. Jl. MT Haryono (Dinoyo → Simpang Betek)

## Fitur Inti

| # | Fungsi | Implementasi |
|---|--------|--------------|
| 1 | Klasifikasi kepadatan (≥35 LANCAR · 15–34.9 MERAYAP · <15 MACET) | `ClassifyTrafficDensityUseCase` |
| 2 | Validasi geofence koridor (≤100 m, point-to-segment) | `GeofenceValidator` + `GeoUtils` |
| 3 | Telemetri pasif (opt-in, foreground service, interval 10 dtk) | `TelemetryService` + `ProcessTelemetryFixUseCase` |
| 4 | Laporan aktif 1-ketukan (LANCAR/MERAYAP/MACET) | `SubmitTrafficReportUseCase` |

Plus: Mode Tamu, sinkronisasi hybrid Room ⇄ Firestore via WorkManager,
peta Google Maps dengan polyline berwarna status, dan mock traffic generator
(hanya build debug).

## Teknologi

Kotlin · Jetpack Compose (Material 3) · MVVM + Clean Architecture · Hilt ·
Navigation Compose · Room · DataStore · WorkManager · Firebase Auth ·
Cloud Firestore · Google Maps SDK (Maps Compose) · Fused Location Provider ·
JUnit + MockK + Compose UI Testing.

---

## Menjalankan Proyek

### Prasyarat

- Android Studio (Ladybug/terbaru) dengan Android SDK 36
- JDK 17 (bawaan Android Studio / JBR)
- Perangkat/emulator Android 8.0+ (API 26, dengan Google Play Services)
- Akun Firebase (gratis, paket Spark cukup)
- Google Maps API key

### 1. Clone & buka

```
git clone https://github.com/Zeezoy/GoOut.git
```
Buka folder di Android Studio; Gradle sync berjalan otomatis.

### 2. Google Maps API key

1. Buka [Google Cloud Console](https://console.cloud.google.com) → buat/pilih project.
2. Aktifkan **Maps SDK for Android**, buat API key.
3. Tambahkan ke `local.properties` (file ini **tidak** di-commit):

```properties
MAPS_API_KEY=AIza...
```

Tanpa key, aplikasi tetap berjalan tetapi tile peta tidak tampil.

### 3. Firebase

Lihat panduan lengkap di [docs/FIREBASE_SETUP.md](docs/FIREBASE_SETUP.md). Ringkas:

1. Buat project di [Firebase Console](https://console.firebase.google.com).
2. Tambahkan aplikasi Android dengan package `com.example.goout`.
3. Unduh `google-services.json` → letakkan di folder `app/`.
4. Aktifkan **Authentication → Email/Password**.
5. Buat database **Cloud Firestore** (production mode).
6. Deploy security rules: `firebase deploy --only firestore:rules`
   (file [firestore.rules](firestore.rules)), atau salin-tempel isinya di tab
   Rules pada Firebase Console.

> Build tetap sukses tanpa `google-services.json` (plugin Google Services hanya
> diterapkan bila file ada), tetapi login/sinkronisasi cloud tidak berfungsi.

### 4. Inisialisasi data koridor

Geometri ketiga koridor terpusat di
`app/src/main/java/com/example/goout/core/common/CorridorConfig.kt`.
Saat aplikasi pertama berjalan, Room di-seed dari file ini dan dokumen
`corridors/{corridorId}` dibuat otomatis di Firestore (merge, hanya
metadata/geometri — tidak pernah menulis data lalu lintas palsu).

⚠ Koordinat polyline bertanda **APPROXIMATE**: cukup untuk demo, tetapi
sebaiknya diverifikasi dengan menelusuri jalan di Google Maps lalu mengganti
daftar titik pada file tersebut (satu-satunya sumber geometri).

### 5. Menjalankan mock data (demo)

Build **debug** → buka **Profil & Pengaturan → Pengembang — Mock Traffic** →
aktifkan. Generator mensimulasikan kecepatan 5–50 km/h per koridor setiap
10 detik melalui pipeline agregasi normal. Tidak tersedia di build release.

### 6. Menjalankan tes

```bash
# Unit test (klasifikasi, geofence, telemetri, laporan)
./gradlew :app:testDebugUnitTest

# UI test (butuh emulator/perangkat terhubung)
./gradlew :app:connectedDebugAndroidTest
```

### 7. Membuat APK

```bash
./gradlew :app:assembleDebug     # app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:assembleRelease   # perlu konfigurasi signing untuk distribusi
```

---

## Dokumentasi

- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) — arsitektur, diagram, keputusan teknik
- [docs/DATABASE_SCHEMA.md](docs/DATABASE_SCHEMA.md) — skema Room & Firestore
- [docs/APP_FLOW.md](docs/APP_FLOW.md) — alur layar & alur keempat fungsi inti
- [docs/FIREBASE_SETUP.md](docs/FIREBASE_SETUP.md) — setup Firebase langkah demi langkah

## Keamanan & Privasi

- Telemetri pasif **opt-in**, berjalan sebagai foreground service dengan
  notifikasi yang selalu terlihat, dan berhenti saat logout/dinonaktifkan.
- Password tidak pernah disimpan di Firestore/Room; sesi dikelola Firebase Auth.
- `google-services.json`, `local.properties`, dan API key tidak di-commit.
- Firestore Security Rules membatasi penulisan pada pengguna terautentikasi
  dan mencegah impersonasi UID (lihat [firestore.rules](firestore.rules)).
- Keterbatasan MVP yang terdokumentasi: validasi geofence berjalan di sisi
  klien; penguatan yang disarankan adalah Cloud Function untuk validasi dan
  agregasi server-side (lihat docs/ARCHITECTURE.md).
