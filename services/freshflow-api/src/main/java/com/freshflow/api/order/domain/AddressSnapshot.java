package com.freshflow.api.order.domain;

import java.util.Objects;

// AddressSnapshot stores the delivery address copied into an order at checkout. It is a snapshot,
// not a live reference to the customer's editable address book.
public final class AddressSnapshot {
  private final String recipientName;
  private final String phone;
  private final String addressLine;
  private final String ward;
  private final String district;
  private final String province;

  public AddressSnapshot(
      String recipientName,
      String phone,
      String addressLine,
      String ward,
      String district,
      String province) {
    this.recipientName = validateAndTrim(recipientName, "recipientName");
    this.phone = validateAndTrim(phone, "phone");
    this.addressLine = validateAndTrim(addressLine, "addressLine");
    this.ward = validateAndTrim(ward, "ward");
    this.district = validateAndTrim(district, "district");
    this.province = validateAndTrim(province, "province");
  }

  private String validateAndTrim(String value, String fieldName) {
    if (value == null) {
      throw new IllegalArgumentException(fieldName + " cannot be null");
    }
    String trimmed = value.trim();
    if (trimmed.isEmpty()) {
      throw new IllegalArgumentException(fieldName + " cannot be blank");
    }
    return trimmed;
  }

  public String getRecipientName() {
    return recipientName;
  }

  public String getPhone() {
    return phone;
  }

  public String getAddressLine() {
    return addressLine;
  }

  public String getWard() {
    return ward;
  }

  public String getDistrict() {
    return district;
  }

  public String getProvince() {
    return province;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AddressSnapshot that = (AddressSnapshot) o;
    return recipientName.equals(that.recipientName)
        && phone.equals(that.phone)
        && addressLine.equals(that.addressLine)
        && ward.equals(that.ward)
        && district.equals(that.district)
        && province.equals(that.province);
  }

  @Override
  public int hashCode() {
    return Objects.hash(recipientName, phone, addressLine, ward, district, province);
  }

  @Override
  public String toString() {
    return "AddressSnapshot{"
        + "recipientName='"
        + recipientName
        + '\''
        + ", phone='"
        + phone
        + '\''
        + ", addressLine='"
        + addressLine
        + '\''
        + ", ward='"
        + ward
        + '\''
        + ", district='"
        + district
        + '\''
        + ", province='"
        + province
        + '\''
        + '}';
  }
}
