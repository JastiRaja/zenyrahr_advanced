package com.zenyrahr.hrms.utils;

import java.text.DecimalFormat;

public class NumberToWordsConverter {

    private static final String[] UNITS = {
            "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten",
            "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"
    };

    private static final String[] TENS = {
            "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    };

    private static final String[] SCALES = {
            "", "Thousand", "Lakh", "Crore"
    };

    // Public method to convert numbers to words (with currency)
    public static String convert(double number) {
        if (number == 0) {
            return "Zero Rupees";
        }

        // Format the number to two decimal places (paisa)
        String formattedNumber = new DecimalFormat("#.00").format(number);
        String[] parts = formattedNumber.split("\\.");
        String integerPart = convertIntegerPart(Integer.parseInt(parts[0]));
        String decimalPart = convertIntegerPart(Integer.parseInt(parts[1]));

        // Construct the final output with "Rupees" and "Paisa"
        String result = integerPart + " Rupees";
        if (Integer.parseInt(parts[1]) > 0) {
            result += " and " + decimalPart + " Paisa";
        }

        return result;
    }

    // Helper method to convert the integer part
    private static String convertIntegerPart(int number) {
        if (number == 0) {
            return "";
        }
        if (number < 20) {
            return UNITS[number];
        } else if (number < 100) {
            return TENS[number / 10] + (number % 10 != 0 ? " " + UNITS[number % 10] : "");
        } else if (number < 1000) {
            return UNITS[number / 100] + " Hundred" + (number % 100 != 0 ? " " + convertIntegerPart(number % 100) : "");
        } else {
            return convertLargeNumber(number);
        }
    }

    // Helper method to handle large numbers (thousands, lakhs, crores)
    private static String convertLargeNumber(int number) {
        StringBuilder result = new StringBuilder();
        int[] divisors = {10000000, 100000, 1000}; // For Crore, Lakh, Thousand
        String[] scales = {"Crore", "Lakh", "Thousand"}; // Scale names

        for (int i = 0; i < divisors.length; i++) {
            int scaleValue = number / divisors[i];
            if (scaleValue > 0) {
                result.append(convertIntegerPart(scaleValue)).append(" ").append(scales[i]).append(" ");
                number %= divisors[i];  // Get the remainder to process smaller units
            }
        }

        // If there's anything left after processing crores, lakhs, and thousands
        if (number > 0) {
            result.append(convertIntegerPart(number));
        }

        return result.toString().trim();
    }

}
