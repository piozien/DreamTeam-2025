package tech.project.schedule.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Configuration
public class DateTimeConfig {

    private static final String DATE_FORMAT = "yyyy-MM-dd";

   
    public static class CustomLocalDateDeserializer extends JsonDeserializer<LocalDate> {
        private final LocalDateDeserializer standardDeserializer = 
                new LocalDateDeserializer(DateTimeFormatter.ofPattern(DATE_FORMAT));
        
        @Override
        public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String dateStr = p.getText();
            
            if (dateStr.contains("T")) {
                try {
                    dateStr = dateStr.substring(0, dateStr.indexOf('T'));
                    return LocalDate.parse(dateStr);
                } catch (DateTimeParseException e) {
                }
            }
            
            return standardDeserializer.deserialize(p, ctxt);
        }
    }
    
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
        return builder -> {
            builder.simpleDateFormat(DATE_FORMAT);
            builder.serializers(new LocalDateSerializer(DateTimeFormatter.ofPattern(DATE_FORMAT)));
            builder.deserializers(new CustomLocalDateDeserializer());
        };
    }
}
