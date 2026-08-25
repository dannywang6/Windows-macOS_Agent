package com.studyagent.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityRecord {

    private long id;
    private String app;
    private String title;
    private String url;
    private String category;
    private LocalDateTime startTime;
    private long durationMillis;
    private boolean uploaded;
}
