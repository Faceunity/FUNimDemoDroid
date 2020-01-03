package com.netease.nim.uikit.common.ui.recyclerview.holder;

import androidx.recyclerview.widget.RecyclerView;

/**
 * Created by huangjun on 2016/12/11.
 */

public abstract class RecyclerViewHolder<T extends RecyclerView.Adapter, V extends BaseViewHolder, K> {
    final private T adapter;

    public RecyclerViewHolder(T adapter) {
        this.adapter = adapter;
    }

    public T getAdapter() {
        return adapter;
    }

    public abstract void convert(V holder, K data, int position, boolean isScrolling);
}
