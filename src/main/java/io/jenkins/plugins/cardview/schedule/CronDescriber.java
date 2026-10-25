package io.jenkins.plugins.cardview.schedule;

import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns a Jenkins cron spec into a short phrase: {@code H 2 * * *} becomes "Daily, ~02:00".
 * Specs it cannot put into words come back as "cron &lt;spec&gt;".
 */
public final class CronDescriber {

    private static final Map<String, String> ALIASES = Map.of(
            "@yearly", "Yearly",
            "@annually", "Yearly",
            "@monthly", "Monthly",
            "@weekly", "Weekly",
            "@daily", "Daily",
            "@midnight", "Daily, around midnight",
            "@hourly", "Hourly");

    private static final Pattern STEP = Pattern.compile("(?:H|\\*)(?:\\([0-9]+-[0-9]+\\))?/([0-9]+)");
    private static final Pattern NUMBER = Pattern.compile("[0-9]+");
    private static final Pattern HASHED = Pattern.compile("H(?:\\([0-9]+-[0-9]+\\))?");

    private CronDescriber() {}

    public static String describe(String spec) {
        String zone = "";
        List<String> phrases = new ArrayList<>();
        for (String raw : spec.split("\\R")) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (line.startsWith("TZ=")) {
                zone = " " + line.substring(3).trim();
                continue;
            }
            phrases.add(describeLine(line));
        }
        if (phrases.isEmpty()) {
            return "cron " + spec.trim();
        }
        return String.join(" and ", phrases) + zone;
    }

    private static String describeLine(String line) {
        String alias = ALIASES.get(line.toLowerCase(Locale.ROOT));
        if (alias != null) {
            return alias;
        }
        String[] f = line.split("\\s+");
        if (f.length != 5) {
            return "cron " + line;
        }
        String minute = f[0];
        String hour = f[1];
        String dayOfMonth = f[2];
        String month = f[3];
        String dayOfWeek = f[4];
        boolean everyDay = "*".equals(dayOfMonth) && "*".equals(month) && "*".equals(dayOfWeek);

        Matcher minuteStep = STEP.matcher(minute);
        if (minuteStep.matches() && "*".equals(hour) && everyDay) {
            return "Every " + minuteStep.group(1) + " min";
        }
        if (isAnyMinute(minute) && "*".equals(hour) && everyDay) {
            return "Hourly";
        }
        Matcher hourStep = STEP.matcher(hour);
        if (isAnyMinute(minute) && hourStep.matches() && everyDay) {
            return "Every " + hourStep.group(1) + " hours";
        }

        String time = timeOf(minute, hour);
        if (time == null || !"*".equals(month)) {
            return "cron " + line;
        }
        if (everyDay) {
            return "Daily, " + time;
        }
        if ("*".equals(dayOfMonth) && "1-5".equals(dayOfWeek)) {
            return "Weekdays, " + time;
        }
        String weekday = weekdayName(dayOfWeek);
        if ("*".equals(dayOfMonth) && weekday != null) {
            return "Weekly, " + weekday + " " + time;
        }
        if (NUMBER.matcher(dayOfMonth).matches() && "*".equals(dayOfWeek)) {
            return "Monthly, " + ordinal(Integer.parseInt(dayOfMonth)) + " " + time;
        }
        return "cron " + line;
    }

    private static boolean isAnyMinute(String minute) {
        return NUMBER.matcher(minute).matches() || HASHED.matcher(minute).matches();
    }

    /** "02:30" for a fixed minute, "~02:00" for a hashed one, null when the hour is not a single value. */
    private static String timeOf(String minute, String hour) {
        if (!NUMBER.matcher(hour).matches()) {
            return null;
        }
        int h = Integer.parseInt(hour);
        if (NUMBER.matcher(minute).matches()) {
            return String.format(Locale.ROOT, "%02d:%02d", h, Integer.parseInt(minute));
        }
        if (HASHED.matcher(minute).matches()) {
            return String.format(Locale.ROOT, "~%02d:00", h);
        }
        return null;
    }

    private static String weekdayName(String field) {
        int day;
        if (NUMBER.matcher(field).matches()) {
            day = Integer.parseInt(field);
            if (day > 7) {
                return null;
            }
        } else {
            day = switch (field.toUpperCase(Locale.ROOT)) {
                case "SUN" -> 0;
                case "MON" -> 1;
                case "TUE" -> 2;
                case "WED" -> 3;
                case "THU" -> 4;
                case "FRI" -> 5;
                case "SAT" -> 6;
                default -> -1;
            };
            if (day < 0) {
                return null;
            }
        }
        DayOfWeek dow = day == 0 || day == 7 ? DayOfWeek.SUNDAY : DayOfWeek.of(day);
        return dow.getDisplayName(TextStyle.FULL, Locale.ENGLISH);
    }

    private static String ordinal(int n) {
        if (n % 100 >= 11 && n % 100 <= 13) {
            return n + "th";
        }
        return switch (n % 10) {
            case 1 -> n + "st";
            case 2 -> n + "nd";
            case 3 -> n + "rd";
            default -> n + "th";
        };
    }
}
