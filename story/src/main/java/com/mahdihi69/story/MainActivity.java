package com.mahdihi69.story;

import android.app.*;
import android.os.*;
import android.graphics.Color;
import android.content.*;
import android.view.*;
import android.widget.*;
import android.graphics.drawable.GradientDrawable;
import android.speech.tts.TextToSpeech;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    EditText topic, character, lesson, ending;
    Spinner genre, age;
    TextView output, status;
    Button generate, speak, save, share;
    TextToSpeech tts;
    final ExecutorService net=Executors.newSingleThreadExecutor();
    int dp(float x){return (int)(x*getResources().getDisplayMetrics().density+.5f);}
    TextView text(String s,float size,int color){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(color);return v;}
    TextView label(String s){TextView v=text(s,14,Color.rgb(65,65,85));v.setPadding(dp(4),dp(10),dp(4),dp(5));return v;}
    EditText edit(String hint){EditText e=new EditText(this);e.setHint(hint);e.setTextSize(15);e.setSingleLine(false);e.setPadding(dp(14),dp(9),dp(14),dp(9));e.setBackground(round(Color.WHITE,dp(14)));return e;}
    GradientDrawable round(int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(radius);g.setStroke(dp(1),Color.rgb(225,226,235));return g;}
    LinearLayout card(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(12),dp(5),dp(12),dp(9));c.setBackground(round(Color.WHITE,dp(18)));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(5),0,dp(5));c.setLayoutParams(p);return c;}
    TextView iconLabel(String icon,String title,String hint){TextView v=text(icon+"  "+title+"\n"+hint,15,Color.rgb(45,45,65));v.setPadding(dp(5),dp(8),dp(5),dp(7));return v;}
    Button action(String icon,String title){Button b=new Button(this);b.setText(icon+"  "+title);b.setTextSize(14);b.setAllCaps(false);b.setTextColor(Color.WHITE);b.setBackground(round(Color.rgb(91,82,190),dp(15)));return b;}

    @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(Color.rgb(246,247,251));getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(14),dp(8),dp(14),dp(12));root.setBackgroundColor(Color.rgb(246,247,251));
        ScrollView scroll=new ScrollView(this);LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);
        TextView title=text("📖  داستان‌ساز",28,Color.rgb(48,43,95));title.setGravity(Gravity.CENTER);box.addView(title,new LinearLayout.LayoutParams(-1,dp(58)));
        TextView sub=text("✨ قصه‌ای که دقیقاً برای شنونده تو ساخته می‌شود",14,Color.rgb(95,94,115));sub.setGravity(Gravity.CENTER);box.addView(sub,new LinearLayout.LayoutParams(-1,dp(30)));
        LinearLayout smart=card();smart.addView(iconLabel("🧠","داستان هوشمند","موضوع + شخصیت + سن + سبک + پیام را ترکیب می‌کنیم"));box.addView(smart);

        LinearLayout c=card();c.addView(iconLabel("🎯","موضوع داستان","ایده اصلی داستان را بنویسید"));topic=edit("مثلاً سفر یک کودک به شهر ستاره‌ها");c.addView(topic);box.addView(c);
        c=card();c.addView(iconLabel("🧑‍🤝‍🧑","شخصیت‌ها","نام‌ها اختیاری؛ چند شخصیت را با «و» جدا کنید"));character=edit("مثلاً آرش و نیلوفر و ربات کوچولو");c.addView(character);box.addView(c);
        c=card();c.addView(iconLabel("🎭","ژانر و حال‌وهوا","سبک داستان را انتخاب کنید"));genre=new Spinner(this);genre.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"ماجراجویی","آموزشی","طنز","فانتزی","اخلاقی","علمی-تخیلی","احساسی","رازآلود","حماسی"}));c.addView(genre);box.addView(c);
        c=card();c.addView(iconLabel("👶","رده سنی","زبان و پیچیدگی داستان بر اساس سن تنظیم می‌شود"));age=new Spinner(this);age.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"کودک ۴ تا ۷ سال","کودک ۸ تا ۱۲ سال","نوجوان","جوان","بزرگسال","خانوادگی"}));c.addView(age);box.addView(c);
        c=card();c.addView(iconLabel("💡","پند یا آموزش","پیام موردنظر را وارد کنید؛ اختیاری"));lesson=edit("مثلاً اهمیت راستگویی و کمک به دیگران");c.addView(lesson);box.addView(c);
        c=card();c.addView(iconLabel("🏁","پایان و نتیجه","نوع پایان را مشخص کنید؛ اختیاری"));ending=edit("مثلاً پایان شاد، غافلگیرکننده و آموزنده");c.addView(ending);box.addView(c);

        generate=action("✨","ساخت داستان اختصاصی");LinearLayout.LayoutParams gp=new LinearLayout.LayoutParams(-1,dp(55));gp.setMargins(0,dp(8),0,dp(8));box.addView(generate,gp);
        status=text("آماده داستان‌پردازی است",13,Color.rgb(100,99,120));status.setGravity(Gravity.CENTER);box.addView(status,new LinearLayout.LayoutParams(-1,dp(28)));
        LinearLayout outCard=card();TextView outTitle=text("📜  داستان شما",18,Color.rgb(48,43,95));outTitle.setPadding(dp(5),dp(7),dp(5),dp(7));outCard.addView(outTitle);output=text("داستان شما اینجا ساخته می‌شود…",16,Color.rgb(40,40,55));output.setLineSpacing(0,1.4f);output.setTextIsSelectable(true);output.setPadding(dp(10),dp(10),dp(10),dp(14));outCard.addView(output);box.addView(outCard);
        LinearLayout actions=new LinearLayout(this);actions.setGravity(Gravity.CENTER);actions.setPadding(0,dp(3),0,dp(8));speak=action("🔊","خواندن");save=action("💾","ذخیره");share=action("📤","اشتراک");actions.addView(speak,new LinearLayout.LayoutParams(0,dp(50),1));LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(0,dp(50),1);sp.setMargins(dp(5),0,dp(5),0);actions.addView(save,sp);actions.addView(share,new LinearLayout.LayoutParams(0,dp(50),1));box.addView(actions);
        TextView footer=text("🎬 سری‌سازی  •  🎨 داستان اختصاصی  •  🎧 کتاب صوتی",13,Color.rgb(110,108,130));footer.setGravity(Gravity.CENTER);box.addView(footer,new LinearLayout.LayoutParams(-1,dp(35)));
        scroll.addView(box);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);
        tts=new TextToSpeech(this,s->{if(s==TextToSpeech.SUCCESS){try{tts.setLanguage(new java.util.Locale("fa","IR"));}catch(Exception ignored){}tts.setSpeechRate(.9f);}});
        generate.setOnClickListener(v->generate());
        speak.setOnClickListener(v->{String s=output.getText().toString();if(!s.isEmpty()&&!s.startsWith("داستان شما"))tts.speak(s,TextToSpeech.QUEUE_FLUSH,null,"story");});
        save.setOnClickListener(v->{getPreferences(0).edit().putString("last_story",output.getText().toString()).apply();status.setText("💾 داستان در گوشی ذخیره شد");});
        share.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(Intent.EXTRA_TEXT,output.getText().toString());startActivity(Intent.createChooser(i,"اشتراک‌گذاری داستان"));});
    }
    void generate(){String t=topic.getText().toString().trim();if(t.isEmpty()){topic.setError("موضوع را وارد کنید");return;}generate.setEnabled(false);status.setText("🪄 در حال خلق یک داستان تازه…");output.setText("⏳ نویسنده هوشمند در حال داستان‌پردازی است…");
        final String prompt="یک داستان کاملاً تازه و غیرکپی به زبان فارسی بنویس. موضوع: "+t+". شخصیت‌ها: "+character.getText().toString().trim()+". ژانر: "+genre.getSelectedItem()+". رده سنی: "+age.getSelectedItem()+". پند/آموزش: "+lesson.getText().toString().trim()+". پایان/نتیجه: "+ending.getText().toString().trim()+". عنوان جذاب، شروع قوی، شخصیت‌پردازی، اتفاقات تصویری، اوج، حل مسئله و پایان رضایت‌بخش داشته باشد. برای شنیدن با صدای بلند روان باشد. اگر شخصیت‌ها خالی بودند خودت نام‌های مناسب انتخاب کن. از کلیشه و تکرار دوری کن.";
        net.execute(()->{try{String key=BuildConfig.GEMINI_API_KEY;if(key==null||key.trim().isEmpty()){runOnUiThread(()->{output.setText("⚠️ سرویس هوش مصنوعی روی نسخه آزمایشی فعال نشده است.");status.setText("اتصال هوش مصنوعی آماده نیست");generate.setEnabled(true);});return;}JSONObject body=new JSONObject();JSONArray c=new JSONArray();JSONObject co=new JSONObject();JSONArray p=new JSONArray();p.put(new JSONObject().put("text",prompt));co.put("role","user").put("parts",p);c.put(co);body.put("contents",c);String[] models={"gemini-3.8-flash","gemini-3.7-flash","gemini-3.5-flash-lite"};String result=null;for(String m:models){HttpURLConnection x=(HttpURLConnection)new URL("https://generativelanguage.googleapis.com/v1beta/models/"+m+":generateContent").openConnection();x.setRequestMethod("POST");x.setConnectTimeout(15000);x.setReadTimeout(45000);x.setDoOutput(true);x.setRequestProperty("Content-Type","application/json");x.setRequestProperty("x-goog-api-key",key);try(OutputStream o=x.getOutputStream()){o.write(body.toString().getBytes(StandardCharsets.UTF_8));}int code=x.getResponseCode();InputStream in=code>=200&&code<300?x.getInputStream():x.getErrorStream();String r=read(in);x.disconnect();if(code>=200&&code<300){JSONObject q=new JSONObject(r);result=q.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text");break;}}final String result2=result;runOnUiThread(()->{output.setText(result2==null?"❌ ساخت داستان انجام نشد؛ اتصال سرویس را بررسی کنید.":result2);status.setText(result2==null?"خطا در داستان‌پردازی":"✅ داستان با موفقیت ساخته شد");generate.setEnabled(true);});}catch(Exception e){runOnUiThread(()->{output.setText("❌ خطا در ساخت داستان. دوباره تلاش کنید.");status.setText("خطای اتصال");generate.setEnabled(true);});}});}
    String read(InputStream in)throws Exception{if(in==null)return "";StringBuilder s=new StringBuilder();try(BufferedReader r=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8))){String l;while((l=r.readLine())!=null)s.append(l);}return s.toString();}
    @Override protected void onDestroy(){net.shutdownNow();if(tts!=null){tts.stop();tts.shutdown();}super.onDestroy();}
}
