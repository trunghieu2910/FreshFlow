package com.freshflow.api.catalog.dto;

import com.freshflow.api.catalog.model.StoreStatus;

public record UpdateStoreRequest(
    String name, String phone, String addressLine, Boolean autoAcceptDefault, StoreStatus status) {}
