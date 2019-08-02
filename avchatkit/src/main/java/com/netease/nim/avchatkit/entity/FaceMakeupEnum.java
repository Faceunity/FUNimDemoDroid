package com.netease.nim.avchatkit.entity;

import android.util.Pair;
import android.util.SparseArray;

import com.faceunity.beautycontrolview.entity.FaceMakeup;
import com.faceunity.beautycontrolview.entity.Filter;
import com.faceunity.beautycontrolview.entity.MakeupItem;
import com.netease.nim.avchatkit.R;

import java.util.ArrayList;
import java.util.List;

/**
 * @author LiuQiang on 2018.11.12
 * 口红用 JSON 表示，其他都是图片
 */
public enum FaceMakeupEnum {

    /**
     * 美妆项，前几项是预置的效果
     * 排在列表最前方，顺序为桃花妆、雀斑妆、朋克妆（其中朋克没有腮红，3个妆容的眼线、眼睫毛共用1个的）
     */

    // 腮红
    MAKEUP_BLUSHER_01("MAKEUP_BLUSHER_01", "makeup/mu_blush_01.png", FaceMakeup.FACE_MAKEUP_TYPE_BLUSHER, 0, R.string.makeup_radio_blusher, false),
    // 眼影
    MAKEUP_EYE_SHADOW_01("MAKEUP_EYESHADOW_01", "makeup/mu_eyeshadow_01.png", FaceMakeup.FACE_MAKEUP_TYPE_EYE_SHADOW, 0, R.string.makeup_radio_eye_shadow, false),
    // 眉毛
    MAKEUP_EYEBROW_01("MAKEUP_EYEBROW_01", "makeup/mu_eyebrow_01.png", FaceMakeup.FACE_MAKEUP_TYPE_EYEBROW, 0, R.string.makeup_radio_eyebrow, false),
    // 口红
    MAKEUP_LIPSTICK_01("MAKEUP_LIPSTICK_01", "makeup/mu_lip_01.json", FaceMakeup.FACE_MAKEUP_TYPE_LIPSTICK, 0, R.string.makeup_radio_lipstick, false);

    // 每个妆容默认强度 0.7，对应至尊美颜效果
    public static final float DEFAULT_BATCH_MAKEUP_LEVEL = 0.7f;

    private String name;
    private String path;
    private int type;
    private int iconId;
    private int strId;
    /**
     * 妆容组合 整体强度
     */
    public final static SparseArray<Float> MAKEUP_OVERALL_LEVEL = new SparseArray<>();
    /**
     * http://confluence.faceunity.com/pages/viewpage.action?pageId=20332259
     * 妆容和滤镜的组合
     */
    public static final SparseArray<Pair<Filter, Float>> MAKEUP_FILTERS = new SparseArray<>(16);

    private boolean showInMakeup;

    FaceMakeupEnum(String name, String path, int type, int iconId, int strId, boolean showInMakeup) {
        this.name = name;
        this.path = path;
        this.type = type;
        this.iconId = iconId;
        this.strId = strId;
        this.showInMakeup = showInMakeup;
    }

    /**
     * 美颜模块的美妆组合 资源和顺序为：桃花、西柚、清透、男友
     *
     * @return
     */
    public static List<FaceMakeup> getBeautyFaceMakeup() {
        List<FaceMakeup> faceMakeups = new ArrayList<>();
        FaceMakeup none = new FaceMakeup(null, R.string.makeup_radio_remove, R.drawable.makeup_none_normal);
        faceMakeups.add(none);
        // 桃花
        List<MakeupItem> peachBlossomMakeups = new ArrayList<>(8);
        peachBlossomMakeups.add(MAKEUP_BLUSHER_01.faceMakeup(1.0f));
        peachBlossomMakeups.add(MAKEUP_EYE_SHADOW_01.faceMakeup(1.0f));
        peachBlossomMakeups.add(MAKEUP_EYEBROW_01.faceMakeup(1.0f));
        peachBlossomMakeups.add(MAKEUP_LIPSTICK_01.faceMakeup(1.0f));
        FaceMakeup peachBlossom = new FaceMakeup(peachBlossomMakeups, R.string.makeup_peach_blossom, 0);
        faceMakeups.add(peachBlossom);
        return faceMakeups;
    }

    public MakeupItem faceMakeup() {
        return new MakeupItem(name, path, type, strId, iconId);
    }

    public MakeupItem faceMakeup(float level) {
        return new MakeupItem(name, path, type, strId, iconId, level);
    }
}