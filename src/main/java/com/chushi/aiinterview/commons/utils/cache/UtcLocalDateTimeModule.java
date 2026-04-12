package com.chushi.aiinterview.commons.utils.cache;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class UtcLocalDateTimeModule extends SimpleModule {
    public UtcLocalDateTimeModule() {
        addSerializer(LocalDateTime.class, new UtcLocalDateTimeSerializer());
        addDeserializer(LocalDateTime.class, new UtcLocalDateTimeDeserializer());
    }

    public static class UtcLocalDateTimeSerializer extends JsonSerializer<LocalDateTime> {
        @Override
        public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            if (value == null) {
                gen.writeNull();
            } else {
                long timestamp = value.atOffset(ZoneOffset.UTC).toInstant().toEpochMilli();
                gen.writeNumber(timestamp);
            }
        }
    }

    public static class UtcLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {
        @Override
        public LocalDateTime deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
            long timestamp = p.getValueAsLong();
            return Instant.ofEpochMilli(timestamp).atOffset(ZoneOffset.UTC).toLocalDateTime();
        }
    }
}
