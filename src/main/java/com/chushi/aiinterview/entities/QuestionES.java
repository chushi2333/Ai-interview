package com.chushi.aiinterview.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(indexName = "question")
public class QuestionES {
    @Id
    @Field(type = FieldType.Long, index = false)
    private Long id;

    @Field(type = FieldType.Text)
    private String title;

    @Field(type = FieldType.Text)
    private String content;

    @Field(type = FieldType.Text)
    private String answer;

    @Field(type = FieldType.Keyword)
    private List<String> tags;

    @Field(type = FieldType.Integer)
    private Integer difficulty;

    @Field(type = FieldType.Integer, index = false)
    private Integer isMemberOnly;

    @Field(type = FieldType.Long, index = false)
    private Long userId;

    // ES 侧按真正的日期字段建模，避免 Long + Date 组合带来的映射 warning
    @Field(type = FieldType.Date, format = DateFormat.date_time)
    private Instant createdAt;

    public static QuestionES fromQuestion(Question question, List<String> tags) {
        return QuestionES.builder()
                .id(question.getId())
                .title(question.getTitle())
                .content(question.getContent())
                .answer(question.getAnswer())
                .tags(tags)
                .difficulty(question.getDifficulty())
                .isMemberOnly(question.getIsMemberOnly())
                .userId(question.getUserId())
                .createdAt(question.getCreateTime() == null ? null : question.getCreateTime().atZone(ZoneId.systemDefault()).toInstant())
                .build();
    }
}
