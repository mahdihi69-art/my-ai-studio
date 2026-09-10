package com.mahdihi69.story;

import android.app.*;
import android.os.*;
import android.graphics.Color;
import android.content.*;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    EditText topic, character, lesson; Spinner genre, age; TextView output; Button generate, speak; android.speech.tts.TextToSpeech tts; final ExecutorService net=Executors.newSingleThreadExecutor();
    int dp(float x){return (int)(x*getResources().getDisplayMetrics().density+.5f);}
    TextView label(String s){TextView v=new TextView(this);v.setText(s);v.setTextSize(15);v.setTextColor(Color.rgb(35,35,55));v.setPadding(0,dp(7),0,dp(4));return v;}
    EditText edit(String hint){EditText e=new EditText(this);e.setHint(hint);e.setTextSize(15);e.setSingleLine(false);e.setPadding(dp(14),dp(10),dp(14),dp(10));return e;}
    @Override public void onCreate(Bundle b){super.onCreate(b); getWindow().setStatusBarColor(Color.WHITE);getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(18),dp(12),dp(18),dp(18));root.setBackgroundColor(Color.rgb(246,247,251));
        ScrollView scroll=new ScrollView(this);LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);
        TextView title=label("📚 داستان‌ساز هوشمند");title.setTextSize(26);title.setGravity(Gravity.CENTER);box.addView(title,new LinearLayout.LayoutParams(-1,dp(60)));
        TextView sub=label("داستان اختصاصی برای کودک، نوجوان و بزرگسال");sub.setGravity(Gravity.CENTER);box.addView(sub);
        box.addView(label("موضوع داستان"));topic=edit("مثلاً دوستی، شجاعت یا یک ماجرای فضایی");box.addView(topic);
        box.addView(label("نام شخصیت‌ها (اختیاری)"));character=edit("مثلاً آرش و نیلوفر");box.addView(character);
        box.addView(label("ژانر"));genre=new Spinner(this);genre.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"ماجراجویی","آموزشی","طنز","فانتزی","اخلاقی","علمی-تخیلی","احساسی"}));box.addView(genre);
        box.addView(label("رده سنی"));age=new Spinner(this);age.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"کودک ۴ تا ۷ سال","کودک ۸ تا ۱۲ سال","نوجوان","بزرگسال"}));box.addView(age);
        box.addView(label("پند یا آموزش"));lesson=edit("مثلاً اهمیت راستگویی؛ اختیاری");box.addView(lesson);
        generate=new Button(this);generate.setText("✨ ساخت داستان جدید");box.addView(generate,new LinearLayout.LayoutParams(-1,dp(52)));
        output=label("داستان شما اینجا ساخته می‌شود…");output.setTextSize(16);output.setLineSpacing(0,1.35f);output.setPadding(dp(14),dp(18),dp(14),dp(18));box.addView(output);
        speak=new Button(this);speak.setText("🔊 خواندن داستان");box.addView(speak,new LinearLayout.LayoutParams(-1,dp(50)));
        scroll.addView(box);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);
        tts=new android.speech.tts.TextToSpeech(this,s->{if(s==android.speech.tts.TextToSpeech.SUCCESS){tts.setLanguage(new java.util.Locale("fa","IR"));tts.setSpeechRate(.9f);}});
        generate.setOnClickListener(v->generate());speak.setOnClickListener(v->{if(tts!=null)tts.speak(output.getText().toString(),android.speech.tts.TextToSpeech.QUEUE_FLUSH,null,"story");});
    }
    void generate(){String t=topic.getText().toString().trim();if(t.isEmpty()){topic.setError("موضوع را وارد کنید");return;}generate.setEnabled(false);output.setText("⏳ در حال داستان‌پردازی…");
        final String prompt="یک داستان کاملاً تازه و جذاب به فارسی بنویس. موضوع: "+t+". شخصیت‌ها: "+character.getText().toString().trim()+". ژانر: "+genre.getSelectedItem()+". رده سنی: "+age.getSelectedItem()+". پند/آموزش: "+lesson.getText().toString().trim()+". داستان عنوان داشته باشد، شروع جذاب، اوج و پایان آموزنده داشته باشد. از کلیشه و کپی‌برداری دوری کن و برای شنیدن با صدای بلند روان بنویس.";
        net.execute(()->{try{String key=BuildConfig.GEMINI_API_KEY;if(key==null||key.trim().isEmpty()){runOnUiThread(()->{output.setText("⚠️ سرویس هوش مصنوعی هنوز روی نسخه آزمایشی فعال نشده است.");generate.setEnabled(true);});return;}JSONObject body=new JSONObject();JSONArray c=new JSONArray();JSONObject co=new JSONObject();JSONArray p=new JSONArray();p.put(new JSONObject().put("text",prompt));co.put("role","user").put("parts",p);c.put(co);body.put("contents",c);String[] models={"gemini-3.8-flash","gemini-3.7-flash","gemini-3.5-flash-lite"};String text=null;for(String m:models){HttpURLConnection x=(HttpURLConnection)new URL("https://generativelanguage.googleapis.com/v1beta/models/"+m+":generateContent").openConnection();x.setRequestMethod("POST");x.setConnectTimeout(15000);x.setReadTimeout(45000);x.setDoOutput(true);x.setRequestProperty("Content-Type","application/json");x.setRequestProperty("x-goog-api-key",key);try(OutputStream o=x.getOutputStream()){o.write(body.toString().getBytes(StandardCharsets.UTF_8));}int code=x.getResponseCode();InputStream in=code>=200&&code<300?x.getInputStream():x.getErrorStream();String r=read(in);x.disconnect();if(code>=200&&code<300){JSONObject q=new JSONObject(r);text=q.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text");break;}}final String result=text;runOnUiThread(()->{output.setText(result==null?"❌ ساخت داستان انجام نشد؛ اتصال سرویس را بررسی کنید.":result);generate.setEnabled(true);});}catch(Exception e){runOnUiThread(()->{output.setText("❌ خطا در ساخت داستان. دوباره تلاش کنید.");generate.setEnabled(true);});}});}
    String read(InputStream in)throws Exception{if(in==null)return "";StringBuilder s=new StringBuilder();try(BufferedReader r=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8))){String l;while((l=r.readLine())!=null)s.append(l);}return s.toString();}
    @Override protected void onDestroy(){net.shutdownNow();if(tts!=null){tts.stop();tts.shutdown();}super.onDestroy();}
}
