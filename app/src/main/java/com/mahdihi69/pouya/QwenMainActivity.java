package com.mahdihi69.pouya;

import android.app.*;
import android.os.*;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.speech.tts.TextToSpeech;
import java.lang.reflect.Field;
import java.util.Locale;

/** Adds Qwen3-TTS to the existing Pouya app without disturbing its existing voice/finance engine. */
public class QwenMainActivity extends MainActivity {
    private WebView page;
    private QwenTts qwen;
    private TextToSpeech fallback;
    private boolean fallbackReady;

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        qwen=new QwenTts(this);
        fallback=new TextToSpeech(this,s->{if(s==TextToSpeech.SUCCESS){fallback.setLanguage(new Locale("fa","IR"));fallback.setSpeechRate(.92f);fallbackReady=true;}});
        new Handler(Looper.getMainLooper()).postDelayed(this::installQwenBridge,1800);
    }
    private void installQwenBridge(){
        try{
            Field f=MainActivity.class.getDeclaredField("webView");f.setAccessible(true);page=(WebView)f.get(this);if(page==null)return;
            page.addJavascriptInterface(new QwenBridge(),"QwenVoice");
            page.evaluateJavascript("(function(){window.speak=function(t){QwenVoice.speak(String(t));};window.qwenSettings=function(){QwenVoice.settings();};var c=document.querySelector('.container');if(c&&!document.getElementById('qwenVoiceTools')){var b=document.createElement('div');b.id='qwenVoiceTools';b.innerHTML='<button onclick=QwenVoice.settings()>🔊 تنظیم صدای Qwen</button>';b.style='margin:10px 0;text-align:center';c.insertBefore(b,c.firstChild);} })();",null);
        }catch(Exception ignored){}
    }
    private void nativeSpeak(String text){runOnUiThread(()->{if(fallbackReady&&fallback!=null){fallback.stop();fallback.speak(text,TextToSpeech.QUEUE_FLUSH,null,"qwen_fallback");}});}
    private void settings(){runOnUiThread(()->{final android.widget.EditText in=new android.widget.EditText(this);in.setSingleLine(true);in.setHint("sk-...");String old=getSharedPreferences("pouya_settings",MODE_PRIVATE).getString("qwen_api_key","");in.setText(old);new AlertDialog.Builder(this).setTitle("🔊 Qwen3-TTS").setMessage("کلید Qwen / DashScope را وارد کن. اگر Qwen برای متن فارسی در دسترس نباشد، صدای فارسی گوشی خودکار استفاده می‌شود.").setView(in).setPositiveButton("ذخیره و تست",(d,w)->{qwen.setKey(in.getText().toString());nativeSpeak("تنظیمات صدا ذخیره شد");}).setNegativeButton("لغو",null).show();});}
    private class QwenBridge{
        @JavascriptInterface public void speak(String text){
            if(!qwen.hasKey()){settings();nativeSpeak(text);return;}
            qwen.speak(text,new QwenTts.Callback(){public void success(){}public void failure(String m){nativeSpeak(text);runOnUiThread(()->{if(page!=null)page.evaluateJavascript("try{showToast('⚠️ Qwen در دسترس نبود؛ صدای گوشی فعال شد')}catch(e){}",null);});}});
        }
        @JavascriptInterface public void settings(){QwenMainActivity.this.settings();}
    }
    @Override protected void onDestroy(){if(qwen!=null)qwen.stop();if(fallback!=null){fallback.stop();fallback.shutdown();}super.onDestroy();}
}
