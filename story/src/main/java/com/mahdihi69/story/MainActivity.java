package com.mahdihi69.story;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.*;
import android.speech.*;
import android.speech.tts.TextToSpeech;
import android.view.*;
import android.widget.*;
import org.json.*;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.SpeechService;
import org.vosk.android.StorageService;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity { // Story Maker production build
    EditText topic, chars, lesson, ending;
    Spinner genre, age, length, provider, modelChoice;
    CheckBox poem;
    TextView output, status, libraryInfo;
    TextToSpeech nativeTts;
    SpeechRecognizer rec;
    MediaPlayer player;
    boolean ttsReady=false, voiceMode=false, voskReady=false;
    Model voskModel;
    SpeechService voskService;
        final ExecutorService net=Executors.newSingleThreadExecutor();
    static final int PICK=31;
    android.content.SharedPreferences prefs;

    int dp(int x){ return (int)(x*getResources().getDisplayMetrics().density+.5f); }
    TextView tx(String s,int z){
        TextView v=new TextView(this); v.setText(s); v.setTextSize(z);
        v.setTextColor(Color.rgb(35,38,55)); return v;
    }
    EditText ed(String h){
        EditText e=new EditText(this); e.setHint(h); e.setTextSize(16);
        e.setPadding(dp(14),dp(8),dp(14),dp(8)); e.setMinHeight(dp(54));
        e.setSingleLine(false); return e;
    }
    Button bt(String s){
        Button b=new Button(this); b.setText(s); b.setTextSize(14); b.setAllCaps(false);
        return b;
    }
    GradientDrawable cardBg(){
        GradientDrawable g=new GradientDrawable(); g.setColor(Color.WHITE); g.setCornerRadius(dp(18));
        g.setStroke(dp(1),Color.rgb(225,227,236)); return g;
    }
    void add(LinearLayout b,String title,View v){ b.addView(tx(title,15)); b.addView(v); }
    void card(LinearLayout parent,View child){
        FrameLayout f=new FrameLayout(this); f.setPadding(dp(10),dp(10),dp(10),dp(10));
        f.setBackground(cardBg()); f.addView(child); parent.addView(f,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout.LayoutParams p=(LinearLayout.LayoutParams)f.getLayoutParams(); p.setMargins(0,dp(6),0,dp(6)); f.setLayoutParams(p);
    }

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.rgb(78,70,170));
        prefs=getSharedPreferences("story_maker",0);

        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(246,247,252));
        ScrollView sc=new ScrollView(this);
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(14),dp(10),dp(14),dp(22));

        TextView title=tx("📖 داستان‌ساز",29); title.setGravity(Gravity.CENTER);
        title.setTextColor(Color.rgb(70,61,150)); box.addView(title,new LinearLayout.LayoutParams(-1,dp(58)));
        TextView sub=tx("داستان، کتاب صوتی و قصه شب با کمک هوش مصنوعی",14); sub.setGravity(Gravity.CENTER);
        box.addView(sub,new LinearLayout.LayoutParams(-1,dp(34)));

        LinearLayout quick=new LinearLayout(this); quick.setOrientation(LinearLayout.HORIZONTAL);
        Button bedtime=bt("🌙 قصه شب"), random=bt("🎲 موضوع شانسی"), library=bt("📚 کتابخانه");
        quick.addView(bedtime,new LinearLayout.LayoutParams(0,dp(50),1));
        quick.addView(random,new LinearLayout.LayoutParams(0,dp(50),1));
        quick.addView(library,new LinearLayout.LayoutParams(0,dp(50),1));
        box.addView(quick);

        topic=ed("موضوع داستان را بنویسید یا با صدا بگویید");
        chars=ed("نام شخصیت‌ها (اختیاری)");
        lesson=ed("پند یا آموزش (اختیاری)");
        ending=ed("پایان یا نتیجه دلخواه (اختیاری)");
        add(box,"🎯 موضوع",topic); add(box,"🧑‍🤝‍🧑 شخصیت‌ها",chars);

        genre=new Spinner(this); genre.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,
                new String[]{"ماجراجویی","فانتزی","آموزشی","طنز","اخلاقی","علمی-تخیلی","احساسی","رازآلود","حماسی","معمایی","دوستی","محیط‌زیستی"}));
        add(box,"🎭 ژانر",genre);

        provider=new Spinner(this); provider.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,
                new String[]{"⚡ Qwen","🧠 DeepSeek","🪄 آفلاین"}));
        add(box,"🤖 موتور هوش مصنوعی",provider);

        age=new Spinner(this); age.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,
                new String[]{"کودک ۴ تا ۷ سال","کودک ۸ تا ۱۲ سال","نوجوان","جوان","بزرگسال","خانوادگی"}));
        add(box,"👶 رده سنی",age);

        length=new Spinner(this); length.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,
                new String[]{"کوتاه","متوسط","بلند","خیلی بلند"}));
        add(box,"📏 اندازه داستان",length);

        provider=new Spinner(this); provider.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,
                new String[]{"خودکار — پل هوشمند","Qwen — مستقیم","DeepSeek — مستقیم","Gemini — مستقیم","AvalAI — مسیر ایران"}));
        add(box,"🧠 انتخاب اتصال هوش مصنوعی",provider);
        modelChoice=new Spinner(this); modelChoice.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Qwen Max — خلاقیت","DeepSeek Flash — استدلال","Gemini 2.5 Pro — مستقیم","GPT-5.4 — از مسیر AvalAI","Claude Sonnet 4.6 — از مسیر AvalAI"}));
        add(box,"🤖 مدل مورد استفاده",modelChoice);

        poem=new CheckBox(this); poem.setText("🎵 شعر کوتاه و تازه هم داخل داستان باشد"); box.addView(poem);
        add(box,"💡 پند یا آموزش",lesson); add(box,"🏁 پایان",ending);

        LinearLayout voice=new LinearLayout(this); voice.setOrientation(LinearLayout.VERTICAL);
        voice.setPadding(dp(12),dp(8),dp(12),dp(8)); voice.setBackground(cardBg());
        voice.addView(tx("🎙️ دستیار صوتی فارسی",18));
        voice.addView(tx("موضوع را بگویید؛ برنامه آن را در فرم می‌گذارد و می‌تواند داستان را بسازد.",13));
        Button vb=bt("🎤 شروع گفت‌وگوی صوتی"); voice.addView(vb); box.addView(voice);
        LinearLayout.LayoutParams vp=(LinearLayout.LayoutParams)voice.getLayoutParams(); vp.setMargins(0,dp(8),0,dp(8)); voice.setLayoutParams(vp);

        Button gen=bt("✨ ساخت داستان جدید");
        box.addView(gen,new LinearLayout.LayoutParams(-1,dp(58)));
        status=tx("آماده است",13); status.setGravity(Gravity.CENTER);
        box.addView(status,new LinearLayout.LayoutParams(-1,dp(36)));

        LinearLayout out=new LinearLayout(this); out.setOrientation(LinearLayout.VERTICAL);
        out.setPadding(dp(12),dp(10),dp(12),dp(10)); out.setBackground(cardBg());
        TextView ot=tx("📜 داستان شما",19); ot.setTextColor(Color.rgb(70,61,150)); out.addView(ot);
        output=tx("اینجا داستان شما ساخته می‌شود…",17); output.setLineSpacing(0,1.5f);
        output.setTextIsSelectable(true); out.addView(output);
        box.addView(out);

        LinearLayout row=new LinearLayout(this);
        Button speak=bt("🔊 خواندن"), stop=bt("⏹️ توقف");
        row.addView(speak,new LinearLayout.LayoutParams(0,dp(54),1));
        row.addView(stop,new LinearLayout.LayoutParams(0,dp(54),1)); box.addView(row);

        LinearLayout row2=new LinearLayout(this);
        Button save=bt("💾 ذخیره"), share=bt("📤 اشتراک"), clear=bt("🗑️ پاک‌کردن");
        row2.addView(save,new LinearLayout.LayoutParams(0,dp(52),1));
        row2.addView(share,new LinearLayout.LayoutParams(0,dp(52),1));
        row2.addView(clear,new LinearLayout.LayoutParams(0,dp(52),1)); box.addView(row2);

        libraryInfo=tx("",12); libraryInfo.setGravity(Gravity.CENTER); box.addView(libraryInfo,new LinearLayout.LayoutParams(-1,dp(30)));

        sc.addView(box); root.addView(sc,new LinearLayout.LayoutParams(-1,0,1)); setContentView(root);

        loadLast();
        new Handler(getMainLooper()).postDelayed(()->{initTts();initSpeech();initOfflineVoice();},700);

        gen.setOnClickListener(v->generate());
        vb.setOnClickListener(v->toggleVoice());
        speak.setOnClickListener(v->speak(output.getText().toString()));
        stop.setOnClickListener(v->stopAudio());
        save.setOnClickListener(v->saveStory());
        share.setOnClickListener(v->shareStory());
        clear.setOnClickListener(v->clearStory());
        random.setOnClickListener(v->randomTopic());
        bedtime.setOnClickListener(v->bedtimeMode());
        library.setOnClickListener(v->showLibrary());
    }

    void saveStory(){
        String s=output.getText().toString().trim();
        if(s.length()<20){status.setText("اول یک داستان بسازید");return;}
        JSONArray a; try{a=new JSONArray(prefs.getString("library","[]"));}catch(Exception e){a=new JSONArray();}
        JSONObject o=new JSONObject();
        try{
            o.put("title",topic.getText().toString().trim().isEmpty()?"داستان من":topic.getText().toString().trim());
            o.put("text",s); o.put("time",System.currentTimeMillis());
            a.put(o); while(a.length()>20)a.remove(0);
            prefs.edit().putString("library",a.toString()).putString("last",s).apply();
            status.setText("💾 داستان در کتابخانه ذخیره شد"); updateLibraryInfo();
        }catch(Exception e){status.setText("ذخیره انجام نشد");}
    }
    void shareStory(){
        String s=output.getText().toString().trim(); if(s.isEmpty())return;
        Intent i=new Intent(Intent.ACTION_SEND); i.setType("text/plain"); i.putExtra(Intent.EXTRA_TEXT,s);
        startActivity(Intent.createChooser(i,"اشتراک داستان"));
    }
    void clearStory(){ new AlertDialog.Builder(this).setTitle("پاک کردن داستان").setMessage("متن داستان فعلی پاک شود؟")
        .setNegativeButton("لغو",null).setPositiveButton("پاک کن",(d,w)->{output.setText("اینجا داستان شما ساخته می‌شود…");status.setText("پاک شد");}).show(); }

    void showLibrary(){
        JSONArray a;
        try{a=new JSONArray(prefs.getString("library","[]"));}catch(Exception e){a=new JSONArray();}
        if(a.length()==0){new AlertDialog.Builder(this).setTitle("📚 کتابخانه").setMessage("هنوز داستانی ذخیره نشده است.").setPositiveButton("باشه",null).show();return;}
        String[] titles=new String[a.length()];
        for(int i=0;i<a.length();i++)try{titles[i]=a.getJSONObject(a.length()-1-i).optString("title","داستان");}catch(Exception e){titles[i]="داستان";}
        new AlertDialog.Builder(this).setTitle("📚 داستان‌های ذخیره‌شده").setItems(titles,(d,which)->{
            try{JSONObject o=a.getJSONObject(a.length()-1-which);topic.setText(o.optString("title"));output.setText(o.optString("text"));status.setText("📖 داستان از کتابخانه باز شد");}catch(Exception ignored){}
        }).setNegativeButton("بستن",null).show();
    }
    void updateLibraryInfo(){
        try{JSONArray a=new JSONArray(prefs.getString("library","[]"));libraryInfo.setText("📚 "+a.length()+" داستان در کتابخانه شما");}catch(Exception ignored){}
    }
    void loadLast(){String s=prefs.getString("last","");if(!s.isEmpty())output.setText(s);updateLibraryInfo();}

    void randomTopic(){
        String[] t={"دوستی یک کودک با یک ربات مهربان","راز یک خانه قدیمی کنار دریا","گربه‌ای که می‌توانست ستاره‌ها را ببیند","ماجراجویی در جنگل درخشان","مدرسه‌ای که شب‌ها زنده می‌شد","بادبادکی که آرزوی پرواز تا ماه داشت","یک سفر شگفت‌انگیز به آینده","درختی که قصه‌های مردم را حفظ می‌کرد"};
        topic.setText(t[new Random().nextInt(t.length)]); status.setText("🎲 موضوع تازه انتخاب شد");
    }
    void bedtimeMode(){
        genre.setSelection(7); age.setSelection(0); length.setSelection(1); poem.setChecked(false);
        topic.setText("یک قصه آرام و شیرین برای قبل از خواب درباره دوستی و یک اتفاق جادویی");
        lesson.setText("مهربانی، شجاعت و اعتماد به خود");
        ending.setText("پایانی آرام، امیدبخش و مناسب خواب کودک");
        status.setText("🌙 حالت قصه شب آماده شد");
    }

    String qkey(){return BuildConfig.QWEN_API_KEY==null?"":BuildConfig.QWEN_API_KEY.trim();}
    String dkey(){return BuildConfig.DEEPSEEK_API_KEY==null?"":BuildConfig.DEEPSEEK_API_KEY.trim();}
    String gkey(){return BuildConfig.GEMINI_API_KEY==null?"":BuildConfig.GEMINI_API_KEY.trim();}
    String akey(){return BuildConfig.AVALAI_API_KEY==null?"":BuildConfig.AVALAI_API_KEY.trim();}

    String storyPrompt(String t){
        return "یک داستان کاملاً جدید و غیرتکراری فارسی برای کتاب صوتی بنویس. موضوع: "+t+
        ". شخصیت‌ها: "+chars.getText()+". ژانر: "+genre.getSelectedItem()+". سن: "+age.getSelectedItem()+
        ". طول: "+length.getSelectedItem()+". پند: "+lesson.getText()+". پایان: "+ending.getText()+
        ". "+(poem.isChecked()?"یک شعر کوتاه کاملاً جدید و مرتبط داخل داستان بیاور. ":"")+
        "شروع قوی، شخصیت‌پردازی، گفت‌وگوی طبیعی، حداقل دو اتفاق مهم، یک نقطه عطف و پایان رضایت‌بخش داشته باشد. برای کودک ساده و تصویری و برای بزرگسال داستانی‌تر بنویس. فقط متن نهایی داستان را برگردان.";
    }

    String aiCall(String name,String prompt)throws Exception{
        String key=""; String url=""; String model=""; String label=name;
        String chosen=String.valueOf(modelChoice.getSelectedItem());
        if(name.equals("Qwen")){
            key=qkey(); url="https://dashscope-intl.aliyuncs.com/compatible-mode/v1/chat/completions";
            model="qwen-max-latest";
        } else if(name.equals("DeepSeek")){
            key=dkey(); url="https://api.deepseek.com/chat/completions";
            model="deepseek-flash";
        } else if(name.equals("Gemini")) {
            return geminiCall(prompt);
        } else {
            key=akey();
            url="https://api.avalai.ir/v1/chat/completions";
            if(chosen.startsWith("Claude")) model="claude-sonnet-4-6";
            else if(chosen.startsWith("Gemini")) model="gemini-2.5-pro";
            else model="gpt-5.4";
        }
        if(key.isEmpty())throw new IOException(label+" key missing");
        JSONObject body=new JSONObject().put("model",model).put("temperature",1.0).put("max_tokens",7000);
        JSONArray m=new JSONArray();
        m.put(new JSONObject().put("role","system").put("content","فقط فارسی بنویس. خلاق، منسجم، دقیق و غیرتکراری باش."));
        m.put(new JSONObject().put("role","user").put("content",prompt)); body.put("messages",m);
        String r=post(url,key,body.toString());
        JSONObject root=new JSONObject(r);
        String s=root.getJSONArray("choices").getJSONObject(0).getJSONObject("message").optString("content","").trim();
        if(s.isEmpty())throw new IOException(label+" empty response");
        return s;
    }


    String geminiCall(String prompt)throws Exception{
        String key=gkey();
        if(key.isEmpty()) throw new IOException("Gemini key missing");
        String model="gemini-2.5-pro";
        String url="https://generativelanguage.googleapis.com/v1beta/models/"+model+":generateContent?key="+URLEncoder.encode(key,"UTF-8");
        JSONObject part=new JSONObject().put("text",prompt);
        JSONArray parts=new JSONArray().put(part);
        JSONObject user=new JSONObject().put("role","user").put("parts",parts);
        JSONArray contents=new JSONArray().put(user);
        JSONObject gen=new JSONObject().put("temperature",1.0).put("maxOutputTokens",7000);
        JSONObject body=new JSONObject().put("contents",contents).put("generationConfig",gen);
        String r=postNoAuth(url,body.toString());
        JSONObject root=new JSONObject(r);
        JSONArray candidates=root.optJSONArray("candidates");
        if(candidates==null||candidates.length()==0) throw new IOException("Gemini empty response");
        JSONObject content=candidates.getJSONObject(0).optJSONObject("content");
        if(content==null) throw new IOException("Gemini missing content");
        JSONArray pp=content.optJSONArray("parts");
        StringBuilder out=new StringBuilder();
        if(pp!=null) for(int i=0;i<pp.length();i++) out.append(pp.getJSONObject(i).optString("text",""));
        if(out.toString().trim().isEmpty()) throw new IOException("Gemini empty text");
        return out.toString().trim();
    }

    String generateWithBridge(String prompt)throws Exception{
        String selected=String.valueOf(provider.getSelectedItem());
        ArrayList<String> order=new ArrayList<>();
        if(selected.startsWith("Qwen")) order.add("Qwen");
        else if(selected.startsWith("DeepSeek")) order.add("DeepSeek");
        else if(selected.startsWith("Gemini")) order.add("Gemini");
        else if(selected.startsWith("AvalAI")) order.add("AvalAI");
        else { order.add("Gemini"); order.add("AvalAI"); order.add("DeepSeek"); order.add("Qwen"); }
        Exception last=null;
        for(String p:order)try{
            runOnUiThread(()->status.setText("🔗 اتصال "+p+" در حال پاسخ‌گویی…"));
            String s=aiCall(p,prompt);
            final String used=p;
            runOnUiThread(()->status.setText("✅ داستان با "+used+" ساخته شد"));
            return s;
        }catch(Exception e){last=e;}
        throw last==null?new IOException("No provider"):last;
    }

    void generate(){
        String t=topic.getText().toString().trim();
        if(t.isEmpty()){status.setText("موضوع را وارد کنید یا موضوع شانسی بگیرید");return;}
        String engine=String.valueOf(provider.getSelectedItem());
        if(engine.contains("آفلاین")){status.setText("🪄 ساخت داستان آفلاین…");localStory(t);return;}
        final String key=engine.contains("DeepSeek")?dkey():qkey();
        if(key.isEmpty()){status.setText("کلید این موتور در نسخه آزمایشی تنظیم نشده؛ نسخه آفلاین اجرا شد");localStory(t);return;}
        status.setText("🧠 "+engine+" در حال نوشتن است…");
        String p="یک داستان کاملاً جدید و غیرتکراری فارسی برای کتاب صوتی بنویس. موضوع: "+t+
        ". شخصیت‌ها: "+chars.getText()+". ژانر: "+genre.getSelectedItem()+". سن: "+age.getSelectedItem()+
        ". طول: "+length.getSelectedItem()+". پند: "+lesson.getText()+". پایان: "+ending.getText()+
        ". "+(poem.isChecked()?"یک شعر کوتاه کاملاً جدید و مرتبط داخل داستان بیاور. ":"")+
        "شروع داستان باید قوی باشد، شخصیت‌ها هدف و ویژگی داشته باشند، گفت‌وگو طبیعی باشد، حداقل دو اتفاق مهم و یک نقطه عطف داشته باشد و پایان مشخص و رضایت‌بخش باشد. فقط متن نهایی داستان را برگردان.";
        final String endpoint=engine.contains("DeepSeek")?"https://api.deepseek.com/chat/completions":"https://dashscope-intl.aliyuncs.com/compatible-mode/v1/chat/completions";
        final String model=engine.contains("DeepSeek")?"deepseek-chat":"qwen3.7-plus";
        net.execute(()->{try{
            JSONObject body=new JSONObject().put("model",model).put("temperature",1.1).put("max_tokens",6500);
            JSONArray m=new JSONArray();
            m.put(new JSONObject().put("role","system").put("content","فقط فارسی بنویس. خلاق، منسجم و غیرتکراری باش."));
            m.put(new JSONObject().put("role","user").put("content",p)); body.put("messages",m);
            String r=post(endpoint,key,body.toString());
            String s=new JSONObject(r).getJSONArray("choices").getJSONObject(0).getJSONObject("message").optString("content","").trim();
            if(s.isEmpty())throw new IOException();
            runOnUiThread(()->{output.setText(s);prefs.edit().putString("last",s).apply();status.setText("✅ داستان با "+engine+" آماده شد");if(voiceMode)speak(s);});
        }catch(Exception e){runOnUiThread(()->{status.setText("⚠️ اتصال ناموفق؛ نسخه آفلاین اجرا شد");localStory(t);});}});
    }

    void localStory(String t){
        String n=chars.getText().toString().trim(); String l=lesson.getText().toString().trim();
        String end=ending.getText().toString().trim(); String g=String.valueOf(genre.getSelectedItem());
        String a=String.valueOf(age.getSelectedItem()); String hero=n.isEmpty()?"آرین":n.split("[،, ]")[0];
        String friend=n.contains("،")?n.substring(n.indexOf("،")+1).trim():"نیلا";
        Random r=new Random();
        String place=new String[]{"دهکده‌ای کنار دریا","جنگلی پر از نورهای رنگی","شهری شلوغ و آینده‌نگر","خانه‌ای قدیمی روی تپه","مدرسه‌ای عجیب در میان ابرها"}[r.nextInt(5)];
        String object=new String[]{"یک کلید نقره‌ای","یک نقشه قدیمی","یک ساعت که زمان را متوقف می‌کرد","یک کتاب سخنگو","یک فانوس آبی"}[r.nextInt(5)];
        StringBuilder b=new StringBuilder();
        b.append("📖 ").append(t).append("\n\n");
        b.append("در ").append(place).append("، ").append(hero).append(" روزی با یک راز عجیب روبه‌رو شد: ").append(object).append(" درست جلوی او ظاهر شد. ");
        b.append("او اول فکر کرد این فقط یک اتفاق معمولی است، اما وقتی آن را لمس کرد، صدایی آرام گفت: «اگر شجاعت داشته باشی، پاسخ را پیدا می‌کنی.»\n\n");
        b.append("آن شب، ").append(hero).append(" تصمیم گرفت راز را دنبال کند. ").append(friend).append(" هم همراهش شد و گفت: «دو نفری بهتر می‌توانیم از پسش بربیاییم.» ");
        b.append("آن‌ها از مسیر باریکی گذشتند و به جایی رسیدند که هیچ‌کدام قبلاً ندیده بودند. در آنجا انتخاب سختی منتظرشان بود.\n\n");
        b.append("نقطه عطف داستان زمانی رسید که ").append(hero).append(" فهمید پاسخ اصلی بیرون از خودش نیست؛ باید بین ترس و امید یکی را انتخاب کند. ");
        b.append("او یک قدم جلو رفت و ").append(friend).append(" هم کنارش ایستاد. همین همکاری باعث شد راز ").append(object).append(" آشکار شود و راهی تازه پیش رویشان قرار بگیرد.\n\n");
        if(g.contains("طنز")) b.append("البته وسط این ماجرا یک اتفاق خنده‌دار هم افتاد و همه چند دقیقه‌ای از خنده نمی‌توانستند حرف بزنند.\n\n");
        if(g.contains("آموزشی")) b.append("🔎 نکته‌ای که ").append(hero).append(" یاد گرفت این بود که پرسیدن و امتحان کردن، راه رسیدن به پاسخ است.\n\n");
        if(!l.isEmpty()) b.append("💡 پند و آموزش: ").append(l).append("\n\n");
        if(poem.isChecked()) b.append("🎵 شعر:\n«هر جا که امیدی هست / راهی برای فرداست / با دلِ روشن و مهربان / دنیا همیشه زیباست»\n\n");
        b.append("🏁 پایان: ").append(end.isEmpty()?"آن‌ها با آرامش به خانه برگشتند و فهمیدند بعضی از بهترین ماجراهای زندگی از یک انتخاب کوچک و شجاعانه شروع می‌شود.":end);
        output.setText(b.toString()); prefs.edit().putString("last",b.toString()).apply(); status.setText("✅ داستان آماده شد — حالت آفلاین");
        if(voiceMode)speak(output.getText().toString());
    }

    void initTts(){try{nativeTts=new TextToSpeech(this,x->{if(x==TextToSpeech.SUCCESS){int rr=nativeTts.setLanguage(new Locale("fa","IR"));ttsReady=rr!=TextToSpeech.LANG_MISSING_DATA&&rr!=TextToSpeech.LANG_NOT_SUPPORTED;nativeTts.setSpeechRate(.9f);}});}catch(Exception ignored){}}
    void initSpeech(){try{if(!SpeechRecognizer.isRecognitionAvailable(this))return;rec=SpeechRecognizer.createSpeechRecognizer(this);rec.setRecognitionListener(new RecognitionListener(){
        public void onReadyForSpeech(Bundle b){status.setText("🎙️ گوش می‌دهم…");}
        public void onBeginningOfSpeech(){} public void onRmsChanged(float r){} public void onBufferReceived(byte[] b){} public void onEndOfSpeech(){}
        public void onResults(Bundle b){ArrayList<String>a=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);if(a!=null&&!a.isEmpty())voiceCommand(a.get(0));else if(voiceMode)listen();}
        public void onError(int e){if(voiceMode)new Handler(getMainLooper()).postDelayed(()->listen(),600);}
        public void onPartialResults(Bundle b){} public void onEvent(int e,Bundle b){}
    });}catch(Exception ignored){}}

    void initOfflineVoice(){
        if(Build.VERSION.SDK_INT>=23 && checkSelfPermission("android.permission.RECORD_AUDIO")!=android.content.pm.PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{"android.permission.RECORD_AUDIO"},77); return;
        }
        try{
            StorageService.unpack(this,"vosk-model-small-fa-0.4","story-vosk-model",
                m->{voskModel=m;voskReady=true;status.setText("🎙️ فرمان صوتی آفلاین فارسی آماده است");},
                e->{voskReady=false;status.setText("🎙️ حالت صوتی اینترنتی آماده است");});
        }catch(Exception e){voskReady=false;}
    }

    void startVosk(){
        if(!voskReady||voskModel==null){listen();return;}
        try{
            if(voskService!=null){voskService.stop();voskService.shutdown();voskService=null;}
            Recognizer rr=new Recognizer(voskModel,16000.0f);
            voskService=new SpeechService(rr,16000.0f);
            voskService.startListening(new org.vosk.android.RecognitionListener(){
                public void onPartialResult(String s){}
                public void onResult(String s){handleVoskJson(s,false);}
                public void onFinalResult(String s){handleVoskJson(s,true);}
                public void onError(Exception e){if(voiceMode)new Handler(getMainLooper()).postDelayed(()->startVosk(),500);}
                public void onTimeout(){if(voiceMode)new Handler(getMainLooper()).postDelayed(()->startVosk(),300);}
            });
            status.setText("🎙️ گوش می‌دهم — بدون اینترنت");
        }catch(Exception e){listen();}
    }

    void handleVoskJson(String json,boolean fin){
        try{
            String s=new JSONObject(json).optString(fin?"text":"partial","").trim();
            if(fin&&s.length()>1){voiceCommand(s); if(voiceMode)new Handler(getMainLooper()).postDelayed(()->startVosk(),700);}
        }catch(Exception ignored){}
    }

    void toggleVoice(){
        voiceMode=!voiceMode;
        if(voiceMode){
            status.setText(voskReady?"🎙️ فرمان صوتی آفلاین فارسی فعال شد":"🎙️ فرمان صوتی فعال شد");
            if(voskReady)startVosk();else listen();
        }else{
            if(voskService!=null){try{voskService.stop();voskService.shutdown();}catch(Exception ignored){}voskService=null;}
            if(rec!=null)rec.cancel();
            status.setText("⏹️ دستیار صوتی خاموش شد");
        }
    }
    void listen(){
        if(!voiceMode||rec==null)return;
        try{rec.cancel();Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"fa-IR");i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,2);rec.startListening(i);
        }catch(Exception ignored){}
    }
    void voiceCommand(String s){
        s=s.replace('ي','ی').replace('ك','ک').trim(); if(s.isEmpty())return;
        String x=s.replace("داستان"," ").replace("قصه"," ").replace("بساز"," ").replace("بنویس"," ").trim();
        topic.setText(x.length()>3?x:s); generate();
    }

    void speak(String text){if(text==null||text.trim().isEmpty())return;String k=akey();if(!k.isEmpty())net.execute(()->{try{
        JSONObject b=new JSONObject().put("model","gpt-4o-mini-tts").put("voice","coral").put("input",text.length()>4096?text.substring(0,4096):text).put("response_format","mp3");
        byte[] a=bytes("https://api.avalai.ir/v1/audio/speech",k,b.toString());File f=new File(getCacheDir(),"story_voice.mp3");
        try(FileOutputStream o=new FileOutputStream(f)){o.write(a);}runOnUiThread(()->play(f));return;
    }catch(Exception ignored){}runOnUiThread(()->nativeSpeak(text));});else nativeSpeak(text);}

    String postNoAuth(String u,String d)throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(u).openConnection();
        c.setRequestMethod("POST"); c.setConnectTimeout(15000); c.setReadTimeout(90000); c.setDoOutput(true);
        c.setRequestProperty("Content-Type","application/json");
        try(OutputStream o=c.getOutputStream()){o.write(d.getBytes(StandardCharsets.UTF_8));}
        int code=c.getResponseCode();
        InputStream in=code>=200&&code<300?c.getInputStream():c.getErrorStream();
        String r=read(in); c.disconnect();
        if(code<200||code>=300)throw new IOException("HTTP "+code);
        return r;
    }
    String post(String u,String k,String d)throws Exception{HttpURLConnection c=(HttpURLConnection)new URL(u).openConnection();c.setRequestMethod("POST");c.setConnectTimeout(15000);c.setReadTimeout(90000);c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");c.setRequestProperty("Authorization","Bearer "+k);try(OutputStream o=c.getOutputStream()){o.write(d.getBytes(StandardCharsets.UTF_8));}int code=c.getResponseCode();InputStream in=code>=200&&code<300?c.getInputStream():c.getErrorStream();String r=read(in);c.disconnect();if(code<200||code>=300)throw new IOException();return r;}
    byte[] bytes(String u,String k,String d)throws Exception{HttpURLConnection c=(HttpURLConnection)new URL(u).openConnection();c.setRequestMethod("POST");c.setConnectTimeout(15000);c.setReadTimeout(60000);c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");c.setRequestProperty("Authorization","Bearer "+k);try(OutputStream o=c.getOutputStream()){o.write(d.getBytes(StandardCharsets.UTF_8));}int code=c.getResponseCode();if(code<200||code>=300)throw new IOException();InputStream in=c.getInputStream();ByteArrayOutputStream o=new ByteArrayOutputStream();byte[] z=new byte[8192];int n;while((n=in.read(z))!=-1)o.write(z,0,n);in.close();c.disconnect();return o.toByteArray();}
    String read(InputStream in)throws Exception{BufferedReader r=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8));StringBuilder s=new StringBuilder();String z;while((z=r.readLine())!=null)s.append(z);return s.toString();}
    void play(File f){try{stopAudio();player=new MediaPlayer();player.setDataSource(f.getAbsolutePath());player.setOnCompletionListener(p->{p.release();player=null;if(voiceMode)listen();});player.prepare();player.start();}catch(Exception e){nativeSpeak(output.getText().toString());}}
    void nativeSpeak(String s){if(!ttsReady||nativeTts==null)return;nativeTts.stop();nativeTts.speak(s,TextToSpeech.QUEUE_FLUSH,null,"story");}
    void stopAudio(){if(player!=null){try{player.stop();}catch(Exception ignored){}player.release();player=null;}if(nativeTts!=null)nativeTts.stop();}
    void setBg(Uri u){try{InputStream in=getContentResolver().openInputStream(u);Bitmap b=BitmapFactory.decodeStream(in);if(in!=null)in.close();if(b==null)return;getWindow().getDecorView().setBackground(new android.graphics.drawable.BitmapDrawable(getResources(),b));}catch(Exception ignored){}}
    @Override protected void onActivityResult(int r,int c,Intent d){super.onActivityResult(r,c,d);if(r==PICK&&c==RESULT_OK&&d!=null)setBg(d.getData());}
    @Override protected void onDestroy(){voiceMode=false;if(rec!=null)rec.destroy();if(nativeTts!=null)nativeTts.shutdown();if(voskService!=null){try{voskService.stop();voskService.shutdown();}catch(Exception ignored){}} if(voskModel!=null){try{voskModel.close();}catch(Exception ignored){}} if(player!=null)player.release();net.shutdownNow();super.onDestroy();}
}