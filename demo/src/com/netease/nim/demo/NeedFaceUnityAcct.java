package com.netease.nim.demo;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.netease.nim.avchatkit.util.PreferenceUtil;
import com.netease.nim.demo.main.activity.WelcomeActivity;

public class NeedFaceUnityAcct extends AppCompatActivity {

    private boolean isOn = true;//是否使用FaceUnity

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_faceunity);

        Button button = (Button) findViewById(R.id.btn_set);
        String isOn = PreferenceUtil.getString(this, PreferenceUtil.KEY_FACEUNITY_IS_ON);
        if (TextUtils.isEmpty(isOn) || PreferenceUtil.VALUE_OFF.equals(isOn)) {
            this.isOn = false;
        } else {
            this.isOn = true;
        }
        button.setText(this.isOn ? "On" : "Off");

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NeedFaceUnityAcct.this.isOn = !NeedFaceUnityAcct.this.isOn;
                button.setText(NeedFaceUnityAcct.this.isOn ? "On" : "Off");
            }
        });

        Button btnToMain = (Button) findViewById(R.id.btn_to_main);
        btnToMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(NeedFaceUnityAcct.this, WelcomeActivity.class);
                PreferenceUtil.persistString(NeedFaceUnityAcct.this, PreferenceUtil.KEY_FACEUNITY_IS_ON,
                        NeedFaceUnityAcct.this.isOn ? PreferenceUtil.VALUE_ON : PreferenceUtil.VALUE_OFF);
                startActivity(intent);
                finish();
            }
        });

    }
}
