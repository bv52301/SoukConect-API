package com.souk.cuisine.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.souk.common.domain.Cuisine;
import com.souk.common.domain.CuisineImage;
import com.souk.common.port.DataAccessPort;
import com.souk.common.adapters.jpa.repository.CuisineRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CuisineController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for unit tests
public class CuisineControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DataAccessPort<Cuisine, Long> cuisinePort;

    @MockBean
    private DataAccessPort<CuisineImage, Long> cuisineImagePort;

    @MockBean
    private CuisineRepository cuisineRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void listAll_ShouldReturnList() throws Exception {
        Cuisine cuisine = new Cuisine();
        cuisine.setId(1L);
        cuisine.setCuisineName("Italian");

        when(cuisinePort.findAll()).thenReturn(Arrays.asList(cuisine));

        mockMvc.perform(get("/cuisines"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cuisineName").value("Italian"));
    }

    @Test
    void getById_WhenExists_ShouldReturnCuisine() throws Exception {
        Cuisine cuisine = new Cuisine();
        cuisine.setId(1L);
        cuisine.setCuisineName("Italian");

        when(cuisinePort.findById(1L)).thenReturn(Optional.of(cuisine));

        mockMvc.perform(get("/cuisines/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cuisineName").value("Italian"));
    }

    @Test
    void getById_WhenNotExists_ShouldReturnNotFound() throws Exception {
        when(cuisinePort.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/cuisines/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_ShouldReturnCreated() throws Exception {
        Cuisine req = new Cuisine();
        req.setCuisineName("French");

        Cuisine saved = new Cuisine();
        saved.setId(2L);
        saved.setCuisineName("French");

        when(cuisinePort.save(any(Cuisine.class))).thenReturn(saved);

        mockMvc.perform(post("/cuisines")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.cuisineName").value("French"));
    }

    @Test
    void update_WhenExists_ShouldReturnUpdated() throws Exception {
        Cuisine existing = new Cuisine();
        existing.setId(1L);
        existing.setCuisineName("Italian");

        Cuisine req = new Cuisine();
        req.setCuisineName("Updated Italian");

        when(cuisinePort.findById(1L)).thenReturn(Optional.of(existing));
        when(cuisinePort.save(any(Cuisine.class))).thenReturn(req);

        mockMvc.perform(put("/cuisines/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cuisineName").value("Updated Italian"));
    }

    @Test
    void delete_WhenExists_ShouldReturnNoContent() throws Exception {
        Cuisine existing = new Cuisine();
        existing.setId(1L);

        when(cuisinePort.findById(1L)).thenReturn(Optional.of(existing));
        doNothing().when(cuisinePort).deleteById(1L);

        mockMvc.perform(delete("/cuisines/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void listDistinctCategories_ShouldReturnCategories() throws Exception {
        when(cuisineImagePort.findAll()).thenReturn(Collections.emptyList());
        when(cuisineRepository.findDistinctCategories()).thenReturn(Arrays.asList("Italian", "Mexican"));
        when(cuisineRepository.findDistinctSubcategories()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/cuisines/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category").value("Italian"))
                .andExpect(jsonPath("$[1].category").value("Mexican"));
    }
}
