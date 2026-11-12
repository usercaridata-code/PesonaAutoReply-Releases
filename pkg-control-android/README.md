# PKG Control Center Android 1.1 — Google Sheets langsung

APK Android mengambil data dari Apps Script dashboard terpisah dengan koneksi HTTPS, token unik di Android Keystore, tanpa Node.js atau Railway.

**Langkah:** (1) buka file `PKG_APP_DATA_API.gs` di Apps Script proyek *baru* (bukan PKG TERMUX ENGINE), (2) jalankan fungsi `PKG_APP_SETUP` dan simpan kode akses yang tampil di log, (3) Terapkan -> Aplikasi Web -> Eksekusi sebagai saya -> Akses siapa saja -> Salin URL /exec, (4) buka APK -> Pengaturan Koneksi -> tempel URL dan kode -> Simpan & Hubungkan.

**PENTING:** jangan publikasikan kode akses; jangan gunakan BRIDGE_KEY bot. Akses Web App siapa saja diperlukan oleh APK yang tidak memakai akun Google, tetapi seluruh data pribadi hanya keluar setelah token khusus berhasil diverifikasi. Apps Script membaca tab `01 DATA TERPILIH` dari Sheet viewer, bukan ERP MASTER secara langsung.

Aplikasi memperbarui data ketika dibuka dan setiap ~5 menit saat tampak di layar. Sumber Google Sheets sendiri diperbarui mengikuti jadwal sebelumnya; bukan realtime per detik.

Aplikasi dapat membaca status, mencari, memfilter, mencetak PDF, berbagi WhatsApp manual, membuka Termux, dan mengirim file manual lewat Telegram. Kontrol langsung proses Termux, penggantian nomor bot dan arsip otomatis Telegram belum diaktifkan.

Tidak ada data debitur yang dipublikasikan melalui website. Data hanya tampil di aplikasi yang memegang kunci khusus. Apabila perangkat hilang, ganti `PKG_APP_TOKEN` melalui setup ulang yang aman (rotasi), dan hapus koneksi di perangkat lama.

Ini APK debug dari GitHub Actions, bukan rilis Play Store.
