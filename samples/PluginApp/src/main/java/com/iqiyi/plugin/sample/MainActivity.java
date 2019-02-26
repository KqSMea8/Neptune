package com.iqiyi.plugin.sample;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.facebook.common.logging.FLog;
import com.facebook.drawee.backends.pipeline.Fresco;



import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "MainActivity";

    private static final String[] URL_LINKS = {
            "http://desk.fd.zol-img.com.cn/t_s1440x900c5/g5/M00/0F/09/ChMkJlauzbOIb6JqABF4o12gc_AAAH9HgF1sh0AEXi7441.jpg",
            "http://desk.fd.zol-img.com.cn/t_s1440x900c5/g5/M00/08/0A/ChMkJli9XIOIHZlxACrBWTH-3-kAAae8QCVIF4AKsFx521.jpg",
            "http://desk.fd.zol-img.com.cn/t_s1440x900c5/g5/M00/08/0A/ChMkJ1i9XJmIJnFtABXosJGWaOkAAae8QGrHE8AFejI057.jpg",
            "http://desk.fd.zol-img.com.cn/t_s1440x900c5/g5/M00/08/0A/ChMkJ1i9XIWIfPrHACa8wnLl-YYAAae8QDvfUkAJrza322.jpg",
            "http://desk.fd.zol-img.com.cn/t_s1440x900c5/g5/M00/08/0A/ChMkJli9XJuIGhrMADspbh_OzE4AAae8QHCouwAOymG127.jpg",
    };

    private RecyclerView mRecyclerView;
    private ImageAdapter mAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Fresco.initialize(this);
        FLog.setMinimumLoggingLevel(FLog.VERBOSE);

        setContentView(R.layout.activity_main);
        Log.i(TAG, "MainActivity onCreate() called");

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        List<String> mImageUrls = Arrays.asList(URL_LINKS);
        mAdapter = new ImageAdapter(this, mImageUrls);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(mAdapter);

        findViewById(R.id.go_support_design).setOnClickListener(this);
        findViewById(R.id.go_webview).setOnClickListener(this);

        TextView ticker = findViewById(R.id.time_ticker);
        ticker.setText(BuildConfig.BUILD_TIME);
    }


    private void getIdFromDataProvider() {
        ContentResolver cr = getContentResolver();
        Uri uri = Uri.parse("content://com.iqiyi.plugin.sample.dataprovider/id");
        Cursor cursor = cr.query(uri, null, null, null, null);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                String id = cursor.getString(cursor.getColumnIndex("deviceId"));
                Log.i(TAG, "getIdFromDataProvider: " + id);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.go_support_design) {
            Intent intent = new Intent(this, DesignActivity.class);
            startActivity(intent);
        } else if (v.getId() == R.id.go_webview) {
            Intent intent = new Intent(this, WebviewActivity.class);
            startActivity(intent);
        }
    }
}
