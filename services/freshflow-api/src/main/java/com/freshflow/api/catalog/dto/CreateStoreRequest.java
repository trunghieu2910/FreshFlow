package com.freshflow.api.catalog.dto;

import com.freshflow.api.catalog.model.StoreStatus;

public record CreateStoreRequest(
    Long ownerUserId,
    String name,
    String phone,
    String addressLine,
    Boolean autoAcceptDefault,
    StoreStatus status) {}
