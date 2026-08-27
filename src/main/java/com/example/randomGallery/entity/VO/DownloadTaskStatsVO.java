package com.example.randomGallery.entity.VO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DownloadTaskStatsVO {
    private long total;
    private long waitingCount;
    private long completedCount;
    private long failedCount;
}
