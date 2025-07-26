package com.frankies.bootcamp.utils;

public class DateTimeUtils {

    public static String convertMinutesToTimeFormat(Double totalMinutes) {
        if (totalMinutes < 0) {
            throw new IllegalArgumentException("Total minutes cannot be negative.");
        }

        int hours = (int) (totalMinutes / 60); // Integer division gives the number of full hours
        int minutes = (int) (totalMinutes % 60); // Modulo operator gives the remaining minutes

        // Format the output with leading zeros for single-digit hours and minutes
        return String.format("%02dh %02dm", hours, minutes);
    }
}
