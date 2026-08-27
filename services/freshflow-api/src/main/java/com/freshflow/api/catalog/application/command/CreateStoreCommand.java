package com.freshflow.api.catalog.application.command;

import com.freshflow.api.catalog.domain.StoreStatus;

public record CreateStoreCommand(
    Long ownerUserId,
    String name,
    String phone,
    String addressLine,
    Boolean autoAcceptDefault,
    StoreStatus status) {}
