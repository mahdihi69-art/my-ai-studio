package com.mahdihi69.dastansaz;

import android.content.Context;
import android.media.MediaPlayer;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.io.ByteArrayOutputStream;
import org.json.JSONObject;

/** Qwen3-TTS cloud voice; caller supplies fallback when Qwen is unavailable. */
public class QwenTts {
    public interface Callback { void success(); void failure(String message); }
    private final Context context; private MediaPlayer player; private volatile boolean busy;
    private static final String PREFS="dastansaz_settings", KEY="qwen_api_key";
    private static final String MODEL="qwen3-tts-flash";
    private static final String ENDPOINT="https://dashscope-intl.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation";
    public QwenTts(Context c){context=c.getApplicationContext();}
    public boolean hasKey(){return !context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getString(KEY,"").trim().isEmpty();}
    public void setKey(String key){context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putString(KEY,key==null?"":key.trim()).apply();}
    public void stop(){try{if(player!=null){player.stop();player.release();player=null;}}catch(Exception ignored){}busy=false;}
    public void speak(String text,Callback cb){final String key=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getString(KEY,"").trim();if(key.isEmpty()){if(cb!=null)cb.failure("Qwen API key تنظیم نشده");return;}if(text==null||text.trim().isEmpty()){if(cb!=null)cb.failure("متن خالی است");return;}stop();busy=true;new Thread(()->{HttpURLConnection c=null;try{JSONObject input=new JSONObject();input.put("text",text);input.put("voice","Cherry");input.put("language_type",detectLanguage(text));JSONObject body=new JSONObject();body.put("model",MODEL);body.put("input",input);c=(HttpURLConnection)new URL(ENDPOINT).openConnection();c.setRequestMethod("POST");c.setConnectTimeout(15000);c.setReadTimeout(60000);c.setDoOutput(true);c.setRequestProperty("Authorization","Bearer "+key);c.setRequestProperty("Content-Type","application/json");c.getOutputStream().write(body.toString().getBytes(StandardCharsets.UTF_8));int code=c.getResponseCode();InputStream in=(code>=200&&code<300)?c.getInputStream():c.getErrorStream();String raw=read(in);if(code<200||code>=300)throw new Exception("Qwen HTTP "+code);JSONObject out=new JSONObject(raw).optJSONObject("output");JSONObject audio=out==null?null:out.optJSONObject("audio");String url=audio==null?"":audio.optString("url","");if(url.isEmpty())throw new Exception("Qwen فایل صوتی برنگرداند");c.disconnect();c=null;final String audioUrl=url;((android.app.Activity)context).runOnUiThread(()->play(audioUrl,cb));}catch(Exception e){busy=false;if(c!=null)c.disconnect();if(cb!=null)cb.failure(e.getMessage()==null?"Qwen TTS خطا داد":e.getMessage());}}).start();}
    private void play(String url,Callback cb){try{stop();player=new MediaPlayer();player.setDataSource(url);player.setOnPreparedListener(mp->{busy=true;mp.start();if(cb!=null)cb.success();});player.setOnCompletionListener(mp->{busy=false;try{mp.release();}catch(Exception ignored){}player=null;});player.setOnErrorListener((mp,w,e)->{busy=false;try{mp.release();}catch(Exception ignored){}player=null;if(cb!=null)cb.failure("پخش صدای Qwen ناموفق بود");return true;});player.prepareAsync();}catch(Exception e){busy=false;if(cb!=null)cb.failure(e.getMessage()==null?"پخش Qwen ناموفق بود":e.getMessage());}}
    private String detectLanguage(String text){for(int i=0;i<text.length();i++){char ch=text.charAt(i);if((ch>='\u0600'&&ch<='\u06ff')||(ch>='\u0750'&&ch<='\u077f'))return "Auto";}return "English";}
    private String read(InputStream in)throws Exception{if(in==null)return "";ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] x=new byte[4096];int n;while((n=in.read(x))!=-1)b.write(x,0,n);return b.toString("UTF-8");}
}
