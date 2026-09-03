package com.studyagent.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IdleRecord {

    private long id;
    private long activityId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private boolean uploaded;
}