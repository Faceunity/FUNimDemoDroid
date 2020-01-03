package com.netease.nim.uikit.business.session.helper;

import com.netease.nim.uikit.api.NimUIKit;
import com.netease.nim.uikit.api.wrapper.MessageRevokeTip;
import com.netease.nimlib.sdk.NIMClient;
import com.netease.nimlib.sdk.msg.MessageBuilder;
import com.netease.nimlib.sdk.msg.MsgService;
import com.netease.nimlib.sdk.msg.constant.MsgStatusEnum;
import com.netease.nimlib.sdk.msg.constant.SessionTypeEnum;
import com.netease.nimlib.sdk.msg.model.CustomMessageConfig;
import com.netease.nimlib.sdk.msg.model.IMMessage;
import com.netease.nimlib.sdk.superteam.SuperTeam;
import com.netease.nimlib.sdk.team.model.Team;
import com.netease.nimlib.sdk.uinfo.UserService;
import com.netease.nimlib.sdk.uinfo.model.NimUserInfo;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by hzxuwen on 2016/8/19.
 */
public class MessageHelper {
    private static final String TAG = "MessageHelper";

    public static MessageHelper getInstance() {
        return InstanceHolder.instance;
    }

    static class InstanceHolder {
        final static MessageHelper instance = new MessageHelper();
    }

    // 消息撤回
    public void onRevokeMessage(IMMessage item, String revokeAccount) {
        if (item == null) {
            return;
        }

        IMMessage message = MessageBuilder.createTipMessage(item.getSessionId(), item.getSessionType());
        message.setContent(MessageRevokeTip.getRevokeTipContent(item, revokeAccount));
        message.setStatus(MsgStatusEnum.success);
        CustomMessageConfig config = new CustomMessageConfig();
        config.enableUnreadCount = false;
        message.setConfig(config);
        NIMClient.getService(MsgService.class).saveMessageToLocalEx(message, true, item.getTime());
    }

    /**
     * 从 mItems 按顺序取出被勾选的消息
     *
     * @return 被勾选的消息
     */
    public LinkedList<IMMessage> getCheckedItems(List<IMMessage> items) {
        LinkedList<IMMessage> checkedList = new LinkedList<>();
        for (IMMessage msg : items) {
            if (msg.isChecked()) {
                checkedList.add(msg);
            }
        }
        return checkedList;
    }

    /**
     * 通过id和type，从本地存储中查询对应的群名或用户名
     *
     * @param id          群或用户的id
     * @param sessionType 会话类型
     * @return id对应的昵称
     */
    public String getStoredNameFromSessionId(final String id, final SessionTypeEnum sessionType) {
        switch (sessionType) {
            case P2P:
                //读取对方用户名称
                NimUserInfo userInfo = NIMClient.getService(UserService.class).getUserInfo(id);
                if (userInfo == null) {
                    return null;
                }
                return userInfo.getName();
            case Team:
                //获取群信息
                Team team = NimUIKit.getTeamProvider().getTeamById(id);
                if (team == null) {
                    return null;
                }
                return team.getName();
            case SUPER_TEAM:
                //获取群信息
                SuperTeam superTeam = NimUIKit.getSuperTeamProvider().getTeamById(id);
                if (superTeam == null) {
                    return null;
                }
                return superTeam.getName();
            default:
                return null;
        }
    }

}
