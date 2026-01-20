package com.souk.customer.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.souk.common.domain.Customer;
import com.souk.common.port.DataAccessPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CustomerController.class)
@AutoConfigureMockMvc(addFilters = false)
public class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DataAccessPort<Customer, Long> customerPort;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void listAll_ShouldReturnList() throws Exception {
        Customer c = new Customer();
        c.setId(1L);
        c.setFirstName("John");
        c.setLastName("Doe");

        when(customerPort.findAll()).thenReturn(Arrays.asList(c));

        mockMvc.perform(get("/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].firstName").value("John"));
    }

    @Test
    void getById_WhenExists_ShouldReturnCustomer() throws Exception {
        Customer c = new Customer();
        c.setId(1L);
        c.setFirstName("John");

        when(customerPort.findById(1L)).thenReturn(Optional.of(c));

        mockMvc.perform(get("/customers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("John"));
    }

    @Test
    void create_ShouldReturnCreated() throws Exception {
        Customer req = new Customer();
        req.setFirstName("Alice");
        req.setLastName("Smith");
        req.setEmail("alice@example.com");

        Customer saved = new Customer();
        saved.setId(1L);
        saved.setFirstName("Alice");

        when(customerPort.save(any(Customer.class))).thenReturn(saved);

        mockMvc.perform(post("/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.firstName").value("Alice"));
    }

    @Test
    void update_WhenExists_ShouldReturnUpdated() throws Exception {
        Customer existing = new Customer();
        existing.setId(1L);
        existing.setFirstName("Bob");

        Customer req = new Customer();
        req.setFirstName("Robert");
        req.setEmail("robert@example.com");

        Customer saved = new Customer();
        saved.setId(1L);
        saved.setFirstName("Robert");

        when(customerPort.findById(1L)).thenReturn(Optional.of(existing));
        when(customerPort.save(any(Customer.class))).thenReturn(saved);

        mockMvc.perform(put("/customers/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Robert"));
    }

    @Test
    void delete_WhenExists_ShouldReturnNoContent() throws Exception {
        Customer existing = new Customer();
        existing.setId(1L);

        when(customerPort.findById(1L)).thenReturn(Optional.of(existing));
        doNothing().when(customerPort).deleteById(1L);

        mockMvc.perform(delete("/customers/1"))
                .andExpect(status().isNoContent());
    }
}
