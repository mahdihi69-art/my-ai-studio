package com.mahdihi69.story;

import android.app.*;
import android.os.*;
import android.graphics.Color;
import android.content.*;
import android.view.*;
import android.widget.*;
import android.graphics.drawable.GradientDrawable;
import android.speech.tts.TextToSpeech;
import java.util.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    EditText topic, character, lesson, ending;
    Spinner genre, age, voice;
    TextView output, status, voiceStatus;
    Button generate, speak, save, share;
    TextToSpeech tts;
    boolean ttsReady=false;
    final ExecutorService net=Executors.newSingleThreadExecutor();
    final String QWEN_URL="https://dashscope-intl.aliyuncs.com/compatible-mode/v1/chat/completions";

    int dp(float x){return (int)(x*getResources().getDisplayMetrics().density+.5f);}
    TextView text(String s,float size,int color){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(color);return v;}
    EditText edit(String hint){EditText e=new EditText(this);e.setHint(hint);e.setTextSize(16);e.setSingleLine(false);e.setMinHeight(dp(58));e.setPadding(dp(14),dp(10),dp(14),dp(10));e.setBackground(round(Color.WHITE,dp(14)));return e;}
    GradientDrawable round(int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(radius);g.setStroke(dp(1),Color.rgb(225,226,235));return g;}
    LinearLayout card(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(14),dp(7),dp(14),dp(12));c.setBackground(round(Color.WHITE,dp(18)));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(6),0,dp(6));c.setLayoutParams(p);return c;}
    TextView iconLabel(String icon,String title,String hint){TextView v=text(icon+"  "+title+"\n"+hint,16,Color.rgb(45,45,65));v.setPadding(dp(5),dp(8),dp(5),dp(8));return v;}
    Button action(String icon,String title){Button b=new Button(this);b.setText(icon+"  "+title);b.setTextSize(14);b.setAllCaps(false);b.setTextColor(Color.WHITE);b.setMinHeight(dp(50));b.setBackground(round(Color.rgb(91,82,190),dp(15)));return b;}

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.rgb(246,247,251));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(14),dp(8),dp(14),dp(16));root.setBackgroundColor(Color.rgb(246,247,251));
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(0,0,0,dp(20));
        TextView title=text("📖  داستان‌ساز",30,Color.rgb(48,43,95));title.setGravity(Gravity.CENTER);box.addView(title,new LinearLayout.LayoutParams(-1,dp(64)));
        TextView sub=text("✨ قصه‌ای اختصاصی برای شنیدن، ذخیره و اشتراک‌گذاری",14,Color.rgb(95,94,115));sub.setGravity(Gravity.CENTER);box.addView(sub,new LinearLayout.LayoutParams(-1,dp(32)));
        LinearLayout smart=card();smart.addView(iconLabel("🧠","داستان هوشمند","موتور اصلی: Qwen / Alibaba • کلید API از کاربر گرفته نمی‌شود"));box.addView(smart);
        LinearLayout c=card();c.addView(iconLabel("🎯","موضوع داستان","ایده اصلی داستان را بنویسید"));topic=edit("مثلاً سفر یک کودک به شهر ستاره‌ها");c.addView(topic);box.addView(c);
        c=card();c.addView(iconLabel("🧑‍🤝‍🧑","شخصیت‌ها","نام‌ها اختیاری؛ چند شخصیت را با «و» جدا کنید"));character=edit("مثلاً آرش و نیلوفر و ربات کوچولو");c.addView(character);box.addView(c);
        c=card();c.addView(iconLabel("🎭","ژانر و حال‌وهوا","سبک داستان را انتخاب کنید"));genre=new Spinner(this);genre.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"ماجراجویی","آموزشی","طنز","فانتزی","اخلاقی","علمی-تخیلی","احساسی","رازآلود","حماسی"}));c.addView(genre);box.addView(c);
        c=card();c.addView(iconLabel("👶","رده سنی","زبان و پیچیدگی داستان بر اساس سن تنظیم می‌شود"));age=new Spinner(this);age.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"کودک ۴ تا ۷ سال","کودک ۸ تا ۱۲ سال","نوجوان","جوان","بزرگسال","خانوادگی"}));c.addView(age);box.addView(c);
        c=card();c.addView(iconLabel("💡","پند یا آموزش","پیام موردنظر را وارد کنید؛ اختیاری"));lesson=edit("مثلاً اهمیت راستگویی و کمک به دیگران");c.addView(lesson);box.addView(c);
        c=card();c.addView(iconLabel("🏁","پایان و نتیجه","نوع پایان را مشخص کنید؛ اختیاری"));ending=edit("مثلاً پایان شاد، غافلگیرکننده و آموزنده");c.addView(ending);box.addView(c);
        c=card();c.addView(iconLabel("🎙️","صدای گوینده","چهار حالت صدای فارسی؛ خواندن روی خود گوشی انجام می‌شود"));voice=new Spinner(this);voice.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"🎙️ گوینده آرام","👨 گوینده جدی","👩 گوینده گرم","🧒 گوینده کودکانه"}));c.addView(voice);voiceStatus=text("در حال آماده‌سازی صدای فارسی گوشی…",13,Color.rgb(100,99,120));voiceStatus.setPadding(dp(4),dp(8),dp(4),0);c.addView(voiceStatus);box.addView(c);
        generate=action("✨","ساخت داستان اختصاصی");LinearLayout.LayoutParams gp=new LinearLayout.LayoutParams(-1,dp(58));gp.setMargins(0,dp(9),0,dp(8));box.addView(generate,gp);
        status=text("آماده داستان‌پردازی است",13,Color.rgb(100,99,120));status.setGravity(Gravity.CENTER);box.addView(status,new LinearLayout.LayoutParams(-1,dp(30)));
        LinearLayout outCard=card();TextView outTitle=text("📜  داستان شما",19,Color.rgb(48,43,95));outTitle.setPadding(dp(5),dp(8),dp(5),dp(8));outCard.addView(outTitle);output=text("داستان شما اینجا ساخته می‌شود…",17,Color.rgb(40,40,55));output.setLineSpacing(0,1.45f);output.setTextIsSelectable(true);output.setPadding(dp(10),dp(12),dp(10),dp(18));outCard.addView(output);box.addView(outCard);
        LinearLayout actions=new LinearLayout(this);actions.setGravity(Gravity.CENTER);actions.setPadding(0,dp(4),0,dp(8));speak=action("🔊","خواندن");save=action("💾","ذخیره");share=action("📤","اشتراک");actions.addView(speak,new LinearLayout.LayoutParams(0,dp(52),1));LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(0,dp(52),1);sp.setMargins(dp(5),0,dp(5),0);actions.addView(save,sp);actions.addView(share,new LinearLayout.LayoutParams(0,dp(52),1));box.addView(actions);
        TextView footer=text("🎬 سری‌سازی  •  🎨 داستان اختصاصی  •  🎧 کتاب صوتی  •  📱 خواندن بدون اینترنت پس از نصب صدای فارسی",13,Color.rgb(110,108,130));footer.setGravity(Gravity.CENTER);box.addView(footer,new LinearLayout.LayoutParams(-1,dp(48)));
        scroll.addView(box);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);
        initTts();
        generate.setOnClickListener(v->generate());
        voice.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){}public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){applyVoice(pos);}});
        speak.setOnClickListener(v->speakStory());
        save.setOnClickListener(v->{getPreferences(0).edit().putString("last_story",output.getText().toString()).apply();status.setText("💾 داستان در گوشی ذخیره شد");});
        share.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(Intent.EXTRA_TEXT,output.getText().toString());startActivity(Intent.createChooser(i,"اشتراک‌گذاری داستان"));});
    }

    void initTts(){tts=new TextToSpeech(this,s->{if(s==TextToSpeech.SUCCESS){ttsReady=true;int r=tts.isLanguageAvailable(new Locale("fa","IR"));if(r>=TextToSpeech.LANG_AVAILABLE){tts.setLanguage(new Locale("fa","IR"));voiceStatus.setText("✅ صدای فارسی آماده است؛ خواندن می‌تواند بدون اینترنت انجام شود");applyVoice(0);}else voiceStatus.setText("⚠️ صدای فارسی روی گوشی نصب نیست؛ از تنظیمات موتور گفتار، Persian/Farsi را دانلود کنید");}else voiceStatus.setText("⚠️ موتور تبدیل متن به گفتار گوشی آماده نشد");});}
    void applyVoice(int pos){if(!ttsReady)return;float pitch,rate;if(pos==1){pitch=.82f;rate=.88f;}else if(pos==2){pitch=1.08f;rate=.90f;}else if(pos==3){pitch=1.28f;rate=1.02f;}else{pitch=.96f;rate=.86f;}tts.setPitch(pitch);tts.setSpeechRate(rate);try{tts.setLanguage(new Locale("fa","IR"));}catch(Exception ignored){}}
    void speakStory(){String s=output.getText().toString();if(s.isEmpty()||s.startsWith("داستان شما")||s.startsWith("⏳")){status.setText("ابتدا یک داستان بسازید");return;}if(!ttsReady){status.setText("موتور صدای گوشی آماده نیست");return;}applyVoice(voice.getSelectedItemPosition());if(tts.isLanguageAvailable(new Locale("fa","IR"))<TextToSpeech.LANG_AVAILABLE){status.setText("صدای فارسی نصب نیست؛ آن را روی گوشی دانلود کنید");return;}tts.speak(s,TextToSpeech.QUEUE_FLUSH,null,"story");status.setText("🔊 در حال خواندن داستان با صدای انتخابی…");}
    String makePrompt(){return "یک داستان کاملاً تازه و غیرکپی به زبان فارسی بنویس. موضوع: "+topic.getText().toString().trim()+". شخصیت‌ها: "+character.getText().toString().trim()+". ژانر: "+genre.getSelectedItem()+". رده سنی: "+age.getSelectedItem()+". پند/آموزش: "+lesson.getText().toString().trim()+". پایان/نتیجه: "+ending.getText().toString().trim()+". عنوان جذاب، شروع قوی، شخصیت‌پردازی، اتفاقات تصویری، اوج، حل مسئله و پایان رضایت‌بخش داشته باشد. برای شنیدن با صدای بلند روان باشد. اگر شخصیت‌ها خالی بودند خودت نام‌های مناسب انتخاب کن. از کلیشه و تکرار دوری کن.";}

    void generate(){String t=topic.getText().toString().trim();if(t.isEmpty()){topic.setError("موضوع را وارد کنید");return;}generate.setEnabled(false);status.setText("🪄 در حال خلق یک داستان تازه…");output.setText("⏳ نویسنده هوشمند در حال داستان‌پردازی است…");final String prompt=makePrompt();net.execute(()->{String key=BuildConfig.QWEN_API_KEY==null?"":BuildConfig.QWEN_API_KEY.trim();if(key.isEmpty()){String local=localStory();runOnUiThread(()->{output.setText(local);status.setText("📖 داستان آماده شد • حالت پشتیبان");generate.setEnabled(true);});return;}try{String result=callQwen(key,prompt);runOnUiThread(()->{output.setText(result);status.setText("✅ داستان با Qwen ساخته شد");generate.setEnabled(true);});}catch(Exception e){String local=localStory();runOnUiThread(()->{output.setText(local+"\n\n⚠️ سرویس آنلاین در دسترس نبود؛ حالت پشتیبان فعال شد.");status.setText("⚠️ اتصال Qwen برقرار نشد؛ حالت پشتیبان فعال شد");generate.setEnabled(true);});}});}
    String callQwen(String key,String prompt)throws Exception{Exception last=null;String[] models={"qwen3.8-flash","qwen3.7-flash","qwen3.6-flash"};for(String model:models){HttpURLConnection x=null;try{x=(HttpURLConnection)new URL(QWEN_URL).openConnection();x.setRequestMethod("POST");x.setConnectTimeout(20000);x.setReadTimeout(60000);x.setDoOutput(true);x.setRequestProperty("Authorization","Bearer "+key);x.setRequestProperty("Content-Type","application/json; charset=UTF-8");JSONObject body=new JSONObject();body.put("model",model);JSONArray msgs=new JSONArray();msgs.put(new JSONObject().put("role","system").put("content","تو یک نویسنده حرفه‌ای فارسی برای کتاب صوتی هستی. فقط متن داستان نهایی را با عنوان برگردان و توضیح فنی نده."));msgs.put(new JSONObject().put("role","user").put("content",prompt));body.put("messages",msgs);body.put("temperature",0.9);body.put("max_tokens",4096);try(OutputStream os=x.getOutputStream()){os.write(body.toString().getBytes(StandardCharsets.UTF_8));}int code=x.getResponseCode();InputStream is=code>=200&&code<300?x.getInputStream():x.getErrorStream();String resp=read(is);if(code>=200&&code<300){JSONObject j=new JSONObject(resp);String r=j.getJSONArray("choices").getJSONObject(0).getJSONObject("message").optString("content","").trim();if(!r.isEmpty())return r;}last=new IOException("HTTP "+code);}catch(Exception e){last=e;}finally{if(x!=null)x.disconnect();}}throw last==null?new IOException("Qwen unavailable"):last;}
    String read(InputStream is)throws Exception{if(is==null)return "";ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] buf=new byte[4096];int n;while((n=is.read(buf))!=-1)b.write(buf,0,n);return b.toString("UTF-8");}
    String localStory(){String t=topic.getText().toString().trim();String ch=character.getText().toString().trim();if(ch.isEmpty())ch="آرین";String g=genre.getSelectedItem()==null?"ماجراجویی":genre.getSelectedItem().toString();String l=lesson.getText().toString().trim();if(l.isEmpty())l="با شجاعت، فکر کردن و کمک به دیگران می‌توان از سختی‌ها عبور کرد.";String e=ending.getText().toString().trim();if(e.isEmpty())e="پایانی شیرین و امیدوارکننده";return "داستان «"+t+"»\n\nدر یک روز متفاوت، "+ch+" تصمیم گرفت درباره «"+t+"» قدمی تازه بردارد. فضای ماجرا رنگ‌وبوی "+g+" داشت و هر اتفاق، سؤال تازه‌ای در ذهن او می‌ساخت.\n\nدر میانه راه، مشکلی پیش آمد؛ اما "+ch+" به جای ترسیدن، کمی فکر کرد، از دیگران کمک گرفت و راهی پیدا کرد که هیچ‌کس انتظارش را نداشت. همین انتخاب کوچک باعث شد مسیر ماجرا تغییر کند و دوستان تازه‌ای پیدا شوند.\n\nوقتی همه‌چیز به پایان رسید، "+ch+" فهمید که گاهی بزرگ‌ترین پیروزی، نه رسیدن سریع به مقصد، بلکه یاد گرفتن در مسیر است.\n\nپیام داستان: "+l+"\n\nنتیجه: "+e+".";}
    @Override protected void onDestroy(){super.onDestroy();if(tts!=null){tts.stop();tts.shutdown();}net.shutdownNow();}
}
