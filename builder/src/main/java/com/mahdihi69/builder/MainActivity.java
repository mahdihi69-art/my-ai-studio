package com.mahdihi69.builder;

import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
    LinearLayout root, form;
    EditText name, purpose, inputs, outputs, pages, style, colors, extras;
    Spinner type, ai, storage, internet;
    TextView status;

    int dp(float v){ return (int)(v*getResources().getDisplayMetrics().density+0.5f); }
    TextView label(String s){ TextView t=new TextView(this); t.setText(s); t.setTextSize(15); t.setTextColor(Color.DKGRAY); t.setPadding(0,dp(12),0,dp(5)); return t; }
    EditText field(String hint){ EditText e=new EditText(this); e.setHint(hint); e.setTextSize(15); e.setPadding(dp(12),dp(10),dp(12),dp(10)); e.setBackgroundResource(android.R.drawable.edit_text); return e; }
    Spinner spinner(String[] items){ Spinner s=new Spinner(this); ArrayAdapter<String> a=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,items); s.setAdapter(a); return s; }

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        build();
    }
    void build(){
        ScrollView scroll=new ScrollView(this);
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(18),dp(18),dp(18),dp(28)); root.setBackgroundColor(Color.rgb(247,248,252));
        TextView title=label("🧩 برنامه‌ساز هوشمند"); title.setTextSize(26); title.setTextColor(Color.rgb(35,45,70)); title.setGravity(Gravity.CENTER);
        root.addView(title,new LinearLayout.LayoutParams(-1,dp(55)));
        TextView sub=label("مشخصات برنامه را یک‌بار وارد کن؛ بقیه مسیر استاندارد و قابل تکرار می‌شود."); sub.setGravity(Gravity.CENTER); root.addView(sub);
        form=new LinearLayout(this); form.setOrientation(LinearLayout.VERTICAL); root.addView(form);

        form.addView(label("نام برنامه")); name=field("مثلاً داستان‌ساز"); form.addView(name);
        form.addView(label("نوع برنامه")); type=spinner(new String[]{"ابزار","آموزشی","مالی","داستان و سرگرمی","فروشگاهی","خدماتی","سفارشی"}); form.addView(type);
        form.addView(label("برنامه دقیقاً چه کاری انجام دهد؟")); purpose=field("شرح ساده و کامل کار برنامه"); purpose.setMinLines(3); form.addView(purpose);
        form.addView(label("ورودی‌های کاربر")); inputs=field("مثلاً موضوع، نام، مبلغ، عکس..."); form.addView(inputs);
        form.addView(label("خروجی برنامه")); outputs=field("مثلاً متن، صوت، گزارش، فایل..."); form.addView(outputs);
        form.addView(label("صفحه‌ها / بخش‌ها")); pages=field("خانه، تنظیمات، تاریخچه، درباره ما..."); form.addView(pages);
        form.addView(label("تم و ظاهر")); style=field("مدرن، ساده، کودکانه، حرفه‌ای..."); form.addView(style);
        form.addView(label("رنگ‌های پیشنهادی")); colors=field("مثلاً آبی و سفید"); form.addView(colors);
        form.addView(label("هوش مصنوعی")); ai=spinner(new String[]{"بدون هوش مصنوعی","Qwen","Gemini","OpenAI","سرویس سفارشی"}); form.addView(ai);
        form.addView(label("ذخیره اطلاعات")); storage=spinner(new String[]{"فقط روی گوشی","ابری","هر دو"}); form.addView(storage);
        form.addView(label("نیاز به اینترنت")); internet=spinner(new String[]{"خیر","بله","فقط برای هوش مصنوعی"}); form.addView(internet);
        form.addView(label("امکانات و توضیحات اضافی")); extras=field("هر چیزی که دوست داری اضافه شود"); extras.setMinLines(3); form.addView(extras);

        Button build=new Button(this); build.setText("🚀 ساخت مشخصات پروژه"); build.setTextSize(16); build.setOnClickListener(v->generate()); root.addView(build);
        status=label("هنوز پروژه‌ای ساخته نشده."); status.setTextSize(14); root.addView(status);
        scroll.addView(root); setContentView(scroll);
    }
    void generate(){
        try{
            JSONObject o=new JSONObject();
            o.put("appName",name.getText().toString().trim());
            o.put("type",type.getSelectedItem().toString());
            o.put("purpose",purpose.getText().toString().trim());
            o.put("inputs",inputs.getText().toString().trim());
            o.put("outputs",outputs.getText().toString().trim());
            o.put("pages",pages.getText().toString().trim());
            o.put("style",style.getText().toString().trim());
            o.put("colors",colors.getText().toString().trim());
            o.put("ai",ai.getSelectedItem().toString());
            o.put("storage",storage.getSelectedItem().toString());
            o.put("internet",internet.getSelectedItem().toString());
            o.put("extras",extras.getText().toString().trim());
            getSharedPreferences("builder",0).edit().putString("last_spec",o.toString()).apply();
            status.setText("✅ مشخصات پروژه ذخیره شد. نسخه بعدی همین مشخصات را مستقیم به قالب Android + GitHub Actions تبدیل می‌کند.");
        }catch(Exception e){ status.setText("خطا در ساخت مشخصات: "+e.getMessage()); }
    }
}