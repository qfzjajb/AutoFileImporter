package com.example.autofileimporter;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.documentfile.provider.DocumentFile;

import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int REQUEST_SOURCE_DIR = 1001;
    private static final int REQUEST_TARGET_DIR = 1002;
    private static final String PREFS_NAME = "auto_file_importer";
    private static final String KEY_SOURCE_URI = "source_uri";
    private static final String KEY_TARGET_URI = "target_uri";

    private TextView sourceText;
    private TextView targetText;
    private TextView logText;
    private Button importButton;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        buildUi();
        refreshSelectedDirs();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(18));
        root.setBackgroundColor(0xFFF8FAFC);

        TextView title = new TextView(this);
        title.setText("自动文件导入器");
        title.setTextSize(24);
        title.setTextColor(0xFF0F172A);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        title.setPadding(0, dp(8), 0, dp(16));
        root.addView(title, matchWrap());

        sourceText = label("源目录：未选择");
        targetText = label("目标目录：未选择");
        root.addView(sourceText, matchWrap());
        root.addView(targetText, matchWrap());

        Button chooseSourceButton = button("选择源文件夹");
        chooseSourceButton.setOnClickListener(v -> openTreePicker(REQUEST_SOURCE_DIR));
        root.addView(chooseSourceButton, matchWrap());

        Button chooseTargetButton = button("选择目标文件夹");
        chooseTargetButton.setOnClickListener(v -> openTreePicker(REQUEST_TARGET_DIR));
        root.addView(chooseTargetButton, matchWrap());

        importButton = button("开始导入文件");
        importButton.setOnClickListener(v -> startImport());
        root.addView(importButton, matchWrap());

        Button clearLogButton = button("清空日志");
        clearLogButton.setOnClickListener(v -> logText.setText(""));
        root.addView(clearLogButton, matchWrap());

        TextView logTitle = label("运行日志");
        logTitle.setTextSize(18);
        logTitle.setPadding(0, dp(16), 0, dp(8));
        root.addView(logTitle, matchWrap());

        logText = new TextView(this);
        logText.setTextColor(0xFF334155);
        logText.setTextSize(14);
        logText.setPadding(dp(12), dp(12), dp(12), dp(12));
        logText.setBackgroundColor(0xFFFFFFFF);

        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(logText);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1
        );
        scrollView.setLayoutParams(scrollParams);
        root.addView(scrollView);

        setContentView(root);
    }

    private void openTreePicker(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            return;
        }

        Uri uri = data.getData();
        int flags = data.getFlags()
                & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        getContentResolver().takePersistableUriPermission(uri, flags);

        if (requestCode == REQUEST_SOURCE_DIR) {
            prefs.edit().putString(KEY_SOURCE_URI, uri.toString()).apply();
            appendLog("已选择源目录");
        } else if (requestCode == REQUEST_TARGET_DIR) {
            prefs.edit().putString(KEY_TARGET_URI, uri.toString()).apply();
            appendLog("已选择目标目录");
        }
        refreshSelectedDirs();
    }

    private void refreshSelectedDirs() {
        String sourceUri = prefs.getString(KEY_SOURCE_URI, null);
        String targetUri = prefs.getString(KEY_TARGET_URI, null);
        sourceText.setText(sourceUri == null ? "源目录：未选择" : "源目录：已选择");
        targetText.setText(targetUri == null ? "目标目录：未选择" : "目标目录：已选择");
    }

    private void startImport() {
        String sourceUriValue = prefs.getString(KEY_SOURCE_URI, null);
        String targetUriValue = prefs.getString(KEY_TARGET_URI, null);

        if (sourceUriValue == null || targetUriValue == null) {
            Toast.makeText(this, "请先选择源文件夹和目标文件夹", Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentFile sourceDir = DocumentFile.fromTreeUri(this, Uri.parse(sourceUriValue));
        DocumentFile targetDir = DocumentFile.fromTreeUri(this, Uri.parse(targetUriValue));

        if (sourceDir == null || targetDir == null || !sourceDir.isDirectory() || !targetDir.isDirectory()) {
            Toast.makeText(this, "目录授权无效，请重新选择文件夹", Toast.LENGTH_LONG).show();
            return;
        }

        importButton.setEnabled(false);
        appendLog("开始导入文件");

        new Thread(() -> {
            ImportStats stats = new ImportStats();
            try {
                copyDirectory(sourceDir, targetDir, stats);
                runOnUiThread(() -> appendLog("导入完成：成功 " + stats.successCount
                        + " 个，跳过 " + stats.skipCount
                        + " 个，失败 " + stats.failCount + " 个"));
            } catch (Exception e) {
                runOnUiThread(() -> appendLog("导入异常：" + e.getMessage()));
            } finally {
                runOnUiThread(() -> importButton.setEnabled(true));
            }
        }).start();
    }

    private void copyDirectory(DocumentFile sourceDir, DocumentFile targetDir, ImportStats stats) {
        DocumentFile[] files = sourceDir.listFiles();
        for (DocumentFile source : files) {
            if (source == null || source.getName() == null) {
                continue;
            }

            if (source.isDirectory()) {
                DocumentFile childTarget = targetDir.findFile(source.getName());
                if (childTarget == null || !childTarget.isDirectory()) {
                    childTarget = targetDir.createDirectory(source.getName());
                }

                if (childTarget == null) {
                    stats.failCount++;
                    appendLogFromWorker("目录创建失败：" + source.getName());
                    continue;
                }
                copyDirectory(source, childTarget, stats);
            } else if (source.isFile()) {
                copyFile(source, targetDir, stats);
            }
        }
    }

    private void copyFile(DocumentFile sourceFile, DocumentFile targetDir, ImportStats stats) {
        String fileName = sourceFile.getName();
        if (fileName == null) {
            stats.failCount++;
            appendLogFromWorker("跳过未知文件名");
            return;
        }

        DocumentFile existing = targetDir.findFile(fileName);
        if (existing != null && existing.isFile()) {
            stats.skipCount++;
            appendLogFromWorker("已存在，跳过：" + fileName);
            return;
        }

        String mimeType = sourceFile.getType();
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }

        DocumentFile targetFile = targetDir.createFile(mimeType, fileName);
        if (targetFile == null) {
            stats.failCount++;
            appendLogFromWorker("创建失败：" + fileName);
            return;
        }

        try (InputStream input = getContentResolver().openInputStream(sourceFile.getUri());
             OutputStream output = getContentResolver().openOutputStream(targetFile.getUri())) {
            if (input == null || output == null) {
                stats.failCount++;
                appendLogFromWorker("打开文件失败：" + fileName);
                return;
            }

            byte[] buffer = new byte[64 * 1024];
            int length;
            while ((length = input.read(buffer)) != -1) {
                output.write(buffer, 0, length);
            }
            output.flush();
            stats.successCount++;
            appendLogFromWorker("导入成功：" + fileName);
        } catch (Exception e) {
            stats.failCount++;
            appendLogFromWorker("导入失败：" + fileName + "，原因：" + e.getMessage());
        }
    }

    private void appendLogFromWorker(String message) {
        runOnUiThread(() -> appendLog(message));
    }

    private void appendLog(String message) {
        String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        logText.append("[" + time + "] " + message + "\n");
    }

    private TextView label(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(15);
        view.setTextColor(0xFF1E293B);
        view.setPadding(0, dp(8), 0, dp(8));
        return view;
    }

    private Button button(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setPadding(0, dp(8), 0, dp(8));
        return button;
    }

    private LinearLayout.LayoutParams matchWrap() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, dp(4), 0, dp(4));
        return params;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private static class ImportStats {
        int successCount = 0;
        int skipCount = 0;
        int failCount = 0;
    }
}
