package com.eliteseriespay;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite:file:./target/eliteseriespay-context-test.db?busy_timeout=5000",
        "eliteseriespay.database-backup.startup-enabled=false"
})
class EliteSeriesPayApplicationTests {

    @Test
    void contextLoads() {
    }
}
