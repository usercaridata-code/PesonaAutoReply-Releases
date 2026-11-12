# PKG Control Center Android — Offline V1

Versi APK offline, khusus Android, tanpa Railway, server Node.js, login atau izin internet.

**Fitur aktif:** impor CSV Google Sheets secara lokal; KPI 9 status; filter tanggal booking/SP3K/akad, proyek, tim dan status; pencarian debitur/sales/blok/ID; buka detail; cetak/simpan PDF melalui print Android; berbagi teks ke WhatsApp pribadi; kirim file manual ke Telegram Saved Messages via Android sharesheet; tombol membuka Termux; panduan gangguan koneksi.

**Belum terhubung:** pembaruan realtime Google Sheets, monitoring/remote control proses Termux, start/stop otomatis, ganti nomor bot, sinkronisasi akun, backup terenkripsi Telegram. Fitur tersebut memerlukan penghubung lokal tambahan yang belum dibuat agar tidak merusak bot V6 yang sudah aktif. Jangan perlakukan UI offline ini sebagai tampilan langsung status bot/ERP.

Data CSV diproses **di RAM**, tidak dikirim ke internet ataupun disimpan permanen oleh aplikasi. Import kembali ketika dibutuhkan. Ekspor CSV tidak ada dalam build awal.

## Cara membangun
GitHub Actions build di branch `pkg-control-center-android-offline-v1` → download artifact `PKG-Control-Center-Android-Offline-APK` → ekstrak APK → kirim ke Android → instal. APK debug bukan rilis Play Store.

## Format sumber
Google Sheets `PKG TERMUX — DATA FIX & LAPORAN` → `01 DATA TERPILIH` → File → Download → CSV. Header minimal: `PROYEK`, `NAMA DEBITUR`, `STATUS`; header lainnya: `BLOK`, `SALES`, `TIM`, `JENIS PENJUALAN`, `TGL BOOKING`, `TGL SP3K`, `TGL AKAD`, `ID TRANSAKSI`.

Tidak menyentuh Fonnte, ERP, atau WhatsApp Bot Termux.
