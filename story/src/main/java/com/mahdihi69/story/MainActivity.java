package com.mahdihi69.story;

import android.app.*;
import android.os.*;
import android.graphics.Color;
import android.content.*;
import android.view.*;
import android.widget.*;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.speech.SpeechRecognizer;
import android.speech.RecognizerIntent;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;
import com.k2fsa.sherpa.onnx.*;

public class MainActivity extends Activity {
    EditText topic, character, lesson, ending;
    Spinner genre, age, voice, length;
    CheckBox poem;
    TextView output, status, voiceStatus;
    Button generate, speak, stopSpeak, mic, save, share;
    SpeechRecognizer recognizer;
    final ExecutorService work=Executors.newSingleThreadExecutor();
    final String QWEN_URL="https://dashscope-intl.aliyuncs.com/compatible-mode/v1/chat/completions";
    final String[] VOICE_DIR={"vits-piper-fa_IR-amir-medium","vits-piper-fa_IR-ganji_adabi-medium","vits-piper-fa_IR-gyro-medium","vits-piper-fa_IR-reza_ibrahim-medium"};
    final String[] VOICE_FILE={"fa_IR-amir-medium.onnx","fa_IR-ganji_adabi-medium.onnx","fa_IR-gyro-medium.onnx","fa_IR-reza_ibrahim-medium.onnx"};
    OfflineTts neuralTts;
    MediaPlayer player;
    ArrayList<File> audioQueue=new ArrayList<>();
    int audioIndex=0;

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
        TextView sub=text("🧠 نویسندگی آنلاین با هوش مصنوعی + 🎧 صدای واقعی فارسی آفلاین",14,Color.rgb(95,94,115));sub.setGravity(Gravity.CENTER);box.addView(sub,new LinearLayout.LayoutParams(-1,dp(38)));
        LinearLayout ai=card();ai.addView(label("🌐","نویسنده هوشمند","داستان ابتدا با Qwen ساخته می‌شود؛ پاسخ‌های تکراری و قالبی با دستور نویسندگی خلاق کنترل می‌شوند"));box.addView(ai);
        LinearLayout c=card();c.addView(label("🎯","موضوع داستان","ایده اصلی را بنویسید"));topic=edit("مثلاً دخترکی که صدای ستاره‌ها را می‌شنید");c.addView(topic);box.addView(c);
        c=card();c.addView(label("🧑‍🤝‍🧑","شخصیت‌ها","نام شخصیت‌ها اختیاری است"));character=edit("مثلاً آرش، نیلوفر و یک روباه سخنگو");c.addView(character);box.addView(c);
        c=card();c.addView(label("🎭","ژانر","حال‌وهوای داستان را تعیین کنید"));genre=new Spinner(this);genre.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"ماجراجویی","فانتزی","آموزشی","طنز","اخلاقی","علمی-تخیلی","احساسی","رازآلود","حماسی","معمایی","محیط‌زیستی","دوستی"}));c.addView(genre);box.addView(c);
        c=card();c.addView(label("👶","رده سنی","زبان و پیچیدگی بر اساس سن تنظیم می‌شود"));age=new Spinner(this);age.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"کودک ۴ تا ۷ سال","کودک ۸ تا ۱۲ سال","نوجوان","جوان","بزرگسال","خانوادگی"}));c.addView(age);box.addView(c);
        c=card();c.addView(label("📏","اندازه","زمان تقریبی داستان"));length=new Spinner(this);length.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"کوتاه • ۲ تا ۳ دقیقه","متوسط • حدود ۵ دقیقه","بلند • حدود ۱۰ دقیقه","خیلی بلند • کتابچه‌ای"}));c.addView(length);poem=new CheckBox(this);poem.setText("🎵 شعر یا ترانهٔ کاملاً تازه و مخصوص همین داستان");poem.setTextSize(15);c.addView(poem);box.addView(c);
        c=card();c.addView(label("💡","پند یا آموزش","پیام اختیاری داستان"));lesson=edit("مثلاً شجاعت همراه با مهربانی");c.addView(lesson);box.addView(c);
        c=card();c.addView(label("🏁","پایان و نتیجه","نوع پایان اختیاری"));ending=edit("مثلاً پایان غافلگیرکننده و خوش");c.addView(ending);box.addView(c);
        c=card();c.addView(label("🎙️","صدای واقعی فارسی","چهار مدل عصبی فارسی داخل خود APK قرار می‌گیرد و برای خواندن اینترنت لازم نیست"));voice=new Spinner(this);voice.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"🎙️ امیر — طبیعی و آرام","📖 گنجی ادبی — قصه‌گو","👨 گیرو — جدی و واضح","🎧 رضا ابراهیم — گرم و روایی"}));c.addView(voice);voiceStatus=text("⏳ مدل‌های صدای عصبی فارسی هنگام اولین خواندن آماده می‌شوند",13,Color.rgb(90,89,120));c.addView(voiceStatus);box.addView(c);
        generate=action("✨","ساخت داستان با هوش مصنوعی");box.addView(generate,new LinearLayout.LayoutParams(-1,dp(58)));
        status=text("آماده است",13,Color.rgb(100,99,120));status.setGravity(Gravity.CENTER);box.addView(status,new LinearLayout.LayoutParams(-1,dp(30)));
        LinearLayout out=card();TextView ot=text("📜  داستان شما",19,Color.rgb(48,43,95));ot.setPadding(dp(5),dp(8),dp(5),dp(8));out.addView(ot);output=text("اینجا یک داستان کاملاً جدید ساخته می‌شود…",17,Color.rgb(40,40,55));output.setLineSpacing(0,1.45f);output.setTextIsSelectable(true);output.setPadding(dp(10),dp(12),dp(10),dp(18));out.addView(output);box.addView(out);
        LinearLayout a=new LinearLayout(this);a.setGravity(Gravity.CENTER);speak=action("🔊","خواندن با صدای فارسی");stopSpeak=action("⏹️","توقف");a.addView(speak,new LinearLayout.LayoutParams(0,dp(54),1));LinearLayout p=new LinearLayout.LayoutParams(0,dp(54),1);p.setMargins(dp(5),0,0,0);a.addView(stopSpeak,p);box.addView(a);
        mic=action("🎤","گفتن موضوع با صدا");box.addView(mic,new LinearLayout.LayoutParams(-1,dp(52)));save=action("💾","ذخیره");share=action("📤","اشتراک");LinearLayout a2=new LinearLayout(this);a2.addView(save,new LinearLayout.LayoutParams(0,dp(52),1));a2.addView(share,new LinearLayout.LayoutParams(0,dp(52),1));box.addView(a2);
        TextView foot=text("🎧 خواندن عصبی فارسی داخل برنامه  •  🌐 داستان‌پردازی آنلاین  •  💾 ذخیره و اشتراک",13,Color.rgb(110,108,130));foot.setGravity(Gravity.CENTER);box.addView(foot,new LinearLayout.LayoutParams(-1,dp(60)));
        scroll.addView(box);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);
        initSpeech();
        generate.setOnClickListener(v->generateOnline());speak.setOnClickListener(v->speakNeural());stopSpeak.setOnClickListener(v->stopAudio());mic.setOnClickListener(v->startVoiceInput());
        save.setOnClickListener(v->{getPreferences(0).edit().putString("last_story",output.getText().toString()).apply();status.setText("💾 داستان ذخیره شد");});
        share.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(Intent.EXTRA_TEXT,output.getText().toString());startActivity(Intent.createChooser(i,"اشتراک‌گذاری داستان"));});
    }

    String key(){return BuildConfig.QWEN_API_KEY==null?"":BuildConfig.QWEN_API_KEY.trim();}
    String selectedLength(){String s=length.getSelectedItem().toString();if(s.startsWith("کوتاه"))return "حدود 500 تا 750 کلمه";if(s.startsWith("متوسط"))return "حدود 900 تا 1300 کلمه";if(s.startsWith("بلند"))return "حدود 1700 تا 2300 کلمه";return "حدود 2800 تا 3800 کلمه";}
    String clean(String s){return s==null?"":s.trim();}

    void generateOnline(){
        final String api=key();
        if(api.isEmpty()){status.setText("⚠️ کلید Qwen در تنظیمات Build قرار نگرفته است");return;}
        String t=clean(topic.getText().toString());if(t.isEmpty()){status.setText("موضوع داستان را وارد کنید");topic.requestFocus();return;}
        status.setText("🌐 هوش مصنوعی در حال نویسندگی است…");generate.setEnabled(false);output.setText("⏳ در حال ساخت یک داستان تازه و غیرتکراری…");
        final String prompt="تو یک نویسنده حرفه‌ای فارسی برای کتاب صوتی هستی. یک داستان کاملاً تازه و اختصاصی بنویس؛ نه بازنویسی، نه تغییر نام یک داستان قبلی و نه قالب تکراری. موضوع: "+t+". شخصیت‌ها: "+clean(character.getText().toString())+". ژانر: "+genre.getSelectedItem()+". رده سنی: "+age.getSelectedItem()+". پیام/پند: "+clean(lesson.getText().toString())+". پایان و نتیجه: "+clean(ending.getText().toString())+". طول: "+selectedLength()+". "+(poem.isChecked()?"در میانه داستان یک شعر کوتاه و کاملاً جدید و مرتبط با همان شخصیت‌ها و اتفاقات بساز و طبیعی در داستان جای بده. ":"")+"قوانین: شروع، بحران، نقطه عطف و پایان مشخص داشته باش؛ شخصیت‌ها هدف و تغییر واقعی داشته باشند؛ جزئیات حسی، گفت‌وگوهای طبیعی و اتفاقات غیرقابل‌پیش‌بینی اضافه کن؛ از کلیشه‌هایی مثل جنگل جادویی، شاهزاده و گنج تکراری دوری کن مگر موضوع مجبور کند؛ هیچ جمله‌ای درباره هوش مصنوعی یا دستور کار نگو؛ فقط متن نهایی داستان را با تیتر زیبا و پاراگراف‌بندی مناسب برگردان.";
        work.submit(()->{
            try{
                JSONObject body=new JSONObject();body.put("model","qwen3.8-flash");body.put("temperature",1.15);body.put("top_p",0.92);body.put("max_tokens",6500);
                JSONArray msgs=new JSONArray();msgs.put(new JSONObject().put("role","system").put("content","همیشه فارسی روان و ادبی اما متناسب با سن بنویس. هر بار طرح و اتفاقات را از نو اختراع کن."));msgs.put(new JSONObject().put("role","user").put("content",prompt));body.put("messages",msgs);
                HttpURLConnection c=(HttpURLConnection)new URL(QWEN_URL).openConnection();c.setRequestMethod("POST");c.setConnectTimeout(25000);c.setReadTimeout(90000);c.setRequestProperty("Authorization","Bearer "+api);c.setRequestProperty("Content-Type","application/json; charset=UTF-8");c.setDoOutput(true);
                OutputStream os=c.getOutputStream();os.write(body.toString().getBytes(StandardCharsets.UTF_8));os.close();int code=c.getResponseCode();InputStream is=code>=200&&code<300?c.getInputStream():c.getErrorStream();String resp=read(is);c.disconnect();
                if(code<200||code>=300)throw new Exception("Qwen HTTP "+code+": "+resp);
                JSONObject r=new JSONObject(resp);String story=r.getJSONArray("choices").getJSONObject(0).getJSONObject("message").optString("content","").trim();if(story.isEmpty())throw new Exception("پاسخ خالی بود");
                runOnUiThread(()->{output.setText(story);status.setText("✅ داستان اختصاصی با هوش مصنوعی ساخته شد");generate.setEnabled(true);});
            }catch(Exception e){runOnUiThread(()->{generate.setEnabled(true);status.setText("❌ ساخت آنلاین ناموفق بود؛ اینترنت و کلید Qwen را بررسی کنید");output.setText("خطا: "+e.getMessage());});}
        });
    }
    String read(InputStream in)throws Exception{if(in==null)return "";BufferedReader br=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8));StringBuilder s=new StringBuilder();String l;while((l=br.readLine())!=null)s.append(l);br.close();return s.toString();}

    void initSpeech(){
        if(!SpeechRecognizer.isRecognitionAvailable(this))return;
        recognizer=SpeechRecognizer.createSpeechRecognizer(this);
        recognizer.setRecognitionListener(new android.speech.RecognitionListener(){public void onReadyForSpeech(Bundle p){}public void onBeginningOfSpeech(){}public void onRmsChanged(float r){}public void onBufferReceived(byte[] b){}public void onEndOfSpeech(){}public void onError(int e){status.setText("🎤 تشخیص صدا انجام نشد");}public void onResults(Bundle b){ArrayList<String> r=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);if(r!=null&&!r.isEmpty()){topic.setText(r.get(0));status.setText("🎤 موضوع از صدای شما دریافت شد؛ حالا ساخت داستان را بزنید");}}public void onPartialResults(Bundle b){}public void onEvent(int a,Bundle b){}});
    }
    void startVoiceInput(){if(recognizer==null){status.setText("تشخیص گفتار روی این گوشی در دسترس نیست");return;}Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"fa-IR");i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,"fa-IR");i.putExtra(RecognizerIntent.EXTRA_PROMPT,"موضوع داستان را بگویید");recognizer.startListening(i);status.setText("🎤 گوش می‌دهم…");}

    void copyAssets(String asset,String dst)throws Exception{File out=new File(dst);String[] list=getAssets().list(asset);if(list==null||list.length==0){if(!out.exists()){out.getParentFile().mkdirs();InputStream in=getAssets().open(asset);FileOutputStream fo=new FileOutputStream(out);byte[] b=new byte[8192];int n;while((n=in.read(b))>0)fo.write(b,0,n);in.close();fo.close();}return;}if(!out.exists())out.mkdirs();for(String x:list)copyAssets(asset+"/"+x,new File(out,x).getAbsolutePath());}
    File prepareVoice(int pos)throws Exception{String dir=VOICE_DIR[Math.max(0,Math.min(pos,VOICE_DIR.length-1))];File root=new File(getFilesDir(),"tts/"+dir);if(!root.exists())copyAssets("tts/"+dir,root.getAbsolutePath());return root;}
    OfflineTts createNeural(File root,String model)throws Exception{OfflineTtsVitsModelConfig v=new OfflineTtsVitsModelConfig();v.setModel(new File(root,model).getAbsolutePath());v.setTokens(new File(root,"tokens.txt").getAbsolutePath());v.setDataDir(new File(root,"espeak-ng-data").getAbsolutePath());OfflineTtsModelConfig m=new OfflineTtsModelConfig();m.setVits(v);m.setNumThreads(Math.max(1,Math.min(4,Runtime.getRuntime().availableProcessors()-1)));m.setDebug(false);m.setProvider("cpu");OfflineTtsConfig c=new OfflineTtsConfig();c.setModel(m);c.setMaxNumSentences(1);return new OfflineTts(c);}
    ArrayList<String> speechChunks(String s){ArrayList<String>a=new ArrayList<>();String[] p=s.split("(?<=[.!؟!?])\\s+|\\n+");StringBuilder cur=new StringBuilder();for(String x:p){x=x.trim();if(x.isEmpty())continue;if(cur.length()+x.length()>900){if(cur.length()>0){a.add(cur.toString());cur.setLength(0);}while(x.length()>900){a.add(x.substring(0,900));x=x.substring(900);}}if(cur.length()>0)cur.append(" ");cur.append(x);}if(cur.length()>0)a.add(cur.toString());return a;}

    void speakNeural(){String s=output.getText().toString().trim();if(s.isEmpty()||s.startsWith("اینجا")||s.startsWith("⏳")||s.startsWith("خطا:")){status.setText("ابتدا یک داستان بسازید");return;}stopAudio();status.setText("🎧 آماده‌سازی صدای عصبی فارسی…");speak.setEnabled(false);final int pos=voice.getSelectedItemPosition();work.submit(()->{try{File root=prepareVoice(pos);OfflineTts t=createNeural(root,VOICE_FILE[pos]);neuralTts=t;voiceStatus.setText("✅ صدای فارسی واقعی داخل برنامه آماده است؛ کاملاً آفلاین");audioQueue.clear();ArrayList<String> chunks=speechChunks(s);for(int i=0;i<chunks.size();i++){File f=new File(getCacheDir(),"story_voice_"+System.currentTimeMillis()+"_"+i+".wav");GeneratedAudio a=t.generateWithConfigAndCallback(chunks.get(i),new GenerationConfig(){ {setSid(0);setSpeed(pos==1?0.92f:pos==2?1.0f:pos==3?0.98f:0.95f);setSilenceScale(0.2f);} },samples->1);if(!a.save(f.getAbsolutePath()))throw new Exception("ذخیره صدا ناموفق بود");audioQueue.add(f);}runOnUiThread(()->{speak.setEnabled(true);audioIndex=0;status.setText("🔊 در حال خواندن با صدای واقعی فارسی…");playNextAudio();});}catch(Exception e){runOnUiThread(()->{speak.setEnabled(true);status.setText("⚠️ صدای عصبی آماده نشد؛ موتور گوشی را امتحان کنید");voiceStatus.setText("خطای مدل: "+e.getMessage());});}});}
    void playNextAudio(){if(audioIndex>=audioQueue.size()){status.setText("✅ خواندن داستان تمام شد");cleanupAudio();return;}try{if(player!=null)player.release();player=new MediaPlayer();player.setDataSource(audioQueue.get(audioIndex).getAbsolutePath());player.setOnCompletionListener(mp->{audioIndex++;playNextAudio();});player.prepare();player.start();}catch(Exception e){status.setText("⚠️ پخش صدا ناموفق بود");cleanupAudio();}}
    void stopAudio(){try{if(player!=null){player.stop();player.release();player=null;}}catch(Exception ignored){}if(neuralTts!=null){try{neuralTts.release();}catch(Exception ignored){}neuralTts=null;}cleanupAudio();status.setText("⏹️ خواندن متوقف شد");}
    void cleanupAudio(){for(File f:audioQueue)try{f.delete();}catch(Exception ignored){}audioQueue.clear();}
    @Override protected void onDestroy(){stopAudio();if(recognizer!=null)recognizer.destroy();work.shutdownNow();super.onDestroy();}
}
