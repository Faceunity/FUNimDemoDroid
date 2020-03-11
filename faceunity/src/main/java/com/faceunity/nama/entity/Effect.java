package com.faceunity.nama.entity;

/**
 * 道具贴纸
 *
 * @author Richie on 2019.12.20
 */
public class Effect {
    private String bundleName;
    private int iconId;
    private String filePath;
    private String desc;

    public Effect(String bundleName, int iconId, String filePath, String desc) {
        this.bundleName = bundleName;
        this.iconId = iconId;
        this.filePath = filePath;
        this.desc = desc;
    }

    public String getBundleName() {
        return bundleName;
    }

    public void setBundleName(String bundleName) {
        this.bundleName = bundleName;
    }

    public int getIconId() {
        return iconId;
    }

    public void setIconId(int iconId) {
        this.iconId = iconId;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    @Override
    public String toString() {
        return "Effect{" +
                "bundleName='" + bundleName + '\'' +
                ", filePath='" + filePath + '\'' +
                ", desc='" + desc + '\'' +
                '}';
    }
}
