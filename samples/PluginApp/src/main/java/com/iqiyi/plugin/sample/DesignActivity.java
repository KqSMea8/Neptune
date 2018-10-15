package com.iqiyi.plugin.sample;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

public class DesignActivity extends AppCompatActivity {

    private AppBarLayout mAppBar;
    private CollapsingToolbarLayout mToolbarLayout;
    private Toolbar mToolbar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_design);

        mAppBar = findViewById(R.id.appbar_layout);
        mToolbarLayout = findViewById(R.id.collapsing_toolbar_layout);
        mToolbar = findViewById(R.id.tool_bar);
    }
}
