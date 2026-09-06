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

public class MainActivity extends Activity {
    private WebView webView;
    private SpeechRecognizer speechRecognizer;
    private TextToSpeech textToSpeech;
    private boolean ttsReady = false;
    private String pendingSpeech = null;
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
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);

        webView.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                installNativeVoiceBridge();
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public void onPermissionRequest(final PermissionRequest request) {
                runOnUiThread(() -> request.grant(request.getResources()));
            }
        });
        webView.addJavascriptInterface(new VoiceBridge(), "AndroidVoice");

        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(new Locale("fa", "IR"));
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    result = textToSpeech.setLanguage(new Locale("fa"));
                }
                textToSpeech.setSpeechRate(0.92f);
                textToSpeech.setPitch(1.0f);
                ttsReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED;
                if (ttsReady && pendingSpeech != null) {
                    String text = pendingSpeech;
                    pendingSpeech = null;
                    speakNative(text);
                }
            }
        });

        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override public void onReadyForSpeech(Bundle params) { js("updateVoice('👂 پویا گوش می‌دهد...');"); }
                @Override public void onBeginningOfSpeech() { js("updateVoice('🗣️ در حال شنیدن...');"); }
                @Override public void onRmsChanged(float rmsdB) {}
                @Override public void onBufferReceived(byte[] buffer) {}
                @Override public void onEndOfSpeech() { js("updateVoice('⏳ در حال پردازش...');"); }
                @Override public void onError(int error) {
                    String msg = "⚠️ خطا در تشخیص صدا";
                    if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) msg = "🎤 صدایی دریافت نشد";
                    if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) msg = "🎤 اجازه میکروفون را فعال کنید";
                    js("updateVoice(" + JSONObject.quote(msg) + ");");
                }
                @Override public void onResults(Bundle results) {
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        String text = matches.get(0).trim();
                        js("updateVoice(" + JSONObject.quote("🗣️ " + text) + ");");
                        handleVoiceWithGemini(text);
                    }
                }
                @Override public void onPartialResults(Bundle partialResults) {}
                @Override public void onEvent(int eventType, Bundle params) {}
            });
        }

        if (android.os.Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, MIC_REQUEST);
        }
        webView.loadUrl("https://mahdihi69-art.github.io/my-ai-studio/");
    }

    private void installNativeVoiceBridge() {
        webView.evaluateJavascript("(function(){" +
                "window.speak=function(text){if(window.AndroidVoice){AndroidVoice.speak(String(text));}" +
                "else if('speechSynthesis' in window){speechSynthesis.cancel();var u=new SpeechSynthesisUtterance(String(text));u.lang='fa-IR';u.rate=.9;u.onend=function(){};speechSynthesis.speak(u);}};" +
                "window.toggleVoice=function(){if(window.AndroidVoice){AndroidVoice.toggleVoice();}else{showToast('🎤 تشخیص صدا در دسترس نیست');}};" +
                "})();", null);
    }

    private void js(String script) {
        runOnUiThread(() -> webView.evaluateJavascript("try{" + script + "}catch(e){}", null));
    }

    private String getGeminiKey() {
        String saved = getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_GEMINI, "").trim();
        if (!saved.isEmpty()) return saved;
        return BuildConfig.GEMINI_API_KEY == null ? "" : BuildConfig.GEMINI_API_KEY.trim();
    }

    private void handleVoiceWithGemini(String text) {
        String key = getGeminiKey();
        if (key.isEmpty()) {
            js("showToast('🔑 کلید Gemini تنظیم نشده است'); AndroidVoice.requestGeminiKey();");
            return;
        }
        js("updateVoice('🧠 در حال فهم فرمان شما...');");
        network.execute(() -> {
            try {
                String prompt = "تو مغز صوتی اپلیکیشن مدیریت مالی فارسی به نام پویا هستی. فرمان کاربر را دقیق تحلیل کن و فقط JSON معتبر برگردان، بدون markdown. schema: {intent:'EXPENSE|INCOME|BALANCE|REPORT|CHAT', description:string, amount:number, note:string, reply:string}. مبلغ فقط عدد صحیح تومان باشد. اگر مبلغ مشخص نیست amount=0. اگر کاربر فقط سوال عمومی پرسید intent=CHAT و پاسخ کوتاه و فارسی در reply بده. اگر کاربر درباره موجودی/گزارش مالی پرسید intent مناسب را بده. فرمان کاربر: " + text;
                JSONObject body = new JSONObject();
                JSONArray contents = new JSONArray();
                JSONObject content = new JSONObject();
                JSONArray parts = new JSONArray();
                parts.put(new JSONObject().put("text", prompt));
                content.put("parts", parts);
                contents.put(content);
                body.put("contents", contents);

                URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(30000);
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setRequestProperty("x-goog-api-key", key);
                byte[] data = body.toString().getBytes(StandardCharsets.UTF_8);
                try (OutputStream os = conn.getOutputStream()) { os.write(data); }
                int code = conn.getResponseCode();
                InputStream stream = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
                String response = readAll(stream);
                conn.disconnect();
                if (code < 200 || code >= 300) {
                    js("showToast(" + JSONObject.quote("❌ خطای Gemini: " + code) + "); updateVoice('⚠️ Gemini پاسخ نداد');");
                    return;
                }
                JSONObject root = new JSONObject(response);
                String raw = root.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text").trim();
                raw = raw.replace("```json", "").replace("```", "").trim();
                JSONObject ai = new JSONObject(raw);
                applyGeminiIntent(ai);
            } catch (Exception e) {
                js("showToast('❌ اتصال یا پردازش Gemini ناموفق بود'); updateVoice('⚠️ خطای هوش مصنوعی');");
            }
        });
    }

    private void applyGeminiIntent(JSONObject ai) {
        try {
            String intent = ai.optString("intent", "CHAT");
            String desc = ai.optString("description", "هزینه").trim();
            long amount = ai.optLong("amount", 0);
            String note = ai.optString("note", "").trim();
            String reply = ai.optString("reply", "انجام شد.").trim();
            if ("EXPENSE".equals(intent) && amount > 0) {
                js("addTransaction('expense'," + JSONObject.quote(desc.isEmpty()?"هزینه":desc) + "," + amount + "," + JSONObject.quote(note) + "); showAIMessage(" + JSONObject.quote("💸 " + reply) + "); speak(" + JSONObject.quote(reply) + "); updateVoice('✅ هزینه ثبت شد');");
            } else if ("INCOME".equals(intent) && amount > 0) {
                js("addTransaction('income'," + JSONObject.quote(desc.isEmpty()?"درآمد":desc) + "," + amount + "," + JSONObject.quote(note) + "); showAIMessage(" + JSONObject.quote("💰 " + reply) + "); speak(" + JSONObject.quote(reply) + "); updateVoice('✅ درآمد ثبت شد');");
            } else if ("BALANCE".equals(intent)) {
                js("(function(){var t=getTotals(); var r='موجودی شما ' + t.balance.toLocaleString('fa-IR') + ' تومان است.'; showAIMessage('💳 '+r); speak(r); updateVoice('✅ پاسخ آماده شد');})();");
            } else if ("REPORT".equals(intent)) {
                js("dailyReport(); speak('گزارش امروز آماده شد'); updateVoice('📊 گزارش آماده شد');");
            } else {
                js("showAIMessage(" + JSONObject.quote(reply) + "); speak(" + JSONObject.quote(reply) + "); updateVoice('✅ پاسخ آماده شد');");
            }
        } catch (Exception e) {
            js("showToast('⚠️ پاسخ Gemini قابل پردازش نبود');");
        }
    }

    private void speakNative(String text) {
        if (text == null || text.trim().isEmpty()) return;
        if (!ttsReady || textToSpeech == null) {
            pendingSpeech = text;
            return;
        }
        try {
            AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
            if (am != null) am.requestAudioFocus(focus -> {}, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
            textToSpeech.stop();
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "pouya_voice");
        } catch (Exception ignored) {}
    }

    private String readAll(InputStream input) throws Exception {
        if (input == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }

    private class VoiceBridge {
        @JavascriptInterface public void toggleVoice() {
            runOnUiThread(() -> {
                if (android.os.Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, MIC_REQUEST);
                    return;
                }
                if (speechRecognizer == null) {
                    js("showToast('❌ موتور تشخیص گفتار گوشی در دسترس نیست.');");
                    return;
                }
                try {
                    speechRecognizer.cancel();
                    Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR");
                    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "fa-IR");
                    intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
                    intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
                    speechRecognizer.startListening(intent);
                } catch (Exception e) {
                    js("showToast('⚠️ شروع تشخیص صدا ناموفق بود');");
                }
            });
        }

        @JavascriptInterface public void speak(String text) {
            runOnUiThread(() -> speakNative(text));
        }

        @JavascriptInterface public void requestGeminiKey() {
            runOnUiThread(() -> {
                final android.widget.EditText input = new android.widget.EditText(MainActivity.this);
                input.setSingleLine(true);
                input.setHint("کلید Gemini API");
                new android.app.AlertDialog.Builder(MainActivity.this)
                        .setTitle("اتصال به Gemini")
                        .setMessage("کلید Gemini API را وارد کنید. کلید فقط روی همین گوشی ذخیره می‌شود.")
                        .setView(input)
                        .setPositiveButton("ذخیره", (d, w) -> {
                            String key = input.getText().toString().trim();
                            if (key.isEmpty()) { js("showToast('⚠️ کلید وارد نشد');"); return; }
                            getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(KEY_GEMINI, key).apply();
                            js("showToast('✅ Gemini آماده شد؛ دوباره روی میکروفون بزنید.');");
                        })
                        .setNegativeButton("لغو", null)
                        .show();
            });
        }
    }

    @Override public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack(); else super.onBackPressed();
    }

    @Override protected void onDestroy() {
        if (speechRecognizer != null) { speechRecognizer.destroy(); speechRecognizer = null; }
        if (textToSpeech != null) { textToSpeech.stop(); textToSpeech.shutdown(); textToSpeech = null; }
        network.shutdownNow();
        super.onDestroy();
    }
}
