package id.pesonakahuripan.pkgcontrol;

import android.app.Activity;
import android.app.PrintManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebResourceRequest;
import android.widget.Toast;

public final class MainActivity extends Activity {
  private static final int CSV_PICK = 17;
  private static final int TELEGRAM_PICK = 18;
  private WebView web;
  private ValueCallback<Uri[]> csvResult;

  @Override public void onCreate(Bundle bundle) {
    super.onCreate(bundle);
    getWindow().setStatusBarColor(0xff102a38);
    getWindow().setNavigationBarColor(0xff102a38);
    web = new WebView(this);
    setContentView(web);
    WebSettings s = web.getSettings();
    s.setJavaScriptEnabled(true);
    s.setDomStorageEnabled(false);
    s.setAllowFileAccess(true);
    s.setAllowContentAccess(true);
    s.setAllowUniversalAccessFromFileURLs(false);
    s.setAllowFileAccessFromFileURLs(false);
    s.setJavaScriptCanOpenWindowsAutomatically(false);
    web.setWebViewClient(new WebViewClient(){
      @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        String target=request.getUrl().toString();
        return !target.startsWith("file:///android_asset/");
      }
    });
    web.setWebChromeClient(new WebChromeClient(){
      @Override public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> callback, FileChooserParams params) {
        if (csvResult!=null) csvResult.onReceiveValue(null);
        csvResult=callback;
        try {
          Intent i=new Intent(Intent.ACTION_GET_CONTENT);
          i.addCategory(Intent.CATEGORY_OPENABLE);
          i.setType("*/*");
          i.addCategory(Intent.CATEGORY_OPENABLE);
          startActivityForResult(Intent.createChooser(i,"Pilih CSV penjualan"),CSV_PICK);
          return true;
        } catch(Exception e) {csvResult=null;return false;}
      }
    });
    web.addJavascriptInterface(new Bridge(),"PKG");
    web.loadUrl("file:///android_asset/index.html");
  }

  public final class Bridge {
    @JavascriptInterface public void shareWhatsApp(String msg) {
      runOnUiThread(()->{
        try{
          Intent i=new Intent(Intent.ACTION_SEND);
          i.setType("text/plain");
          i.putExtra(Intent.EXTRA_TEXT,msg);
          i.setPackage("com.whatsapp");
          startActivity(i);
        }catch(Exception ex){
          try {
            Intent alt=new Intent(Intent.ACTION_SEND);
            alt.setType("text/plain");
            alt.putExtra(Intent.EXTRA_TEXT,msg);
            startActivity(Intent.createChooser(alt,"Pilih aplikasi WhatsApp"));
          }catch(Exception error){toast("Tidak ditemukan aplikasi berbagi pesan.");}
        }
      });
    }
    @JavascriptInterface public void printPage(){
      runOnUiThread(()->{
        try{
          PrintManager manager=(PrintManager)getSystemService(Context.PRINT_SERVICE);
          manager.print("PKG-Laporan-Debitur",web.createPrintDocumentAdapter("PKG-Debitur"),null);
        }catch(Exception e){toast("Fitur PDF tidak tersedia pada perangkat.");}
      });
    }
    @JavascriptInterface public void openTermux(){
      runOnUiThread(()->{
        try{
          Intent i=getPackageManager().getLaunchIntentForPackage("com.termux");
          if(i==null){toast("Termux belum terdeteksi.");return;}
          startActivity(i);
        }catch(Exception e){toast("Gagal membuka Termux.");}
      });
    }
    @JavascriptInterface public void shareTelegramFile(){
      runOnUiThread(()->{
        try{
          Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
          i.addCategory(Intent.CATEGORY_OPENABLE);
          i.setType("*/*");
          startActivityForResult(i,TELEGRAM_PICK);
        }catch(Exception e){toast("Pemilih file tidak tersedia.");}
      });
    }
  }
  private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
  @Override protected void onActivityResult(int request,int result,Intent data){
    super.onActivityResult(request,result,data);
    if(request==CSV_PICK){
      if(csvResult!=null){
        Uri[] uris=WebChromeClient.FileChooserParams.parseResult(result,data);
        csvResult.onReceiveValue(uris);
        csvResult=null;
      }
    } else if(request==TELEGRAM_PICK && result==RESULT_OK && data!=null){
      Uri selected=data.getData();if(selected==null)return;
      try{
        Intent share=new Intent(Intent.ACTION_SEND);
        share.setType(getContentResolver().getType(selected)==null?"application/octet-stream":getContentResolver().getType(selected));
        share.putExtra(Intent.EXTRA_STREAM,selected);
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        share.setPackage("org.telegram.messenger");
        startActivity(share);
      } catch(Exception e) {
        try{
          Intent share=new Intent(Intent.ACTION_SEND);
          share.setType("*/*");
          share.putExtra(Intent.EXTRA_STREAM,selected);
          share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
          startActivity(Intent.createChooser(share,"Kirim file melalui Telegram"));
        } catch(Exception ex){toast("Tidak ada aplikasi penerima file.");}
      }
    }
  }
  @Override public void onBackPressed(){if(web!=null&&web.canGoBack())web.goBack();else super.onBackPressed();}
  @Override protected void onDestroy(){if(web!=null)web.destroy();super.onDestroy();}
}