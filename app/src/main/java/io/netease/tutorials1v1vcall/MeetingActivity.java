package io.netease.tutorials1v1vcall;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.EGL14;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.faceunity.core.enumeration.CameraFacingEnum;
import com.faceunity.core.enumeration.FUAIProcessorEnum;
import com.faceunity.core.enumeration.FUInputTextureEnum;
import com.faceunity.core.enumeration.FUTransformMatrixEnum;
import com.faceunity.core.faceunity.FUAIKit;
import com.faceunity.core.faceunity.FURenderKit;
import com.faceunity.core.model.facebeauty.FaceBeautyBlurTypeEnum;
import com.faceunity.nama.FUConfig;
import com.faceunity.nama.FURenderer;
import com.faceunity.nama.data.FaceUnityDataFactory;
import com.faceunity.nama.listener.FURendererListener;
import com.faceunity.nama.ui.FaceUnityView;
import com.faceunity.nama.utils.FuDeviceUtils;
import com.netease.lava.api.IVideoRender;
import com.netease.lava.api.model.RTCVideoCropMode;
import com.netease.lava.nertc.sdk.NERtcCallback;
import com.netease.lava.nertc.sdk.NERtcConstants;
import com.netease.lava.nertc.sdk.NERtcEx;
import com.netease.lava.nertc.sdk.NERtcParameters;
import com.netease.lava.nertc.sdk.NERtcUserJoinExtraInfo;
import com.netease.lava.nertc.sdk.NERtcUserLeaveExtraInfo;
import com.netease.lava.nertc.sdk.video.NERtcRemoteVideoStreamType;
import com.netease.lava.nertc.sdk.video.NERtcVideoCallback;
import com.netease.lava.nertc.sdk.video.NERtcVideoConfig;
import com.netease.lava.nertc.sdk.video.NERtcVideoFrame;
import com.netease.lava.nertc.sdk.video.NERtcVideoView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import androidx.appcompat.app.AppCompatActivity;

import io.netease.tutorials1v1vcall.custom.CameraUtils;
import io.netease.tutorials1v1vcall.profile.CSVUtils;
import io.netease.tutorials1v1vcall.profile.Constant;

//  Created by NetEase on 7/31/20.
//  Copyright (c) 2014-2020 NetEase, Inc. All rights reserved.
//
public class MeetingActivity extends AppCompatActivity implements NERtcCallback, View.OnClickListener, SensorEventListener {

    private static final String TAG = "MeetingActivity";
    private static final String EXTRA_ROOM_ID = "extra_room_id";

    private boolean enableLocalVideo = true;
    private boolean enableLocalAudio = true;
    private boolean joinedChannel = false;

    private NERtcVideoView localUserVv;
    private NERtcVideoView remoteUserVv;
    private TextView waitHintTv;
    private ImageButton enableAudioIb;
    private ImageButton leaveIb;
    private ImageButton enableVideoIb;
    private ImageView cameraFlipImg;
    private TextView mTvFps;
    private TextView mTvTraceFace;
    private View localUserBgV;
    private FURenderer mFURenderer;
    private Handler mHandler;
    private boolean isFirstInit = true;
    private boolean isFUOn = false;
    private int mCameraFacing = Camera.CameraInfo.CAMERA_FACING_FRONT;
    private SensorManager mSensorManager;
    private int mSkipFrame = 5;
    private CSVUtils mCSVUtils;
    private FaceUnityDataFactory mFaceUnityDataFactory;

    public static void startActivity(Activity from, String roomId) {
        Intent intent = new Intent(from, MeetingActivity.class);
        intent.putExtra(EXTRA_ROOM_ID, roomId);
        from.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_meeting);

