package br.com.vistoriapredial.integration.oci.genai.config;

import com.oracle.bmc.auth.AbstractAuthenticationDetailsProvider;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OciGenAiHealthControllerTest {

    @Test
    void deveExporSaudeDaConfiguracaoSemChamarOServicoExterno() throws Exception {
        OciGenAiProperties properties = new OciGenAiProperties();
        properties.setCompartmentId("ocid1.compartment.oc1..fixture");
        OciGenAiConfigCheck check = new OciGenAiConfigCheck(
                properties,
                mock(AbstractAuthenticationDetailsProvider.class));
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new OciGenAiHealthController(check))
                .build();

        mockMvc.perform(get("/api/health/ia"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("oci"))
                .andExpect(jsonPath("$.configured").value(true))
                .andExpect(jsonPath("$.authenticationAvailable").value(true))
                .andExpect(jsonPath("$.region").value("sa-saopaulo-1"))
                .andExpect(jsonPath("$.model").value("google.gemini-2.5-flash"))
                .andExpect(jsonPath("$.authMode").value("config_file"))
                .andExpect(jsonPath("$.externalCallPerformed").value(false));
    }
}
