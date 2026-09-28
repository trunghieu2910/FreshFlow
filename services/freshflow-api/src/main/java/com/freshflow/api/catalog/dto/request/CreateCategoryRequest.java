package com.freshflow.api.catalog.dto.request;

import com.freshflow.api.catalog.enums.*;

public record CreateCategoryRequest(String name, String description, Boolean active) {}
