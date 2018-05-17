本代码由云信 即时通讯DEMO修改
# 对接步骤
## 添加module
添加beautycontrolview module到工程中，在app dependencies里添加compile project(':beautycontrolview')
## 修改代码
### 生成与销毁
在AVChatActivity的
onCreate方法中添加（初始化并加载美颜道具、默认道具）
~~~
mFURenderer = new FURenderer.Builder(this).createEGLContext(true).build();
mFURenderer.loadItems();
~~~
onDestroy方法中添加（销毁道具）
~~~
mFURenderer.destroyItems();
~~~
### 渲染道具到原始数据上
修改AVChatActivity的onVideoFrameFilter方法使用FURenderer将道具渲染到原始数据上
~~~
@Override
public boolean onVideoFrameFilter(AVChatVideoFrame frame, boolean maybeDualInput) {
    mFURenderer.drawFrame(frame.data, frame.width, frame.height, frame.rotation);

    return true;
}
~~~
## 添加界面（可选）
### 修改layout
在avchat_video_layout的末尾修改（在界面底部显示默认的道具选择控件）
~~~
<com.faceunity.beautycontrolview.BeautyControlView
    android:id="@+id/faceunity_control"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_above="@+id/avchat_video_bottom_control" />
~~~
在onCreate方法中添加
~~~
mFaceunityControlView = (BeautyControlView) findViewById(R.id.faceunity_control);
mFaceunityControlView.setOnFaceUnityControlListener(mFURenderer);
~~~
# 更新SDK
[Nama SDK发布地址](https://github.com/Faceunity/FULiveDemoDroid/releases),可查看Nama的所有版本和发布说明。
更新方法为下载Faceunity*.zip解压后替换faceunity模块中的相应文件。
# 定制需求
## 定制界面
修改beautycontrolview中的界面代码
BeautyControlView等或者自己编写。
## 定制道具
beautycontrolview中EffectEnum指定的是effects里对应的道具的文件名，故如需增删道具只需要在effects增删相应的道具文件并在EffectEnum增删相应的项即可。
## 修改默认美颜参数
修改beautycontrolview中BeautyControlView中以下代码
~~~
private float mFaceBeautyALLBlurLevel = 1.0f;//精准磨皮
private float mFaceBeautyType = 0.0f;//美肤类型
private float mFaceBeautyBlurLevel = 0.7f;//磨皮
private float mFaceBeautyColorLevel = 0.5f;//美白
private float mFaceBeautyRedLevel = 0.5f;//红润
private float mBrightEyesLevel = 1000.7f;//亮眼
private float mBeautyTeethLevel = 1000.7f;//美牙

private float mFaceBeautyFaceShape = 4.0f;//脸型
private float mFaceBeautyEnlargeEye = 0.4f;//大眼
private float mFaceBeautyCheekThin = 0.4f;//瘦脸
private float mFaceBeautyEnlargeEye_old = 0.4f;//大眼
private float mFaceBeautyCheekThin_old = 0.4f;//瘦脸
private float mChinLevel = 0.3f;//下巴
private float mForeheadLevel = 0.3f;//额头
private float mThinNoseLevel = 0.5f;//瘦鼻
private float mMouthShape = 0.4f;//嘴形
~~~
参数含义与取值范围参考[这里](http://www.faceunity.com/technical/android-beauty.html)，如果使用界面，则需要同时修改界面中的初始值。
## 其他需求
nama库的使用参考[这里](http://www.faceunity.com/technical/android-api.html)。
# 2D 3D道具制作
除了使用制作好的道具外，还可以自行制作2D和3D道具，参考[这里](http://www.faceunity.com/technical/fueditor-intro.html)。