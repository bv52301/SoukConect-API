package com.souk.common.adapters.jpa.repository;

import com.souk.common.domain.Cuisine;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CuisineRepository extends JpaRepository<Cuisine, Long> {}

