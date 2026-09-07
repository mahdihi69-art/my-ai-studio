package com.mahdihi69.pouya;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {
    private WebView webView;
    private SpeechRecognizer speechRecognizer;
    private TextToSpeech tts;
    private boolean ttsReady = false;
    private boolean usingOnDevice = false;
    private boolean liveVoice = false;
    private boolean speaking = false;
    private String pendingSpeech;
    private static final int MIC_REQUEST = 10;
    private static final String PREFS = "pouya_settings";
    private static final String KEY_GEMINI = "gemini_api_key";
    private final ExecutorService network = Executors.newSingleThreadExecutor();

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        webView = new WebView(this);
        setContentView(webView);
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        webView.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView v, String u) { installBridge(); installQuickActions(); installManagementTools(); }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public void onPermissionRequest(PermissionRequest r) { runOnUiThread(() -> r.grant(r.getResources())); }
        });
        webView.addJavascriptInterface(new VoiceBridge(), "AndroidVoice");
        initTts();
        initRecognizer();
        if (android.os.Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, MIC_REQUEST);
        webView.loadUrl("https://mahdihi69-art.github.io/my-ai-studio/");
    }

    private void initTts() {
        tts = new TextToSpeech(this, status -> {
            if (status != TextToSpeech.SUCCESS) return;
            int r = tts.setLanguage(new Locale("fa", "IR"));
            if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) r = tts.setLanguage(new Locale("fa"));
            tts.setSpeechRate(0.92f);
            tts.setPitch(1.0f);
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override public void onStart(String id) { speaking = true; }
                @Override public void onDone(String id) { runOnUiThread(() -> { speaking = false; if (liveVoice) startListening(); }); }
                @Override public void onError(String id) { runOnUiThread(() -> { speaking = false; if (liveVoice) startListening(); }); }
            });
            ttsReady = r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED;
            if (!ttsReady) js("showToast('🔊 صدای فارسی روی گوشی نصب نیست؛ Voice data فارسی را نصب کنید.');");
            if (pendingSpeech != null) { String x = pendingSpeech; pendingSpeech = null; speakNative(x); }
        });
    }

    private void initRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) return;
        try {
            if (android.os.Build.VERSION.SDK_INT >= 31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(this)) {
                speechRecognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(this); usingOnDevice = true;
            } else { speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this); usingOnDevice = false; }
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override public void onReadyForSpeech(Bundle b) { js("updateVoice(" + JSONObject.quote(usingOnDevice ? "🔒 آفلاین؛ پویا گوش می‌دهد..." : "🎙️ پویا گوش می‌دهد...") + ");"); }
                @Override public void onBeginningOfSpeech() { js("updateVoice('🗣️ دارم گوش می‌دهم...');"); }
                @Override public void onRmsChanged(float r) {}
                @Override public void onBufferReceived(byte[] b) {}
                @Override public void onEndOfSpeech() { js("updateVoice('🧠 دارم فکر می‌کنم...');"); }
                @Override public void onResults(Bundle r) { ArrayList<String> m=r.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION); if(m!=null&&!m.isEmpty()) processVoice(m.get(0)); else if(liveVoice) startListening(); }
                @Override public void onError(int e) { js("updateVoice('⚠️ صدای شما دریافت نشد');"); if(liveVoice) startListening(); }
                @Override public void onPartialResults(Bundle b) {}
                @Override public void onEvent(int e, Bundle b) {}
            });
        } catch(Exception e){ speechRecognizer=null; }
    }

    private void processVoice(String text){
        text=normalize(text);
        if(text.isEmpty()){if(liveVoice)startListening();return;}
        js("updateVoice("+JSONObject.quote("🗣️ "+text)+");");
        if(!handleLocalVoice(text)) askGemini(text);
    }

    private void startListening(){
        if(!liveVoice||speaking||speechRecognizer==null)return;
        runOnUiThread(()->{try{
            speechRecognizer.cancel();
            Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"fa-IR");
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,"fa-IR");
            i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,3);
            i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true);
            speechRecognizer.startListening(i);
        }catch(Exception ignored){}});
    }

    private void installBridge(){webView.evaluateJavascript("(function(){window.speak=function(t){AndroidVoice.speak(String(t));};window.toggleVoice=function(){AndroidVoice.toggleVoice();};})();",null);}

    private void installQuickActions(){String js="(function(){if(document.getElementById('pouyaNativeTools'))return;var b=document.createElement('div');b.id='pouyaNativeTools';b.innerHTML='<button onclick=quickBalance()>💳 موجودی</button><button onclick=dailyReport()>📊 گزارش امروز</button>';var c=document.querySelector('.container');if(c)c.insertBefore(b,c.children[2]);window.quickBalance=function(){var t=getTotals();var r='موجودی شما '+t.balance.toLocaleString('fa-IR')+' تومان است.';showAIMessage(r);speak(r);};})();";webView.evaluateJavascript(js,null);}

    private void installManagementTools(){
        String js="(function(){"+
            "if(window.__pouyaManagementInstalled)return;window.__pouyaManagementInstalled=true;"+
            "var style=document.createElement('style');style.textContent="+
            JSONObject.quote("#pouyaMgmtTools{display:grid;grid-template-columns:1fr 1fr;gap:8px;margin-top:10px}#pouyaMgmtTools button{padding:11px;border-radius:12px;background:#f0efff;color:#35306f;font-weight:bold;font-size:12px}#pouyaMgmtTools .print{background:#eee}.row-actions{display:flex;gap:7px;margin-top:10px}.row-actions button{flex:1;padding:8px;border-radius:10px;font-size:11px;background:#fff0f0;color:#b42318}.row-actions .save{background:#eef8f3;color:#087443}.row-actions .print{background:#eef2ff;color:#3949ab}")+
            ";document.head.appendChild(style);"+
            "var card=document.querySelector('.danger')?.parentElement;if(card&&!document.getElementById('pouyaMgmtTools')){"+
            "var box=document.createElement('div');box.id='pouyaMgmtTools';box.innerHTML='<button onclick=exportTransactions(\"expense\",\"save\")>💾 ذخیره هزینه‌ها</button><button onclick=exportTransactions(\"income\",\"save\")>💾 ذخیره درآمدها</button><button class=print onclick=exportTransactions(\"expense\",\"print\")>🖨️ چاپ هزینه‌ها</button><button class=print onclick=exportTransactions(\"income\",\"print\")>🖨️ چاپ درآمدها</button>';card.appendChild(box);}"+
            "if(window.render&&!window.__pouyaRenderWrapped){var oldRender=window.render;window.render=function(){oldRender();setTimeout(addRowActions,0)};window.__pouyaRenderWrapped=true;}addRowActions();"+
            "window.deleteTransaction=function(id){id=Number(id);var t=transactions.find(function(x){return Number(x.id)===id});if(!t)return;if(!confirm('این مورد حذف شود؟\\n'+t.desc+' - '+Number(t.amount).toLocaleString('fa-IR')+' تومان'))return;transactions=transactions.filter(function(x){return Number(x.id)!==id});saveData();showToast('🗑️ مورد حذف شد');};"+
            "window.transactionText=function(t){return (t.type==='income'?'درآمد':'هزینه')+' | '+t.desc+' | '+Number(t.amount).toLocaleString('fa-IR')+' تومان | '+t.date+' '+t.time+(t.note?' | '+t.note:'')};"+
            "window.exportTransactions=function(type,mode){var list=transactions.filter(function(t){return t.type===type});if(!list.length){showToast('اطلاعاتی برای خروجی وجود ندارد');return;}var title=type==='income'?'گزارش درآمدها':'گزارش هزینه‌ها';var total=list.reduce(function(s,t){return s+Number(t.amount||0)},0);var rows=list.map(function(t,i){return '<tr><td>'+(i+1)+'</td><td>'+escapeHTML(t.desc)+'</td><td>'+Number(t.amount).toLocaleString('fa-IR')+'</td><td>'+escapeHTML(t.date||'')+'</td><td>'+escapeHTML(t.time||'')+'</td><td>'+escapeHTML(t.note||'')+'</td></tr>'}).join('');var html='<!doctype html><html lang=fa dir=rtl><meta charset=utf-8><title>'+title+'</title><style>body{font-family:Tahoma,Arial;padding:24px}h1{text-align:center}table{width:100%;border-collapse:collapse;margin-top:20px}th,td{border:1px solid #bbb;padding:9px;text-align:right}th{background:#eee}.total{font-weight:bold;margin-top:15px}</style><h1>'+title+'</h1><p>تعداد: '+list.length+'</p><table><tr><th>ردیف</th><th>عنوان</th><th>مبلغ</th><th>تاریخ</th><th>ساعت</th><th>توضیحات</th></tr>'+rows+'</table><div class=total>جمع کل: '+total.toLocaleString('fa-IR')+' تومان</div></html>';if(mode==='print'){var w=window.open('','_blank');if(!w){showToast('⚠️ پنجره چاپ باز نشد');return}w.document.write(html);w.document.close();setTimeout(function(){w.print()},400);return;}var blob=new Blob([html],{type:'text/html;charset=utf-8'});var a=document.createElement('a');a.href=URL.createObjectURL(blob);a.download=(type==='income'?'درآمدها':'هزینه‌ها')+'-پویا.html';a.click();setTimeout(function(){URL.revokeObjectURL(a.href)},1000);showToast('💾 فایل ذخیره شد');};"+
            "window.printTransaction=function(id){var t=transactions.find(function(x){return Number(x.id)===Number(id)});if(!t)return;var html='<!doctype html><html lang=fa dir=rtl><meta charset=utf-8><title>رسید تراکنش</title><style>body{font-family:Tahoma;padding:25px;line-height:2}</style><h2>رسید تراکنش پویا</h2><p>'+escapeHTML(window.transactionText(t))+'</p></html>';var w=window.open('','_blank');if(!w){showToast('⚠️ پنجره چاپ باز نشد');return}w.document.write(html);w.document.close();setTimeout(function(){w.print()},300)};"+
            "window.saveTransaction=function(id){var t=transactions.find(function(x){return Number(x.id)===Number(id)});if(!t)return;var blob=new Blob([window.transactionText(t)],{type:'text/plain;charset=utf-8'});var a=document.createElement('a');a.href=URL.createObjectURL(blob);a.download='تراکنش-'+t.id+'.txt';a.click();setTimeout(function(){URL.revokeObjectURL(a.href)},1000);showToast('💾 تراکنش ذخیره شد')};"+
            "function addRowActions(){document.querySelectorAll('.history-item').forEach(function(row){if(row.querySelector('.row-actions'))return;var title=row.querySelector('.item-title');if(!title)return;var desc=title.textContent.trim();var t=transactions.find(function(x){return x.desc===desc});if(!t)return;var a=document.createElement('div');a.className='row-actions';a.innerHTML='<button onclick=deleteTransaction('+Number(t.id)+')>🗑️ حذف این مورد</button><button class=save onclick=saveTransaction('+Number(t.id)+')>💾 ذخیره</button><button class=print onclick=printTransaction('+Number(t.id)+')>🖨️ چاپ</button>';row.appendChild(a)})}"+
            "})();";
        webView.evaluateJavascript(js,null);
    }

    private void js(String x){runOnUiThread(()->webView.evaluateJavascript("try{"+x+"}catch(e){}",null));}
    private String key(){String k=getSharedPreferences(PREFS,MODE_PRIVATE).getString(KEY_GEMINI,"").trim();return !k.isEmpty()?k:(BuildConfig.GEMINI_API_KEY==null?"":BuildConfig.GEMINI_API_KEY.trim());}

    private boolean handleLocalVoice(String t){
        Long n=amount(t);
        if(t.contains("سلام")||t.contains("خوبی")){answer("سلام! من پویا هستم. بگو چه کاری انجام بدهم.");return true;}
        if(t.contains("موجودی")||t.contains("چقدر پول دارم")||t.contains("مانده")){js("quickBalance();");return true;}
        if(t.contains("گزارش امروز")||t.equals("گزارش")){js("dailyReport();");answer("گزارش امروز آماده شد.");return true;}
        if(t.contains("این ماه")&&(t.contains("هزینه")||t.contains("خرج"))){js("monthlyReport();");answer("گزارش هزینه‌های این ماه آماده شد.");return true;}
        if(t.contains("آخرین خرید")||t.contains("آخرین هزینه")){js("(function(){if(!transactions.length){speak('هنوز تراکنشی ثبت نشده است.');return;}var x=transactions[0];speak('آخرین تراکنش '+x.desc+' به مبلغ '+x.amount.toLocaleString('fa-IR')+' تومان بود.');})();");return true;}
        boolean income=t.contains("درآمد")||t.contains("حقوق گرفتم")||t.contains("پول گرفتم")||t.contains("واریز شد");
        boolean expense=t.contains("هزینه")||t.contains("خرج کردم")||t.contains("خریدم")||t.contains("خرید کردم")||t.contains("پرداخت کردم");
        if(n!=null&&n>0&&(income||expense)){String type=income?"income":"expense";String d=income?"درآمد":description(t);String sp=(income?"درآمد ":"هزینه ")+n+" تومان ثبت شد.";js("addTransaction("+JSONObject.quote(type)+","+JSONObject.quote(d)+","+n+");showAIMessage("+JSONObject.quote(sp)+");speak("+JSONObject.quote(sp)+");updateVoice('✅ ثبت شد');");return true;}
        return false;
    }

    private void answer(String s){js("showAIMessage("+JSONObject.quote(s)+");speak("+JSONObject.quote(s)+");updateVoice('🔊 پویا پاسخ داد');");}

    private void askGemini(String text){
        String k=key(); if(k.isEmpty()){js("showToast('🔑 کلید Gemini تنظیم نشده است');AndroidVoice.requestGeminiKey();");return;}
        js("updateVoice('🧠 پویا در حال فکر کردن...');");
        network.execute(()->{
            HttpURLConnection c=null;
            try{
                JSONObject body=new JSONObject();JSONArray contents=new JSONArray();JSONObject content=new JSONObject();JSONArray parts=new JSONArray();
                String prompt="تو دستیار صوتی فارسی اپلیکیشن مدیر مالی پویا هستی. فرمان کاربر را بفهم. فقط JSON معتبر برگردان با ساختار {\"intent\":\"EXPENSE|INCOME|BALANCE|REPORT|CHAT\",\"description\":\"\",\"amount\":0,\"reply\":\"\"}. مبلغ به تومان و عدد صحیح باشد. reply کوتاه و طبیعی فارسی باشد. فرمان: "+text;
                parts.put(new JSONObject().put("text",prompt));content.put("role","user");content.put("parts",parts);contents.put(content);body.put("contents",contents);
                String[] models={"gemini-3.8-flash","gemini-3.7-flash","gemini-3.5-flash-lite"};String raw=null;int code=0;String err="";
                for(String model:models){
                    c=(HttpURLConnection)new URL("https://generativelanguage.googleapis.com/v1beta/models/"+model+":generateContent").openConnection();
                    c.setRequestMethod("POST");c.setConnectTimeout(15000);c.setReadTimeout(30000);c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");c.setRequestProperty("x-goog-api-key",k);
                    try(OutputStream o=c.getOutputStream()){o.write(body.toString().getBytes(StandardCharsets.UTF_8));}
                    code=c.getResponseCode();InputStream in=(code>=200&&code<300)?c.getInputStream():c.getErrorStream();err=readAll(in);c.disconnect();c=null;
                    if(code>=200&&code<300){raw=err;break;} if(code==401||code==403)break;
                }
                if(raw==null){String safe="Gemini HTTP "+code;try{JSONObject er=new JSONObject(err);JSONObject eo=er.optJSONObject("error");if(eo!=null)safe+=" : "+eo.optString("message","خطای API");}catch(Exception ignored){}js("showToast("+JSONObject.quote("❌ "+safe)+");updateVoice('⚠️ خطای اتصال به Gemini');");return;}
                JSONObject root=new JSONObject(raw);String out=root.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text").trim();out=out.replace("```json","").replace("```","").trim();applyAi(new JSONObject(out));
            }catch(Exception e){js("showToast('❌ پاسخ هوش مصنوعی قابل پردازش نبود');updateVoice('⚠️ خطای پاسخ Gemini');");}finally{if(c!=null)c.disconnect();}
        });
    }

    private void applyAi(JSONObject a){String in=a.optString("intent","CHAT"),d=a.optString("description","هزینه"),r=a.optString("reply","انجام شد.");long n=a.optLong("amount",0);if("EXPENSE".equals(in)&&n>0)js("addTransaction('expense',"+JSONObject.quote(d)+","+n+");showAIMessage("+JSONObject.quote(r)+");speak("+JSONObject.quote(r)+");updateVoice('✅ هزینه ثبت شد');");else if("INCOME".equals(in)&&n>0)js("addTransaction('income',"+JSONObject.quote(d)+","+n+");showAIMessage("+JSONObject.quote(r)+");speak("+JSONObject.quote(r)+");updateVoice('✅ درآمد ثبت شد');");else if("BALANCE".equals(in))js("quickBalance();");else if("REPORT".equals(in)){js("dailyReport();");answer(r);}else answer(r);}

    private void speakNative(String text){if(text==null||text.trim().isEmpty())return;if(!ttsReady||tts==null){pendingSpeech=text;return;}try{AudioManager am=(AudioManager)getSystemService(AUDIO_SERVICE);if(am!=null)am.requestAudioFocus(f->{},AudioManager.STREAM_MUSIC,AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);tts.stop();tts.speak(text,TextToSpeech.QUEUE_FLUSH,null,"pouya_live_voice");}catch(Exception ignored){}}
    private Long amount(String t){Matcher m=Pattern.compile("(\\d[\\d,]*)").matcher(t.replace("٬","").replace("تومان",""));long v=0;boolean f=false;while(m.find()){try{v=Long.parseLong(m.group(1).replace(",",""));f=true;}catch(Exception ignored){}}if(f){if(t.contains("میلیون"))v*=1000000L;else if(t.contains("هزار"))v*=1000L;return v;}String[] w={"یک","دو","سه","چهار","پنج","شش","هفت","هشت","نه","ده","بیست","سی","چهل","پنجاه","صد","دویست","سیصد","پانصد"};for(String x:w)if(t.contains(x)){long z=word(x);if(t.contains("میلیون"))z*=1000000L;else if(t.contains("هزار"))z*=1000L;return z;}return null;}
    private long word(String x){switch(x){case"یک":return 1;case"دو":return 2;case"سه":return 3;case"چهار":return 4;case"پنج":return 5;case"شش":return 6;case"هفت":return 7;case"هشت":return 8;case"نه":return 9;case"ده":return 10;case"بیست":return 20;case"سی":return 30;case"چهل":return 40;case"پنجاه":return 50;case"صد":return 100;case"دویست":return 200;case"سیصد":return 300;case"پانصد":return 500;default:return 0;}}
    private String description(String t){return t.replaceAll("\\d[\\d,]*"," ").replace("تومان","").replace("هزار","").replace("میلیون","").replace("خرج کردم","").replace("هزینه کردم","").replace("پرداخت کردم","").replace("خرید کردم","").replace("خریدم","").trim();}
    private String normalize(String t){return t.replace('ي','ی').replace('ى','ی').replace('ك','ک').replace('‌',' ').replaceAll("\\s+"," ").trim();}
    private String readAll(InputStream in)throws Exception{if(in==null)return"";StringBuilder s=new StringBuilder();try(BufferedReader r=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8))){String l;while((l=r.readLine())!=null)s.append(l);}return s.toString();}

    private class VoiceBridge{
        @JavascriptInterface public void toggleVoice(){runOnUiThread(()->{if(android.os.Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},MIC_REQUEST);return;}if(speechRecognizer==null){js("showToast('❌ تشخیص صدا در دسترس نیست');");return;}liveVoice=!liveVoice;if(liveVoice){js("updateVoice('🟢 گفت‌وگوی زنده روشن است؛ صحبت کن...');showToast('🎙️ حالت گفت‌وگوی زنده فعال شد');");startListening();}else{speechRecognizer.cancel();js("updateVoice('⏹️ گفت‌وگوی زنده خاموش شد');showToast('گفت‌وگوی زنده متوقف شد');");}});}
        @JavascriptInterface public void speak(String text){runOnUiThread(()->speakNative(text));}
        @JavascriptInterface public void requestGeminiKey(){runOnUiThread(()->{final android.widget.EditText in=new android.widget.EditText(MainActivity.this);in.setSingleLine(true);in.setHint("کلید Gemini API");new android.app.AlertDialog.Builder(MainActivity.this).setTitle("اتصال Gemini").setMessage("کلید روی همین گوشی ذخیره می‌شود.").setView(in).setPositiveButton("ذخیره",(d,w)->{String k=in.getText().toString().trim();if(!k.isEmpty())getSharedPreferences(PREFS,MODE_PRIVATE).edit().putString(KEY_GEMINI,k).apply();}).setNegativeButton("لغو",null).show();});}
    }
    @Override public void onBackPressed(){if(webView.canGoBack())webView.goBack();else super.onBackPressed();}
    @Override protected void onDestroy(){liveVoice=false;if(speechRecognizer!=null){speechRecognizer.destroy();speechRecognizer=null;}if(tts!=null){tts.stop();tts.shutdown();tts=null;}network.shutdownNow();super.onDestroy();}
}
