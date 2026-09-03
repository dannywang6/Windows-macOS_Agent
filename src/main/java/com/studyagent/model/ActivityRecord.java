package com.studyagent.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class ActivityRecord {

    private long id;
    private String app;
    private String title;
    private String url;
    private String category;
    private String subcategory;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private boolean uploaded;

    /** 与活动关联的 Idle 记录，仅用于上传序列化，不落库 */
    @JsonIgnore
    private List<IdleRecord> idleRecords = new ArrayList<>();

    public ActivityRecord(long id, String app, String title, String url,
                          String category, String subcategory,
                          LocalDateTime startTime, LocalDateTime endTime,
                          boolean uploaded) {
        this.id = id;
        this.app = app;
        this.title = title;
        this.url = url;
        this.category = category;
        this.subcategory = subcategory;
        this.startTime = startTime;
        this.endTime = endTime;
        this.uploaded = uploaded;
        this.idleRecords = new ArrayList<>();
    }
}
