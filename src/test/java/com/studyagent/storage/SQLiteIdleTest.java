package com.studyagent.storage;

import com.studyagent.model.ActivityRecord;
import com.studyagent.model.IdleRecord;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SQLiteIdleTest {

    @Test
    void saveActivityWithIdleAndAssociate() throws Exception {
        Path tmp = Files.createTempFile("studyagent-test", ".db");
        Files.deleteIfExists(tmp);

        SQLiteManager db = new SQLiteManager(tmp.toString());
        db.init();

        // 保存活动
        ActivityRecord act = new ActivityRecord(
                0L, "chrome", "Java教程 - YouTube", "https://youtube.com",
                "学习", "Java教程 - YouTube",
                LocalDateTime.of(2026, 9, 4, 10, 0),
                LocalDateTime.of(2026, 9, 4, 11, 0), false);
        db.save(act);
        long actId = act.getId();
        assertTrue(actId > 0, "活动应生成自增 id");

        // 关联两条 idle 到该活动
        db.saveIdle(new IdleRecord(0L, actId,
                LocalDateTime.of(2026, 9, 4, 10, 20),
                LocalDateTime.of(2026, 9, 4, 10, 35), false));
        db.saveIdle(new IdleRecord(0L, actId,
                LocalDateTime.of(2026, 9, 4, 10, 45),
                LocalDateTime.of(2026, 9, 4, 10, 50), false));

        // 查询该活动的 idle
        var map = db.findIdleByActivityIds(List.of(actId));
        List<IdleRecord> idles = map.get(actId);
        assertEquals(2, idles.size());
        assertEquals(actId, idles.get(0).getActivityId());

        // 标记上传
        db.markIdleUploaded(idles.get(0).getId());

        // 已上传清理
        int deleted = db.deleteUploadedIdleBefore(LocalDateTime.of(2026, 9, 5, 0, 0));
        assertEquals(1, deleted, "已上传且 end_time 更早的 idle 应被清理");

        db.close();
        Files.deleteIfExists(tmp);
    }
}