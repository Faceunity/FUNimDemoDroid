package com.netease.nim.uikit.business.session.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.netease.nim.uikit.R;
import com.netease.nim.uikit.api.NimUIKit;
import com.netease.nim.uikit.api.model.CreateMessageCallback;
import com.netease.nim.uikit.business.contact.selector.activity.ContactSelectActivity;
import com.netease.nim.uikit.business.session.constant.Extras;
import com.netease.nim.uikit.business.session.helper.MessageHelper;
import com.netease.nim.uikit.business.session.module.Container;
import com.netease.nim.uikit.business.session.module.ModuleProxy;
import com.netease.nim.uikit.business.session.module.MultiRetweetMsgCreatorFactory;
import com.netease.nim.uikit.business.session.module.list.MessageListPanelEx;
import com.netease.nim.uikit.business.session.module.list.MsgAdapter;
import com.netease.nim.uikit.common.activity.UI;
import com.netease.nim.uikit.common.ui.dialog.CustomAlertDialog;
import com.netease.nim.uikit.common.util.log.sdk.wrapper.NimLog;
import com.netease.nim.uikit.impl.NimUIKitImpl;
import com.netease.nimlib.sdk.msg.constant.MsgTypeEnum;
import com.netease.nimlib.sdk.msg.constant.SessionTypeEnum;
import com.netease.nimlib.sdk.msg.model.IMMessage;

import java.util.ArrayList;
import java.util.List;

import static com.netease.nimlib.sdk.msg.constant.SessionTypeEnum.None;
import static com.netease.nimlib.sdk.msg.constant.SessionTypeEnum.P2P;
import static com.netease.nimlib.sdk.msg.constant.SessionTypeEnum.Team;
import static com.netease.nimlib.sdk.msg.constant.SessionTypeEnum.typeOfValue;

public class MsgSelectActivity extends UI implements ModuleProxy {
    // 用到的EXTRA key
    // Extras.EXTRA_ITEMS: 数据源
    // Extras.EXTRA_TYPE: 会话类型
    // Extras.ACCOUNT: 会话ID
    // Extras.EXTRA_FROM: 选中的项在列表中的位置

    private static final String TAG = "MsgSelectActivity";
    /** 可合并发送的最小消息条数 */
    private static final int MIN_MSG_COUNT = 1;

