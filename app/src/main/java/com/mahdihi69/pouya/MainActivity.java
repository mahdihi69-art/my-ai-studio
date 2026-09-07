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
    private SpeechRecognizer onlineSpeechRecognizer;
    private TextToSpeech textToSpeech;
    private boolean ttsReady = false;
    private boolean usingOnDeviceVoice = false;
    private String pendingSpeech;
    private static final int MIC_REQUEST = 10;
    private static final String PREFS = "pouya_settings";
    private static final String KEY_GEMINI = "gemini_api_key";
    private final ExecutorService network = Executors.newSingleThreadExecutor();

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        webView = new WebView(this);
        setContentView(webView);
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowContentAccess(true);
        s.setAllowFileAccess(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                installNativeVoiceBridge();
                installQuickActions();
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public void onPermissionRequest(final PermissionRequest request) {
                runOnUiThread(() -> request.grant(request.getResources()));
            }
        });
        webView.addJavascriptInterface(new VoiceBridge(), "AndroidVoice");
        initTts();
        initVoiceRecognizer();
        if (android.os.Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, MIC_REQUEST);
        }
        webView.loadUrl("https://mahdihi69-art.github.io/my-ai-studio/");
    }

    private void initTts() {
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(new Locale("fa", "IR"));
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) result = textToSpeech.setLanguage(new Locale("fa"));
                textToSpeech.setSpeechRate(0.92f);
                ttsReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED;
                if (ttsReady && pendingSpeech != null) { String t = pendingSpeech; pendingSpeech = null; speakNative(t); }
            }
        });
    }

    private void initVoiceRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) return;
        try {
            if (android.os.Build.VERSION.SDK_INT >= 31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(this)) {
                speechRecognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(this);
                usingOnDeviceVoice = true;
            } else {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
                usingOnDeviceVoice = false;
            }
            setRecognitionListener(speechRecognizer);
        } catch (Exception e) {
            usingOnDeviceVoice = false;
            try { speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this); setRecognitionListener(speechRecognizer); } catch (Exception ignored) {}
        }
    }

    private void setRecognitionListener(SpeechRecognizer recognizer) {
        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) { js("updateVoice(" + JSONObject.quote(usingOnDeviceVoice ? "🔒 آفلاین: پویا گوش می‌دهد..." : "👂 پویا گوش می‌دهد...") + ");"); }
            @Override public void onBeginningOfSpeech() { js("updateVoice('🗣️ در حال شنیدن...');"); }
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() { js("updateVoice('⏳ در حال پردازش...');"); }
            @Override public void onError(int error) {
                if (usingOnDeviceVoice && shouldFallbackToOnline(error)) {
                    switchToOnlineRecognizer();
                    js("showToast('🌐 تشخیص آفلاین فارسی روی این گوشی در دسترس نیست؛ حالت آنلاین فعال شد.');");
                    return;
                }
                String msg = "⚠️ خطا در تشخیص صدا";
                if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) msg = "🎤 صدایی دریافت نشد";
                if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) msg = "🎤 اجازه میکروفون را فعال کنید";
                js("updateVoice(" + JSONObject.quote(msg) + ");");
            }
            @Override public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches == null || matches.isEmpty()) return;
                String text = matches.get(0).trim();
                js("updateVoice(" + JSONObject.quote((usingOnDeviceVoice ? "🔒 آفلاین: " : "🗣️ ") + text) + ");");
                if (!handleLocalVoiceCommand(text)) handleVoiceWithGemini(text);
            }
            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });
    }

    private boolean shouldFallbackToOnline(int error) {
        return error == SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED || error == SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE || error == SpeechRecognizer.ERROR_SERVER_DISCONNECTED || error == SpeechRecognizer.ERROR_NETWORK || error == SpeechRecognizer.ERROR_NETWORK_TIMEOUT;
    }

    private void switchToOnlineRecognizer() {
        if (onlineSpeechRecognizer == null) {
            try { onlineSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this); setOnlineListener(onlineSpeechRecognizer); } catch (Exception ignored) {}
        }
        if (onlineSpeechRecognizer != null) { speechRecognizer = onlineSpeechRecognizer; usingOnDeviceVoice = false; }
    }

    private void setOnlineListener(SpeechRecognizer recognizer) {
        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle b) { js("updateVoice('🌐 آنلاین: پویا گوش می‌دهد...');"); }
            @Override public void onBeginningOfSpeech() { js("updateVoice('🗣️ در حال شنیدن...');"); }
            @Override public void onRmsChanged(float r) {}
            @Override public void onBufferReceived(byte[] b) {}
            @Override public void onEndOfSpeech() { js("updateVoice('⏳ در حال پردازش...');"); }
            @Override public void onError(int e) { js("updateVoice('⚠️ تشخیص صدا ناموفق بود');"); }
            @Override public void onResults(Bundle r) {
                ArrayList<String> m = r.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (m != null && !m.isEmpty()) { String t = m.get(0).trim(); js("updateVoice(" + JSONObject.quote("🗣️ " + t) + ");"); if (!handleLocalVoiceCommand(t)) handleVoiceWithGemini(t); }
            }
            @Override public void onPartialResults(Bundle b) {}
            @Override public void onEvent(int e, Bundle b) {}
        });
    }

    private void installNativeVoiceBridge() {
        webView.evaluateJavascript("(function(){window.speak=function(text){if(window.AndroidVoice){AndroidVoice.speak(String(text));}else if('speechSynthesis' in window){speechSynthesis.cancel();var u=new SpeechSynthesisUtterance(String(text));u.lang='fa-IR';u.rate=.9;speechSynthesis.speak(u);}};window.toggleVoice=function(){if(window.AndroidVoice){AndroidVoice.toggleVoice();}else{showToast('🎤 تشخیص صدا در دسترس نیست');}};})();", null);
    }

    private void installQuickActions() {
        String js = "(function(){if(document.getElementById('pouyaNativeTools'))return;var s=document.createElement('style');s.id='pouyaNativeToolsStyle';s.innerHTML='#pouyaNativeTools{display:grid;grid-template-columns:repeat(2,1fr);gap:9px;margin:0 0 16px}.pqt{padding:12px;border-radius:15px;border:0;background:#fff;box-shadow:0 4px 16px rgba(0,0,0,.07);font-weight:700;font-size:13px}.pqt:active{transform:scale(.97)}';document.head.appendChild(s);var box=document.createElement('div');box.id='pouyaNativeTools';box.innerHTML='<button class=pqt onclick=quickExpense()>💸 هزینه سریع</button><button class=pqt onclick=quickIncome()>💰 درآمد سریع</button><button class=pqt onclick=dailyReport()>📊 گزارش امروز</button><button class=pqt onclick=quickBalance()>💳 موجودی</button>';var c=document.querySelector('.container');if(c)c.insertBefore(box,c.children[2]);window.quickExpense=function(){document.getElementById('typeInput').value='expense';document.getElementById('descInput').focus();showAIMessage('💸 عنوان و مبلغ هزینه را وارد کن یا با میکروفون بگو.');};window.quickIncome=function(){document.getElementById('typeInput').value='income';document.getElementById('descInput').focus();showAIMessage('💰 درآمد را وارد کن یا با میکروفون بگو.');};window.quickBalance=function(){var t=getTotals();var r='موجودی شما '+t.balance.toLocaleString('fa-IR')+' تومان است.';showAIMessage('💳 '+r);speak(r);};})();";
        webView.evaluateJavascript(js, null);
    }

    private void js(String script) { runOnUiThread(() -> webView.evaluateJavascript("try{" + script + "}catch(e){}", null)); }

    private String getGeminiKey() {
        String saved = getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_GEMINI, "").trim();
        if (!saved.isEmpty()) return saved;
        return BuildConfig.GEMINI_API_KEY == null ? "" : BuildConfig.GEMINI_API_KEY.trim();
    }

    private boolean handleLocalVoiceCommand(String text) {
        String t = normalize(text);
        if (t.isEmpty()) return true;
        Long amount = extractAmount(t);
        if (t.contains("سلام") || t.contains("خوبی") || t.contains("چه خبر")) { reply("سلام! من پویا هستم. آماده‌ام به مدیریت دخل و خرجت کمک کنم."); return true; }
        if (t.contains("کمک") || t.contains("دستورات") || t.contains("چه کارهایی")) { reply("می‌توانی بگویی هزینه ثبت کن، درآمد ثبت کن، موجودی، گزارش امروز، آخرین خرید یا هزینه این ماه."); return true; }
        if (t.contains("موجودی") || t.contains("چقدر پول دارم") || t.contains("چقدر دارم") || t.contains("مانده")) { js("quickBalance();"); return true; }
        if (t.contains("گزارش") || t.contains("چقدر خرج") || t.contains("چقدر هزینه") || t.contains("امروز چقدر")) { js("dailyReport(); updateVoice('📊 گزارش آماده شد');"); return true; }
        if (t.contains("این ماه") && (t.contains("خرج") || t.contains("هزینه"))) { js("monthlyReport();"); return true; }
        if (t.contains("آخرین خرید") || t.contains("آخرین هزینه") || t.contains("آخرین تراکنش")) { js("(function(){if(!transactions.length){speak('هنوز تراکنشی ثبت نشده است.');return;}var x=transactions[0];speak('آخرین تراکنش '+x.desc+' به مبلغ '+x.amount.toLocaleString('fa-IR')+' تومان بود.');})();"); return true; }
        if (t.contains("حذف همه") || t.contains("پاک کردن همه") || t.contains("همه را پاک کن")) { js("clearAll();"); return true; }
        boolean income = t.contains("حقوق گرفتم") || t.contains("درآمد") || t.contains("پول گرفتم") || t.contains("واریز شد") || t.contains("دریافت کردم");
        boolean expense = t.contains("خرج کردم") || t.contains("خریدم") || t.contains("خرید کردم") || t.contains("هزینه کردم") || t.contains("پرداخت کردم") || t.contains("خرج شد");
        if ((income || expense) && amount != null && amount > 0) {
            String desc = income ? (t.contains("حقوق") ? "حقوق" : "درآمد") : extractDescription(t);
            String type = income ? "income" : "expense";
            js("addTransaction(" + JSONObject.quote(type) + "," + JSONObject.quote(desc) + "," + amount + ");showAIMessage(" + JSONObject.quote(income ? "💰 درآمد با موفقیت ثبت شد." : "💸 هزینه با موفقیت ثبت شد.") + ");speak(" + JSONObject.quote((income ? "درآمد " : "هزینه ") + amount + " تومان ثبت شد.") + ");updateVoice('✅ ثبت شد');");
            return true;
        }
        return false;
    }

    private void reply(String text) { js("showAIMessage(" + JSONObject.quote(text) + ");speak(" + JSONObject.quote(text) + ");updateVoice('✅ پاسخ آماده شد');"); }

    private String normalize(String text) {
        return text.replace('ي','ی').replace('ى','ی').replace('ك','ک').replace('ة','ه').replace('ۀ','ه').replace('‌',' ').replaceAll("\\s+", " ").trim();
    }

    private Long extractAmount(String text) {
        String n = text.replace('،', ',').replace("٬", "").replace("تومان", "").replace("ریال", "");
        Matcher m = Pattern.compile("(\\d[\\d,]*)").matcher(n);
        long value = 0; boolean found = false;
        while (m.find()) { String raw = m.group(1).replace(",", ""); try { value = Long.parseLong(raw); found = true; } catch (Exception ignored) {} }
        if (found) {
            if (n.contains("میلیون")) value *= 1000000L;
            else if (n.contains("هزار")) value *= 1000L;
            return value;
        }
        String[] units = {"صفر","یک","دو","سه","چهار","پنج","شش","هفت","هشت","نه","ده","بیست","سی","چهل","پنجاه","صد","دویست","سیصد","پانصد"};
        long base = 0;
        for (String u : units) if (n.contains(u)) { base = wordValue(u); break; }
        if (base > 0 && n.contains("میلیون")) return base * 1000000L;
        if (base > 0 && n.contains("هزار")) return base * 1000L;
        return base > 0 ? base : null;
    }

    private long wordValue(String w) { switch(w) { case "یک":return 1; case "دو":return 2; case "سه":return 3; case "چهار":return 4; case "پنج":return 5; case "شش":return 6; case "هفت":return 7; case "هشت":return 8; case "نه":return 9; case "ده":return 10; case "بیست":return 20; case "سی":return 30; case "چهل":return 40; case "پنجاه":return 50; case "صد":return 100; case "دویست":return 200; case "سیصد":return 300; case "پانصد":return 500; default:return 0; } }

    private String extractDescription(String text) {
        return text.replaceAll("\\d[\\d,]*", "").replace("هزار", "").replace("میلیون", "").replace("تومان", "").replace("ریال", "").replace("خرج کردم", "").replace("هزینه کردم", "").replace("پرداخت کردم", "").replace("خرید کردم", "").replace("خریدم", "").replace("خرج شد", "").replace("برای", "").replace("بابت", "").trim();
    }

    private void handleVoiceWithGemini(String text) {
        String key = getGeminiKey();
        if (key.isEmpty()) { js("showToast('🔑 کلید Gemini تنظیم نشده است');AndroidVoice.requestGeminiKey();"); return; }
        js("updateVoice('🧠 در حال فهم فرمان شما...');");
        network.execute(() -> {
            try {
                JSONObject body = new JSONObject();
                JSONArray contents = new JSONArray();
                JSONObject content = new JSONObject();
                JSONArray parts = new JSONArray();
                String prompt = "تو دستیار صوتی فارسی اپلیکیشن مدیر مالی پویا هستی. فرمان کاربر را تحلیل کن و فقط JSON معتبر بدون markdown برگردان. schema: {intent:'EXPENSE|INCOME|BALANCE|REPORT|CHAT',description:string,amount:number,note:string,reply:string}. amount عدد صحیح تومان باشد؛ اگر نامشخص است 0. فرمان: " + text;
                parts.put(new JSONObject().put("text", prompt));
                content.put("parts", parts); contents.put(content); body.put("contents", contents);
                String[] models = {"gemini-3.5-flash-lite", "gemini-3.5-flash"};
                String response = null; int lastCode = 0;
                for (String model : models) {
                    HttpURLConnection conn = null;
                    try {
                        URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent");
                        conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("POST"); conn.setConnectTimeout(12000); conn.setReadTimeout(30000); conn.setDoOutput(true);
                        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                        conn.setRequestProperty("x-goog-api-key", key);
                        byte[] data = body.toString().getBytes(StandardCharsets.UTF_8);
                        try (OutputStream os = conn.getOutputStream()) { os.write(data); }
                        lastCode = conn.getResponseCode();
                        InputStream stream = lastCode >= 200 && lastCode < 300 ? conn.getInputStream() : conn.getErrorStream();
                        String r = readAll(stream);
                        if (lastCode >= 200 && lastCode < 300) { response = r; break; }
                        if (lastCode != 400 && lastCode != 404 && lastCode != 429 && lastCode < 500) { response = r; break; }
                    } finally { if (conn != null) conn.disconnect(); }
                }
                if (response == null) { js("showToast(" + JSONObject.quote("❌ Gemini خطا داد: HTTP " + lastCode) + ");updateVoice('⚠️ Gemini در دسترس نیست');"); return; }
                JSONObject root = new JSONObject(response);
                String raw = root.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text").trim();
                raw = raw.replace("```json", "").replace("```", "").trim();
                applyGeminiIntent(new JSONObject(raw));
            } catch (Exception e) { js("showToast('❌ پاسخ Gemini قابل پردازش نبود');updateVoice('⚠️ خطای هوش مصنوعی');"); }
        });
    }

    private void applyGeminiIntent(JSONObject ai) {
        String intent = ai.optString("intent", "CHAT");
        String desc = ai.optString("description", "هزینه").trim();
        long amount = ai.optLong("amount", 0);
        String note = ai.optString("note", "").trim();
        String reply = ai.optString("reply", "انجام شد.").trim();
        if ("EXPENSE".equals(intent) && amount > 0) js("addTransaction('expense'," + JSONObject.quote(desc.isEmpty()?"هزینه":desc) + "," + amount + "," + JSONObject.quote(note) + ");showAIMessage(" + JSONObject.quote("💸 " + reply) + ");speak(" + JSONObject.quote(reply) + ");updateVoice('✅ هزینه ثبت شد');");
        else if ("INCOME".equals(intent) && amount > 0) js("addTransaction('income'," + JSONObject.quote(desc.isEmpty()?"درآمد":desc) + "," + amount + "," + JSONObject.quote(note) + ");showAIMessage(" + JSONObject.quote("💰 " + reply) + ");speak(" + JSONObject.quote(reply) + ");updateVoice('✅ درآمد ثبت شد');");
        else if ("BALANCE".equals(intent)) js("quickBalance();");
        else if ("REPORT".equals(intent)) js("dailyReport();speak('گزارش امروز آماده شد');");
        else reply(reply);
    }

    private void speakNative(String text) {
        if (text == null || text.trim().isEmpty()) return;
        if (!ttsReady || textToSpeech == null) { pendingSpeech = text; return; }
        try { AudioManager am = (AudioManager)getSystemService(AUDIO_SERVICE); if (am != null) am.requestAudioFocus(focus -> {}, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK); textToSpeech.stop(); textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "pouya_voice"); } catch (Exception ignored) {}
    }

    private String readAll(InputStream input) throws Exception {
        if (input == null) return ""; StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) { String line; while ((line = br.readLine()) != null) sb.append(line); }
        return sb.toString();
    }

    private class VoiceBridge {
        @JavascriptInterface public void toggleVoice() {
            runOnUiThread(() -> {
                if (android.os.Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) { requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, MIC_REQUEST); return; }
                if (speechRecognizer == null) { js("showToast('❌ موتور تشخیص گفتار در دسترس نیست.');"); return; }
                try {
                    speechRecognizer.cancel();
                    Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR");
                    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "fa-IR");
                    intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
                    intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
                    speechRecognizer.startListening(intent);
                } catch (Exception e) { js("showToast('⚠️ شروع تشخیص صدا ناموفق بود');"); }
            });
        }
        @JavascriptInterface public void speak(String text) { runOnUiThread(() -> speakNative(text)); }
        @JavascriptInterface public void requestGeminiKey() {
            runOnUiThread(() -> {
                final android.widget.EditText input = new android.widget.EditText(MainActivity.this); input.setSingleLine(true); input.setHint("کلید Gemini API");
                new android.app.AlertDialog.Builder(MainActivity.this).setTitle("اتصال به Gemini").setMessage("کلید Gemini API فقط روی همین گوشی ذخیره می‌شود.").setView(input).setPositiveButton("ذخیره", (d,w) -> { String key=input.getText().toString().trim(); if(key.isEmpty()){js("showToast('⚠️ کلید وارد نشد');");return;} getSharedPreferences(PREFS,MODE_PRIVATE).edit().putString(KEY_GEMINI,key).apply(); js("showToast('✅ Gemini آماده شد؛ دوباره فرمان را بگو.');"); }).setNegativeButton("لغو",null).show();
            });
        }
    }

    @Override public void onBackPressed() { if (webView.canGoBack()) webView.goBack(); else super.onBackPressed(); }
    @Override protected void onDestroy() { if(speechRecognizer!=null){speechRecognizer.destroy();speechRecognizer=null;} if(onlineSpeechRecognizer!=null && onlineSpeechRecognizer!=speechRecognizer){onlineSpeechRecognizer.destroy();onlineSpeechRecognizer=null;} if(textToSpeech!=null){textToSpeech.stop();textToSpeech.shutdown();textToSpeech=null;} network.shutdownNow(); super.onDestroy(); }
}
