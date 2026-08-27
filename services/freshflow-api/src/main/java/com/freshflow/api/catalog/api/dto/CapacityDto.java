package com.freshflow.api.catalog.api.dto;

import java.time.LocalDate;

public record CapacityDto(LocalDate capacityDate, int remaining) {}