    public static void startForResult(int reqCode, Activity activity, ArrayList<IMMessage> msgArr, SessionTypeEnum sessionType, String sessionID, int selectedPosition) {
        Intent intent = new Intent();
        if (msgArr != null) {
            intent.putExtra(Extras.EXTRA_ITEMS, msgArr);
            intent.putExtra(Extras.EXTRA_TYPE, sessionType.getValue());
            intent.putExtra(Extras.EXTRA_ACCOUNT, sessionID);
            intent.putExtra(Extras.EXTRA_FROM, selectedPosition);
        }

        intent.setClass(activity, MsgSelectActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        activity.startActivityForResult(intent, reqCode);
    }


    /** 发送按钮 */
    private TextView mSendTV;

    /** 返回按钮 */
    private TextView mBackTV;

    /** 可选消息的列表 */
    private RecyclerView mMsgSelectorRV;

    private TextView mSessionNameTV;

    /** 在线状态，在P2P会话中显示 */
    private TextView mOnlineStateTV;

    /** 消息列表原始数据，列表的源数据通过adapter.getData获取 */
    private List<IMMessage> mItems;

    /** RecyclerView的初始位置，即被长按的消息的位置 */
    private int mSelectedPosition = 0;

    /** 转发目的会话的类型 */
    private SessionTypeEnum mSessionType;

    /** 会话ID */
    private String mSessionID;

    /** 选中的会话的个数 */
    private int mCheckedCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.nim_msg_select);
        getExtras();
        initViews();
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
     * 选择合并发送目标结束的回调
     */
    private void onSelectSessionResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode != Activity.RESULT_OK || data == null) {
            return;
        }
        //用于确认发送的会话框
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

                    class P2PCallback extends CreateMessageCallbackImpl {

                        @Override
                        public void onFinished(IMMessage multiRetweetMsg) {
                            sendMsg(P2P, multiRetweetMsg);
                        }
                    }

                    class TeamCallback extends CreateMessageCallbackImpl {

                        @Override
                        public void onFinished(IMMessage multiRetweetMsg) {
                            sendMsg(Team, multiRetweetMsg);
                        }
                    }

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //获取被勾选的消息
                        List<IMMessage> checked = MessageHelper.getInstance().getCheckedItems(mItems);
                        switch (requestCode) {
                            //转发给个人
                            case MessageListPanelEx.REQUEST_CODE_FORWARD_PERSON:
                                MultiRetweetMsgCreatorFactory.createMsg(checked, true, new P2PCallback());
                                break;
                            //转发给群组
                            case MessageListPanelEx.REQUEST_CODE_FORWARD_TEAM:
                                MultiRetweetMsgCreatorFactory.createMsg(checked, true, new TeamCallback());
                                break;
                            default:
                                break;
                        }
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

    private void getExtras() {
        final Bundle arguments = getIntent().getExtras();
        if (arguments == null) {
            return;
        }
        //获取RecyclerView的初始化位置
        mSelectedPosition = arguments.getInt(Extras.EXTRA_FROM);

        //获取Items
        ArrayList<IMMessage> itemArr = (ArrayList<IMMessage>) arguments.getSerializable(Extras.EXTRA_ITEMS);
        if (itemArr == null) {
            return;
        }
        int size = itemArr.size();
        mItems = new ArrayList<>(size);
        for (int i = 0; i < size; ++i) {
            IMMessage imMessage = itemArr.get(i);
            if(imMessage == null){
                continue;
            }

            if (i == mSelectedPosition){
                mSelectedPosition = mItems.size();
            }
            final MsgTypeEnum msgType = imMessage.getMsgType();
            if (NimUIKitImpl.getMsgForwardFilter().shouldIgnore(imMessage)) {
                continue;
            }
            //过滤掉null、未知类型消息、音视频通话、通知消息和提醒类消息
            if (msgType == null || MsgTypeEnum.undef.equals(msgType) || MsgTypeEnum.avchat.equals(msgType) || MsgTypeEnum.notification.equals(msgType) || MsgTypeEnum.tip.equals(msgType)) {
                continue;
            }
            imMessage.setChecked(false);
            mItems.add(imMessage);

        }


        //获取会话类型
        mSessionType = typeOfValue(arguments.getInt(Extras.EXTRA_TYPE, None.getValue()));
        //获取会话ID
        mSessionID = arguments.getString(Extras.EXTRA_ACCOUNT, "");
    }

    private void initViews() {
        mSendTV = findViewById(R.id.tv_send);
        //点击发送按钮，弹出选择类型(P2P/Team)的会话框
        mSendTV.setOnClickListener((v) -> {
            if (mCheckedCount < MIN_MSG_COUNT) {
                Toast.makeText(getApplicationContext(), "请选择不少于" + MIN_MSG_COUNT + "条要合并转发的消息", Toast.LENGTH_SHORT).show();
                return;
            }
            showTransFormTypeDialog();
        });

        mBackTV = findViewById(R.id.txt_back);
        mBackTV.setOnClickListener((v -> this.finish()));

        initSessionNameAndState();
        initMsgSelector();
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
            NimUIKit.startContactSelector(MsgSelectActivity.this, option, MessageListPanelEx.REQUEST_CODE_FORWARD_PERSON);
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
            NimUIKit.startContactSelector(MsgSelectActivity.this, option, MessageListPanelEx.REQUEST_CODE_FORWARD_TEAM);
        });
    }

    /**
     * 初始化选择消息的部分
     */
    private void initMsgSelector() {
        if (mItems == null || mItems.isEmpty()) {
            return;
        }
        //初始化消息列表
        mMsgSelectorRV = findViewById(R.id.rv_msg_selector);
        mMsgSelectorRV.setLayoutManager(new LinearLayoutManager(this));
        mMsgSelectorRV.requestDisallowInterceptTouchEvent(true);
        mMsgSelectorRV.setOverScrollMode(View.OVER_SCROLL_NEVER);
        Container container = new Container(this, NimUIKit.getAccount(), mSessionType, this);
        MsgAdapter adapter = new MsgAdapter(mMsgSelectorRV, mItems, container);
        adapter.setEventListener(new MsgAdapter.BaseViewHolderEventListener() {
            @Override
            public void onCheckStateChanged(int index, Boolean newState) {
                //更新数据源
                final IMMessage msg = mItems.get(index);
                if (msg.isChecked() == newState) {
                    return;
                }
                msg.setChecked(newState);
                //更新界面
                adapter.notifyItemChanged(index);
                //跟踪选中数目
                mCheckedCount += Boolean.TRUE.equals(newState) ? 1 : -1;
            }
        });
        mMsgSelectorRV.setAdapter(adapter);
//        mMsgSelectorRV.clearAnimation();
        LinearLayoutManager manager = (LinearLayoutManager) mMsgSelectorRV.getLayoutManager();
        if (manager == null) {
            mMsgSelectorRV.scrollToPosition(mSelectedPosition);
            return;
        }
        manager.scrollToPosition(Math.max(0, mSelectedPosition));
    }

    /**
     * 初始化聊天名称和状态信息。如果是P2P才有状态信息
     */
    private void initSessionNameAndState() {
        mSessionNameTV = findViewById(R.id.tv_session_name);
        mOnlineStateTV = findViewById(R.id.tv_online_state);
        //将会话ID作为默认值
        String name = MessageHelper.getInstance().getStoredNameFromSessionId(mSessionID, mSessionType);
        mSessionNameTV.setText(name == null ? mSessionID : name);
        if (mSessionType == P2P) {
            //获取在线状态
            String onlineState = NimUIKitImpl.getOnlineStateContentProvider().getSimpleDisplay(mSessionID);
            //设置状态内容
            mOnlineStateTV.setText(onlineState == null ? "" : onlineState);
            //设置状态可见
            mOnlineStateTV.setVisibility(View.VISIBLE);
        } else {
            //设置状态不可见
            mOnlineStateTV.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean sendMessage(IMMessage msg) {
        return false;
    }

    @Override
    public void onInputPanelExpand() {
    }

    @Override
    public void shouldCollapseInputPanel() {
    }

    @Override
    public boolean isLongClickEnabled() {
        return false;
    }

    @Override
    public void onItemFooterClick(IMMessage message) {
    }


    public static class CreateMessageCallbackImpl implements CreateMessageCallback {
        @Override
        public void onFinished(IMMessage message) {
        }

        @Override
        public void onFailed(int code) {
            NimLog.d(TAG, "创建消息失败, code=" + code);
        }

        @Override
        public void onException(Throwable exception) {
            NimLog.d(TAG, "创建消息异常, e=" + exception.getMessage());
        }
    }
}
