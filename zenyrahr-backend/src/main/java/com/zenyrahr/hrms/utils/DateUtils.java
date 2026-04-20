package com.zenyrahr.hrms.utils;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;

public class DateUtils {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    // Method to get the first date of the month for a given month name and year
    public static String getFirstDateOfMonth(String monthName, int year) {
        // Convert the month name to a Month enum
        Month month = Month.valueOf(monthName.toUpperCase());

        LocalDate firstDate = LocalDate.of(year, month, 1);

        // Format the date in "dd-MM-yyyy" format
        return firstDate.format(formatter);
    }

    // Method to get the last date of the month for a given month name and year
    public static String getLastDateOfMonth(String monthName, int year) {
        // Convert the month name to a Month enum
        Month month = Month.valueOf(monthName.toUpperCase());
        // Get the last date of the month
        LocalDate lastDate = LocalDate.of(year, month, month.length(LocalDate.of(year, month, 1).isLeapYear()));

        // Format the date in "dd-MM-yyyy" format
        return lastDate.format(formatter);
    }

}
