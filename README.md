# EchoChat

Aplikasi chat pribadi ala WhatsApp — akun tamu (tanpa OTP/OAuth), UID acak per perangkat, sinkron real-time lewat Firestore.

**Slogan:** Ngobrol tanpa jejak, sepenuhnya milikmu
**Package:** `com.echochat.cid`
**Versi:** v1.5

## Fitur v1.5

- Chat teks 1-on-1 real-time + validasi UID aktif sebelum tambah teman
- **Grup chat**: admin (pembuat), jadikan admin, keluarkan anggota, tambah anggota langsung via UID
- Blokir & hapus pertemanan (otomatis hapus riwayat chat)
- Navbar bawah 4 tab: **Chat** (personal+grup, dengan badge belum-dibaca & pesan terakhir), **Kontak**, **Info Akun**, **Opsi Developer**
- **Dark mode** — toggle di Pengaturan
- **Wallpaper chat** custom per gambar, dengan 4 mode cakupan (semua/kontak tersimpan/kontak tertentu/tidak ada) — wallpaper tidak ikut ke-backup
- **UID bisa disembunyikan** dari tampilan
- **Link otomatis** biru+underline di chat
- **Notifikasi latar belakang 24/7** via Foreground Service (mirip app musik/VPN) — user perlu nonaktifkan optimasi baterai manual di HP-nya
- **Backup/restore** jadi satu file `.zip`: `profile.json`, `contacts.json`, dan per kontak `<nama>-<uid>-avatar.json` (base64) + `<nama>-<uid>-fullchat.json`. Bisa dipulihkan (termasuk UID lama) lewat tombol "Impor data" di pojok kiri atas layar setup — berguna setelah install ulang.

Tidak ada fitur media (foto/video/audio) maupun telepon/panggilan video — chat sengaja teks saja.

## Setup Firebase (wajib sebelum build)

1. `app/google-services.json` sudah disertakan (project Firebase milik pembuat proyek).
2. Firebase Console → **Firestore Database** → aktifkan (mode Native).
3. Tab **Rules** → tempel isi [`firestore.rules`](./firestore.rules) → **Publish**.
4. **Composite index** (baru di v1.5): fitur notifikasi latar belakang memakai collection-group query (`messages` + `array-contains participants` + `orderBy timestamp`). Firestore akan menolak query ini sampai index-nya dibuat. Cara termudah: aktifkan dulu toggle "Notifikasi latar belakang" di Pengaturan lewat app yang sudah running (misal via `adb logcat` atau Android Studio Logcat), lalu cari error dari Firestore yang berisi link `https://console.firebase.google.com/.../create_composite_index?...` — klik link itu sekali, tunggu index selesai dibangun (beberapa menit), habis itu fitur ini akan langsung jalan otomatis untuk semua user selanjutnya.
5. Tidak perlu SHA-1 apa pun — tidak pakai Firebase Auth/Google Sign-In.

## Build APK

### Lewat Android Studio
Buka folder ini → Run/Build → `app-debug.apk` muncul di `app/build/outputs/apk/debug/`.

### Lewat GitHub Actions
Push ke branch `main`, atau jalankan manual lewat tab **Actions** → **Build APK** → *Run workflow*. Unduh `app-debug` dari bagian **Artifacts**.

## Catatan & keterbatasan v1.5

- Notifikasi latar belakang saat ini hanya untuk chat personal, belum untuk pesan grup (bisa ditambahkan di update berikutnya).
- HP dengan battery-optimizer agresif (MIUI/ColorOS/Vivo) bisa tetap mematikan service walau sudah di-whitelist manual — ini kebiasaan pabrikan, bukan bug aplikasi.
- Foreground service berhenti otomatis saat HP di-restart; user perlu buka app lagi untuk mengaktifkan ulang.

## Struktur proyek

```
app/src/main/java/com/echochat/cid/
├── data/       Entity, DAO, Room database, FirestoreRepository, BackupManager
├── ui/         Activity, Fragment (navbar bawah), adapter RecyclerView
├── service/    NotificationListenerService (foreground service notifikasi)
└── util/       SessionManager, UidGenerator, ImageUtils
```
