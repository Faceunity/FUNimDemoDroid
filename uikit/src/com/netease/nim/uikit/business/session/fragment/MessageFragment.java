package com.netease.nim.uikit.business.session.fragment;

import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.netease.nim.uikit.R;
import com.netease.nim.uikit.api.UIKitOptions;
import com.netease.nim.uikit.api.model.main.CustomPushContentProvider;
import com.netease.nim.uikit.api.model.session.SessionCustomization;
import com.netease.nim.uikit.business.ait.AitManager;
import com.netease.nim.uikit.business.session.actions.BaseAction;
import com.netease.nim.uikit.business.session.actions.ImageAction;
import com.netease.nim.uikit.business.session.actions.LocationAction;
import com.netease.nim.uikit.business.session.actions.VideoAction;
import com.netease.nim.uikit.business.session.constant.Extras;
import com.netease.nim.uikit.business.session.module.Container;
import com.netease.nim.uikit.business.session.module.ModuleProxy;
import com.netease.nim.uikit.business.session.module.input.InputPanel;
import com.netease.nim.uikit.business.session.module.list.MessageListPanelEx;
import com.netease.nim.uikit.common.fragment.TFragment;
import com.netease.nim.uikit.impl.NimUIKitImpl;
import com.netease.nimlib.sdk.NIMClient;
import com.netease.nimlib.sdk.Observer;
import com.netease.nimlib.sdk.RequestCallback;
import com.netease.nimlib.sdk.ResponseCode;
import com.netease.nimlib.sdk.msg.MessageBuilder;
import com.netease.nimlib.sdk.msg.MsgService;
import com.netease.nimlib.sdk.msg.MsgServiceObserve;
import com.netease.nimlib.sdk.msg.constant.MsgStatusEnum;
import com.netease.nimlib.sdk.msg.constant.MsgTypeEnum;
import com.netease.nimlib.sdk.msg.constant.SessionTypeEnum;
import com.netease.nimlib.sdk.msg.model.CustomMessageConfig;
import com.netease.nimlib.sdk.msg.model.IMMessage;
import com.netease.nimlib.sdk.msg.model.MemberPushOption;
import com.netease.nimlib.sdk.msg.model.MessageReceipt;
import com.netease.nimlib.sdk.robot.model.NimRobotInfo;
import com.netease.nimlib.sdk.robot.model.RobotAttachment;
import com.netease.nimlib.sdk.robot.model.RobotMsgType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 聊天界面基类
 * <p/>
 * Created by huangjun on 2015/2/1.
 */
public class MessageFragment extends TFragment implements ModuleProxy {

    private View rootView;

    private SessionCustomization customization;

    protected static final String TAG = "MessageActivity";

    // 聊天对象
    protected String sessionId; // p2p对方Account或者群id

    protected SessionTypeEnum sessionType;

    // modules
    protected InputPanel inputPanel;
    protected MessageListPanelEx messageListPanel;

