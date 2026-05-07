package com.chushi.aiinterview.commons.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

public final class QuestionSelfTestAnswerUtils {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> STRING_LIST_TYPE_REFERENCE = new TypeReference<>() {
    };

    private QuestionSelfTestAnswerUtils() {
    }

    public static String toJson(List<String> answers) {
        try {
            return OBJECT_MAPPER.writeValueAsString(answers == null ? List.of() : answers);
        } catch (Exception e) {
            throw new IllegalArgumentException("Serialize self test answers failed", e);
        }
    }

    public static List<String> parse(String answers) {
        if (answers == null || answers.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return OBJECT_MAPPER.readValue(answers, STRING_LIST_TYPE_REFERENCE);
        } catch (Exception e) {
            throw new IllegalArgumentException("Parse self test answers failed", e);
        }
    }
}
