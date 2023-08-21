package io.netease.tutorials1v1vcall.custom;

import android.app.Activity;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.EGL14;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.os.CpuUsageInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.util.Log;

import com.faceunity.core.camera.FUCamera;
import com.faceunity.core.camera.FUCameraPreviewData;
import com.faceunity.core.entity.FUCameraConfig;
import com.faceunity.core.entity.FURenderOutputData;
import com.faceunity.core.enumeration.CameraFacingEnum;
import com.faceunity.core.enumeration.FUInputTextureEnum;
import com.faceunity.core.enumeration.FUTransformMatrixEnum;
import com.faceunity.core.faceunity.FURenderKit;
import com.faceunity.core.faceunity.OffLineRenderHandler;
import com.faceunity.core.listener.OnFUCameraListener;
import com.faceunity.nama.FURenderer;
import com.faceunity.nama.data.FaceUnityDataFactory;
import com.faceunity.nama.listener.FURendererListener;
import com.netease.lava.nertc.sdk.NERtcEx;
import com.netease.lava.nertc.sdk.video.NERtcVideoFrame;

import io.netease.tutorials1v1vcall.PreferenceUtil;
import io.netease.tutorials1v1vcall.profile.CSVUtils;
import io.netease.tutorials1v1vcall.profile.Constant;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;

/**
 * 用户自定义数据采集及数据处理，接入 faceunity 美颜贴纸
 *
 * @author Richie on 2019.12.20
 */
public class CameraRenderer {
    private static final String TAG = "CameraRenderer";
    private static final int DEFAULT_CAMERA_WIDTH = 1280;
    private static final int DEFAULT_CAMERA_HEIGHT = 720;
    private static final int PREVIEW_BUFFER_COUNT = 3;
    private Activity mActivity;
    private int mCameraWidth = DEFAULT_CAMERA_WIDTH;
    private int mCameraHeight = DEFAULT_CAMERA_HEIGHT;
    private int mCameraFacing = Camera.CameraInfo.CAMERA_FACING_FRONT;
    private int mCameraOrientation = 270;
    private int mCameraTextureId;
    private int mSkippedFrames = 5;
    private FURenderer mFURenderer;
    private CSVUtils mCSVUtils;
    private boolean openFU;
    private FURendererListener mRenderListener;
    private FaceUnityDataFactory mFaceUnityDataFactory;
    private FUCamera fuCamera;
    private OffLineRenderHandler mOffLineRenderHandler;

    public CameraRenderer(Activity activity, FaceUnityDataFactory faceUnityDataFactory, FURendererListener renderListener) {
        mActivity = activity;
        mRenderListener = renderListener;
        mFaceUnityDataFactory = faceUnityDataFactory;
        String isOpen = PreferenceUtil.getString(activity, PreferenceUtil.KEY_FACEUNITY_IS_ON);
        openFU = !TextUtils.isEmpty(isOpen) && isOpen.equals("true");
        FURenderer.getInstance().setup(activity);
        FURenderKit.getInstance().setReadBackSync(true);
        mFURenderer = FURenderer.getInstance();
        mFURenderer.setMarkFPSEnable(true);
        mFURenderer.setInputTextureType(FUInputTextureEnum.FU_ADM_FLAG_EXTERNAL_OES_TEXTURE);
        mFURenderer.setCameraFacing(CameraFacingEnum.CAMERA_FRONT);
        mFURenderer.setInputOrientation(CameraUtils.getCameraOrientation(Camera.CameraInfo.CAMERA_FACING_FRONT));
        mFURenderer.setInputTextureMatrix(FUTransformMatrixEnum.CCROT0_FLIPVERTICAL);
        mFURenderer.setInputBufferMatrix(FUTransformMatrixEnum.CCROT0_FLIPVERTICAL);
        mFURenderer.setOutputMatrix(FUTransformMatrixEnum.CCROT0_FLIPVERTICAL);
        mFURenderer.setCreateEGLContext(true);

        fuCamera = FUCamera.getInstance();
        mOffLineRenderHandler = OffLineRenderHandler.getInstance();
    }

