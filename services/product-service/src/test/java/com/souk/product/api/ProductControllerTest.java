package com.souk.product.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.souk.common.domain.Product;
import com.souk.common.domain.Vendor;
import com.souk.common.domain.VendorLocation;
import com.souk.common.domain.ProductMedia;
import com.souk.common.port.DataAccessPort;
import com.souk.common.port.ProductQueryPort;
import com.souk.product.api.dto.ProductCreateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
public class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DataAccessPort<Product, Long> productPort;

    @MockitoBean
    private ProductQueryPort productQueryPort;

    @MockitoBean
    private DataAccessPort<ProductMedia, Long> mediaPort;

    @MockitoBean
    private DataAccessPort<Vendor, Long> vendorPort;

    @MockitoBean
    private DataAccessPort<VendorLocation, Long> vendorLocationPort;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void listAll_ShouldReturnList() throws Exception {
        Product p = new Product();
        p.setId(1L);
        p.setVendorId(10L);
        p.setName("Test Product");

        Vendor v = new Vendor();
        v.setVendorId(10L);

        when(productPort.findAll()).thenReturn(Arrays.asList(p));
        when(vendorPort.findById(10L)).thenReturn(Optional.of(v));

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Test Product"));
    }

    @Test
    void getById_WhenExists_ShouldReturnProduct() throws Exception {
        Product p = new Product();
        p.setId(1L);
        p.setVendorId(10L);
        p.setName("Test Product");

        Vendor v = new Vendor();
        v.setVendorId(10L);

        when(productPort.findById(1L)).thenReturn(Optional.of(p));
        when(vendorPort.findById(10L)).thenReturn(Optional.of(v));

        mockMvc.perform(get("/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Product"));
    }

    @Test
    void create_ShouldReturnCreated() throws Exception {
        // ProductCreateRequest(name, sku, price, vendorId, available, description,
        // categoryDetails, schedule, useVendorSchedule, locations, media)
        ProductCreateRequest req = new ProductCreateRequest(
                "Test Product",
                "SKU123",
                new BigDecimal("10.00"),
                10L,
                true,
                "Description",
                null,
                null,
                false,
                null,
                null);

        Product saved = new Product();
        saved.setId(1L);
        saved.setVendorId(10L);
        saved.setName("Test Product");
        saved.setSku("SKU123");

        Vendor v = new Vendor();
        v.setVendorId(10L);

        when(productPort.save(any(Product.class))).thenReturn(saved);
        when(vendorPort.findById(10L)).thenReturn(Optional.of(v));

        mockMvc.perform(post("/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test Product"));
    }

    @Test
    void update_WhenExists_ShouldReturnUpdated() throws Exception {
        Product existing = new Product();
        existing.setId(1L);
        existing.setVendorId(10L);
        existing.setName("Old Name");

        Product saved = new Product();
        saved.setId(1L);
        saved.setVendorId(10L);
        saved.setName("New Name");

        Vendor v = new Vendor();
        v.setVendorId(10L);

        when(productPort.findById(1L)).thenReturn(Optional.of(existing));
        when(productPort.save(any(Product.class))).thenReturn(saved);
        when(vendorPort.findById(10L)).thenReturn(Optional.of(v));

        // Construct JSON manually
        String json = """
                    {
                        "name": "New Name",
                        "price": 100.00
                    }
                """;

        mockMvc.perform(put("/products/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Name"));
    }
}
