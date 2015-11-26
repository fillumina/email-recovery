package com.fillumina.emailrecoverer;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Manipulation of dates in JDK minor than 1.8 is simply horrible.
 *
 * @author Francesco Illuminati <fillumina@gmail.com>
 */
public class DateExtractor {
    private static final String[] MONTHS = new String[] {
        "jan", "feb", "mar", "apr", "may", "jun",
        "jul", "aug", "sep", "oct", "nov", "dic"
    };

    /**
     * Many spam or newsgroup mails have a wrong formatted date to trick
     * the mail reader into putting them at the top of the list (often
     * ordered by date). That's against RFCs and as such, in my opinion,
     * those messages should be rejected by mail servers. Anyway here is
     * a way to try to read something out of those gibberish.
     *
     * ... what I am nuts? The spammers are the ones that pays for email
     * readers!
     */
    public static Date parse(String date) {
        date = date.toLowerCase();
        int month=0; int year=0;
        for (String m : MONTHS) {
            if (date.contains(m)) {
                date = date.replace(m, "");
                break;
            }
            month++;
        }
        for (int y=1990; y<2020; y++) {
            final String yyyy = Integer.toString(y);
            if (date.contains(yyyy)) {
                date = date.replace(yyyy, "");
                year = y;
                break;
            }
        }
        int l = date.length();
        StringBuilder buf = new StringBuilder();
        char c;
        int i;
        for (i=0; i<l; i++) {
            c = date.charAt(i);
            if (c >= '0' && c <= '9') {
                buf.append(c);
                if (i+1<l) {
                    c = date.charAt(i+1);
                    if (c >= '0' && c <= '9') {
                        buf.append(c);
                    }
                }
                break;
            }
        }
        int hour = 0, min = 0, sec = 0;
        String zone = null;
        try {
            date = date.substring(i);
            int o = date.indexOf(':');
            hour = Integer.valueOf("" + date.charAt(o-2) + date.charAt(o-1) );
            min = Integer.valueOf("" + date.charAt(o+1) + date.charAt(o+2) );
            date = date.substring(o + 3);
            sec = 0;
            o = date.indexOf(':');
            if (o != -1) {
                sec = Integer.valueOf("" + date.charAt(o + 1) + date.charAt(o + 2));
            }
            date = date.substring(o + 3);
            o = date.indexOf('+');
            if (o == -1) {
                o = date.indexOf('-');
            }
            date = date.substring(o);
            zone = date;
        } catch (Exception e) {
        }

        int day = (buf.length() == 0) ? 1 : Integer.valueOf(buf.toString());
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"), Locale.US);
        cal.setTimeInMillis(0);
        if (zone != null && zone.length() >= 2) {
            try {
                // just watch this and tell if it's a good api
                cal.setTimeZone(TimeZone.getTimeZone(
                        "GMT" + zone.substring(0, 3) + ":00"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        cal.set(year, month, day, hour, min, sec);
        return cal.getTime();
    }

    public static String getYearAsString(Date d) {
        Calendar cal = createCalendar();
        cal.setTime(d);
        int yearNumber = cal.get(Calendar.YEAR);
        return Integer.toString(yearNumber);
    }

    /**
     * @return the date printed as a string of numbers (i.e. '201526111413')
     *          which has the characteristics of being coherent with
     *          number and alphabetical ordering and at the same time still
     *          readable as a date.
     */
    public static String toCompactString(Date d) {
        Calendar cal = createCalendar();
        cal.setTime(d);
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int month = cal.get(Calendar.MONTH);
        int year = cal.get(Calendar.YEAR);
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int min = cal.get(Calendar.MINUTE);
        int sec = cal.get(Calendar.SECOND);

        return String.format("%02d%02d%02d%02d%02d%02d",
                year, month, day, hour, min, sec);
    }

    private static Calendar createCalendar() {
        Calendar cal = Calendar.getInstance(Locale.US);
        cal.setTimeZone(TimeZone.getTimeZone("GMT"));
        cal.setTimeInMillis(0);
        return cal;
    }
}
