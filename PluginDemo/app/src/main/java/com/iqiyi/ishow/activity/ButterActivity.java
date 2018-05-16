package com.iqiyi.ishow.activity;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.iqiyi.ishow.R;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * author: liuchun
 * date: 2018/5/14
 */
public class ButterActivity extends Activity {

    @BindView(R.id.butter_title) TextView mTitle;
    @BindView(R.id.butter_display) Button mDisplay;
    @BindView(R.id.butter_share) Button mShare;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_butter);
        ButterKnife.bind(this);

        mTitle.setText("I am create from butter knife");
        mShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(ButterActivity.this, "Share Button is Clicked", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
