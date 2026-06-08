package org.example.backend.controller;

public class ResourceRequest {
    private String title;
    private String description;
    private Boolean isPrivate;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String content) {
        this.description = content;
    }

    public Boolean getPrivate() {
        return isPrivate;
    }

    public void setTitle(Boolean isPrivate) {
        this.isPrivate = isPrivate;
    }
}

