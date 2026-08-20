package com.freshflow.api.common.domain;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class MoneyTest {

    // --- Constructor ---

    @Test
    void shouldAllowZeroAmount() {
        Money money = new Money(BigDecimal.ZERO);
        assertEquals(new BigDecimal("0.00"), money.getAmount());
    }

    @Test
    void shouldRejectNullAmount() {
        assertThrows(IllegalArgumentException.class, () -> new Money(null));
    }

    @Test
    void shouldRejectNegativeAmount() {
        assertThrows(IllegalArgumentException.class, () -> new Money(new BigDecimal("-100.00")));
    }

    @Test
    void shouldRejectAmountWithMoreThanTwoDecimalPlaces() {
        assertThrows(IllegalArgumentException.class, () -> new Money(new BigDecimal("100.123")));
    }

    @Test
    void shouldNormalizeTrailingZerosToScaleTwo() {
        Money money = new Money(new BigDecimal("100.000"));
        assertEquals(new BigDecimal("100.00"), money.getAmount());
    }

    @Test
    void shouldNormalizeIntegerToScaleTwo() {
        Money money = new Money(new BigDecimal("100"));
        assertEquals(new BigDecimal("100.00"), money.getAmount());
    }

    // --- add ---

    @Test
    void addMoney() {
        Money money = new Money(new BigDecimal("100.00"));
        Money other = new Money(new BigDecimal("100.00"));
        Money sum = money.add(other);
        assertEquals(new BigDecimal("200.00"), sum.getAmount());
    }

    @Test
    void shouldRejectNullWhenAdding() {
        Money money = new Money(new BigDecimal("100.00"));
        assertThrows(IllegalArgumentException.class, () -> money.add(null));
    }

    @Test
    void shouldKeepOriginalValuesWhenAdding() {
        Money money = new Money(new BigDecimal("100.00"));
        Money other = new Money(new BigDecimal("25.50"));
        Money sum = money.add(other);
        assertEquals(new BigDecimal("125.50"), sum.getAmount());
        assertEquals(new BigDecimal("100.00"), money.getAmount());
        assertEquals(new BigDecimal("25.50"), other.getAmount());
    }

    // --- subtract ---

    @Test
    void subtractMoney() {
        Money money = new Money(new BigDecimal("100.00"));
        Money other = new Money(new BigDecimal("50.00"));
        Money difference = money.subtract(other);
        assertEquals(new BigDecimal("50.00"), difference.getAmount());
    }

    @Test
    void shouldRejectNullWhenSubtracting() {
        Money money = new Money(new BigDecimal("100.00"));
        assertThrows(IllegalArgumentException.class, () -> money.subtract(null));
    }

    @Test
    void shouldKeepOriginalValuesWhenSubtracting() {
        Money money = new Money(new BigDecimal("100.00"));
        Money other = new Money(new BigDecimal("25.50"));
        Money difference = money.subtract(other);
        assertEquals(new BigDecimal("74.50"), difference.getAmount());
        assertEquals(new BigDecimal("100.00"), money.getAmount());
        assertEquals(new BigDecimal("25.50"), other.getAmount());
    }

    @Test
    void shouldRejectNegativeResultWhenSubtracting() {
        Money money = new Money(new BigDecimal("100.00"));
        Money other = new Money(new BigDecimal("150.00"));
        assertThrows(IllegalArgumentException.class, () -> money.subtract(other));
    }

    // --- multiply ---

    @Test
    void shouldMultiplyByPositiveMultiplier() {
        Money money = new Money(new BigDecimal("35000.00"));
        Money result = money.multiply(BigDecimal.valueOf(2));
        assertEquals(new BigDecimal("70000.00"), result.getAmount());
    }

    @Test
    void shouldMultiplyByDecimalMultiplier() {
        Money money = new Money(new BigDecimal("100000.00"));
        Money result = money.multiply(new BigDecimal("0.10"));
        assertEquals(new BigDecimal("10000.00"), result.getAmount());
    }

    @Test
    void shouldRoundMultiplyResultHalfUp() {
        // 1.00 * 0.005 = 0.005 -> HALF_UP -> 0.01
        Money money = new Money(new BigDecimal("1.00"));
        Money result = money.multiply(new BigDecimal("0.005"));
        assertEquals(new BigDecimal("0.01"), result.getAmount());
    }

    @Test
    void shouldMultiplyByZeroReturnZero() {
        Money money = new Money(new BigDecimal("1000.00"));
        Money result = money.multiply(BigDecimal.ZERO);
        assertEquals(new BigDecimal("0.00"), result.getAmount());
    }

    @Test
    void shouldRejectNullMultiplier() {
        Money money = new Money(new BigDecimal("100.00"));
        assertThrows(IllegalArgumentException.class, () -> money.multiply(null));
    }

    @Test
    void shouldRejectNegativeMultiplier() {
        Money money = new Money(new BigDecimal("100.00"));
        assertThrows(IllegalArgumentException.class, () -> money.multiply(new BigDecimal("-1")));
    }

    // --- compareTo ---

    @Test
    void shouldReturnNegativeWhenLess() {
        Money smaller = new Money(new BigDecimal("50.00"));
        Money larger = new Money(new BigDecimal("100.00"));
        assertTrue(smaller.compareTo(larger) < 0);
    }

    @Test
    void shouldReturnZeroWhenEqual() {
        Money a = new Money(new BigDecimal("100.00"));
        Money b = new Money(new BigDecimal("100.00"));
        assertEquals(0, a.compareTo(b));
    }

    @Test
    void shouldReturnPositiveWhenGreater() {
        Money larger = new Money(new BigDecimal("100.00"));
        Money smaller = new Money(new BigDecimal("50.00"));
        assertTrue(larger.compareTo(smaller) > 0);
    }

    @Test
    void shouldRejectNullWhenComparingTo() {
        Money money = new Money(new BigDecimal("100.00"));
        assertThrows(IllegalArgumentException.class, () -> money.compareTo(null));
    }

    // --- isZero ---

    @Test
    void shouldReturnTrueWhenZero() {
        assertTrue(new Money(BigDecimal.ZERO).isZero());
    }

    @Test
    void shouldReturnFalseWhenNotZero() {
        assertFalse(new Money(new BigDecimal("0.01")).isZero());
    }

    // --- isGreaterThan / isLessThan ---

    @Test
    void shouldReturnTrueWhenGreaterThan() {
        Money big = new Money(new BigDecimal("100.00"));
        Money small = new Money(new BigDecimal("50.00"));
        assertTrue(big.isGreaterThan(small));
    }

    @Test
    void shouldReturnTrueWhenLessThan() {
        Money small = new Money(new BigDecimal("50.00"));
        Money big = new Money(new BigDecimal("100.00"));
        assertTrue(small.isLessThan(big));
    }

    // --- equals / hashCode ---

    @Test
    void shouldBeEqualWhenSameAmount() {
        Money a = new Money(new BigDecimal("100.00"));
        Money b = new Money(new BigDecimal("100.00"));
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void shouldNotBeEqualWhenDifferentAmount() {
        Money a = new Money(new BigDecimal("100.00"));
        Money b = new Money(new BigDecimal("200.00"));
        assertNotEquals(a, b);
    }

    @Test
    void shouldNotBeEqualToNull() {
        Money money = new Money(new BigDecimal("100.00"));
        assertNotEquals(null, money);
    }

    // --- toString ---

    @Test
    void shouldReturnReadableString() {
        Money money = new Money(new BigDecimal("100.00"));
        assertEquals("100.00", money.toString());
    }
}
