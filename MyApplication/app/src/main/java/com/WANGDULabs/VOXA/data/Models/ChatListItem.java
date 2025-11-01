package com.WANGDULabs.VOXA.data.Models;

public class ChatListItem {
    private String conversationId;
    private String otherUid;
    private String displayName;
    private String photoUrl;
    private String lastMessage;
    private long updatedAt;

    public ChatListItem() {}

    public ChatListItem(String conversationId, String otherUid, String displayName, String photoUrl, String lastMessage, long updatedAt) {
        this.conversationId = conversationId;
        this.otherUid = otherUid;
        this.displayName = displayName;
        this.photoUrl = photoUrl;
        this.lastMessage = lastMessage;
        this.updatedAt = updatedAt;
    }

    public String getConversationId() { return conversationId; }
    public String getOtherUid() { return otherUid; }
    public String getDisplayName() { return displayName; }
    public String getPhotoUrl() { return photoUrl; }
    public String getLastMessage() { return lastMessage; }
    public long getUpdatedAt() { return updatedAt; }

    public void setConversationId(String conversationId) { this.conversationId = conversationId; }
    public void setOtherUid(String otherUid) { this.otherUid = otherUid; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
