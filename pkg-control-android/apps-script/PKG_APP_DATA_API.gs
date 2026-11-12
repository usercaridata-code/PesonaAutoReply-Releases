/**
 * PKG CONTROL CENTER - API BACA SAJA UNTUK ANDROID
 * Pasang pada proyek Apps Script BARU (standalone), bukan proyek bot.
 * Jangan bagikan PKG_APP_TOKEN, password ERP, atau BRIDGE_KEY.
 * Data dikirim hanya via HTTPS setelah verifikasi token khusus.
 */
const PKG_APP_VIEWER = '1podkIpS4kT-J0e4U86JWXsR6yeHSmbmf56m-MQw4ook';
const PKG_APP_TAB = '01 DATA TERPILIH';
const PKG_APP_PAGE_MAX = 700;

/** Jalankan sekali sebelum deploy; catat kode akses rahasia dari Execution Log. */
function PKG_APP_SETUP() {
  const ss = SpreadsheetApp.openById(PKG_APP_VIEWER);
  const sheet = ss.getSheetByName(PKG_APP_TAB);
  if (!sheet) throw new Error('Tab sumber tidak ditemukan: ' + PKG_APP_TAB);
  const p = PropertiesService.getScriptProperties();
  if (!p.getProperty('PKG_APP_TOKEN')) {
    p.setProperty('PKG_APP_TOKEN',
      Utilities.getUuid().replace(/-/g,'') + Utilities.getUuid().replace(/-/g,''));
  }
  console.log('SETUP BERHASIL | SUMBER=' + ss.getName() + ' | TOTAL BARIS=' + Math.max(0,sheet.getLastRow()-1));
  console.log('KODE AKSES APLIKASI (RAHASIA): ' + p.getProperty('PKG_APP_TOKEN'));
  console.log('JANGAN MASUKKAN KODE INI KE GITHUB, WHATSAPP, ATAU WEBSITE PUBLIK.');
}

/** Jika kode bocor, jalankan fungsi ini untuk membatalkan kode lama. */
function PKG_APP_ROTASI_KODE() {
  const token=Utilities.getUuid().replace(/-/g,'')+Utilities.getUuid().replace(/-/g,'');
  PropertiesService.getScriptProperties().setProperty('PKG_APP_TOKEN',token);
  console.log('KODE BARU (RAHASIA): '+token);
}

/** Kode Apps Script ini hanya membaca dan tidak pernah mengubah spreadsheet. */
function doPost(e) {
  try {
    if (!e || !e.postData || !e.postData.contents) return PKG_APP_JSON_({ok:false,error:'No request'});
    const q = JSON.parse(e.postData.contents);
    const token = PropertiesService.getScriptProperties().getProperty('PKG_APP_TOKEN');
    if (!token || typeof q.token!=='string' || q.token.length!==token.length || q.token!==token)
      return PKG_APP_JSON_({ok:false,error:'Kode akses tidak valid'});
    if (q.action!=='rows') return PKG_APP_JSON_({ok:false,error:'Action tidak diizinkan'});
    const offset = Number(q.offset);
    const limit = Number(q.limit);
    if (!Number.isSafeInteger(offset) || offset<0 || offset>50000 ||
        !Number.isSafeInteger(limit) || limit<1 || limit>PKG_APP_PAGE_MAX)
      return PKG_APP_JSON_({ok:false,error:'Offset/limit invalid'});
    const sheet=SpreadsheetApp.openById(PKG_APP_VIEWER).getSheetByName(PKG_APP_TAB);
    if (!sheet) throw new Error('Sumber tidak ditemukan');
    const cols=13;
    const total=Math.max(0,sheet.getLastRow()-1);
    if(total>50000) return PKG_APP_JSON_({ok:false,error:'Data terlalu besar'});
    const columns=sheet.getRange(1,1,1,cols).getDisplayValues()[0];
    const n=Math.max(0,Math.min(limit,total-offset));
    const rows=n?sheet.getRange(offset+2,1,n,cols).getDisplayValues():[];
    return PKG_APP_JSON_({ok:true,columns,total,offset,rows});
  } catch(err) {
    console.error('PKG APP API ERR: '+String(err && err.message || err));
    return PKG_APP_JSON_({ok:false,error:'Gangguan membaca spreadsheet'});
  }
}
function doGet(){return ContentService.createTextOutput('PKG APP DATA API - PRIVATE');}
function PKG_APP_JSON_(obj){
  return ContentService.createTextOutput(JSON.stringify(obj)).setMimeType(ContentService.MimeType.JSON);
}
