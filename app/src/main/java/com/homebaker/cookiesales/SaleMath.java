package com.homebaker.cookiesales;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public final class SaleMath {
    public static final long MAX_MONEY = 999_999_999;
    private SaleMath() { }
    public static long parseMoney(String text) {
        if (text == null || !text.trim().matches("[0-9]{1,8}(\\.[0-9]{1,2})?"))
            throw new IllegalArgumentException("Enter a valid amount with at most two decimal places.");
        long value = new BigDecimal(text.trim()).movePointRight(2).longValueExact();
        checkMoney(value); return value;
    }
    public static void checkMoney(long value) {
        if (value < 0 || value > MAX_MONEY) throw new IllegalArgumentException("Amount must be between ₱0.00 and ₱9,999,999.99.");
    }
    public static long total(long price, int count, String mode, int bundleSize, long bundlePrice, long custom) {
        if (mode == null) throw new IllegalArgumentException("Choose Regular, Promo or Custom pricing.");
        checkMoney(price);
        if (price == 0 || count < 1 || count > 999999) throw new IllegalArgumentException("Use a positive price and 1–999,999 cookies.");
        long regular = price * count; checkMoney(regular);
        long result;
        switch (mode) {
            case "Regular": result = regular; break;
            case "Promo":
                checkMoney(bundlePrice);
                if (bundleSize < 2 || bundleSize > 999999 || bundlePrice == 0 || bundlePrice > price * bundleSize)
                    throw new IllegalArgumentException("Set a valid product bundle offer first.");
                result = (count / bundleSize) * bundlePrice + (count % bundleSize) * price; break;
            case "Custom": result = custom; break;
            default: throw new IllegalArgumentException("Choose Regular, Promo or Custom pricing.");
        }
        checkMoney(result); return result;
    }
    public static String money(long value) { return String.format(Locale.US, "₱%,.2f", BigDecimal.valueOf(value, 2)); }
    public static String inputMoney(long value) { return BigDecimal.valueOf(value, 2).toPlainString(); }
    public static String today() { return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date()); }
    public static String validDate(String day) {
        if (day == null || !day.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) throw new IllegalArgumentException("Choose a valid date.");
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US); f.setLenient(false);
        try { f.parse(day); } catch (ParseException ex) { throw new IllegalArgumentException("Choose a valid date."); }
        if (day.compareTo("1970-01-01") < 0 || day.compareTo("2100-12-31") > 0) throw new IllegalArgumentException("Choose a date between 1970 and 2100.");
        return day;
    }
    public static Calendar calendar(String day) {
        validDate(day); Calendar c=Calendar.getInstance();
        c.clear(); c.set(Integer.parseInt(day.substring(0,4)),Integer.parseInt(day.substring(5,7))-1,Integer.parseInt(day.substring(8,10)),12,0);
        return c;
    }
    public static String weekStart(String day) {
        Calendar c=calendar(day); c.add(Calendar.DAY_OF_MONTH,-((c.get(Calendar.DAY_OF_WEEK)+5)%7));
        return new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(c.getTime());
    }
    public static String displayDate(String day) { return new SimpleDateFormat("MMM d, yyyy",Locale.US).format(calendar(day).getTime()); }
}
