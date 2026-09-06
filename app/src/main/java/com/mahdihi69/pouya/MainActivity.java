package com.mahdihi69.pouya;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
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

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {
    private WebView webView;
    private SpeechRecognizer speechRecognizer;
    private TextToSpeech textToSpeech;
    private static final int MIC_REQUEST = 10;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
                    textToSpeech.setLanguage(new Locale("fa"));
                }
                textToSpeech.setSpeechRate(0.9f);
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
                        js("updateVoice(" + JSONObject.quote("🗣️ " + text) + "); processVoice(" + JSONObject.quote(text) + ");");
                    }
                }
                @Override public void onPartialResults(Bundle partialResults) {}
                @Override public void onEvent(int eventType, Bundle params) {}
            });
        }

        if (android.os.Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, MIC_REQUEST);

        webView.loadUrl("https://mahdihi69-art.github.io/my-ai-studio/");
    }

    private void installNativeVoiceBridge() {
        webView.evaluateJavascript("(function(){" +
                "window.speak=function(text){if(window.AndroidVoice){AndroidVoice.speak(String(text));}else if('speechSynthesis' in window){speechSynthesis.cancel();var u=new SpeechSynthesisUtterance(text);u.lang='fa-IR';u.rate=.9;speechSynthesis.speak(u);}};" +
                "window.toggleVoice=function(){if(window.AndroidVoice){AndroidVoice.toggleVoice();}else{showToast('🎤 تشخیص صدا در دسترس نیست');}};" +
                "})();", null);
    }

    private void js(String script) {
        runOnUiThread(() -> webView.evaluateJavascript("try{" + script + "}catch(e){}", null));
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
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR");
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "fa-IR");
                intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
                speechRecognizer.startListening(intent);
            });
        }

        @JavascriptInterface public void speak(String text) {
            runOnUiThread(() -> {
                if (textToSpeech != null) {
                    textToSpeech.stop();
                    textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "pouya");
                }
            });
        }
    }

    @Override public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack(); else super.onBackPressed();
    }

    @Override protected void onDestroy() {
        if (speechRecognizer != null) { speechRecognizer.destroy(); speechRecognizer = null; }
        if (textToSpeech != null) { textToSpeech.stop(); textToSpeech.shutdown(); textToSpeech = null; }
        super.onDestroy();
    }
}
