package com.netease.nim.uikit.business.session.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.netease.nim.uikit.R;
import com.netease.nim.uikit.business.session.constant.Extras;
import com.netease.nim.uikit.common.ToastHelper;
import com.netease.nim.uikit.common.activity.UI;

/**
 * 查看合并消息中的图片
 */
public class WatchMultiRetweetPictureActivity extends UI {

    /** 图片地址 */
    private String url;

    /** 展示图 */
    private ImageView detailsIV;

    public static void start(Activity activity, String url){
        Intent intent = new Intent(activity, WatchMultiRetweetPictureActivity.class);
        intent.putExtra(Extras.EXTRA_DATA, url);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_watch_multi_retweet_picture);
        getExtra();
        initViews();
    }

    private void getExtra(){
        Intent intent = getIntent();
        url = intent.getStringExtra(Extras.EXTRA_DATA);
    }

    private void initViews(){
        detailsIV = findViewById(R.id.img_details);
        if (TextUtils.isEmpty(url)){
            ToastHelper.showToast(this, "empty url");
            return;
        }
        Glide.with(this).load(url).into(detailsIV);
    }
}
