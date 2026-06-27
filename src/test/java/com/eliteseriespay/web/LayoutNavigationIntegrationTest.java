package com.eliteseriespay.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite:file:./target/eliteseriespay-layout-test.db?busy_timeout=5000",
        "eliteseriespay.database-backup.startup-enabled=false"
})
class LayoutNavigationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void homePage_rendersSuccessfullyWithActiveHomeNav() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("nav-link active")))
                .andExpect(content().string(containsString(">Главная</a>")));
    }

    @Test
    void projectsPage_rendersSuccessfullyWithActiveProjectsNav() throws Exception {
        mockMvc.perform(get("/projects"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/projects\"")))
                .andExpect(content().string(containsString("nav-link active")))
                .andExpect(content().string(containsString(">Проекты</a>")));
    }
}
