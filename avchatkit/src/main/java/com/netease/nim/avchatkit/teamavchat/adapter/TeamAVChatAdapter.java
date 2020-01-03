package com.netease.nim.avchatkit.teamavchat.adapter;

import androidx.recyclerview.widget.RecyclerView;

import com.netease.nim.avchatkit.R;
import com.netease.nim.avchatkit.common.recyclerview.adapter.BaseMultiItemFetchLoadAdapter;
import com.netease.nim.avchatkit.common.recyclerview.holder.BaseViewHolder;
import com.netease.nim.avchatkit.common.recyclerview.holder.RecyclerViewHolder;
import com.netease.nim.avchatkit.teamavchat.holder.TeamAVChatEmptyViewHolder;
import com.netease.nim.avchatkit.teamavchat.holder.TeamAVChatItemViewHolder;
import com.netease.nim.avchatkit.teamavchat.module.TeamAVChatItem;
import com.netease.nrtc.video.render.IVideoRender;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by huangjun on 2017/5/4.
 */

public class TeamAVChatAdapter extends BaseMultiItemFetchLoadAdapter<TeamAVChatItem, BaseViewHolder> {

    private static final int VIEW_TYPE_DATA = 1;
    private static final int VIEW_TYPE_ADD = 2;
    private static final int VIEW_TYPE_HOLDER = 3;

    private Map<Class<? extends RecyclerViewHolder>, Integer> holder2ViewType;

    public TeamAVChatAdapter(RecyclerView recyclerView, List<TeamAVChatItem> data) {
        super(recyclerView, data);

        holder2ViewType = new HashMap<>();
        addItemType(VIEW_TYPE_DATA, R.layout.team_avchat_item, TeamAVChatItemViewHolder.class);
        addItemType(VIEW_TYPE_HOLDER, R.layout.team_avchat_holder, TeamAVChatEmptyViewHolder.class);
        holder2ViewType.put(TeamAVChatItemViewHolder.class, VIEW_TYPE_DATA);
        holder2ViewType.put(TeamAVChatEmptyViewHolder.class, VIEW_TYPE_HOLDER);
    }

    @Override
    protected int getViewType(TeamAVChatItem item) {
        if (item.type == TeamAVChatItem.TYPE.TYPE_DATA) {
            return VIEW_TYPE_DATA;
        } else if (item.type == TeamAVChatItem.TYPE.TYPE_HOLDER) {
            return VIEW_TYPE_HOLDER;
        } else {
            return VIEW_TYPE_ADD;
        }
    }

    @Override
    protected String getItemKey(TeamAVChatItem item) {
        return item.type + "_" + item.teamId + "_" + item.account;
    }

    public IVideoRender getViewHolderSurfaceView(TeamAVChatItem item) {
        RecyclerViewHolder holder = getViewHolder(VIEW_TYPE_DATA, getItemKey(item));
        if (holder instanceof TeamAVChatItemViewHolder) {
            return ((TeamAVChatItemViewHolder) holder).getSurfaceView();
        }

        return null;
    }

    public void updateVolumeBar(TeamAVChatItem item) {
        RecyclerViewHolder holder = getViewHolder(VIEW_TYPE_DATA, getItemKey(item));
        if (holder instanceof TeamAVChatItemViewHolder) {
            ((TeamAVChatItemViewHolder) holder).updateVolume(item.volume);
        }
    }
}
