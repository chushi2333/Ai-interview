package com.chushi.aiinterview.components;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiEmbeddingModelProviderTest {
    @Test
    void toPgVectorLiteralConvertsFloatArray() {
        var vector = new float[]{0.1f, -2.5f, 3.0f};

        var literal = AiEmbeddingModelProvider.toPgVectorLiteral(vector);

        assertThat(literal).isEqualTo("[0.1,-2.5,3.0]");
    }

    @Test
    void toPgVectorLiteralRejectsEmptyVector() {
        assertThatThrownBy(() -> AiEmbeddingModelProvider.toPgVectorLiteral(new float[]{}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("embedding vector must not be empty");
    }

    @Test
    void toPgVectorLiteralRejectsNan() {
        assertThatThrownBy(() -> AiEmbeddingModelProvider.toPgVectorLiteral(new float[]{Float.NaN}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("embedding vector contains non-finite value at index 0");
    }
}
