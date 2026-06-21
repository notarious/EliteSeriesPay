package com.eliteseriespay.domain.converter;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class LocalDateStringConverterTest {

    private final LocalDateStringConverter converter = new LocalDateStringConverter();

    @Test
    void convertToDatabaseColumn_writesIsoDateString() {
        assertThat(converter.convertToDatabaseColumn(LocalDate.of(2026, 6, 21)))
                .isEqualTo("2026-06-21");
    }

    @Test
    void convertToEntityAttribute_readsIsoDateString() {
        assertThat(converter.convertToEntityAttribute("2026-06-21"))
                .isEqualTo(LocalDate.of(2026, 6, 21));
    }

    @Test
    void convertsNullValues() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }
}
