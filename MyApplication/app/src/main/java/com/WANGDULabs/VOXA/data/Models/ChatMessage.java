package com.WANGDULabs.VOXA.data.Models;

import java.util.List;
import java.util.Map;

public class ChatMessage {
    private String id;
    private String senderId;
    private String text;
    private String imageUrl;
    private long timestamp;
    private Map<String, String> reactions; // userId -> emoji
    private List<String> deliveredTo;
    private List<String> seenBy;

    public ChatMessage() {}

    public ChatMessage(String id, String senderId, String text, long timestamp) {
        this.id = id;
        this.senderId = senderId;
        this.text = text;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public String getSenderId() { return senderId; }
    public String getText() { return text; }
    public String getImageUrl() { return imageUrl; }
    public long getTimestamp() { return timestamp; }
    public Map<String, String> getReactions() { return reactions; }
    public List<String> getDeliveredTo() { return deliveredTo; }
    public List<String> getSeenBy() { return seenBy; }

    public void setId(String id) { this.id = id; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
    public void setText(String text) { this.text = text; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public void setReactions(Map<String, String> reactions) { this.reactions = reactions; }
    public void setDeliveredTo(List<String> deliveredTo) { this.deliveredTo = deliveredTo; }
    public void setSeenBy(List<String> seenBy) { this.seenBy = seenBy; }
}
