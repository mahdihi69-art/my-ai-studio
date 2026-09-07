from pathlib import Path
import re

p = Path('app/src/main/java/com/mahdihi69/pouya/MainActivity.java')
s = p.read_text(encoding='utf-8')

s = s.replace('import android.speech.RecognitionListener;\n', 'import android.speech.RecognitionListener;\n')
marker = '    private boolean speaking = false;\n'
if 'private OfflinePersianVoice offlineVoice;' not in s:
    s = s.replace(marker, marker + '    private OfflinePersianVoice offlineVoice;\n    private boolean offlineVoiceActive = false;\n')

old = '        initTts();\n        initRecognizer();\n'
new = '''        initTts();\n        initRecognizer();\n        offlineVoice = new OfflinePersianVoice(this, new OfflinePersianVoice.Callback() {\n            @Override public void status(String text) { js("updateVoice(" + JSONObject.quote(text) + ");"); }\n            @Override public void result(String text) { processVoice(text); }\n            @Override public void error(String text) { offlineVoiceActive = false; js("updateVoice(" + JSONObject.quote("⚠️ " + text) + ");showToast(" + JSONObject.quote(text) + ");"); }\n        });\n'''
if 'offlineVoice = new OfflinePersianVoice' not in s:
    if old not in s: raise SystemExit('init marker not found')
    s = s.replace(old, new, 1)

# Prefer the bundled/local Vosk engine. It downloads the Persian model only once, then runs without network.
s = s.replace('            i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true);\n', '            i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true);\n            i.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE,true);\n')

old_toggle = '''        @JavascriptInterface public void toggleVoice(){runOnUiThread(()->{if(android.os.Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},MIC_REQUEST);return;}if(speechRecognizer==null){js("showToast('❌ تشخیص صدا در دسترس نیست');");return;}liveVoice=!liveVoice;if(liveVoice){js("updateVoice('🟢 گفت‌وگوی زنده روشن است؛ صحبت کن...');showToast('🎙️ حالت گفت‌وگوی زنده فعال شد');");startListening();}else{speechRecognizer.cancel();js("updateVoice('⏹️ گفت‌وگوی زنده خاموش شد');showToast('گفت‌وگوی زنده متوقف شد');");}});}'''
new_toggle = '''        @JavascriptInterface public void toggleVoice(){runOnUiThread(()->{\n            if(android.os.Build.VERSION.SDK_INT>=23&&checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},MIC_REQUEST);return;}\n            liveVoice=!liveVoice;\n            if(liveVoice){\n                offlineVoiceActive=true;\n                js("updateVoice('🔒 حالت صوتی داخلی؛ بدون Gemini و بدون اینترنت...');showToast('🎙️ فرمان صوتی داخلی فعال شد');");\n                if(offlineVoice!=null) offlineVoice.start();\n            }else{\n                offlineVoiceActive=false;\n                if(offlineVoice!=null) offlineVoice.stop();\n                if(speechRecognizer!=null) speechRecognizer.cancel();\n                js("updateVoice('⏹️ فرمان صوتی خاموش شد');showToast('فرمان صوتی متوقف شد');");\n            }\n        });}'''
if old_toggle not in s: raise SystemExit('toggle marker not found')
s = s.replace(old_toggle, new_toggle, 1)

old_destroy = '    @Override protected void onDestroy(){liveVoice=false;if(speechRecognizer!=null){speechRecognizer.destroy();speechRecognizer=null;}if(tts!=null){tts.stop();tts.shutdown();tts=null;}network.shutdownNow();super.onDestroy();}\n'
new_destroy = '    @Override protected void onDestroy(){liveVoice=false;offlineVoiceActive=false;if(offlineVoice!=null){offlineVoice.release();offlineVoice=null;}if(speechRecognizer!=null){speechRecognizer.destroy();speechRecognizer=null;}if(tts!=null){tts.stop();tts.shutdown();tts=null;}network.shutdownNow();super.onDestroy();}\n'
if old_destroy not in s: raise SystemExit('destroy marker not found')
s = s.replace(old_destroy, new_destroy, 1)

p.write_text(s, encoding='utf-8')
print('patched MainActivity.java')
