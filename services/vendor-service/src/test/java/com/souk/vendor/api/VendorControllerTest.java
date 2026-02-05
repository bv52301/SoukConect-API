package com.souk.vendor.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.souk.common.domain.Vendor;
import com.souk.common.domain.Address;
import com.souk.common.port.DataAccessPort;
import com.souk.common.adapters.jpa.repository.VendorRepository;
import com.souk.vendor.api.dto.VendorCreateRequest;
import com.souk.vendor.api.dto.VendorUpdateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Optional;
import java.math.BigDecimal;

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

    @MockitoBean
    private DataAccessPort<Vendor, Long> vendorPort;

    @MockitoBean
    private VendorRepository vendorRepository;

    @MockitoBean
    private DataAccessPort<Address, Long> addressPort;

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
        // VendorCreateRequest(name, description, supportedCategories, schedule, image,
        // address1, address2, state, landmark, pincode, contactName, phoneNumber,
        // email, latitude, longitude)
        VendorCreateRequest req = new VendorCreateRequest(
                "New Vendor",
                "Desc",
                null,
                null,
                "image.jpg",
                "Addr1",
                "Addr2",
                "State",
                "Landmark",
                "123456",
                "Contact Name",
                "1234567890",
                "test@example.com",
                BigDecimal.ZERO,
                BigDecimal.ZERO);

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
        // VendorUpdateRequest has same fields
        VendorUpdateRequest req = new VendorUpdateRequest(
                "Updated Vendor",
                "Desc",
                null,
                null,
                "image.jpg",
                "Addr1",
                "Addr2",
                "State",
                "Landmark",
                "123456",
                "Contact Name",
                "9876543210",
                "update@example.com",
                BigDecimal.ZERO,
                BigDecimal.ZERO);

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
                .content(objectMapper.writeValueAsString(req)))
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
