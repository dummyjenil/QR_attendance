package com.modifenil.qrattendance;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.room.Room;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.modifenil.permissionmanager.PermissionManager;
import com.modifenil.qrcodescanner.CodeScannerView;
import com.modifenil.qrcodescanner.CodeScanner;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class MainActivity extends AppCompatActivity {
    private final PermissionManager permission = new PermissionManager() {
        @Override
        public void ifCancelledAndCanRequest(Activity activity) {
            Toast.makeText(activity, "Please Enable Permission", Toast.LENGTH_SHORT).show();
            finish();
            super.ifCancelledAndCanRequest(activity);
        }
        @Override
        public void ifCancelledAndCannotRequest(Activity activity) {
            Toast.makeText(activity, "Please Enable Permission", Toast.LENGTH_SHORT).show();
            finish();
            super.ifCancelledAndCannotRequest(activity);
        }
    };
    private CodeScanner mCodeScanner;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        AppDatabase appDatabase = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "vector-store-db").allowMainThreadQueries().build();
        DataDao dataDao = appDatabase.dataDao();
        permission.checkAndRequestPermissions(this);
        if (permission.getStatus().get(0).denied.isEmpty()) {

            CodeScannerView scannerView = findViewById(R.id.scanner_view);
            FloatingActionButton fab = findViewById(R.id.fab);
            mCodeScanner = new CodeScanner(this, scannerView);
            mCodeScanner.startPreview();
            mCodeScanner.setDecodeCallback(result -> {
            try {
                String[] x = AESDecryption.decrypt(result.getText(), getString(R.string.SECRET_KEY)).split(":");
                String name = x[0];
                String mobile = x[1];
                dataDao.insert(new DataEntity(mobile, name, System.currentTimeMillis()));
                runOnUiThread(()->showDialog("Success", "Name: "+name));
            }
            catch (Exception e){
                runOnUiThread(()-> runOnUiThread(()->showDialog("Error", "Invalid QR Code")));
            }
            });
            scannerView.setOnClickListener(view -> {
                if (mCodeScanner.isPreviewActive()){
                    mCodeScanner.stopPreview();
                }
                else {
                    mCodeScanner.startPreview();
                }
            });
            fab.setOnClickListener(view -> {
                StringBuilder csv_file = new StringBuilder();
                csv_file.append("Name,Mobile,Date,Time,weekday\n");
                for (DataEntity data : dataDao.getAllData()) {
                    long timestamp = data.getTimestamp();
                    Instant instant;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        instant = Instant.ofEpochMilli(timestamp);
                        ZoneId zoneId = ZoneId.systemDefault(); // Use the default time zone

                        LocalDateTime dateTime = LocalDateTime.ofInstant(instant, zoneId);
                        String date = dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                        String time = dateTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                        String weekday = dateTime.format(DateTimeFormatter.ofPattern("EEEE"));

                        csv_file.append(data.getName()).append(",")
                                .append(data.getMobile()).append(",")
                                .append(date).append(",")
                                .append(time).append(",")
                                .append(weekday).append("\n");
                    }
                }
                shareCsv(csv_file);
            });
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permission.checkResult(requestCode,permissions, grantResults);
        if (permission.getStatus().get(0).denied.isEmpty()){
            Toast.makeText(this, "Please restart app", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        mCodeScanner.startPreview();
    }
    @Override
    protected void onPause() {
        mCodeScanner.releaseResources();
        super.onPause();
    }
    private void showDialog(String title, String message) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("Yes", (dialogInterface, i) -> {
                    mCodeScanner.startPreview();
                    dialogInterface.cancel();
                })
                .setNegativeButton("No", (dialogInterface, i) -> finish());
        alertDialog.show();
    }
    public void shareCsv(StringBuilder csvStringBuilder) {
        try {
            File cacheDir = getCacheDir();
            File tempFile = new File(cacheDir, "attendance.csv");
            FileWriter writer = new FileWriter(tempFile);
            writer.write(csvStringBuilder.toString());
            writer.close();
            Uri fileUri = FileProvider.getUriForFile(
                    this,
                    getApplicationContext().getPackageName() + ".provider",
                    tempFile
            );
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/csv");
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Share Attendance CSV"));
        } catch (IOException ignored) {}
    }

}
