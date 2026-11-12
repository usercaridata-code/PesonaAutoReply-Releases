package id.pesonakahuripan.pkgcontrol;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Akses data melalui Google Apps Script: HTTPS, token terenkripsi Android Keystore. */
public final class PkgPrivateSync {
  private static final String STORE = "pkg_sync_private_v1";
  private static final String URL_KEY = "endpoint";
  private static final String TOKEN_KEY = "token_enc";
  private static final String MASTER = "pkg_control_sync_aes_gcm_v1";
  private static final int PAGE = 700;
  private final Activity activity;
  private final WebView web;
  private final SharedPreferences store;
  private final AtomicBoolean busy = new AtomicBoolean(false);

  public PkgPrivateSync(Activity activity, WebView web) {
    this.activity = activity;
    this.web = web;
    this.store = activity.getSharedPreferences(STORE, Context.MODE_PRIVATE);
  }

  @JavascriptInterface public boolean configured() {
    return store.contains(URL_KEY) && store.contains(TOKEN_KEY);
  }

  /** Kode akses tidak disimpan sebagai teks biasa dan tidak dikirim kembali ke JavaScript. */
  @JavascriptInterface public String saveConnection(String endpoint, String token) {
    try {
      String url = endpoint == null ? "" : endpoint.trim();
      if (!url.matches("^https://script\\.google\\.com/macros/s/[A-Za-z0-9_-]+/exec$"))
        return "URL tidak sesuai alamat Web App Apps Script /exec.";
      if (token == null || token.trim().length() < 32)
        return "Kode akses minimal 32 karakter. Ambil dari log fungsi PKG_APP_SETUP.";
      String sealed = encrypt(token.trim());
      if (!store.edit().putString(URL_KEY, url).putString(TOKEN_KEY, sealed).commit())
        return "Gagal menyimpan pengaturan.";
      return "OK";
    } catch (Exception e) {
      return "Penyimpanan aman Android belum tersedia.";
    }
  }

  @JavascriptInterface public void forgetConnection() {
    store.edit().clear().apply();
  }

  @JavascriptInterface public void refresh() {
    if (!busy.compareAndSet(false, true)) {
      js("window.PKGSyncBusy&&window.PKGSyncBusy();");
      return;
    }
    new Thread(() -> {
      try {
        String endpoint=store.getString(URL_KEY,"");
        String sealed=store.getString(TOKEN_KEY,"");
        if (endpoint.isEmpty() || sealed.isEmpty()) throw new IllegalStateException("Sambungan belum diatur.");
        String token=decrypt(sealed);
        int offset=0, total=-1, rounds=0;
        js("window.PKGSyncBegin&&window.PKGSyncBegin();");
        do {
          JSONObject req = new JSONObject();
          req.put("token",token);
          req.put("action","rows");
          req.put("offset",offset);
          req.put("limit",PAGE);
          String body = request(endpoint,req.toString());
          JSONObject result = new JSONObject(body);
          if (!result.optBoolean("ok", false))
            throw new IllegalStateException(result.optString("error","Akses ditolak oleh Apps Script."));
          if (total < 0) total=result.getInt("total");
          JSONArray list=result.getJSONArray("rows");
          if (list.length() <= 0 && offset < total) throw new IllegalStateException("Sumber tidak mengembalikan data.");
          offset+=list.length();
          js("window.PKGSyncPage&&window.PKGSyncPage("+JSONObject.quote(body)+");");
          rounds++;
          if (rounds>80 || total>50000) throw new IllegalStateException("Batas ukuran data terlampaui.");
        } while(offset<total);
        js("window.PKGSyncDone&&window.PKGSyncDone();");
      } catch(Exception e) {
        // Jangan mencetak kode akses maupun body respons ke log.
        String error=e.getMessage()==null?"Gagal menghubungi sumber.":e.getMessage();
        if(error.length()>150) error=error.substring(0,150);
        js("window.PKGSyncError&&window.PKGSyncError("+JSONObject.quote(error)+");");
      } finally {
        busy.set(false);
      }
    },"PKG-Read-Only-Sync").start();
  }

  private static String request(String url, String json) throws Exception {
    HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();
    c.setInstanceFollowRedirects(true);
    c.setRequestMethod("POST");
    c.setDoOutput(true);
    c.setConnectTimeout(25000);
    c.setReadTimeout(75000);
    c.setRequestProperty("Content-Type","application/json; charset=utf-8");
    c.setRequestProperty("Accept","application/json");
    try {
      byte[] bytes=json.getBytes(StandardCharsets.UTF_8);
      try(OutputStream out=c.getOutputStream()){out.write(bytes);}
      int status=c.getResponseCode();
      if(status>=400) throw new IllegalStateException("HTTP "+status+" dari Apps Script. Periksa deployment dan akses.");
      try(InputStream in=c.getInputStream(); ByteArrayOutputStream out=new ByteArrayOutputStream()){
        byte[] buf=new byte[8192];int n;
        while((n=in.read(buf))!=-1){
          out.write(buf,0,n);
          if(out.size()>4_000_000) throw new IllegalStateException("Satu halaman data terlalu besar.");
        }
        return out.toString("UTF-8");
      }
    } finally {c.disconnect();}
  }

  private void js(String script) {
    activity.runOnUiThread(() -> {
      if(!activity.isFinishing() && !activity.isDestroyed()) web.evaluateJavascript(script,null);
    });
  }

  private static SecretKey key() throws Exception {
    KeyStore ks=KeyStore.getInstance("AndroidKeyStore");
    ks.load(null);
    if(!ks.containsAlias(MASTER)){
      KeyGenerator gen=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
      gen.init(new KeyGenParameterSpec.Builder(MASTER,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
        .setKeySize(256)
        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
      gen.generateKey();
    }
    return (SecretKey)ks.getKey(MASTER,null);
  }

  private static String encrypt(String value) throws Exception {
    Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(Cipher.ENCRYPT_MODE,key());
    byte[] iv=cipher.getIV(), encrypted=cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
    byte[] data=new byte[iv.length+encrypted.length];
    System.arraycopy(iv,0,data,0,iv.length);
    System.arraycopy(encrypted,0,data,iv.length,encrypted.length);
    return Base64.encodeToString(data,Base64.NO_WRAP);
  }

  private static String decrypt(String encoded) throws Exception {
    byte[] payload=Base64.decode(encoded,Base64.NO_WRAP);
    if(payload.length<29) throw new IllegalStateException("Kode tersimpan rusak.");
    byte[] nonce=new byte[12];System.arraycopy(payload,0,nonce,0,12);
    Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,nonce));
    return new String(cipher.doFinal(payload,12,payload.length-12),StandardCharsets.UTF_8);
  }
}
