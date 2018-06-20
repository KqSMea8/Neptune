package com.iqiyi.ishow.activity;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
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
import com.iqiyi.ishow.R;



import java.util.Arrays;
import java.util.List;

/**
 * author: liuchun
 * date: 2018/1/26
 */
public class MainPageActivity extends AppCompatActivity implements View.OnClickListener{
    private static final String TAG = "ISHOW_PLUGIN";

    private static final String[] URL_LINKS = {
//            "https://78.media.tumblr.com/2acd0cd79d1c0fa42feee28e3decde40/tumblr_oxg2jk26g71shn04do1_500.gif",
//            "https://78.media.tumblr.com/f7c41c1435b0934f95670d9e3c511e06/tumblr_oxg2x8Fh7x1shn04do1_500.gif",
//            "https://78.media.tumblr.com/4cc1832f2793185d5f860842b76356c4/tumblr_oxg2uwK2nC1shn04do1_500.gif",
//            "https://78.media.tumblr.com/d970004bf2c225c21813098b3b58fcb1/tumblr_oxg2v2sEvL1shn04do1_500.gif",
//            "https://78.media.tumblr.com/eb96a9f1225011bf5b035f7bb96095e9/tumblr_oxg2wyIhtY1shn04do1_500.gif",
            "http://desk.fd.zol-img.com.cn/t_s1440x900c5/g5/M00/0F/09/ChMkJlauzbOIb6JqABF4o12gc_AAAH9HgF1sh0AEXi7441.jpg",
            "http://desk.fd.zol-img.com.cn/t_s1440x900c5/g5/M00/08/0A/ChMkJli9XIOIHZlxACrBWTH-3-kAAae8QCVIF4AKsFx521.jpg",
            "http://desk.fd.zol-img.com.cn/t_s1440x900c5/g5/M00/08/0A/ChMkJ1i9XJmIJnFtABXosJGWaOkAAae8QGrHE8AFejI057.jpg",
            "http://desk.fd.zol-img.com.cn/t_s1440x900c5/g5/M00/08/0A/ChMkJ1i9XIWIfPrHACa8wnLl-YYAAae8QDvfUkAJrza322.jpg",
            "http://desk.fd.zol-img.com.cn/t_s1440x900c5/g5/M00/08/0A/ChMkJli9XJuIGhrMADspbh_OzE4AAae8QHCouwAOymG127.jpg",
//            "http://pic6.qiyipic.com/common/20170324/43b5a409192a4890821042efa5aba1d7.gif"
    };

    private static final String[] ANIM_WEBP = {
            "https://www.gstatic.com/webp/animated/1.webp",
            "https://mathiasbynens.be/demo/animated-webp-supported.webp",
            "https://isparta.github.io/compare-webp/image/gif_webp/webp/2.webp",
            "http://m.qiyipic.com/common/lego/20171008/0b5d37c752f54f53b2a6c029ff29a68b.webp",
            "http://m.qiyipic.com/common/lego/20171015/d53c119b81104bdcb3f1b12f9a97bab1.webp",
            "http://m.qiyipic.com/common/lego/20171024/d0c539f81d724090bb8254703cf9d3c8.webp",
            "http://m.qiyipic.com/common/lego/20171024/abeaeff54ae141708e1e99f68d0a2869.webp",
            "http://m.qiyipic.com/common/lego/20171022/72c6badb2712454cadb8dd82df36aa02.webp",
            "http://m.qiyipic.com/common/lego/20171024/30359441b7dd4924b8d698d692fa210d.webp",
            "http://m.qiyipic.com/common/lego/20171024/98975d61e673411a96ffbdd37650c2d3.webp",
            "http://m.qiyipic.com/common/lego/20171020/d28a1202fb924af0ae4bfb2d50b66538.webp",
            "http://m.qiyipic.com/common/lego/20171024/fb48a596803f4f9f94d5511bdd009d67.webp",
            "http://m.qiyipic.com/common/lego/20171024/00100b87668141bfa5cecd9f599f8bb0.webp",
    };

    private static final String WEBP_LOSSLESS = "https://www.gstatic.com/webp/gallery3/1_webp_ll.webp";
    private static final String WEBP_LOSSLESS_WITH_ALPHA = "https://www.gstatic.com/webp/gallery3/1_webp_a.webp";

    private RecyclerView mRecyclerView;
    private ImageAdapter mAdapter;

    private TextView mGoButter;
    private TextView mGoSingle;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Fresco.initialize(this);
        FLog.setMinimumLoggingLevel(FLog.VERBOSE);

        setContentView(R.layout.home_page);

        Log.i(TAG, "ManiPageActivity onCreate() called");

        dumpContext();

        mGoButter = (TextView) findViewById(R.id.go_butter);
        mGoButter.setOnClickListener(this);

        mGoSingle = (TextView) findViewById(R.id.go_single);
        mGoSingle.setOnClickListener(this);

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        List<String> mImageUrls = Arrays.asList(URL_LINKS);
        mAdapter = new ImageAdapter(this, mImageUrls);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        //mRecyclerView.addItemDecoration(new ImageItemDecoration());
        mRecyclerView.setAdapter(mAdapter);
    }


    @Override
    protected void onResume() {
        super.onResume();


        //Log.i(TAG, "current login state: " + VariableCollection.isLogin());
    }

    @Override
    public void onClick(View v) {

        if (v.getId() == R.id.go_single) {
            Intent intent = new Intent(MainPageActivity.this, SingleActivity.class);
            startActivity(intent);
        }
    }

    private void dumpContext() {
        Context baseContext = getBaseContext();
        Log.i(TAG, "Activity baseContext: " + baseContext);

        Context appBase = getApplication().getBaseContext();
        Log.i(TAG, "Application baseContext: " + appBase);

        if (baseContext instanceof ContextWrapper) {
            Context superBase = ((ContextWrapper)baseContext).getBaseContext();
            Log.i(TAG, "super base: " + superBase);
        }
    }
}
