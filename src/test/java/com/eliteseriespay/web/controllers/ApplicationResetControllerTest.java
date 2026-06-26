package com.eliteseriespay.web.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.eliteseriespay.service.ApplicationResetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ApplicationResetControllerTest {

    private StubApplicationResetService applicationResetService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        applicationResetService = new StubApplicationResetService();
        mockMvc = MockMvcBuilders.standaloneSetup(new ApplicationResetController(applicationResetService)).build();
    }

    @Test
    void reset_redirectsToHomeWithSuccessMessage() throws Exception {
        mockMvc.perform(post("/reset-data"))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/"))
                .andExpect(flash().attribute("successMessage", ApplicationResetController.SUCCESS_MESSAGE));

        assertThat(applicationResetService.resetCalled).isTrue();
    }

    private static final class StubApplicationResetService extends ApplicationResetService {

        private boolean resetCalled;

        StubApplicationResetService() {
            super(null, null, null, null, null);
        }

        @Override
        public void resetAllData() {
            resetCalled = true;
        }
    }
}
