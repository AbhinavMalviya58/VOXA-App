package com.WANGDULabs.VOXA.data.Models;

public class SearchUser {
    private String uid;
    private String displayName;
    private String userId_lc;
    private String photoUrl;

    public SearchUser() {}

    public SearchUser(String uid, String displayName, String userId_lc, String photoUrl) {
        this.uid = uid;
        this.displayName = displayName;
        this.userId_lc = userId_lc;
        this.photoUrl = photoUrl;
    }

    public String getUid() { return uid; }
    public String getDisplayName() { return displayName; }
    public String getUserId_lc() { return userId_lc; }
    public String getPhotoUrl() { return photoUrl; }

    public void setUid(String uid) { this.uid = uid; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public void setUserId_lc(String userId_lc) { this.userId_lc = userId_lc; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
}
