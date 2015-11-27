package com.fillumina.emailrecoverer;

import java.util.Calendar;
import org.junit.Test;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import static org.junit.Assert.*;

/**
 *
 * @author Francesco Illuminati <fillumina@gmail.com>
 */
public class DateExtractorTest {

    @Test
    public void shouldParseDefaultFormat() {
        Date date = DateExtractor.parse("Tue, 6 May 2004 23:47:16 +0400");
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"), Locale.US);
        cal.setTimeInMillis(0);
        cal.setTime(date);
        assertEquals(6, cal.get(Calendar.DAY_OF_MONTH));
        assertEquals(4, cal.get(Calendar.MONTH));
        assertEquals(2004, cal.get(Calendar.YEAR));
        assertEquals(19, cal.get(Calendar.HOUR_OF_DAY)); // because of timezone
        assertEquals(47, cal.get(Calendar.MINUTE));
        assertEquals(16, cal.get(Calendar.SECOND));
        assertEquals(0, cal.get(Calendar.ZONE_OFFSET));
    }

    @Test
    public void shouldParseWrongFormat() {
        Date date = DateExtractor.parse("6 May 2004 23:47:16");
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"), Locale.US);
        cal.setTimeInMillis(0);
        cal.setTime(date);
        assertEquals(6, cal.get(Calendar.DAY_OF_MONTH));
        assertEquals(4, cal.get(Calendar.MONTH));
        assertEquals(2004, cal.get(Calendar.YEAR));
        assertEquals(23, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(47, cal.get(Calendar.MINUTE));
        assertEquals(16, cal.get(Calendar.SECOND));
        assertEquals(0, cal.get(Calendar.ZONE_OFFSET));
    }

    @Test
    public void shouldParseWrongFormatWithErrors() {
        Date date = DateExtractor.parse("6C May - 2004 at 23:47:16");
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"), Locale.US);
        cal.setTimeInMillis(0);
        cal.setTime(date);
        assertEquals(6, cal.get(Calendar.DAY_OF_MONTH));
        assertEquals(4, cal.get(Calendar.MONTH));
        assertEquals(2004, cal.get(Calendar.YEAR));
        assertEquals(23, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(47, cal.get(Calendar.MINUTE));
        assertEquals(16, cal.get(Calendar.SECOND));
        assertEquals(0, cal.get(Calendar.ZONE_OFFSET));
    }

    @Test
    public void shouldGetTheYear() {
        Date date = DateExtractor.parse("6C May - 2004 at 23:47:16");
        String year = DateExtractor.getYearAsString(date);
        assertEquals("2004", year);
    }

    @Test
    public void shouldReturnACompactString() {
        Date date = DateExtractor.parse("6C May - 2004 at 23:47:16");
        String compact = DateExtractor.toCompactString(date);
        assertEquals("20040406234716", compact);
    }

    @Test
    public void shouldReturnTheRightCompactString() {
        Date date = DateExtractor.parse("Tue, 21 Jan 2014 09:15:06 +0100");
        String compact = DateExtractor.toCompactString(date);
        assertEquals("20140021081506", compact);
    }
}
