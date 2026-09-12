package com.mahdihi69.story;

import android.app.*;
import android.os.*;
import android.graphics.Color;
import android.content.*;
import android.view.*;
import android.widget.*;
import android.graphics.drawable.GradientDrawable;
import android.speech.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;

public class MainActivity extends Activity {
    EditText topic, character, lesson, ending;
    Spinner genre, age, voice, length;
    CheckBox poem;
    TextView output, status, voiceStatus;
    Button generate, speak, stopSpeak, mic, save, share;
    SpeechRecognizer recognizer;
    android.speech.tts.TextToSpeech tts;
    boolean ttsReady=false;
    final ExecutorService work=Executors.newSingleThreadExecutor();
    final String QWEN_URL="https://dashscope-intl.aliyuncs.com/compatible-mode/v1/chat/completions";

    int dp(float x){return (int)(x*getResources().getDisplayMetrics().density+.5f);}
    TextView text(String s,float size,int color){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(color);return v;}
    EditText edit(String hint){EditText e=new EditText(this);e.setHint(hint);e.setTextSize(16);e.setSingleLine(false);e.setMinHeight(dp(58));e.setPadding(dp(14),dp(10),dp(14),dp(10));e.setBackground(round(Color.WHITE,dp(14)));return e;}
    GradientDrawable round(int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(radius);g.setStroke(dp(1),Color.rgb(225,226,235));return g;}
    LinearLayout card(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(14),dp(7),dp(14),dp(12));c.setBackground(round(Color.WHITE,dp(18)));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(6),0,dp(6));c.setLayoutParams(p);return c;}
    TextView label(String a,String b,String c){TextView v=text(a+"  "+b+"\n"+c,16,Color.rgb(45,45,65));v.setPadding(dp(5),dp(8),dp(5),dp(8));return v;}
    Button action(String icon,String title){Button b=new Button(this);b.setText(icon+"  "+title);b.setTextSize(14);b.setAllCaps(false);b.setTextColor(Color.WHITE);b.setMinHeight(dp(50));b.setBackground(round(Color.rgb(91,82,190),dp(15)));return b;}

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.rgb(246,247,251));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(14),dp(8),dp(14),dp(16));root.setBackgroundColor(Color.rgb(246,247,251));
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(0,0,0,dp(20));
        TextView title=text("📖  داستان‌ساز",30,Color.rgb(48,43,95));title.setGravity(Gravity.CENTER);box.addView(title,new LinearLayout.LayoutParams(-1,dp(64)));
        TextView sub=text("🧠 نویسندگی آنلاین با هوش مصنوعی + 🔊 خواندن فارسی",14,Color.rgb(95,94,115));sub.setGravity(Gravity.CENTER);box.addView(sub,new LinearLayout.LayoutParams(-1,dp(38)));
        LinearLayout ai=card();ai.addView(label("🌐","نویسنده هوشمند","داستان با Qwen ساخته می‌شود و برای تنوع، ساختار و جزئیات خلاقانه کنترل می‌شود"));box.addView(ai);
        LinearLayout c=card();c.addView(label("🎯","موضوع داستان","ایده اصلی را بنویسید"));topic=edit("مثلاً دخترکی که صدای ستاره‌ها را می‌شنید");c.addView(topic);box.addView(c);
        c=card();c.addView(label("🧑‍🤝‍🧑","شخصیت‌ها","نام شخصیت‌ها اختیاری است"));character=edit("مثلاً آرش، نیلوفر و یک روباه سخنگو");c.addView(character);box.addView(c);
        c=card();c.addView(label("🎭","ژانر","حال‌وهوای داستان را تعیین کنید"));genre=new Spinner(this);genre.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"ماجراجویی","فانتزی","آموزشی","طنز","اخلاقی","علمی-تخیلی","احساسی","رازآلود","حماسی","معمایی","محیط‌زیستی","دوستی"}));c.addView(genre);box.addView(c);
        c=card();c.addView(label("👶","رده سنی","زبان و پیچیدگی بر اساس سن تنظیم می‌شود"));age=new Spinner(this);age.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"کودک ۴ تا ۷ سال","کودک ۸ تا ۱۲ سال","نوجوان","جوان","بزرگسال","خانوادگی"}));c.addView(age);box.addView(c);
        c=card();c.addView(label("📏","اندازه","زمان تقریبی داستان"));length=new Spinner(this);length.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"کوتاه • ۲ تا ۳ دقیقه","متوسط • حدود ۵ دقیقه","بلند • حدود ۱۰ دقیقه","خیلی بلند • کتابچه‌ای"}));c.addView(length);poem=new CheckBox(this);poem.setText("🎵 شعر یا ترانهٔ کاملاً تازه و مخصوص همین داستان");poem.setTextSize(15);c.addView(poem);box.addView(c);
        c=card();c.addView(label("💡","پند یا آموزش","پیام اختیاری داستان"));lesson=edit("مثلاً شجاعت همراه با مهربانی");c.addView(lesson);box.addView(c);
        c=card();c.addView(label("🏁","پایان و نتیجه","نوع پایان اختیاری"));ending=edit("مثلاً پایان غافلگیرکننده و خوش");c.addView(ending);box.addView(c);
        c=card();c.addView(label("🔊","صدای فارسی","فعلاً از موتور گفتار گوشی استفاده می‌کنیم تا نسخه آزمایشی سبک و سریع باشد؛ بعداً صدای آفلاین حرفه‌ای را اضافه می‌کنیم"));voice=new Spinner(this);voice.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"صدای فارسی پیش‌فرض","صدای فارسی آرام","صدای فارسی سریع"}));c.addView(voice);voiceStatus=text("⏳ در حال آماده‌سازی موتور گفتار گوشی…",13,Color.rgb(90,89,120));c.addView(voiceStatus);box.addView(c);
        generate=action("✨","ساخت داستان با هوش مصنوعی");box.addView(generate,new LinearLayout.LayoutParams(-1,dp(58)));
        status=text("آماده است",13,Color.rgb(100,99,120));status.setGravity(Gravity.CENTER);box.addView(status,new LinearLayout.LayoutParams(-1,dp(30)));
        LinearLayout out=card();TextView ot=text("📜  داستان شما",19,Color.rgb(48,43,95));ot.setPadding(dp(5),dp(8),dp(5),dp(8));out.addView(ot);output=text("اینجا یک داستان کاملاً جدید ساخته می‌شود…",17,Color.rgb(40,40,55));output.setLineSpacing(0,1.45f);output.setTextIsSelectable(true);output.setPadding(dp(10),dp(12),dp(10),dp(18));out.addView(output);box.addView(out);
        LinearLayout a=new LinearLayout(this);a.setGravity(Gravity.CENTER);speak=action("🔊","خواندن فارسی");stopSpeak=action("⏹️","توقف");a.addView(speak,new LinearLayout.LayoutParams(0,dp(54),1));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(54),1);p.setMargins(dp(5),0,0,0);a.addView(stopSpeak,p);box.addView(a);
        mic=action("🎤","گفتن موضوع با صدا");box.addView(mic,new LinearLayout.LayoutParams(-1,dp(52)));save=action("💾","ذخیره");share=action("📤","اشتراک");LinearLayout a2=new LinearLayout(this);a2.addView(save,new LinearLayout.LayoutParams(0,dp(52),1));a2.addView(share,new LinearLayout.LayoutParams(0,dp(52),1));box.addView(a2);
        TextView foot=text("🔊 خواندن فارسی با موتور گوشی  •  🌐 داستان‌پردازی آنلاین  •  💾 ذخیره و اشتراک",13,Color.rgb(110,108,130));foot.setGravity(Gravity.CENTER);box.addView(foot,new LinearLayout.LayoutParams(-1,dp(60)));
        scroll.addView(box);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);
        initTts();initSpeech();
        generate.setOnClickListener(v->generateOnline());speak.setOnClickListener(v->speakStory());stopSpeak.setOnClickListener(v->stopTts());mic.setOnClickListener(v->startVoiceInput());
        save.setOnClickListener(v->{getPreferences(0).edit().putString("last_story",output.getText().toString()).apply();status.setText("💾 داستان ذخیره شد");});
        share.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(Intent.EXTRA_TEXT,output.getText().toString());startActivity(Intent.createChooser(i,"اشتراک‌گذاری داستان"));});
    }

    String key(){return BuildConfig.QWEN_API_KEY==null?"":BuildConfig.QWEN_API_KEY.trim();}
    String selectedLength(){String s=length.getSelectedItem().toString();if(s.startsWith("کوتاه"))return "حدود 500 تا 750 کلمه";if(s.startsWith("متوسط"))return "حدود 900 تا 1300 کلمه";if(s.startsWith("بلند"))return "حدود 1700 تا 2300 کلمه";return "حدود 2800 تا 3800 کلمه";}
    String clean(String s){return s==null?"":s.trim();}

    void generateOnline(){
        final String api=key();
        if(api.isEmpty()){status.setText("⚠️ کلید Qwen در Build قرار نگرفته است");return;}
        String t=clean(topic.getText().toString());if(t.isEmpty()){status.setText("موضوع داستان را وارد کنید");topic.requestFocus();return;}
        status.setText("🌐 هوش مصنوعی در حال نویسندگی است…");generate.setEnabled(false);output.setText("⏳ در حال ساخت یک داستان تازه و غیرتکراری…");
        final String prompt="تو یک نویسنده حرفه‌ای فارسی برای کتاب صوتی هستی. یک داستان کاملاً تازه و اختصاصی بنویس؛ نه بازنویسی، نه تغییر نام یک داستان قبلی و نه قالب تکراری. موضوع: "+t+". شخصیت‌ها: "+clean(character.getText().toString())+". ژانر: "+genre.getSelectedItem()+". رده سنی: "+age.getSelectedItem()+". پیام/پند: "+clean(lesson.getText().toString())+". پایان و نتیجه: "+clean(ending.getText().toString())+". طول: "+selectedLength()+". "+(poem.isChecked()?"در میانه داستان یک شعر کوتاه و کاملاً جدید و مرتبط با همان داستان بساز و طبیعی جای بده. ":"")+"قوانین: شروع، بحران، نقطه عطف و پایان مشخص داشته باش؛ شخصیت‌ها هدف و تغییر واقعی داشته باشند؛ جزئیات حسی، گفت‌وگوهای طبیعی و اتفاقات غیرقابل‌پیش‌بینی اضافه کن؛ از کلیشه‌های تکراری دوری کن؛ فقط متن نهایی داستان را با تیتر زیبا و پاراگراف‌بندی مناسب برگردان.";
        work.submit(()->{
            try{
                JSONObject body=new JSONObject();body.put("model","qwen3.8-flash");body.put("temperature",1.15);body.put("top_p",0.92);body.put("max_tokens",6500);
                JSONArray msgs=new JSONArray();msgs.put(new JSONObject().put("role","system").put("content","پاسخ فقط به زبان فارسی باشد."));msgs.put(new JSONObject().put("role","user").put("content",prompt));body.put("messages",msgs);
                HttpURLConnection c=(HttpURLConnection)new URL(QWEN_URL).openConnection();c.setRequestMethod("POST");c.setConnectTimeout(20000);c.setReadTimeout(90000);c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");c.setRequestProperty("Authorization","Bearer "+api);OutputStream os=c.getOutputStream();os.write(body.toString().getBytes(StandardCharsets.UTF_8));os.close();int code=c.getResponseCode();InputStream is=code>=200&&code<300?c.getInputStream():c.getErrorStream();String resp=read(is);c.disconnect();
                if(code<200||code>=300)throw new Exception("Qwen HTTP "+code+": "+resp);
                JSONObject r=new JSONObject(resp);String story=r.getJSONArray("choices").getJSONObject(0).getJSONObject("message").optString("content","").trim();if(story.isEmpty())throw new Exception("پاسخ خالی بود");
                runOnUiThread(()->{output.setText(story);status.setText("✅ داستان اختصاصی ساخته شد");generate.setEnabled(true);});
            }catch(Exception e){runOnUiThread(()->{generate.setEnabled(true);status.setText("❌ ساخت آنلاین ناموفق بود");output.setText("خطا: "+e.getMessage());});}
        });
    }
    String read(InputStream in)throws Exception{if(in==null)return "";BufferedReader br=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8));StringBuilder s=new StringBuilder();String l;while((l=br.readLine())!=null)s.append(l);br.close();return s.toString();}

    void initTts(){tts=new android.speech.tts.TextToSpeech(this,code->{if(code==android.speech.tts.TextToSpeech.SUCCESS){int r=tts.setLanguage(new Locale("fa","IR"));ttsReady=r!=android.speech.tts.TextToSpeech.LANG_MISSING_DATA&&r!=android.speech.tts.TextToSpeech.LANG_NOT_SUPPORTED;tts.setSpeechRate(0.9f);voiceStatus.setText(ttsReady?"✅ موتور فارسی آماده است":"⚠️ صدای فارسی روی موتور گوشی نصب نیست؛ از تنظیمات TTS زبان فارسی را نصب کنید");}else voiceStatus.setText("⚠️ موتور گفتار گوشی آماده نشد");});}
    void speakStory(){String s=output.getText().toString().trim();if(s.isEmpty()||s.startsWith("اینجا")||s.startsWith("⏳")||s.startsWith("خطا:")){status.setText("ابتدا یک داستان بسازید");return;}if(!ttsReady){status.setText("⚠️ صدای فارسی روی گوشی در دسترس نیست");return;}tts.stop();tts.setSpeechRate(voice.getSelectedItemPosition()==1?0.82f:voice.getSelectedItemPosition()==2?1.0f:0.9f);int max=3500;for(int i=0,start=0;start<s.length();i++){int end=Math.min(start+max,s.length());if(end<s.length()){int q=s.lastIndexOf(' ',end);if(q>start+100)end=q;}String part=s.substring(start,end);tts.speak(part,TextToSpeech.QUEUE_ADD,null,"story"+i);start=end;}status.setText("🔊 در حال خواندن داستان با صدای فارسی…");}
    void stopTts(){if(tts!=null)tts.stop();status.setText("⏹️ خواندن متوقف شد");}

    void initSpeech(){if(!SpeechRecognizer.isRecognitionAvailable(this))return;recognizer=SpeechRecognizer.createSpeechRecognizer(this);recognizer.setRecognitionListener(new RecognitionListener(){public void onReadyForSpeech(Bundle p){}public void onBeginningOfSpeech(){}public void onRmsChanged(float r){}public void onBufferReceived(byte[] b){}public void onEndOfSpeech(){}public void onError(int e){status.setText("🎤 تشخیص صدا انجام نشد");}public void onResults(Bundle b){ArrayList<String> r=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);if(r!=null&&!r.isEmpty()){topic.setText(r.get(0));status.setText("🎤 موضوع دریافت شد؛ حالا ساخت داستان را بزنید");}}public void onPartialResults(Bundle b){}public void onEvent(int a,Bundle b){}});}
    void startVoiceInput(){if(recognizer==null){status.setText("تشخیص گفتار روی این گوشی در دسترس نیست");return;}Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"fa-IR");i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,"fa-IR");i.putExtra(RecognizerIntent.EXTRA_PROMPT,"موضوع داستان را بگویید");recognizer.startListening(i);status.setText("🎤 گوش می‌دهم…");}
    @Override protected void onDestroy(){stopTts();if(tts!=null)tts.shutdown();if(recognizer!=null)recognizer.destroy();work.shutdownNow();super.onDestroy();}
}
