package com.souk.vendor.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.souk.common.domain.Vendor;
import com.souk.common.domain.VendorLocation;
import com.souk.common.port.DataAccessPort;
import com.souk.common.adapters.jpa.repository.VendorRepository;
import com.souk.vendor.api.dto.VendorCreateRequest;
import com.souk.vendor.api.dto.VendorUpdateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VendorController.class)
@AutoConfigureMockMvc(addFilters = false)
public class VendorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DataAccessPort<Vendor, Long> vendorPort;

    @MockBean
    private VendorRepository vendorRepository;

    @MockBean
    private DataAccessPort<VendorLocation, Long> vendorLocationPort;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void listAll_ShouldReturnList() throws Exception {
        Vendor v = new Vendor();
        v.setVendorId(1L);
        v.setName("Test Vendor");

        when(vendorPort.findAll()).thenReturn(Arrays.asList(v));

        mockMvc.perform(get("/vendors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Test Vendor"));
    }

    @Test
    void getById_WhenExists_ShouldReturnVendor() throws Exception {
        Vendor v = new Vendor();
        v.setVendorId(1L);
        v.setName("Test Vendor");

        when(vendorPort.findById(1L)).thenReturn(Optional.of(v));

        mockMvc.perform(get("/vendors/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Vendor"));
    }

    @Test
    void create_ShouldReturnCreated() throws Exception {
        // VendorCreateRequest needs to match constructor or setters used by Jackson
        // Assuming record or POJO.
        VendorCreateRequest req = new VendorCreateRequest(
                "New Vendor", "user@example.com", "1234567890", "Desc", "A1", "A2", "State", "City", "LM", "123123",
                "Contact", null, null, null, null, null);

        Vendor saved = new Vendor();
        saved.setVendorId(1L);
        saved.setName("New Vendor");

        when(vendorPort.save(any(Vendor.class))).thenReturn(saved);

        mockMvc.perform(post("/vendors")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("New Vendor"));
    }

    @Test
    void update_WhenExists_ShouldReturnUpdated() throws Exception {
        // Minimal request object for update
        String json = """
                    {
                        "name": "Updated Vendor",
                        "email": "update@example.com",
                        "phoneNumber": "9876543210"
                    }
                """;

        Vendor existing = new Vendor();
        existing.setVendorId(1L);
        existing.setName("Old Vendor");

        Vendor saved = new Vendor();
        saved.setVendorId(1L);
        saved.setName("Updated Vendor");

        when(vendorPort.findById(1L)).thenReturn(Optional.of(existing));
        when(vendorPort.save(any(Vendor.class))).thenReturn(saved);

        mockMvc.perform(put("/vendors/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Vendor"));
    }

    @Test
    void delete_WhenExists_ShouldReturnNoContent() throws Exception {
        Vendor v = new Vendor();
        v.setVendorId(1L);

        when(vendorPort.findById(1L)).thenReturn(Optional.of(v));
        doNothing().when(vendorPort).deleteById(1L);

        mockMvc.perform(delete("/vendors/1"))
                .andExpect(status().isNoContent());
    }
}
