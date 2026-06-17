package com.example.autofileimporter;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(40, 40, 40, 40);

        TextView title = new TextView(this);
        title.setText("自动文件导入器");
        title.setTextSize(28);
        title.setGravity(Gravity.CENTER);

        TextView desc = new TextView(this);
        desc.setText("APK 已成功生成。后续可升级为完整文件导入功能。");
        desc.setTextSize(16);
        desc.setGravity(Gravity.CENTER);
        desc.setPadding(0, 30, 0, 0);

        root.addView(title);
        root.addView(desc);

        setContentView(root);
    }
}
