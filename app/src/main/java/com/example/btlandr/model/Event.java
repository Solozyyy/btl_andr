package com.example.btlandr.model;

public class Event {
    private String id;
    private String title;
    private String note;
    private long startTime;
    private long endTime;
    private String category = "Cá nhân";
    private boolean important; // ⭐ Thêm trường mới
    private boolean done = false; // Đánh dấu hoàn thành

    // Constructor mặc định (bắt buộc cho Firestore)
    public Event() {
    }


    // Constructor đầy đủ
    public Event(String title, String note, long startTime, long endTime, String category) {
        this.title = title;
        this.note = note;
        this.startTime = startTime;
        this.endTime = endTime;
        this.category = category;
        this.important = false; // Mặc định không quan trọng
        this.done = false;
    }

    // Constructor với important
    public Event(String title, String note, long startTime, long endTime, String category, boolean important) {
        this.title = title;
        this.note = note;
        this.startTime = startTime;
        this.endTime = endTime;
        this.category = category;
        this.important = important;
        this.done = false;
    }

    // Constructor với done
    public Event(String title, String note, long startTime, long endTime, String category, boolean important, boolean done) {
        this.title = title;
        this.note = note;
        this.startTime = startTime;
        this.endTime = endTime;
        this.category = category;
        this.important = important;
        this.done = done;
    }
    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    // Getters và Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public boolean isImportant() {
        return important;
    }

    public void setImportant(boolean important) {
        this.important = important;
    }
}