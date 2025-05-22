package me.clearedspore.util;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeUtil {
    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d+)([smhdMy])");

    public static long parseTime(String timeString) {
        if (timeString == null || timeString.isEmpty()) {
            return -1;
        }

        Matcher matcher = TIME_PATTERN.matcher(timeString);
        long totalMillis = 0;
        boolean found = false;

        while (matcher.find()) {
            found = true;
            int amount = Integer.parseInt(matcher.group(1));
            char unit = matcher.group(2).charAt(0);

            switch (unit) {
                case 's':
                    totalMillis += TimeUnit.SECONDS.toMillis(amount);
                    break;
                case 'm':
                    totalMillis += TimeUnit.MINUTES.toMillis(amount);
                    break;
                case 'h':
                    totalMillis += TimeUnit.HOURS.toMillis(amount);
                    break;
                case 'd':
                    totalMillis += TimeUnit.DAYS.toMillis(amount);
                    break;
                case 'M':
                    totalMillis += TimeUnit.DAYS.toMillis(amount * 30L);
                    break;
                case 'y':
                    totalMillis += TimeUnit.DAYS.toMillis(amount * 365L);
                    break;
            }
        }

        return found ? totalMillis : -1;
    }

    public static String formatDuration(long millis) {
        if (millis < 0) {
            return "Permanent";
        }

        if (millis == 0) {
            return "0 seconds";
        }

        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        long years = days / 365;
        days %= 365;
        long months = days / 30;
        days %= 30;
        
        hours %= 24;
        minutes %= 60;
        seconds %= 60;

        StringBuilder sb = new StringBuilder();

        if (years > 0) {
            sb.append(years).append(years == 1 ? " year" : " years");
            if (months > 0 || days > 0 || hours > 0 || minutes > 0 || seconds > 0) {
                sb.append(" ");
            }
        }

        if (months > 0) {
            sb.append(months).append(months == 1 ? " month" : " months");
            if (days > 0 || hours > 0 || minutes > 0 || seconds > 0) {
                sb.append(" ");
            }
        }

        if (days > 0) {
            sb.append(days).append(days == 1 ? " day" : " days");
            if (hours > 0 || minutes > 0 || seconds > 0) {
                sb.append(" ");
            }
        }

        if (hours > 0) {
            sb.append(hours).append(hours == 1 ? " hour" : " hours");
            if (minutes > 0 || seconds > 0) {
                sb.append(" ");
            }
        }

        if (minutes > 0) {
            sb.append(minutes).append(minutes == 1 ? " minute" : " minutes");
            if (seconds > 0) {
                sb.append(" ");
            }
        }

        if (seconds > 0 || (years == 0 && months == 0 && days == 0 && hours == 0 && minutes == 0)) {
            sb.append(seconds).append(seconds == 1 ? " second" : " seconds");
        }

        return sb.toString();
    }

    public static String formatRemainingTime(long millis) {
        if (millis < 0) {
            return "Permanent";
        }

        if (millis == 0) {
            return "Expired";
        }

        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        long years = days / 365;
        if (years > 0) {
            return years + (years == 1 ? " year" : " years");
        }
        
        long months = days / 30;
        if (months > 0) {
            return months + (months == 1 ? " month" : " months");
        }
        
        if (days > 0) {
            return days + (days == 1 ? " day" : " days");
        }
        
        hours %= 24;
        if (hours > 0) {
            return hours + (hours == 1 ? " hour" : " hours");
        }
        
        minutes %= 60;
        if (minutes > 0) {
            return minutes + (minutes == 1 ? " minute" : " minutes");
        }
        
        seconds %= 60;
        return seconds + (seconds == 1 ? " second" : " seconds");
    }
}