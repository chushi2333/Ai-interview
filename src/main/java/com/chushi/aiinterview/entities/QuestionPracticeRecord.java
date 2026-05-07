package com.chushi.aiinterview.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

// 刷题记录实体，表示用户一次题目练习结果
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QuestionPracticeRecord {
    private Long id;

    private Long userId;

    private Long questionId;

    private Integer isCorrect;

    private Integer durationSeconds;

    private LocalDate practiceDate;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
