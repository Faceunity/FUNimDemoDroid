本代码由云信 即时通讯DEMO 4.1.0修改
faceunity nama库功能已由云信集成在faceunity module
# 集成步骤
## 加入授权文件
复制authpack.java文件到com.faceunity.auth包下（Auth类AUTH_CLASS_PATH 原值为com.faceunity.auth.AuthPack已修改为com.faceunity.auth.authpack）
## 初始化FaceUnity并关联UI布局
FaceU.createAndAttach()，支持同步/异步加载
demo中为在AVChatActivity的onCreate中调用以下方法
~~~
private void initFaceU() {
    showOrHideFaceULayout(false); // hide default

    if (VersionUtil.isCompatible(Build.VERSION_CODES.JELLY_BEAN_MR2) && FaceU.hasAuthorized()) {
        // async load FaceU
        FaceU.createAndAttach(AVChatActivity.this, findView(R.id.avchat_video_face_unity), new FaceU.Response<FaceU>() {
            @Override
            public void onResult(FaceU faceU) {
                AVChatActivity.this.faceU = faceU;
                showOrHideFaceULayout(true); // show
            }
        });
    }
}
~~~
## FaceUnity的销毁
faceU.destroy()，一般在Activity的onDestory中调用
## 给视频帧添加人脸识别/道具/美颜效果
faceU.effect()， 支持I420和NV21两种帧图像格式
## 显示和隐藏UI布局切换
faceU.showOrHideLayout()，默认显示
# 定制需求
## 定制界面
修改com.faceunity.ui中的代码或者自己编写
## 定制道具
EffectAndFilterSelectAdapter中EFFECT_NAMES指定的是assets里对应的道具的文件名，故如需增删道具只需要在assets增删相应的道具文件并在EFFECT_NAMES增删相应的文件名即可。
## 修改默认美颜参数
修改FaceU中以下代码
~~~
private float mFaceBeautyBlurLevel = 6.0f;
private float mFaceBeautyColorLevel = 0.2f;
private float mFaceBeautyCheckThin = 1.0f;
private float mFaceBeautyRedLevel = 0.5f;
private float mFaceBeautyEnlargeEye = 0.5f;
private float mFaceShapeLevel = 0.5f;
private int mFaceShape = 3;
~~~
参数含义与取值范围参考[这里](http://www.faceunity.com/technical/android-beauty.html)，如果使用界面，则需要同时修改界面中的初始值。
## 其他需求

nama库的使用参考[这里](http://www.faceunity.com/technical/android-api.html)。

# 2D 3D道具制作

除了使用制作好的道具外，还可以自行制作2D和3D道具，参考[这里](http://www.faceunity.com/technical/fueditor-intro.html)。