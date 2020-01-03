package com.netease.nim.demo.session;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.netease.nim.demo.R;
import com.netease.nim.demo.main.helper.MessageHelper;
import com.netease.nim.demo.session.adapter.MultiRetweetAdapter;
import com.netease.nim.demo.session.extension.MultiRetweetAttachment;
import com.netease.nim.uikit.api.NimUIKit;
import com.netease.nim.uikit.business.contact.selector.activity.ContactSelectActivity;
import com.netease.nim.uikit.business.session.constant.Extras;
import com.netease.nim.uikit.business.session.module.list.MessageListPanelEx;
import com.netease.nim.uikit.common.ToastHelper;
import com.netease.nim.uikit.common.activity.UI;
import com.netease.nim.uikit.common.ui.dialog.CustomAlertDialog;
import com.netease.nim.uikit.common.util.log.sdk.wrapper.NimLog;
import com.netease.nim.uikit.common.util.string.MD5;
import com.netease.nimlib.sdk.NIMClient;
import com.netease.nimlib.sdk.RequestCallback;
import com.netease.nimlib.sdk.ResponseCode;
import com.netease.nimlib.sdk.msg.MessageBuilder;
import com.netease.nimlib.sdk.msg.MsgService;
import com.netease.nimlib.sdk.msg.constant.MsgDirectionEnum;
import com.netease.nimlib.sdk.msg.constant.SessionTypeEnum;
import com.netease.nimlib.sdk.msg.model.IMMessage;
import com.netease.nimlib.sdk.nos.NosService;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class WatchMultiRetweetActivity extends UI {

    private static final String TAG = "WatchMultiRetweetActivity";
    private static final String QUERY_FILE_THREAD_NAME = TAG + "/queryFile";
    private static final int DEFAULT_TIMEOUT = 3000;

    /** 接收消息 */
    private static final String INTENT_EXTRA_DATA = Extras.EXTRA_DATA;

    /** 合并消息对象 */
    private IMMessage mMessage;

    /** 被合并的消息组成的列表 */
    private List<IMMessage> mItems;

    /** 是否可以转发消息 */
    private boolean mCanForward;

    /** 展示消息的列表 */
    private RecyclerView mMsgListRV;

    /** 转发按钮 */
    private TextView mForwardTV;

    /** 返回按钮 */
    private ImageButton mBackBtn;

    /** 标题，展示合并消息的来源会话 */
    private TextView mSessionNameTV;

    /**
     * 可以再次转发的打开方式
     *
     * @param reqCode  请求码
     * @param activity 触发Activity
     * @param message  聊天记录
     */
    public static void startForResult(int reqCode, Activity activity, IMMessage message) {
        Intent intent = new Intent();
        intent.putExtra(INTENT_EXTRA_DATA, message);
        intent.putExtra(Extras.EXTRA_FORWARD, true);
        intent.setClass(activity, WatchMultiRetweetActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        activity.startActivityForResult(intent, reqCode);
    }

    /**
     * 不再次转发消息的打开方式
     *
     * @param activity 触发Activity
     * @param message  聊天记录
     */
    public static void start(Activity activity, IMMessage message) {
        Intent intent = new Intent();
        intent.putExtra(INTENT_EXTRA_DATA, message);
        intent.putExtra(Extras.EXTRA_FORWARD, false);
        intent.setClass(activity, WatchMultiRetweetActivity.class);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.nim_watch_multi_retweet_activity);
        onParseIntent();
        findViews();
        queryFileBackground(new QueryFileCallbackImp() {
            @Override
            public void onFinished(MultiRetweetAttachment attachment) {
                runOnUiThread(() -> {
                    setTitle(attachment);
                    setList();
                });
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case MessageListPanelEx.REQUEST_CODE_FORWARD_PERSON:
            case MessageListPanelEx.REQUEST_CODE_FORWARD_TEAM:
                onSelectSessionResult(requestCode, resultCode, data);
                break;
            default:
                break;
        }
    }

    /**
     * 选择转发目标结束的回调
     */
    private void onSelectSessionResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode != Activity.RESULT_OK || data == null) {
            return;
        }

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle(R.string.confirm_forwarded)
                .setMessage(getString(R.string.confirm_forwarded_to) + data.getStringArrayListExtra(Extras.RESULT_NAME).get(0) + "?")
                .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                    private void sendMsg(SessionTypeEnum sessionType, IMMessage packedMsg) {
                        data.putExtra(Extras.EXTRA_DATA, packedMsg);
                        data.putExtra(Extras.EXTRA_TYPE, sessionType.getValue());
                        setResult(Activity.RESULT_OK, data);
                        finish();
                    }

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SessionTypeEnum type;
                        switch (requestCode) {
                            //转发给个人
                            case MessageListPanelEx.REQUEST_CODE_FORWARD_PERSON:
                                type = SessionTypeEnum.P2P;
                                break;
                            //转发给群组
                            case MessageListPanelEx.REQUEST_CODE_FORWARD_TEAM:
                                type = SessionTypeEnum.Team;
                                break;
                            default:
                                return;
                        }
                        sendMsg(type, mMessage);
                    }
                })
                .setNegativeButton(getString(R.string.cancel), (dialog, which) -> {
                    setResult(Activity.RESULT_CANCELED);
                    finish();
                })
                .setOnCancelListener(dialog -> {
                    setResult(Activity.RESULT_CANCELED);
                    finish();
                });
        dialogBuilder.create().show();
    }

    /**
     * 在标题处写上会话名称
     */
    private void setTitle(MultiRetweetAttachment attachment) {
        String sessionName = attachment == null ? null : attachment.getSessionName();
        mSessionNameTV.setText(sessionName == null ? "" : sessionName);
    }

    private void setList() {
        mMsgListRV.setLayoutManager(new LinearLayoutManager(getApplicationContext(), RecyclerView.VERTICAL, false));
        MultiRetweetAdapter adapter = new MultiRetweetAdapter(mMsgListRV, mItems, this);
        mMsgListRV.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }


    private void onParseIntent() {
        this.mMessage = (IMMessage) getIntent().getSerializableExtra(INTENT_EXTRA_DATA);
        this.mCanForward = getIntent().getBooleanExtra(Extras.EXTRA_FORWARD, false);
    }

    private void findViews() {
        mMsgListRV = findViewById(R.id.rv_msg_history);

        mForwardTV = findViewById(R.id.tv_forward);
        mForwardTV.setOnClickListener((v) -> {
            showTransFormTypeDialog();
        });
        mForwardTV.setVisibility(mCanForward ? View.VISIBLE : View.INVISIBLE);

        mBackBtn = findViewById(R.id.ib_back);
        mBackBtn.setOnClickListener(v -> {
            WatchMultiRetweetActivity.this.finish();
        });

        mSessionNameTV = findViewById(R.id.tv_session_name);
    }


    /**
     * 展示选择转发类型的会话框
     */
    private void showTransFormTypeDialog() {
        CustomAlertDialog alertDialog = new CustomAlertDialog(this);
        alertDialog.setCancelable(true);
        alertDialog.setCanceledOnTouchOutside(true);
        addForwardToPersonItem(alertDialog);
        addForwardToTeamItem(alertDialog);
        alertDialog.show();
    }

    /**
     * 添加转发到个人的项
     *
     * @param alertDialog 所在会话框
     */
    private void addForwardToPersonItem(CustomAlertDialog alertDialog) {
        alertDialog.addItem(getString(R.string.forward_to_person), () -> {
            ContactSelectActivity.Option option = new ContactSelectActivity.Option();
            option.title = "个人";
            option.type = ContactSelectActivity.ContactSelectType.BUDDY;
            option.multi = false;
            option.maxSelectNum = 1;
            NimUIKit.startContactSelector(WatchMultiRetweetActivity.this, option, MessageListPanelEx.REQUEST_CODE_FORWARD_PERSON);
        });
    }

    /**
     * 添加转发到群组的项
     *
     * @param alertDialog 所在会话框
     */
    private void addForwardToTeamItem(CustomAlertDialog alertDialog) {
        alertDialog.addItem(getString(R.string.forward_to_team), () -> {
            ContactSelectActivity.Option option = new ContactSelectActivity.Option();
            option.title = "群组";
            option.type = ContactSelectActivity.ContactSelectType.TEAM;
            option.multi = false;
            option.maxSelectNum = 1;
            NimUIKit.startContactSelector(WatchMultiRetweetActivity.this, option, MessageListPanelEx.REQUEST_CODE_FORWARD_TEAM);
        });
    }

    /**
     * 异步获取与解析Nos上存储的附件
     *
     * @param callback 进度回调
     */
    private void queryFileBackground(IQueryFileCallback callback) {
        if (mMessage == null || !(mMessage.getAttachment() instanceof MultiRetweetAttachment)) {
            return;
        }
        final MultiRetweetAttachment attachment = (MultiRetweetAttachment) mMessage.getAttachment();
        //短链换长链
        NIMClient.getService(NosService.class).getOriginUrlFromShortUrl(attachment.getUrl()).setCallback(new RequestCallback<String>() {
            @Override
            public void onSuccess(String param) {
                if (TextUtils.isEmpty(param)) {
                    return;
                }
                attachment.setUrl(param);
                HandlerThread thread = new HandlerThread(QUERY_FILE_THREAD_NAME);
                thread.start();
                Handler backgroundHandler = new Handler(thread.getLooper());
                backgroundHandler.post(() -> {
                    try {
                        HttpURLConnection connection = (HttpURLConnection) new URL(attachment.getUrl()).openConnection();
                        connection.setRequestMethod("GET");
                        connection.setReadTimeout(DEFAULT_TIMEOUT);
                        connection.setConnectTimeout(DEFAULT_TIMEOUT);
                        connection.setUseCaches(false);
                        connection.setDoInput(true);
                        callback.onProgress(0);
                        //开始下载
                        int resCode = connection.getResponseCode();
                        if (resCode != ResponseCode.RES_SUCCESS) {
                            runOnUiThread(() -> {
                                backgroundHandler.getLooper().quit();
                                callback.onFailed("download failed, code=" + resCode);
                            });
                            return;
                        }
                        //读取文件内容
                        InputStream inputStream = connection.getInputStream();
                        byte[] src = readFromInputStream(inputStream);
                        callback.onProgress(35);

                        //检验MD5
                        String fileMd5 = MD5.getMD5(src).toUpperCase();
                        String recordedMd5 = attachment.getMd5().toUpperCase();
                        if (!fileMd5.equals(recordedMd5)) {
                            callback.onProgress(40);
                            NimLog.d(TAG, "MD5 check failed, fileMD5=" + fileMd5 + "; record = " + recordedMd5);
//                    callback.onFailed("MD5 check failed, fileMD5=" + fileMd5 + "; record = " + recordedMd5);
//                    return;
                        }
                        callback.onProgress(40);
                        //解密
                        if (attachment.isEncrypted()) {
                            byte[] key = attachment.getPassword().getBytes();
                            src = MessageHelper.decryptByRC4(src, key);
                            callback.onProgress(45);
                        }
                        if (attachment.isCompressed()) {
                            callback.onProgress(50);
                        }

                        //解码
                        String[] blocks = new String(src).split("\n");
                        int count = getMsgCount(blocks[0]);
                        mItems = new ArrayList<>(count);
                        for (int i = 1; i <= count; ++i) {
                            IMMessage lineMsg = getMessage(blocks[i]);
                            if (lineMsg == null) {
                                continue;
                            }
                            final IMMessage msg = getMessage(blocks[i]);
                            double progressUnit = 40 / count;
                            if (msg == null) {
                                continue;
                            }
                            msg.setDirect(MsgDirectionEnum.In);
                            mItems.add(msg);
                            callback.onProgress(50 + (int) (progressUnit * i));
                        }
                        callback.onProgress(100);
                        runOnUiThread(() -> {
                            backgroundHandler.getLooper().quit();
                            callback.onFinished(attachment);
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        runOnUiThread(() -> {
                            backgroundHandler.getLooper().quit();
                            callback.onException(e);
                        });
                    }
                });
            }

            @Override
            public void onFailed(int code) {
                callback.onFailed("failed to get origin url from short url, code=" + code);
            }

            @Override
            public void onException(Throwable exception) {
                callback.onException(exception);
            }
        });
    }

    private byte[] readFromInputStream(InputStream inputStream) throws IOException {
        LinkedList<Byte> fileByteList = new LinkedList<>();
        int newByte;
        while ((newByte = inputStream.read()) != -1) {
            fileByteList.add((byte) newByte);
        }
        byte[] fileBytes = new byte[fileByteList.size()];
        int index = 0;
        for (byte b : fileByteList) {
            fileBytes[index++] = b;
        }
        return fileBytes;
    }

    private int getMsgCount(String firstLine) {
        try {
            JSONObject object = new JSONObject(firstLine);
            return object.getInt("message_count");
        } catch (JSONException e) {
            e.printStackTrace();
            return 0;
        }
    }

    private IMMessage getMessage(String line) {
        return MessageBuilder.createFromJson(line);
    }


    interface IQueryFileCallback {

        /**
         * 读取文件进度回调
         *
         * @param percent 进度百分比
         */
        void onProgress(int percent);

        /**
         * 完成加载
         */
        void onFinished(MultiRetweetAttachment attachment);

        /**
         * 加载失败
         *
         * @param msg 错误信息
         */
        void onFailed(String msg);

        /**
         * 加载中出现异常
         *
         * @param e 异常
         */
        void onException(Throwable e);
    }

    class QueryFileCallbackImp implements IQueryFileCallback {

        @Override
        public void onProgress(int percent) {
            NimLog.d(TAG, "query file on progress: " + percent + "%");
            runOnUiThread(() -> {
                ToastHelper.showToast(WatchMultiRetweetActivity.this, percent + "%");
            });
        }

        @Override
        public void onFinished(MultiRetweetAttachment attachment) {
            NimLog.d(TAG, "query file finished, attachment=" + (attachment == null ? String.valueOf(null) : attachment.toJson(false)));
        }

        @Override
        public void onFailed(String msg) {
            final String briefMsg = "query file failed";
            NimLog.d(TAG, briefMsg + ", msg=" + msg);
            runOnUiThread(() -> {
                ToastHelper.showToast(WatchMultiRetweetActivity.this, briefMsg);
            });
        }

        @Override
        public void onException(Throwable e) {
            final String briefMsg = "query file failed";
            NimLog.d(TAG, briefMsg + ", msg=" + e.getMessage());
            runOnUiThread(() -> {
                ToastHelper.showToast(WatchMultiRetweetActivity.this, briefMsg);
            });
        }
    }
}
