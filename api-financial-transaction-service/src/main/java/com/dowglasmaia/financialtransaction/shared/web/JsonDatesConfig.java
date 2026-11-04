package com.dowglasmaia.financialtransaction.shared.web;

import com.fasterxml.jackson.databind.SerializationFeature;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Dates go out as ISO-8601 strings, as the contract says ("2026-03-11", "2026-03-11T12:00:00Z"). With {@code @EnableWebMvc}
 * Spring Boot's Jackson properties are not applied to the MVC converters, and the default would write epoch numbers and
 * [y,m,d] arrays.
 */
@Configuration
class JsonDatesConfig implements WebMvcConfigurer {

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.stream()
              .filter(MappingJackson2HttpMessageConverter.class::isInstance)
              .map(MappingJackson2HttpMessageConverter.class::cast)
              .forEach(c -> c.getObjectMapper().disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS));
    }
}
