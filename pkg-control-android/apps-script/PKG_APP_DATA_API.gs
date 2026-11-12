/**
 * PKG CONTROL CENTER - API ANDROID: SUMBER ERP RAW T22 (READ ONLY)
 *
 * Khusus Apps Script proyek "PKG CONTROL CENTER ANDROID API".
 * JANGAN tempel di PKG TERMUX DATABASE atau WA BOT FONNTE.
 *
 * Tujuan: jumlah status dashboard sama dengan snapshot ERP RAW, bukan
 * data "01 DATA TERPILIH" yang telah dideduplikasi.
 *
 * Aman: tidak menulis Google Sheet, tidak mengubah bot, tidak memanggil API ERP,
 * dan tetap memerlukan PKG_APP_TOKEN yang sudah disimpan sebelumnya.
 */

const PKG_APP_ERP_SHEET = '1DS1W7vRSmE8AGfaG4Hz-2st9tWJ1ADKwP4z6ASAXj14';
const PKG_APP_CONTROL = 'T22_CONTROL';
const PKG_APP_PAGE_MAX = 700;
// Android V1.1 requests 700, but can receive >700 rows and advances by rows.length.
// Send up to 2000 rows per response to reduce 13 network trips to about 5.
const PKG_APP_SEND_PAGE = 2000;
const PKG_APP_COLUMNS = [
  'NO', 'PROYEK', 'NAMA DEBITUR', 'BLOK', 'SALES',
  'TIM', 'STATUS', 'JENIS PENJUALAN', 'TGL BOOKING',
  'TGL SP3K', 'TGL AKAD', 'ID TRANSAKSI', 'AUDIT'
];

/** Setup aman; memakai kode yang lama jika sudah ada. */
function PKG_APP_SETUP() {
  const ss = SpreadsheetApp.openById(PKG_APP_ERP_SHEET);
  if (!ss.getSheetByName(PKG_APP_CONTROL)) {
    throw new Error('T22_CONTROL tidak ditemukan');
  }
  const p = PropertiesService.getScriptProperties();
  if (!p.getProperty('PKG_APP_TOKEN')) {
    p.setProperty('PKG_APP_TOKEN',
      Utilities.getUuid().replace(/-/g,'') +
      Utilities.getUuid().replace(/-/g,''));
  }
  console.log('SETUP BERHASIL | SUMBER=ERP RAW T22 | KODE LAMA DIPERTAHANKAN');
  console.log('KODE AKSES APLIKASI (RAHASIA): ' + p.getProperty('PKG_APP_TOKEN'));
  console.log('JANGAN BAGIKAN KODE KE GRUP, GITHUB, ATAU CHAT.');
}

/** Hanya untuk keadaan kode akses bocor; mengharuskan koneksi ulang Android. */
function PKG_APP_ROTASI_KODE() {
  const token=Utilities.getUuid().replace(/-/g,'') +
    Utilities.getUuid().replace(/-/g,'');
  PropertiesService.getScriptProperties().setProperty('PKG_APP_TOKEN',token);
  console.log('KODE BARU (RAHASIA): ' + token);
}

/** Angka di belakang ID run menentukan slot A/B yang terakhir diperbarui. */
function PKG_APP_RUN_NUMBER_(sheet) {
  if (!sheet || sheet.getLastRow() <= 1) return 0;
  const id = String(sheet.getRange(2,1).getDisplayValue() || '');
  const match = id.match(/^PKGS22_(\d+)$/);
  return match ? Number(match[1]) : 0;
}

/**
 * Membaca daftar cabang, memilih satu slot paling baru untuk setiap cabang.
 * Termasuk data yang biasanya dibuang oleh aturan deduplikasi nama.
 */
function PKG_APP_RAW_SLOTS_() {
  const ss=SpreadsheetApp.openById(PKG_APP_ERP_SHEET);
  const ctrl=ss.getSheetByName(PKG_APP_CONTROL);
  if (!ctrl) throw new Error('Sumber T22_CONTROL tidak ditemukan');
  const last=ctrl.getLastRow();
  const list=last>1?ctrl.getRange(2,1,last-1,3).getDisplayValues():[];
  const slots=[];
  let total=0;
  list.forEach(function(c) {
    const company=String(c[0]||'').trim();
    const branch=String(c[1]||'').trim();
    const proper=String(c[2]||'').trim().toUpperCase();
    if (!/^\d+$/.test(company)||!/^\d+$/.test(branch)) return;
    if (!proper.startsWith('PESONA KAHURIPAN')) return;

    const prefix='T22_B'+company+'_'+branch+'_';
    const a=ss.getSheetByName(prefix+'A');
    const b=ss.getSheetByName(prefix+'B');
    if(!a&&!b)return;
    const nA=PKG_APP_RUN_NUMBER_(a);
    const nB=PKG_APP_RUN_NUMBER_(b);
    const sheet=nA>=nB?(a||b):(b||a);
    const count=sheet?Math.max(0,sheet.getLastRow()-1):0;
    if(count===0)return;
    slots.push({sheet:sheet,proper:proper,count:count,start:total,run:Math.max(nA,nB)});
    total+=count;
  });
  return {slots:slots,total:total};
}