    private volatile byte[] mInputBuffer;
    private final Object mInputBufferLock = new Object();


    private byte[] getCurrentBuffer() {
        synchronized (mInputBufferLock) {
            byte[] currentInputBuffer = new byte[mInputBuffer.length];
            System.arraycopy(mInputBuffer, 0, currentInputBuffer, 0, currentInputBuffer.length);
            return currentInputBuffer;
        }
    }

    private OnFUCameraListener onFUCameraListener = new OnFUCameraListener() {
        @Override
        public void onPreviewFrame(FUCameraPreviewData fuCameraPreviewData) {
            if (fuCameraPreviewData == null) {
                mOffLineRenderHandler.requestRender();
                return;
            }
            if (!openFU) {
                Log.e(TAG, "onPreviewFrame: no fu");
                NERtcVideoFrame videoFrame = new NERtcVideoFrame();
                videoFrame.data = fuCameraPreviewData.getBuffer();
                videoFrame.textureId = mCameraTextureId;
                videoFrame.width = mCameraWidth;
                videoFrame.height = mCameraHeight;
                videoFrame.format = NERtcVideoFrame.Format.NV21;
                videoFrame.rotation = mCameraOrientation;
                NERtcEx.getInstance().pushExternalVideoFrame(videoFrame);
                return;
            }
            synchronized (mInputBufferLock) {
                    mInputBuffer =  fuCameraPreviewData.getBuffer();
            }
            mOffLineRenderHandler.requestRender();
        }
    };

