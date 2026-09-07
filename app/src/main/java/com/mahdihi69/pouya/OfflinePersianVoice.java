package com.mahdihi69.pouya;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.RecognitionListener;
import org.vosk.android.SpeechService;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Persian speech recognition that works locally after the model is installed. */
public final class OfflinePersianVoice {
    public interface Callback {
        void status(String text);
        void result(String text);
        void error(String text);
    }

    private static final String MODEL_URL = "https://alphacephei.com/vosk/models/vosk-model-small-fa-0.42.zip";
    private final Context context;
    private final Callback callback;
    private final Handler main = new Handler(Looper.getMainLooper());
    private Model model;
    private SpeechService service;
    private volatile boolean preparing;

    public OfflinePersianVoice(Context context, Callback callback) {
        this.context = context.getApplicationContext();
        this.callback = callback;
    }

    public boolean isInstalled() {
        File dir = new File(context.getFilesDir(), "vosk-fa");
        return new File(dir, "conf/model.conf").exists() || new File(dir, "am/final.mdl").exists();
    }

    public void start() {
        if (preparing) return;
        if (service != null) { stop(); return; }
        if (isInstalled()) loadAndStart(); else downloadModel();
    }

    private void loadAndStart() {
        preparing = true;
        status("🔒 آماده‌سازی فرمان صوتی آفلاین...");
        new Thread(() -> {
            try {
                if (model == null) model = new Model(new File(context.getFilesDir(), "vosk-fa").getAbsolutePath());
                Recognizer rec = new Recognizer(model, 16000.0f);
                service = new SpeechService(rec, 16000.0f);
                main.post(() -> {
                    preparing = false;
                    service.startListening(new RecognitionListener() {
                        @Override public void onPartialResult(String hypothesis) { }
                        @Override public void onResult(String hypothesis) { deliver(hypothesis); }
                        @Override public void onFinalResult(String hypothesis) { deliver(hypothesis); status("🔒 آفلاین؛ گوش می‌دهم..."); }
                        @Override public void onError(Exception e) { error("خطای صدای آفلاین: " + e.getMessage()); }
                        @Override public void onTimeout() { if (service != null) status("🔒 آفلاین؛ گوش می‌دهم..."); }
                    });
                    status("🔒 آفلاین؛ پویا گوش می‌دهد...");
                });
            } catch (Exception e) {
                preparing = false;
                error("مدل صوتی آفلاین اجرا نشد: " + e.getMessage());
            }
        }).start();
    }

    private void downloadModel() {
        preparing = true;
        status("⬇️ یک‌بار مدل فارسی (~۵۳ مگابایت) دانلود می‌شود؛ بعد از آن بدون اینترنت کار می‌کند...");
        new Thread(() -> {
            File base = new File(context.getFilesDir(), "vosk-download.zip");
            File target = new File(context.getFilesDir(), "vosk-fa");
            try {
                HttpURLConnection c = (HttpURLConnection) new URL(MODEL_URL).openConnection();
                c.setConnectTimeout(20000); c.setReadTimeout(120000); c.setRequestMethod("GET");
                if (c.getResponseCode() != 200) throw new Exception("HTTP " + c.getResponseCode());
                try (InputStream in = c.getInputStream(); FileOutputStream out = new FileOutputStream(base)) {
                    byte[] buf = new byte[8192]; int n; long total=0; int last=-1;
                    while ((n=in.read(buf))!=-1) { out.write(buf,0,n); total+=n; int p=(int)((total*100L)/Math.max(1,c.getContentLengthLong())); if(p!=last&&p%10==0){last=p;status("⬇️ دریافت مدل صوتی: "+p+"٪");} }
                } finally { c.disconnect(); }
                if (target.exists()) delete(target);
                unzip(base, context.getFilesDir());
                File extracted = new File(context.getFilesDir(), "vosk-model-small-fa-0.42");
                if (!extracted.exists()) throw new Exception("مدل استخراج نشد");
                if (!extracted.renameTo(target)) { copyDir(extracted,target); delete(extracted); }
                base.delete();
                preparing=false;
                status("✅ مدل فارسی آماده شد؛ از این به بعد فرمان صوتی کاملاً آفلاین است.");
                loadAndStart();
            } catch (Exception e) {
                preparing=false; base.delete(); error("دانلود مدل صوتی ناموفق بود؛ اینترنت را یک‌بار روشن کنید.");
            }
        }).start();
    }

    private void unzip(File zip, File dest) throws Exception {
        try (ZipInputStream zin = new ZipInputStream(new java.io.FileInputStream(zip))) {
            ZipEntry e; byte[] buf=new byte[8192];
            while((e=zin.getNextEntry())!=null){File out=new File(dest,e.getName());if(e.isDirectory()){out.mkdirs();continue;}File parent=out.getParentFile();if(parent!=null)parent.mkdirs();try(FileOutputStream f=new FileOutputStream(out)){int n;while((n=zin.read(buf))!=-1)f.write(buf,0,n);}}
        }
    }
    private void copyDir(File a, File b) throws Exception { b.mkdirs(); File[] fs=a.listFiles(); if(fs==null)return; for(File f:fs){File d=new File(b,f.getName());if(f.isDirectory())copyDir(f,d);else{try(InputStream in=new java.io.FileInputStream(f);FileOutputStream out=new FileOutputStream(d)){byte[] x=new byte[8192];int n;while((n=in.read(x))!=-1)out.write(x,0,n);}}} }
    private void delete(File f){if(f.isDirectory()){File[] fs=f.listFiles();if(fs!=null)for(File x:fs)delete(x);}f.delete();}
    private void deliver(String hypothesis){try{String t=new JSONObject(hypothesis).optString("text","").trim();if(!t.isEmpty())main.post(()->callback.result(t));}catch(Exception ignored){}}
    private void status(String s){main.post(()->callback.status(s));}
    private void error(String s){main.post(()->callback.error(s));}
    public void stop(){main.post(()->{if(service!=null){service.stop();service.shutdown();service=null;}status("⏹️ فرمان صوتی آفلاین متوقف شد");});}
    public void release(){stop();if(model!=null){model.close();model=null;}}
}
