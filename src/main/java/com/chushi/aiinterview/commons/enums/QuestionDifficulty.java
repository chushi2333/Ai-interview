package com.chushi.aiinterview.commons.enums;

public enum QuestionDifficulty {
    EASY(1),
    MEDIUM(2),
    HARD(3);

    private final int value;

    QuestionDifficulty(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