    private OffLineRenderHandler.Renderer mOffLineRenderHandlerRedner = new OffLineRenderHandler.Renderer() {
        @Override
        public void onDrawFrame() {
            if (mInputBuffer == null) {
                return;
            }
            SurfaceTexture surfaceTexture = fuCamera.getSurfaceTexture();
            try {
                surfaceTexture.updateTexImage();
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
            int orientation = mCameraOrientation;
            long start = System.nanoTime();
            FURenderOutputData outputData;
            byte[] inputBuffer = getCurrentBuffer();
            outputData = mFURenderer.onDrawFrameInputWithReturn(inputBuffer, mCameraTextureId, mCameraWidth, mCameraHeight);
            long time = System.nanoTime() - start;
            mCSVUtils.writeCsv(null, time);
            if (mSkippedFrames > 0) {
                mSkippedFrames--;
            } else {
                if (mPostHandler != null && orientation == mCameraOrientation && outputData != null) {
                    long timeStamp = System.currentTimeMillis();
                    Log.e(TAG, "onPreviewFrame: with fu timeStamp: " + timeStamp);
                    mPostHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            NERtcVideoFrame videoFrame = new NERtcVideoFrame();
                            videoFrame.data = outputData.getImage().getBuffer();
                            videoFrame.textureId = mCameraTextureId;
                            videoFrame.width = mCameraWidth;
                            videoFrame.height = mCameraHeight;
                            videoFrame.format = NERtcVideoFrame.Format.NV21;
                            videoFrame.rotation = mCameraOrientation;
                            NERtcEx.getInstance().pushExternalVideoFrame(videoFrame);
                            long pushTime = System.currentTimeMillis() - timeStamp;
                            Log.e(TAG, "onPreviewFrame: with fu timeStamp: " + timeStamp + "  pushTime: " + pushTime);
                        }
                    });
                }
            }
        }
    };

    public void onResume() {
        startBackgroundThread();
        mOffLineRenderHandler.onResume();
        mOffLineRenderHandler.setRenderer(mOffLineRenderHandlerRedner);
        mOffLineRenderHandler.queueEvent(new Runnable() {
            @Override
            public void run() {
                if (mFURenderer != null) {
                    mFURenderer.prepareRenderer(mRenderListener);
                }
                initCsvUtil(mActivity);
                mCameraTextureId = GlUtil.createTextureObject(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);
                FUCameraConfig config = new FUCameraConfig();
                config.setCameraFacing(mCameraFacing == Camera.CameraInfo.CAMERA_FACING_FRONT ? CameraFacingEnum.CAMERA_FRONT : CameraFacingEnum.CAMERA_BACK);
                fuCamera.openCamera(config, mCameraTextureId, onFUCameraListener);
            }
        });
    }

    public void onPause() {
        if (mPostHandler == null) {
            return;
        }
        CountDownLatch countDownLatch = new CountDownLatch(1);
        mOffLineRenderHandler.queueEvent(new Runnable() {
            @Override
            public void run() {
                fuCamera.closeCamera();
                if (mCameraTextureId > 0) {
                    GLES20.glDeleteTextures(1, new int[]{mCameraTextureId}, 0);
                    mCameraTextureId = 0;
                }
                if (mFURenderer != null) {
                    mFURenderer.release();
                }
                mCSVUtils.close();
                countDownLatch.countDown();
            }
        });
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mOffLineRenderHandler.onPause();
        stopBackgroundThread();
        mSkippedFrames = 5;
    }

    public void onDestroy() {
        fuCamera.releaseCamera();
    }

    /**
     * 切换相机
     */
    public void switchCamera() {
        Log.d(TAG, "switchCamera: ");
        fuCamera.switchCamera();
        boolean isFront = mCameraFacing == Camera.CameraInfo.CAMERA_FACING_FRONT;
        mCameraFacing = isFront ? Camera.CameraInfo.CAMERA_FACING_BACK : Camera.CameraInfo.CAMERA_FACING_FRONT;
        mSkippedFrames = 5;
        if (mFURenderer != null) {
            mFURenderer.setCameraFacing(mCameraFacing == Camera.CameraInfo.CAMERA_FACING_FRONT ? CameraFacingEnum.CAMERA_FRONT : CameraFacingEnum.CAMERA_BACK);
            mFURenderer.setInputOrientation(CameraUtils.getCameraOrientation(mCameraFacing));

            if (mFURenderer != null) {
                mFURenderer.setCameraFacing(mCameraFacing == Camera.CameraInfo.CAMERA_FACING_FRONT ? CameraFacingEnum.CAMERA_FRONT : CameraFacingEnum.CAMERA_BACK);
                mFURenderer.setInputOrientation(CameraUtils.getCameraOrientation(mCameraFacing));
                if (mCameraFacing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    mFURenderer.setInputTextureMatrix(FUTransformMatrixEnum.CCROT0_FLIPVERTICAL);
                    mFURenderer.setInputBufferMatrix(FUTransformMatrixEnum.CCROT0_FLIPVERTICAL);
                    mFURenderer.setOutputMatrix(FUTransformMatrixEnum.CCROT0_FLIPVERTICAL);
                }else {
                    mFURenderer.setInputTextureMatrix(FUTransformMatrixEnum.CCROT0);
                    mFURenderer.setInputBufferMatrix(FUTransformMatrixEnum.CCROT0);
                    mFURenderer.setOutputMatrix(FUTransformMatrixEnum.CCROT0_FLIPHORIZONTAL);
                }
            }

        }
    }

    private Handler mPostHandler;

    private void startBackgroundThread() {
        HandlerThread handlerThread = new HandlerThread("poster");
        handlerThread.start();
        mPostHandler = new Handler(handlerThread.getLooper());
    }

    private void stopBackgroundThread() {
        mPostHandler.getLooper().quitSafely();
        mPostHandler = null;
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
                .append("处理方式：双输入返回Buffer").append(CSVUtils.COMMA);
        mCSVUtils.initHeader(filePath, headerInfo);
    }

}
