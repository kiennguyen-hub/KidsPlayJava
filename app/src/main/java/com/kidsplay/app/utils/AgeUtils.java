package com.kidsplay.app.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Xử lý ngày sinh dd/MM/yyyy, tính tuổi và đọc khoảng tuổi dạng "5 - 8 tuổi". */
public final class AgeUtils {

    private AgeUtils() {}

    private static SimpleDateFormat format() {
        SimpleDateFormat f = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
        f.setLenient(false);
        return f;
    }

    public static String format(Calendar c) {
        return format().format(c.getTime());
    }

    public static Date parse(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        try {
            return format().parse(s.trim());
        } catch (ParseException e) {
            return null;
        }
    }

    /** Tuổi tính theo ngày sinh; -1 nếu không có/không hợp lệ (hoặc ngày sinh ở tương lai). */
    public static int ageOf(String birthDate) {
        Date d = parse(birthDate);
        if (d == null) return -1;
        Calendar birth = Calendar.getInstance();
        birth.setTime(d);
        Calendar now = Calendar.getInstance();
        if (birth.after(now)) return -1;
        int age = now.get(Calendar.YEAR) - birth.get(Calendar.YEAR);
        if (now.get(Calendar.DAY_OF_YEAR) < birth.get(Calendar.DAY_OF_YEAR)) age--;
        return Math.max(age, 0);
    }

    /** "5 - 8 tuổi" -> {5, 8}; "6" -> {6, 99}; để trống -> mọi lứa tuổi {0, 99}. */
    public static int[] parseAgeRange(String s) {
        if (s == null || s.trim().isEmpty()) return new int[]{0, 99};
        Matcher range = Pattern.compile("(\\d+)\\s*[-–—]\\s*(\\d+)").matcher(s);
        if (range.find()) {
            int a = Integer.parseInt(range.group(1));
            int b = Integer.parseInt(range.group(2));
            return new int[]{Math.min(a, b), Math.max(a, b)};
        }
        Matcher single = Pattern.compile("(\\d+)").matcher(s);
        if (single.find()) return new int[]{Integer.parseInt(single.group(1)), 99};
        return new int[]{0, 99};
    }

    public static String rangeLabel(int min, int max) {
        if (min <= 0 && max >= 99) return "Mọi lứa tuổi";
        if (max >= 99) return "Từ " + min + " tuổi";
        return min + " - " + max + " tuổi";
    }
}
