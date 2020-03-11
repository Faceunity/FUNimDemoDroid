package com.faceunity.nama.entity;

import com.faceunity.nama.R;

import java.util.ArrayList;

/**
 * 道具贴纸列表
 *
 * @author Richie on 2019.12.20
 */
public enum EffectEnum {
    /**
     * 道具贴纸
     */
    Effect_none("none", R.drawable.ic_delete_all, "", ""),

    Effect_sdlu("sdlu", R.drawable.sdlu, "normal/sdlu.bundle", ""),
    Effect_daisypig("daisypig", R.drawable.daisypig, "normal/daisypig.bundle", ""),
    Effect_fashi("fashi", R.drawable.fashi, "normal/fashi.bundle", ""),
    //    Effect_chri("chri", R.drawable.chri1, "normal/chri1.bundle", ""),
    Effect_xueqiu("xueqiu", R.drawable.xueqiu_lm_fu, "normal/xueqiu_lm_fu.bundle", "");
//    Effect_wobushi("wobushi", R.drawable.wobushi, "normal/wobushi.bundle", ""),
//    Effect_gaoshiqing("gaoshiqing", R.drawable.gaoshiqing, "normal/gaoshiqing.bundle", "");

    private String bundleName;
    private int iconId;
    private String filePath;
    private String desc;

    EffectEnum(String bundleName, int iconId, String filePath, String desc) {
        this.bundleName = bundleName;
        this.iconId = iconId;
        this.filePath = filePath;
        this.desc = desc;
    }

    public Effect effect() {
        return new Effect(bundleName, iconId, filePath, desc);
    }

    public static ArrayList<Effect> getEffects() {
        EffectEnum[] effectEnums = EffectEnum.values();
        ArrayList<Effect> effects = new ArrayList<>(effectEnums.length);
        for (EffectEnum e : effectEnums) {
            effects.add(e.effect());
        }
        return effects;
    }
}
