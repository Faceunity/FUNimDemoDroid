/*
 * Copyright (C) 2008 Esmertec AG.
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.nim.demo.location.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.netease.nim.demo.R;

import java.util.List;


/**
 * An adapter to store icons.
 */
public class IconListAdapter extends ArrayAdapter<IconListAdapter.IconListItem> {
    protected LayoutInflater mInflater;
    private static final int mResource = R.layout.icon_list_item;

    public IconListAdapter(Context context,
                           List<IconListItem> items) {
        super(context, mResource, items);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView text;
        ImageView image;

        View view;
        if (convertView == null) {
            view = mInflater.inflate(mResource, parent, false);
        } else {
            view = convertView;
        }

        // Set text field
        text = (TextView) view.findViewById(R.id.text1);
        text.setText(getItem(position).getTitle());

        // Set resource icon
        image = (ImageView) view.findViewById(R.id.icon);
        image.setBackgroundDrawable(getItem(position).getResource());

        return view;
    }

    public static class IconListItem {
        private final String mTitle;
        private final Drawable mResource;
        private final Object mAttach;

        public IconListItem(String title, Drawable resource, Object attach) {
            mResource = resource;
            mTitle = title;
            mAttach = attach;
        }

        public String getTitle() {
            return mTitle;
        }

        public Drawable getResource() {
            return mResource;
        }

        public Object getAttach() {
            return mAttach;
        }
    }
}
