package com.netease.nim.demo.chatroom.fragment;

import android.view.View;

import com.netease.nim.uikit.session.actions.BaseAction;
import com.netease.nim.uikit.session.module.Container;
import com.netease.nim.uikit.session.module.input.InputPanel;
import com.netease.nimlib.sdk.chatroom.ChatRoomMessageBuilder;
import com.netease.nimlib.sdk.msg.model.IMMessage;

import java.util.List;

/**
 * Created by zhoujianghua on 2016/6/8.
 */
public class ChatRoomInputPanel extends InputPanel {

    public ChatRoomInputPanel(Container container, View view, List<BaseAction> actions, boolean isTextAudioSwitchShow) {
        super(container, view, actions, isTextAudioSwitchShow);
    }

    public ChatRoomInputPanel(Container container, View view, List<BaseAction> actions) {
        super(container, view, actions);
    }

    @Override
    protected IMMessage createTextMessage(String text) {
        return ChatRoomMessageBuilder.createChatRoomTextMessage(container.account, text);
    }
}
