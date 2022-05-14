package com.example.projekt3;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Pair;
import android.widget.TextView;

import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class AsynchronicExec extends AsyncTask<String, Integer,String[]> {
    TextView fileSize,fileType;
    @Override
    protected String[] doInBackground(String... params) {
        String adres = params[0];
        HttpsURLConnection polaczenie = null;
        long mRozmiar = 0;
        String mTyp= "";
        try {
            URL url = new URL(adres);
            polaczenie = (HttpsURLConnection) url.openConnection();
            polaczenie.setRequestMethod("GET");
            polaczenie.setRequestProperty("Accept-Encoding", "identity");
            mRozmiar = polaczenie.getContentLength();
            mTyp = polaczenie.getContentType();
        } catch (Exception e) {
            e.printStackTrace();
        }
        String tab[]= {String.valueOf(mRozmiar),mTyp};
        return tab;
    }

    @Override
    protected void onPostExecute(String[] strings){
        fileSize.setText(strings[0]);
        fileType.setText(strings[1]);
    }

    public AsynchronicExec (Context parsedContext, TextView fileSize, TextView fileType){
        Context context = parsedContext;
        this.fileType = fileType;
        this.fileSize = fileSize;
    }
}
