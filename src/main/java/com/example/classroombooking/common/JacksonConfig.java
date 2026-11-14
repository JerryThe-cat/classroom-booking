package com.example.classroombooking.common;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.module.SimpleModule;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Configuration
public class JacksonConfig {
    private static final DateTimeFormatter OUTPUT_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    @Bean
    public JsonMapperBuilderCustomizer localDateTimeOffsetCustomizer() {
        SimpleModule module = new SimpleModule("ClassroomDateTimeModule");
        module.addSerializer(LocalDateTime.class, new ValueSerializer<>() {
            @Override
            public void serialize(
                    LocalDateTime value,
                    JsonGenerator gen,
                    SerializationContext context
            ) throws tools.jackson.core.JacksonException {
                gen.writeString(value.atZone(TimeUtils.APP_ZONE).format(OUTPUT_FORMAT));
            }
        });
        return builder -> builder.addModule(module);
    }
}
