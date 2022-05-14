package com.example.projekt3;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.DownloadManager;
import android.app.Notification;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    Button dowloadData,dowloadFile;
    TextView url,fileSize,fileType,dowloadedBytesData;
    ProgressBar progressBar;


    private static final int UPDATE_DOWNLOAD_PROGRESS = 1;
    private  ExecutorService executor;
    private Handler mHandler;


    private NotificationManagerCompat notificationManagerCompat;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        url = findViewById(R.id.url);
        fileSize = findViewById(R.id.fileSize);
        fileType = findViewById(R.id.fileType);
        dowloadData = findViewById(R.id.downloadDataButton);
        dowloadFile= findViewById(R.id.downloadFileButton);
        progressBar = findViewById(R.id.progressBar);
        dowloadedBytesData=findViewById(R.id.bytesDownloaded);
        dowloadData.setOnClickListener(view -> {
            if(!url.getText().toString().isEmpty() || URLUtil.isValidUrl(url.getText().toString()))
            {
                AsynchronicExec zadanie = new AsynchronicExec(getApplicationContext(),fileSize,fileType);
                zadanie.execute(url.getText().toString());
            }
        });

        dowloadFile.setOnClickListener(view -> {
            if(!url.getText().toString().isEmpty() || URLUtil.isValidUrl(url.getText().toString()))
            {
                if(ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                    requestStoragePermission();
                }
                notificationManagerCompat = NotificationManagerCompat.from(this);
                progressBar.setVisibility(View.VISIBLE);
                Download(url.getText().toString().trim());
                executor.shutdown();
                mHandler.removeCallbacksAndMessages(null);
            }
        });
    }

    public void Download(String adres){
        String url = adres;
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        String title = URLUtil.guessFileName(url,null,null);
        request.setTitle(("Pobieranie"));
        request.setDescription("Trwa pobieranie pliku");
        request.allowScanningByMediaScanner();
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,title);
        DownloadManager manager = (DownloadManager) getApplicationContext().getSystemService(Context.DOWNLOAD_SERVICE);
        long downloadId = manager.enqueue(request);
        executor = Executors.newFixedThreadPool(1);
        mHandler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(@NonNull Message msg) {
                if (msg.what == UPDATE_DOWNLOAD_PROGRESS) {
                    int downloadProgress = msg.arg1;
                    progressBar.setProgress(downloadProgress);
                }
                return true;
            }
        });
        executor.execute(new Runnable() {
            @Override
            public void run() {
                int progress = 0;
                long totalAmount = 0;
                boolean isDownloadFinished = false;
                while (!isDownloadFinished) {
                    Cursor cursor = manager.query(new DownloadManager.Query().setFilterById(downloadId));
                    if (cursor.moveToFirst()) {
                        @SuppressLint("Range") int downloadStatus = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                        switch (downloadStatus) {
                            case DownloadManager.STATUS_RUNNING:
                                @SuppressLint("Range") long totalBytes = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                                totalAmount = totalBytes;
                                if (totalBytes > 0) {
                                    @SuppressLint("Range") long downloadedBytes = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                                    progress = (int) (downloadedBytes * 100 / totalBytes);
                                    setText(dowloadedBytesData,String.valueOf(downloadedBytes));
                                }
                                break;
                            case DownloadManager.STATUS_SUCCESSFUL:
                                progress = 100;
                                setText(dowloadedBytesData,String.valueOf(totalAmount));
                                isDownloadFinished = true;
                                break;
                            case DownloadManager.STATUS_FAILED:
                                isDownloadFinished = true;
                                break;
                        }
                        Message message = Message.obtain();
                        message.what = UPDATE_DOWNLOAD_PROGRESS;
                        message.arg1 = progress;
                        mHandler.sendMessage(message);
                        cursor.close();
                    }
                }
            }
        });
    }

    private void setText(final TextView text,final String value){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                text.setText(value);
            }
        });
    }

    private void requestStoragePermission(){
        if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE))
        {
            new AlertDialog.Builder(this).setTitle("Udziel zgody").setMessage("Udziel zgody na użycie pamięci wewnętrznej").setPositiveButton("Zgoda", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    ActivityCompat.requestPermissions(MainActivity.this,new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
                }
            }).setNegativeButton("Nie udzielam zgody", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            }).create().show();
        }else{
            ActivityCompat.requestPermissions(this,new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
        }
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this, "Udzielono dostępu", Toast.LENGTH_LONG).show();
            }else{
                Toast.makeText(this, "Zabroniono dostępu", Toast.LENGTH_LONG).show();
            }
        }
    }


}