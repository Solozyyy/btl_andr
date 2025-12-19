package com.example.btlandr.model;

import java.util.List;

public class Group {
    private String id;
    private String name;
    private String adminId;
    private String adminEmail; // thêm trường email admin
    private List<String> members;
    private long createdAt;

    public Group() {
        // bắt buộc có constructor rỗng cho Firestore
    }

    // getter/setter
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAdminId() {
        return adminId;
    }

    public void setAdminId(String adminId) {
        this.adminId = adminId;
    }

    public String getAdminEmail() {
        return adminEmail;
    } // thêm getter

    public void setAdminEmail(String adminEmail) {
        this.adminEmail = adminEmail;
    } // setter

    public List<String> getMembers() {
        return members;
    }

    public void setMembers(List<String> members) {
        this.members = members;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
