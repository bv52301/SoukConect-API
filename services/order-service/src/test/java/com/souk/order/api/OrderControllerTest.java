package com.souk.order.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.souk.common.domain.Customer;
import com.souk.common.domain.Address;
import com.souk.common.domain.Order;
import com.souk.common.domain.Product;
import com.souk.common.port.DataAccessPort;
import com.souk.order.api.dto.OrderCreateRequest;
import com.souk.order.api.dto.OrderUpdateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
public class OrderControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private DataAccessPort<Order, Long> orderPort;

        @MockitoBean
        private DataAccessPort<Customer, Long> customerPort;

        @MockitoBean
        private DataAccessPort<Address, Long> addressPort;

        @MockitoBean
        private DataAccessPort<Product, Long> productPort;

        @Autowired
        private ObjectMapper objectMapper;

        @Test
        void listAll_ShouldReturnList() throws Exception {
                Order o = new Order();
                o.setId(1L);
                o.setStatus(Order.OrderStatus.PENDING);

                when(orderPort.findAll()).thenReturn(Arrays.asList(o));

                mockMvc.perform(get("/orders"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].status").value("PENDING"));
        }

        @Test
        void getById_WhenExists_ShouldReturnOrder() throws Exception {
                Order o = new Order();
                o.setId(1L);
                o.setStatus(Order.OrderStatus.PENDING);

                when(orderPort.findById(1L)).thenReturn(Optional.of(o));

                mockMvc.perform(get("/orders/1"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("PENDING"));
        }

        @Test
        void create_ShouldReturnCreated() throws Exception {
                // OrderCreateRequest(customerId, addressId, totalAmount, paymentMethod,
                // requestedDeliveryDate, deliveryFlexibility, deliverySlotStart,
                // deliverySlotEnd, notes, items)
                OrderCreateRequest req = new OrderCreateRequest(
                                1L,
                                1L,
                                BigDecimal.TEN,
                                Order.PaymentMethod.CARD,
                                null,
                                null,
                                null,
                                null,
                                "Notes",
                                Collections.emptyList());

                Customer c = new Customer();
                c.setId(1L);

                Address addr = new Address();
                addr.setId(1L);

                Order saved = new Order();
                saved.setId(100L);
                saved.setStatus(Order.OrderStatus.PENDING);

                when(customerPort.findById(1L)).thenReturn(Optional.of(c));
                when(addressPort.findById(1L)).thenReturn(Optional.of(addr));
                when(orderPort.save(any(Order.class))).thenReturn(saved);

                mockMvc.perform(post("/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.status").value("PENDING"));
        }

        @Test
        void update_WhenExists_ShouldReturnUpdated() throws Exception {
                Order existing = new Order();
                existing.setId(1L);
                existing.setStatus(Order.OrderStatus.PENDING);

                // OrderUpdateRequest(status, paymentMethod, requestedDeliveryDate,
                // deliveryFlexibility, deliverySlotStart, deliverySlotEnd, notes)
                OrderUpdateRequest req = new OrderUpdateRequest(
                                Order.OrderStatus.CONFIRMED,
                                Order.PaymentMethod.CASH,
                                null,
                                null,
                                null,
                                null,
                                "Notes");

                Order saved = new Order();
                saved.setId(1L);
                saved.setStatus(Order.OrderStatus.CONFIRMED);

                when(orderPort.findById(1L)).thenReturn(Optional.of(existing));
                when(orderPort.save(any(Order.class))).thenReturn(saved);

                mockMvc.perform(put("/orders/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("CONFIRMED"));
        }

        @Test
        void delete_WhenExists_ShouldReturnNoContent() throws Exception {
                Order o = new Order();
                o.setId(1L);

                when(orderPort.findById(1L)).thenReturn(Optional.of(o));
                doNothing().when(orderPort).deleteById(1L);

                mockMvc.perform(delete("/orders/1"))
                                .andExpect(status().isNoContent());
        }
}