/** Uji READ ONLY. Angka dapat berubah karena sinkronisasi T22 terus berjalan. */
function PKG_APP_UJI_ERP_RAW_TANPA_MENULIS() {
  const data=PKG_APP_RAW_SLOTS_();
  const pk15=data.slots.find(function(x){return x.proper==='PESONA KAHURIPAN 15';});
  if(!pk15)throw new Error('Cabang PK15 belum ditemukan dalam T22');
  const values=pk15.sheet.getRange(2,14,pk15.count,1).getDisplayValues();
  const counts={};
  values.forEach(function(row) {
    const key=String(row[0]||'').trim().toUpperCase()||'TIDAK DIKENALI';
    counts[key]=(counts[key]||0)+1;
  });
  const sum=Object.keys(counts).reduce(function(s,k){return s+counts[k];},0);
  if (sum!==pk15.count)throw new Error('Perhitungan PK15 tidak konsisten');
  console.log('LULUS | ERP RAW T22 | TOTAL SEMUA PROYEK='+data.total);
  console.log('PK15 | RAW='+pk15.count
    +' | BOOKING='+(counts.BOOKING||0)
    +' | KANTOR='+(counts.KANTOR||0)
    +' | BANK='+(counts.BANK||0)
    +' | SP3K='+(counts.SP3K||0)
    +' | SP3K FINAL='+(counts['SP3K FINAL']||0)
    +' | AKAD='+(counts.AKAD||0)
    +' | CASH='+(counts.CASH||0)
    +' | CANCEL='+(counts.CANCEL||0)
    +' | REJECT='+(counts.REJECT||0));
  console.log('BACA SAJA | TIDAK MENULIS ERP, GOOGLE SHEETS, ATAU WHATSAPP');
}

/**
 * Digunakan APK Android 1.1 yang sudah terpasang.
 * Menghasilkan kolom sama persis dengan API sebelumnya, jadi APK tidak berubah.
 */
function doPost(e) {
  try {
    if (!e||!e.postData||!e.postData.contents)
      return PKG_APP_JSON_({ok:false,error:'No request'});
    const q=JSON.parse(e.postData.contents);
    const expected=PropertiesService.getScriptProperties().getProperty('PKG_APP_TOKEN');
    if (!expected||typeof q.token!=='string'||
        q.token.length!==expected.length||q.token!==expected)
      return PKG_APP_JSON_({ok:false,error:'Kode akses tidak valid'});
    if(q.action!=='rows')
      return PKG_APP_JSON_({ok:false,error:'Action tidak diizinkan'});
    const offset=Number(q.offset);
    const limit=Number(q.limit);
    if (!Number.isSafeInteger(offset)||offset<0||offset>50000||
        !Number.isSafeInteger(limit)||limit<1||limit>PKG_APP_PAGE_MAX)
      return PKG_APP_JSON_({ok:false,error:'Offset/limit invalid'});

    const src=PKG_APP_RAW_SLOTS_();
    const total=src.total;
    if(total>50000)
      return PKG_APP_JSON_({ok:false,error:'Dataset terlalu besar'});
    const sendLimit=Math.min(PKG_APP_SEND_PAGE,total-offset);
    let skip=offset,rows=[];
    for(let i=0;i<src.slots.length&&rows.length<sendLimit;i++){
      const slot=src.slots[i];
      if(skip>=slot.count){skip-=slot.count;continue;}
      const take=Math.min(sendLimit-rows.length,slot.count-skip);
      const part=slot.sheet.getRange(skip+2,1,take,20).getDisplayValues();
      for(let j=0;j<part.length;j++){
        const r=part[j];
        // T22 raw: D proyek, E id, G debitur, H blok, I sales,
        // J tim, K jenis, N status, O booking, R SP3K, S akad.
        rows.push([
          String(offset+rows.length+1),r[3],r[6],r[7],r[8],
          r[9],r[13],r[10],r[14],r[17],r[18],r[4],'ERP RAW'
        ]);
      }
      skip=0;
    }
    return PKG_APP_JSON_({
      ok:true,columns:PKG_APP_COLUMNS,total:total,offset:offset,
      rows:rows,source:'ERP RAW T22',
      note:'Snapshot per cabang diperbarui bergiliran; belum realtime per detik.'
    });
  } catch(err) {
    console.error('PKG APP RAW API: '+String(err&&err.message||err));
    return PKG_APP_JSON_({ok:false,error:'Gangguan membaca snapshot ERP RAW'});
  }
}

function doGet(){
  return ContentService.createTextOutput('PKG APP DATA API - AUTH REQUIRED');
}
function PKG_APP_JSON_(data){
  return ContentService.createTextOutput(JSON.stringify(data))
    .setMimeType(ContentService.MimeType.JSON);
}
