package com.sodi.metareports.configuration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = com.sodi.metareports.shared.web.HomeController.class)
@Import(SecurityConfiguration.class)
class SecurityConfigurationTest {
    @Autowired MockMvc mockMvc;

    @Test
    void healthEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isNotFound());
    }

    @Test
    void applicationRoutesRequireAuthentication() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().is3xxRedirection());
    }
}
