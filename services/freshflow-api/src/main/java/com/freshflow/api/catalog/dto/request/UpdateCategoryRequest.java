package com.freshflow.api.catalog.dto.request;

import com.freshflow.api.catalog.enums.*;

public record UpdateCategoryRequest(String name, String description, Boolean active) {}
