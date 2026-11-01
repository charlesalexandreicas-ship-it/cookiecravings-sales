package com.homebaker.cookiesales;

import org.junit.Test;
import static org.junit.Assert.*;

public class SaleMathTest {
    @Test public void parsesExactCentavos() { assertEquals(12345, SaleMath.parseMoney("123.45")); assertEquals(10, SaleMath.parseMoney("0.1")); }
    @Test public void rejectsInvalidMoney() {
        for (String s : new String[]{"", "-1", "1.234", "1e3", "1,00", "NaN", "9999999999999999999"})
            assertThrows(IllegalArgumentException.class, () -> SaleMath.parseMoney(s));
    }
    @Test public void regularTotalsAreExact() { assertEquals(6000, SaleMath.total(2000,3,"Regular",0,0,0)); }
    @Test public void bundleRemainderUsesRegularPrice() { assertEquals(8500, SaleMath.total(2000,5,"Promo",3,4500,0)); }
    @Test public void customIsNetRevenue() { assertEquals(5000, SaleMath.total(2000,3,"Custom",0,0,5000)); }
    @Test public void boundsAndInvalidModesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> SaleMath.total(2000,0,"Regular",0,0,0));
        assertThrows(IllegalArgumentException.class, () -> SaleMath.total(2000,1000000,"Regular",0,0,0));
        assertThrows(IllegalArgumentException.class, () -> SaleMath.total(0,3,"Regular",0,0,0));
        assertThrows(IllegalArgumentException.class, () -> SaleMath.total(2000,3,"Promo",0,0,0));
        assertThrows(IllegalArgumentException.class, () -> SaleMath.total(2000,3,"Custom",0,0,-1));
        assertThrows(IllegalArgumentException.class, () -> SaleMath.total(2000,3,"Other",0,0,0));
        assertThrows(IllegalArgumentException.class, () -> SaleMath.total(SaleMath.MAX_MONEY,2,"Regular",0,0,0));
    }
    @Test public void datesAreStrictAndWeekStartsMonday() {
        assertEquals("2026-10-05", SaleMath.weekStart("2026-10-11"));
        assertEquals("2026-10-12", SaleMath.weekStart("2026-10-12"));
        assertThrows(IllegalArgumentException.class, () -> SaleMath.validDate("2026-02-30"));
        assertThrows(IllegalArgumentException.class, () -> SaleMath.validDate("2026-2-1"));
        assertEquals("2026-02-28", SaleMath.validDate("2026-02-28"));
    }
    @Test public void formattingIsFixedAndUnambiguous() {
        assertEquals("₱1,234.50", SaleMath.money(123450));
        assertEquals("0.00", SaleMath.inputMoney(0));
    }
    @Test public void missingModeIsValidationError() { assertThrows(IllegalArgumentException.class,()->SaleMath.total(2000,1,null,0,0,0)); }
    @Test public void zeroCustomAndUpperMoneyBoundaryAreValid() {
        assertEquals(0,SaleMath.total(2000,1,"Custom",0,0,0));
        assertEquals(SaleMath.MAX_MONEY,SaleMath.parseMoney("9999999.99"));
        assertThrows(IllegalArgumentException.class,()->SaleMath.parseMoney("10000000"));
    }
}