    protected AitManager aitManager;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        parseIntent();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.nim_message_fragment, container, false);
        return rootView;
    }

    /**
     * ***************************** life cycle *******************************
     */

    @Override
    public void onPause() {
        super.onPause();

        NIMClient.getService(MsgService.class).setChattingAccount(MsgService.MSG_CHATTING_ACCOUNT_NONE,
                SessionTypeEnum.None);
        inputPanel.onPause();
        messageListPanel.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        messageListPanel.onResume();
        NIMClient.getService(MsgService.class).setChattingAccount(sessionId, sessionType);
        getActivity().setVolumeControlStream(AudioManager.STREAM_VOICE_CALL); // 默认使用听筒播放
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        messageListPanel.onDestroy();
        registerObservers(false);
        if (inputPanel != null) {
            inputPanel.onDestroy();
        }
        if (aitManager != null) {
            aitManager.reset();
        }
    }

    public boolean onBackPressed() {
        if (inputPanel.collapse(true)) {
            return true;
        }

        if (messageListPanel.onBackPressed()) {
            return true;
        }
        return false;
    }

    public void refreshMessageList() {
        messageListPanel.refreshMessageList();
    }

    private void parseIntent() {
        sessionId = getArguments().getString(Extras.EXTRA_ACCOUNT);
        sessionType = (SessionTypeEnum) getArguments().getSerializable(Extras.EXTRA_TYPE);
        IMMessage anchor = (IMMessage) getArguments().getSerializable(Extras.EXTRA_ANCHOR);

        customization = (SessionCustomization) getArguments().getSerializable(Extras.EXTRA_CUSTOMIZATION);
        Container container = new Container(getActivity(), sessionId, sessionType, this);

        if (messageListPanel == null) {
            messageListPanel = new MessageListPanelEx(container, rootView, anchor, false, false);
        } else {
            messageListPanel.reload(container, anchor);
        }

        if (inputPanel == null) {
            inputPanel = new InputPanel(container, rootView, getActionList());
            inputPanel.setCustomization(customization);
        } else {
            inputPanel.reload(container, customization);
        }

        initAitManager();

        inputPanel.switchRobotMode(NimUIKitImpl.getRobotInfoProvider().getRobotByAccount(sessionId) != null);

        registerObservers(true);

        if (customization != null) {
            messageListPanel.setChattingBackground(customization.backgroundUri, customization.backgroundColor);
        }
    }

    private void initAitManager() {
        UIKitOptions options = NimUIKitImpl.getOptions();
        if (options.aitEnable) {
            aitManager = new AitManager(getContext(), options.aitTeamMember && sessionType == SessionTypeEnum.Team ? sessionId : null, options.aitIMRobot);
            inputPanel.addAitTextWatcher(aitManager);
            aitManager.setTextChangeListener(inputPanel);
        }
    }

    /**
     * ************************* 消息收发 **********************************
     */
    // 是否允许发送消息
    protected boolean isAllowSendMessage(final IMMessage message) {
        return true;
    }

    /**
     * ****************** 观察者 **********************
     */

    private void registerObservers(boolean register) {
        MsgServiceObserve service = NIMClient.getService(MsgServiceObserve.class);
        service.observeReceiveMessage(incomingMessageObserver, register);
        // 已读回执监听
        if (NimUIKitImpl.getOptions().shouldHandleReceipt) {
            service.observeMessageReceipt(messageReceiptObserver, register);
        }
    }

    /**
     * 消息接收观察者
     */
    Observer<List<IMMessage>> incomingMessageObserver = new Observer<List<IMMessage>>() {
        @Override
        public void onEvent(List<IMMessage> messages) {
            if (messages == null || messages.isEmpty()) {
                return;
            }

            messageListPanel.onIncomingMessage(messages);
            sendMsgReceipt(); // 发送已读回执
        }
    };

    private Observer<List<MessageReceipt>> messageReceiptObserver = new Observer<List<MessageReceipt>>() {
        @Override
        public void onEvent(List<MessageReceipt> messageReceipts) {
            receiveReceipt();
        }
    };


    /**
     * ********************** implements ModuleProxy *********************
     */
    @Override
    public boolean sendMessage(IMMessage message) {
        if (!isAllowSendMessage(message)) {
            return false;
        }

        appendTeamMemberPush(message);
        message = changeToRobotMsg(message);
        final IMMessage msg = message;
        appendPushConfig(message);
        // send message to server and save to db
        NIMClient.getService(MsgService.class).sendMessage(message, false).setCallback(new RequestCallback<Void>() {
            @Override
            public void onSuccess(Void param) {

            }

            @Override
            public void onFailed(int code) {
                sendFailWithBlackList(code, msg);
            }

            @Override
            public void onException(Throwable exception) {

            }
        });

        messageListPanel.onMsgSend(message);

        if (aitManager != null) {
            aitManager.reset();
        }
        return true;
    }

    // 被对方拉入黑名单后，发消息失败的交互处理
    private void sendFailWithBlackList(int code, IMMessage msg) {
        if (code == ResponseCode.RES_IN_BLACK_LIST) {
            // 如果被对方拉入黑名单，发送的消息前不显示重发红点
            msg.setStatus(MsgStatusEnum.success);
            NIMClient.getService(MsgService.class).updateIMMessageStatus(msg);
            messageListPanel.refreshMessageList();
            // 同时，本地插入被对方拒收的tip消息
            IMMessage tip = MessageBuilder.createTipMessage(msg.getSessionId(), msg.getSessionType());
            tip.setContent(getActivity().getString(R.string.black_list_send_tip));
            tip.setStatus(MsgStatusEnum.success);
            CustomMessageConfig config = new CustomMessageConfig();
            config.enableUnreadCount = false;
            tip.setConfig(config);
            NIMClient.getService(MsgService.class).saveMessageToLocal(tip, true);
        }
    }

    private void appendTeamMemberPush(IMMessage message) {
        if (aitManager == null) {
            return;
        }
        if (sessionType == SessionTypeEnum.Team) {
            List<String> pushList = aitManager.getAitTeamMember();
            if (pushList == null || pushList.isEmpty()) {
                return;
            }
            MemberPushOption memberPushOption = new MemberPushOption();
            memberPushOption.setForcePush(true);
            memberPushOption.setForcePushContent(message.getContent());
            memberPushOption.setForcePushList(pushList);
            message.setMemberPushOption(memberPushOption);
        }
    }

    private IMMessage changeToRobotMsg(IMMessage message) {
        if (aitManager == null) {
            return message;
        }
        if (message.getMsgType() == MsgTypeEnum.robot) {
            return message;
        }
        if (isChatWithRobot()) {
            if (message.getMsgType() == MsgTypeEnum.text && message.getContent() != null) {
                String content = message.getContent().equals("") ? " " : message.getContent();
                message = MessageBuilder.createRobotMessage(message.getSessionId(), message.getSessionType(), message.getSessionId(), content, RobotMsgType.TEXT, content, null, null);
            }
        } else {
            String robotAccount = aitManager.getAitRobot();
            if (TextUtils.isEmpty(robotAccount)) {
                return message;
            }
            String text = message.getContent();
            String content = aitManager.removeRobotAitString(text, robotAccount);
            content = content.equals("") ? " " : content;
            message = MessageBuilder.createRobotMessage(message.getSessionId(), message.getSessionType(), robotAccount, text, RobotMsgType.TEXT, content, null, null);

        }
        return message;
    }

    private boolean isChatWithRobot() {
        return NimUIKitImpl.getRobotInfoProvider().getRobotByAccount(sessionId) != null;
    }

    private void appendPushConfig(IMMessage message) {
        CustomPushContentProvider customConfig = NimUIKitImpl.getCustomPushContentProvider();
        if (customConfig != null) {
            String content = customConfig.getPushContent(message);
            Map<String, Object> payload = customConfig.getPushPayload(message);
            if(!TextUtils.isEmpty(content)){
                message.setPushContent(content);
            }
            if (payload != null) {
                message.setPushPayload(payload);
            }
        }
    }

    @Override
    public void onInputPanelExpand() {
        messageListPanel.scrollToBottom();
    }

    @Override
    public void shouldCollapseInputPanel() {
        inputPanel.collapse(false);
    }

    @Override
    public boolean isLongClickEnabled() {
        return !inputPanel.isRecording();
    }

    @Override
    public void onItemFooterClick(IMMessage message) {
        if (aitManager == null) {
            return;
        }
        if (messageListPanel.isSessionMode()) {
            RobotAttachment attachment = (RobotAttachment) message.getAttachment();
            NimRobotInfo robot = NimUIKitImpl.getRobotInfoProvider().getRobotByAccount(attachment.getFromRobotAccount());
            aitManager.insertAitRobot(robot.getAccount(), robot.getName(), inputPanel.getEditSelectionStart());
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (aitManager != null) {
            aitManager.onActivityResult(requestCode, resultCode, data);
        }
        inputPanel.onActivityResult(requestCode, resultCode, data);
        messageListPanel.onActivityResult(requestCode, resultCode, data);
    }

    // 操作面板集合
    protected List<BaseAction> getActionList() {
        List<BaseAction> actions = new ArrayList<>();
        actions.add(new ImageAction());
        actions.add(new VideoAction());
        actions.add(new LocationAction());

        if (customization != null && customization.actions != null) {
            actions.addAll(customization.actions);
        }
        return actions;
    }

    /**
     * 发送已读回执
     */
    private void sendMsgReceipt() {
        messageListPanel.sendReceipt();
    }

    /**
     * 收到已读回执
     */
    public void receiveReceipt() {
        messageListPanel.receiveReceipt();
    }
}
