package com.freshflow.api.catalog.application.command;

import com.freshflow.api.catalog.domain.StoreStatus;

public record UpdateStoreCommand(
    String name, String phone, String addressLine, Boolean autoAcceptDefault, StoreStatus status) {}
