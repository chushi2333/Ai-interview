package com.chushi.aiinterview.commons.enums;

import com.chushi.aiinterview.exceptions.BusinessException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;

@Getter
public enum AiQuestionAssistType {
    SIMPLE_EXPLAIN("simple_explain", "用大白话解释当前题目"),
    INTERVIEW_ANSWER("interview_answer", "总结适合面试表达的回答模板"),
    KEY_POINTS("key_points", "提炼回答当前题目的关键点"),
    FOLLOW_UP_QUESTIONS("follow_up_questions", "生成面试官可能继续追问的问题"),
    ANSWER_POLISH("answer_polish", "把用户回答改成更适合面试的表达");

    private final String value;

    private final String description;

    AiQuestionAssistType(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public static AiQuestionAssistType fromValue(String value) {
        for (var type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new BusinessException(HttpServletResponse.SC_BAD_REQUEST, "Unsupported AI assist type");
    }
}
