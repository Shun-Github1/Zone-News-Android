package com.anssy.znewspro.entry;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.io.Serializable;

/**
 * @Author yulu
 * @Date 2023/3/14
 */
public class TagEntry implements Parcelable {
    private String tagName;
    private boolean tagCheck;
    private String level;
    private int tagId;
    private int parentId;

    public TagEntry(String tagName, boolean tagCheck) {
        this.tagName = tagName;
        this.tagCheck = tagCheck;
    }

    protected TagEntry(Parcel in) {
        tagName = in.readString();
        tagCheck = in.readByte() != 0;
        level = in.readString();
        tagId = in.readInt();
        parentId = in.readInt();
    }

    public static final Creator<TagEntry> CREATOR = new Creator<TagEntry>() {
        @Override
        public TagEntry createFromParcel(Parcel in) {
            return new TagEntry(in);
        }

        @Override
        public TagEntry[] newArray(int size) {
            return new TagEntry[size];
        }
    };

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public boolean isTagCheck() {
        return tagCheck;
    }

    public void setTagCheck(boolean tagCheck) {
        this.tagCheck = tagCheck;
    }

    @Override
    public String toString() {
        return "TagEntry{" +
                "tagName='" + tagName + '\'' +
                ", tagCheck=" + tagCheck +
                ", tagId=" + tagId +
                '}';
    }

    public int getTagId() {
        return tagId;
    }

    public void setTagId(int tagId) {
        this.tagId = tagId;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public int getParentId() {
        return parentId;
    }

    public void setParentId(int parentId) {
        this.parentId = parentId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(tagName);
        dest.writeByte((byte) (tagCheck ? 1 : 0));
        dest.writeString(level);
        dest.writeInt(tagId);
        dest.writeInt(parentId);
    }
}