        String isOpen = PreferenceUtil.getString(this, PreferenceUtil.KEY_FACEUNITY_IS_ON);
        if (TextUtils.isEmpty(isOpen) || isOpen.equals("false")) {
            isFUOn = false;
        } else {
            isFUOn = true;
        }
        FaceUnityView faceUnityView = findViewById(R.id.fu_view);
        if (isFUOn) {
            FURenderer.getInstance().setup(this);
            mFURenderer = FURenderer.getInstance();

            mFURenderer.setMarkFPSEnable(true);
            mFURenderer.setInputTextureType(FUInputTextureEnum.FU_ADM_FLAG_EXTERNAL_OES_TEXTURE);
            mFURenderer.setCameraFacing(CameraFacingEnum.CAMERA_FRONT);
            mFURenderer.setInputOrientation(CameraUtils.getCameraOrientation(Camera.CameraInfo.CAMERA_FACING_FRONT));
            mFURenderer.setInputTextureMatrix(mCameraFacing == Camera.CameraInfo.CAMERA_FACING_FRONT ? FUTransformMatrixEnum.CCROT0_FLIPVERTICAL : FUTransformMatrixEnum.CCROT0);
            mFURenderer.setInputBufferMatrix(mCameraFacing == Camera.CameraInfo.CAMERA_FACING_FRONT ? FUTransformMatrixEnum.CCROT0_FLIPVERTICAL : FUTransformMatrixEnum.CCROT0);
            mFURenderer.setOutputMatrix(mCameraFacing == Camera.CameraInfo.CAMERA_FACING_FRONT ? FUTransformMatrixEnum.CCROT0 : FUTransformMatrixEnum.CCROT0_FLIPVERTICAL);
            mFURenderer.setCreateEGLContext(false);
            mFaceUnityDataFactory = new FaceUnityDataFactory(-1);
            faceUnityView.bindDataFactory(mFaceUnityDataFactory);

            mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            Sensor sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        }else {
            faceUnityView.setVisibility(View.INVISIBLE);
        }


