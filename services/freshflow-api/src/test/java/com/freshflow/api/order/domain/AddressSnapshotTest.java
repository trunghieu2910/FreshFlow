package com.freshflow.api.order.domain.domain;

import com.freshflow.api.order.domain.AddressSnapshot;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AddressSnapshotTest {

    @Test
    void shouldCreateValidAddressSnapshot() {
        AddressSnapshot address = new AddressSnapshot(
                "Nguyen Van A", "0901234567", "12 Nguyen Trai",
                "Ben Thanh", "District 1", "Ho Chi Minh City"
        );
        assertEquals("Nguyen Van A", address.getRecipientName());
        assertEquals("0901234567", address.getPhone());
        assertEquals("12 Nguyen Trai", address.getAddressLine());
        assertEquals("Ben Thanh", address.getWard());
        assertEquals("District 1", address.getDistrict());
        assertEquals("Ho Chi Minh City", address.getProvince());
    }

    @Test
    void shouldTrimTextFields() {
        AddressSnapshot address = new AddressSnapshot(
                "  Nguyen Van A  ", "  0901234567  ", "  12 Nguyen Trai  ",
                "  Ben Thanh  ", "  District 1  ", "  Ho Chi Minh City  "
        );
        assertEquals("Nguyen Van A", address.getRecipientName());
        assertEquals("0901234567", address.getPhone());
        assertEquals("12 Nguyen Trai", address.getAddressLine());
        assertEquals("Ben Thanh", address.getWard());
        assertEquals("District 1", address.getDistrict());
        assertEquals("Ho Chi Minh City", address.getProvince());
    }

    @Test
    void shouldRejectNullRecipientName() {
        assertThrows(IllegalArgumentException.class, () ->
                new AddressSnapshot(null, "0901234567", "12 Nguyen Trai", "Ben Thanh", "District 1", "HCM"));
    }

    @Test
    void shouldRejectBlankRecipientName() {
        assertThrows(IllegalArgumentException.class, () ->
                new AddressSnapshot("   ", "0901234567", "12 Nguyen Trai", "Ben Thanh", "District 1", "HCM"));
    }

    @Test
    void shouldRejectNullPhone() {
        assertThrows(IllegalArgumentException.class, () ->
                new AddressSnapshot("Name", null, "12 Nguyen Trai", "Ben Thanh", "District 1", "HCM"));
    }

    @Test
    void shouldRejectBlankAddressLine() {
        assertThrows(IllegalArgumentException.class, () ->
                new AddressSnapshot("Name", "090", "  ", "Ben Thanh", "District 1", "HCM"));
    }

    @Test
    void shouldRejectNullWard() {
        assertThrows(IllegalArgumentException.class, () ->
                new AddressSnapshot("Name", "090", "12", null, "District 1", "HCM"));
    }

    @Test
    void shouldRejectNullDistrict() {
        assertThrows(IllegalArgumentException.class, () ->
                new AddressSnapshot("Name", "090", "12", "Ben Thanh", null, "HCM"));
    }

    @Test
    void shouldRejectNullProvince() {
        assertThrows(IllegalArgumentException.class, () ->
                new AddressSnapshot("Name", "090", "12", "Ben Thanh", "District 1", null));
    }

    @Test
    void shouldBeEqualWhenAllFieldsAreEqual() {
        AddressSnapshot a1 = new AddressSnapshot("A", "B", "C", "D", "E", "F");
        AddressSnapshot a2 = new AddressSnapshot("A", "B", "C", "D", "E", "F");
        assertEquals(a1, a2);
        assertEquals(a1.hashCode(), a2.hashCode());
    }
}
