package io.jenkins.plugins.cardview.schedule;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CronDescriberTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "H 2 * * *        | Daily, ~02:00",
        "30 23 * * *      | Daily, 23:30",
        "H 6 * * 1        | Weekly, Monday ~06:00",
        "0 23 * * 0       | Weekly, Sunday 23:00",
        "0 23 * * SUN     | Weekly, Sunday 23:00",
        "H 4 * * 1-5      | Weekdays, ~04:00",
        "H 2 1 * *        | Monthly, 1st ~02:00",
        "H 2 22 * *       | Monthly, 22nd ~02:00",
        "H/15 * * * *     | Every 15 min",
        "H * * * *        | Hourly",
        "H H/4 * * *      | Every 4 hours",
        "@daily           | Daily",
        "@hourly          | Hourly",
        "H 2,14 * * *     | cron H 2,14 * * *",
        "H 2 * 6 *        | cron H 2 * 6 *",
    })
    void describesCommonSpecs(String spec, String expected) {
        assertEquals(expected, CronDescriber.describe(spec));
    }

    @org.junit.jupiter.api.Test
    void keepsTimeZoneAndJoinsLines() {
        assertEquals(
                "Daily, ~02:00 and Weekly, Sunday 23:00 Europe/London",
                CronDescriber.describe("TZ=Europe/London\n# nightly\nH 2 * * *\n0 23 * * 0"));
    }
}
