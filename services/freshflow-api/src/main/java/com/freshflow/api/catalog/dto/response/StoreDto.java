package com.freshflow.api.catalog.dto.response;

import com.freshflow.api.catalog.model.Store;

public record StoreDto(
    Long id,
    Long ownerUserId,
    String name,
    String phone,
    String addressLine,
    Boolean autoAcceptDefault,
    String status) {
  public static StoreDto from(Store store) {
    return new StoreDto(
        store.getId(),
        store.getOwnerUser().getId(),
        store.getName(),
        store.getPhone(),
        store.getAddressLine(),
        store.getAutoAcceptDefault(),
        store.getStatus());
  }
}
