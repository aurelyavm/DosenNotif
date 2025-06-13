# DosenDer (Dosen Reminder)
DosenDer adalah aplikasi Android yang dirancang sebagai proyek skripsi untuk memberikan notifikasi jadwal mengajar kepada dosen berdasarkan lokasi mereka dari kampus. Aplikasi ini dibangun menggunakan bahasa pemrograman Kotlin dan memanfaatkan teknologi Fused Location Provider API untuk menentukan waktu notifikasi yang optimal berdasarkan jarak dosen dari Fakultas Ilmu Komputer UPN Veteran Jakarta.
## Fitur Utama
Notifikasi Berbasis Lokasi: Mengirimkan notifikasi jadwal mengajar dengan waktu yang disesuaikan berdasarkan jarak dosen dari kampus.  
Integrasi API Jadwal: Mengambil data jadwal mengajar secara otomatis dari API kampus.  
Tampilan Jadwal: Menampilkan jadwal mengajar harian dan keseluruhan per semester.  
Histori Notifikasi: Menyimpan riwayat notifikasi yang telah dikirimkan.  
Profil Pengguna: Menampilkan informasi dosen dan statistik jadwal mengajar.  
## Teknologi yang Digunakan
Platform: Android (Min SDK 24 / Android 7.0 Nougat)  
Bahasa Pemrograman: Kotlin  
Database: Firebase Firestore  
Autentikasi: Firebase Authentication  
Location Service: Fused Location Provider API  
Architecture: MVVM (Model-View-ViewModel)  
UI Components: Material Design Components  
Networking: Retrofit2 dengan OkHttp3  
## Instalasi
1. Clone repository ini:
git clone https://github.com/aurelyavm/DosenNotif.git
2. Buka project di Android Studio
3. Tambahkan file google-services.json dari Firebase Console ke folder app/
4. Sinkronisasi project dengan Gradle files
5. Jalankan aplikasi pada emulator atau perangkat Android yang terhubung
## Cara Penggunaan
1. Download app pada link berikut: https://drive.google.com/drive/folders/1Ko2aW8sUb43zAT4YSSuhFQF_3fUptyhs?usp=sharing
2. Buka aplikasi, lalu lakukan login dengan memasukkan email dan password
3. Jika belum memiliki akun, lakukan registrasi dengan klik "Don't have an account? Register"
4. Saat pertama kali login, izinkan aplikasi untuk mengakses lokasi dan notifikasi pada perangkat
5. Lihat jadwal hari ini pada halaman home, dan jadwal keseluruhan pada halaman kalender.
6. Notifikasi akan muncul otomatis berdasarkan jarak pengguna dari kampus:
   0-10 km: 30 menit sebelum jadwal
   10-20 km: 60 menit sebelum jadwal
   20-30 km: 90 menit sebelum jadwal
   30-40 km: 120 menit sebelum jadwal
   40-50 km: 150 menit sebelum jadwal
## Author
Aurelya Vazila Mirajani  
Program Studi S1 Informatika  
Fakultas Ilmu Komputer  
Universitas Pembangunan Nasional Veteran Jakarta  