        mTvFps = findViewById(R.id.tv_fps);
        mTvTraceFace = findViewById(R.id.tv_trace_face);
        initViews();
        setupNERtc();
        String roomId = getIntent().getStringExtra(EXTRA_ROOM_ID);
        long userId = generateRandomUserID();
        joinChannel(userId, roomId);
    }

    /**
     * 加入房间
     *
     * @param userID 用户ID
     * @param roomID 房间ID
     */
    private void joinChannel(long userID, String roomID) {
        Log.i(TAG, "joinChannel userId: " + userID);
        NERtcEx.getInstance().joinChannel(null, roomID, userID);
        localUserVv.setZOrderMediaOverlay(true);
        localUserVv.setScalingType(IVideoRender.ScalingType.SCALE_ASPECT_FILL);
        NERtcEx.getInstance().setupLocalVideoCanvas(localUserVv);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(this);
        }
        destroyFU();
        NERtcEx.getInstance().release();
    }

    @Override
    public void onBackPressed() {
        exit();
    }

    private void initViews() {
        localUserVv = findViewById(R.id.vv_local_user);
        remoteUserVv = findViewById(R.id.vv_remote_user);
        waitHintTv = findViewById(R.id.tv_wait_hint);
        enableAudioIb = findViewById(R.id.ib_audio);
        leaveIb = findViewById(R.id.ib_leave);
        enableVideoIb = findViewById(R.id.ib_video);
        cameraFlipImg = findViewById(R.id.img_camera_flip);
        localUserBgV = findViewById(R.id.v_local_user_bg);

        localUserVv.setVisibility(View.INVISIBLE);
        enableAudioIb.setOnClickListener(this);
        leaveIb.setOnClickListener(this);
        enableVideoIb.setOnClickListener(this);
        cameraFlipImg.setOnClickListener(this);
    }

    /**
     * 初始化SDK
     */
    private void setupNERtc() {
        NERtcParameters parameters = new NERtcParameters();
        parameters.set(NERtcParameters.KEY_AUTO_SUBSCRIBE_AUDIO, false);
        NERtcEx.getInstance().setParameters(parameters); //先设置参数，后初始化
        NERtcVideoConfig config = new NERtcVideoConfig();
        config.frameRate = NERtcVideoConfig.NERtcVideoFrameRate.FRAME_RATE_FPS_30;
        config.minFramerate = 25;
        config.videoProfile = NERtcConstants.VideoProfile.HD720P;
        config.videoCropMode = RTCVideoCropMode.kRTCVideoCropMode16x9;
        config.colorFormat = NERtcConstants.VideoColorFormat.TEXTURE;
        NERtcEx.getInstance().setLocalVideoConfig(config); //设置本地视频参数

        try {
            NERtcEx.getInstance().init(getApplicationContext(), getString(R.string.app_key), this, null);
        } catch (Exception e) {
            // 可能由于没有release导致初始化失败，release后再试一次
            NERtcEx.getInstance().release();
            try {
                NERtcEx.getInstance().init(getApplicationContext(), getString(R.string.app_key), this, null);
            } catch (Exception ex) {
                Toast.makeText(this, "SDK初始化失败", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
        }
        setLocalAudioEnable(true);
        setLocalVideoEnable(true);

        if (isFUOn) {
            setVideoCallback();
        }
    }

    private FURendererListener mFURendererListener = new FURendererListener() {

        @Override
        public void onPrepare() {
            mFaceUnityDataFactory.bindCurrentRenderer();
        }

        @Override
        public void onTrackStatusChanged(FUAIProcessorEnum type, int status) {
            Log.e(TAG, "onTrackStatusChanged: 人脸数: " + status);
            runOnUiThread(() -> {
                if (mTvTraceFace != null) {
                    mTvTraceFace.setVisibility(status > 0 ? View.GONE : View.VISIBLE);
                    if (type == FUAIProcessorEnum.FACE_PROCESSOR) {
                        mTvTraceFace.setText(R.string.toast_not_detect_face);
                    }else if (type == FUAIProcessorEnum.HUMAN_PROCESSOR) {
                        mTvTraceFace.setText(R.string.toast_not_detect_body);
                    }
                }
            });

        }

        @Override
        public void onFpsChanged(double fps, double callTime) {
            final String FPS = String.format(Locale.getDefault(), "%.2f", fps);
            Log.e(TAG, "onFpsChanged: FPS " + FPS + " callTime " + String.format(Locale.getDefault(), "%.2f", callTime));
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mTvFps.setText(FPS);
                }
            });
        }

        @Override
        public void onRelease() {
        }
    };


    private void setVideoCallback() {

        NERtcEx.getInstance().setVideoCallback(new NERtcVideoCallback() {

            private void cheekFaceNum() {
                //根据有无人脸 + 设备性能 判断开启的磨皮类型
                float faceProcessorGetConfidenceScore = FUAIKit.getInstance().getFaceProcessorGetConfidenceScore(0);
                if (faceProcessorGetConfidenceScore >= 0.95) {
                    //高端手机并且检测到人脸开启均匀磨皮，人脸点位质
                    if (FURenderKit.getInstance().getFaceBeauty() != null && FURenderKit.getInstance().getFaceBeauty().getBlurType() != FaceBeautyBlurTypeEnum.EquallySkin) {
                        FURenderKit.getInstance().getFaceBeauty().setBlurType(FaceBeautyBlurTypeEnum.EquallySkin);
                        FURenderKit.getInstance().getFaceBeauty().setEnableBlurUseMask(true);
                    }
                } else {
                    if (FURenderKit.getInstance().getFaceBeauty() != null && FURenderKit.getInstance().getFaceBeauty().getBlurType() != FaceBeautyBlurTypeEnum.FineSkin) {
                        FURenderKit.getInstance().getFaceBeauty().setBlurType(FaceBeautyBlurTypeEnum.FineSkin);
                        FURenderKit.getInstance().getFaceBeauty().setEnableBlurUseMask(false);
                    }
                }
            }

            private int oldOrientation = 0;

            @Override
            public boolean onVideoCallback(NERtcVideoFrame neRtcVideoFrame) {
                if (isFirstInit) {
                    isFirstInit = false;
                    mHandler = new Handler(Looper.myLooper());
                    mFURenderer.prepareRenderer(mFURendererListener);
                    initCsvUtil(MeetingActivity.this);
                    oldOrientation = neRtcVideoFrame.rotation;
                    return false;
                }

                if (FUConfig.DEVICE_LEVEL > FuDeviceUtils.DEVICE_LEVEL_MID) {
                    //高性能设备
                    cheekFaceNum();
                }
                Log.i(TAG, "onVideoCallback: neRtcVideoFrame " + getNERtcVideoFrameString(neRtcVideoFrame));
                if (oldOrientation != neRtcVideoFrame.rotation) {
                    FURenderKit.getInstance().clearCacheResource();
                    oldOrientation = neRtcVideoFrame.rotation;
                    mSkipFrame = 5;
                }
                mFURenderer.setInputOrientation(neRtcVideoFrame.rotation);

                long start = System.nanoTime();
                int texId = mFURenderer.onDrawFrameSingleInput(neRtcVideoFrame.textureId, neRtcVideoFrame.width, neRtcVideoFrame.height);
                long renderTime = System.nanoTime() - start;
                mCSVUtils.writeCsv(null, renderTime);
                if (mSkipFrame -- > 0) {
                    return false;
                }
                neRtcVideoFrame.format = NERtcVideoFrame.Format.TEXTURE_RGB;
                neRtcVideoFrame.textureId = texId;
                return true;
            }
        }, false);
    }

    private String getNERtcVideoFrameString(NERtcVideoFrame neRtcVideoFrame) {
        return String.format(Locale.getDefault(), "width: %d, height: %d, rotation: %d, format: %s, data: %d", neRtcVideoFrame.width,
                neRtcVideoFrame.height, neRtcVideoFrame.rotation, neRtcVideoFrame.format.toString(), neRtcVideoFrame.data.length);
    }

    /**
     * 随机生成用户ID
     *
     * @return 用户ID
     */
    private int generateRandomUserID() {
        return new Random().nextInt(100000);
    }

    /**
     * 退出房间
     *
     * @return 返回码
     * @see com.netease.lava.nertc.sdk.NERtcConstants.ErrorCode
     */
    private boolean leaveChannel() {
        joinedChannel = false;
        setLocalAudioEnable(false);
        setLocalVideoEnable(false);
        int ret = NERtcEx.getInstance().leaveChannel();
        return ret == NERtcConstants.ErrorCode.OK;
    }

    /**
     * 退出房间并关闭页面
     */
    private void exit() {
        if (joinedChannel) {
            leaveChannel();
        }
        finish();
    }

    private void destroyFU() {
        if (mHandler == null) {
            return;
        }
        CountDownLatch countDownLatch = new CountDownLatch(1);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mFURenderer.release();
                mCSVUtils.close();
                isFirstInit = true;
                countDownLatch.countDown();
            }
        });
        try {
            mHandler = null;
            countDownLatch.await(500, TimeUnit.MICROSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onJoinChannel(int result, long channelId, long elapsed, long l2) {
        Log.i(TAG, "onJoinChannel result: " + result + " channelId: " + channelId + " elapsed: " + elapsed);
        if (result == NERtcConstants.ErrorCode.OK) {
            joinedChannel = true;
            // 加入房间，准备展示己方视频
            localUserVv.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onLeaveChannel(int result) {
        Log.i(TAG, "onLeaveChannel result: " + result);
    }

    @Override
    public void onUserJoined(long uid) {
        Log.i(TAG, "onUserJoined uid: " + uid);
        // 已经有订阅，就不要变了
        if (remoteUserVv.getTag() != null) {
            return;
        }
        // 有用户加入，设置Tag，该用户离开前，只订阅和取消订阅此用户
        remoteUserVv.setTag(uid);
        // 不用等待了
        waitHintTv.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onUserJoined(long l, NERtcUserJoinExtraInfo neRtcUserJoinExtraInfo) {

    }

    @Override
    public void onUserLeave(long uid, int reason) {
        Log.i(TAG, "onUserLeave uid: " + uid + " reason: " + reason);
        // 退出的不是当前订阅的对象，则不作处理
        if (!isCurrentUser(uid)) {
            return;
        }
        // 设置TAG为null，代表当前没有订阅
        remoteUserVv.setTag(null);
        NERtcEx.getInstance().subscribeRemoteVideoStream(uid, NERtcRemoteVideoStreamType.kNERtcRemoteVideoStreamTypeHigh, false);

        // 显示在等待用户进入房间
        waitHintTv.setVisibility(View.VISIBLE);
        // 不展示远端
        remoteUserVv.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onUserLeave(long l, int i, NERtcUserLeaveExtraInfo neRtcUserLeaveExtraInfo) {

    }

    @Override
    public void onUserAudioStart(long uid) {
        Log.i(TAG, "onUserAudioStart uid: " + uid);
        if (!isCurrentUser(uid)) {
            return;
        }
        NERtcEx.getInstance().subscribeRemoteAudioStream(uid, true);
    }

    @Override
    public void onUserAudioStop(long uid) {
        Log.i(TAG, "onUserAudioStop, uid=" + uid);
    }

    @Override
    public void onUserVideoStart(long uid, int profile) {
        Log.i(TAG, "onUserVideoStart uid: " + uid + " profile: " + profile);
        if (!isCurrentUser(uid)) {
            return;
        }

        NERtcEx.getInstance().subscribeRemoteVideoStream(uid, NERtcRemoteVideoStreamType.kNERtcRemoteVideoStreamTypeHigh, true);
        remoteUserVv.setScalingType(NERtcConstants.VideoScalingType.SCALE_ASPECT_FIT);
        NERtcEx.getInstance().setupRemoteVideoCanvas(remoteUserVv, uid);

        // 更新界面
        remoteUserVv.setVisibility(View.VISIBLE);
    }

    @Override
    public void onUserVideoStop(long uid) {
        Log.i(TAG, "onUserVideoStop, uid=" + uid);
        if (!isCurrentUser(uid)) {
            return;
        }
        // 不展示远端
        remoteUserVv.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onDisconnect(int reason) {
        Log.i(TAG, "onDisconnect reason: " + reason);
        if (reason != NERtcConstants.ErrorCode.OK) {
            finish();
        }
    }

    @Override
    public void onClientRoleChange(int i, int i1) {

    }

    /**
     * 判断是否为onUserJoined中，设置了Tag的用户
     *
     * @param uid 用户ID
     * @return 用户ID是否匹配
     */
    private boolean isCurrentUser(long uid) {
        Object tag = remoteUserVv.getTag();
        Log.i(TAG, "isCurrentUser tag=" + tag);
        return tag != null && tag.equals(uid);
    }

    /**
     * 改变本地音频的可用性
     */
    private void changeAudioEnable() {
        enableLocalAudio = !enableLocalAudio;
        setLocalAudioEnable(enableLocalAudio);
    }

    /**
     * 改变本地视频的可用性
     */
    private void changeVideoEnable() {
        enableLocalVideo = !enableLocalVideo;
        setLocalVideoEnable(enableLocalVideo);
    }

    /**
     * 设置本地音频的可用性
     */
    private void setLocalAudioEnable(boolean enable) {
        enableLocalAudio = enable;
        NERtcEx.getInstance().enableLocalAudio(enableLocalAudio);
        enableAudioIb.setImageResource(enable ? R.drawable.selector_meeting_mute : R.drawable.selector_meeting_unmute);
    }

    /**
     * 设置本地视频的可用性
     */
    private void setLocalVideoEnable(boolean enable) {
        enableLocalVideo = enable;
        NERtcEx.getInstance().enableLocalVideo(enableLocalVideo);
        enableVideoIb.setImageResource(enable ? R.drawable.selector_meeting_close_video : R.drawable.selector_meeting_open_video);
        localUserVv.setVisibility(enable ? View.VISIBLE : View.INVISIBLE);
        localUserBgV.setBackgroundColor(getResources().getColor(enable ? R.color.white : R.color.black));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ib_audio:
                changeAudioEnable();
                break;
            case R.id.ib_leave:
                exit();
                break;
            case R.id.ib_video:
                changeVideoEnable();
                break;
            case R.id.img_camera_flip:
                NERtcEx.getInstance().switchCamera();
                break;
            default:
                break;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float x = event.values[0];
        float y = event.values[1];
        if (Math.abs(x) > 3 || Math.abs(y) > 3) {
            if (Math.abs(x) > Math.abs(y)) {
                mFURenderer.setDeviceOrientation(x > 0 ? 0 : 180);
            } else {
                mFURenderer.setDeviceOrientation(y > 0 ? 90 : 270);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void initCsvUtil(Context context) {
        mCSVUtils = new CSVUtils(context);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault());
        String dateStrDir = format.format(new Date(System.currentTimeMillis()));
        dateStrDir = dateStrDir.replaceAll("-", "").replaceAll("_", "");
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.getDefault());
        String dateStrFile = df.format(new Date());
        String filePath = Constant.filePath + dateStrDir + File.separator + "excel-" + dateStrFile + ".csv";
        Log.d(TAG, "initLog: CSV file path:" + filePath);
        StringBuilder headerInfo = new StringBuilder();
        headerInfo.append("version：").append(FURenderer.getInstance().getVersion()).append(CSVUtils.COMMA)
                .append("机型：").append(android.os.Build.MANUFACTURER).append(android.os.Build.MODEL)
                .append("处理方式：Texture").append(CSVUtils.COMMA);
        mCSVUtils.initHeader(filePath, headerInfo);
    }
}