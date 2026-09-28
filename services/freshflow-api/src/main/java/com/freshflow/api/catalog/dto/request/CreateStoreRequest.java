package com.freshflow.api.catalog.dto.request;

import com.freshflow.api.catalog.enums.*;
import com.freshflow.api.catalog.enums.StoreStatus;

public record CreateStoreRequest(
    Long ownerUserId,
    String name,
    String phone,
    String addressLine,
    Boolean autoAcceptDefault,
    StoreStatus status) {}
