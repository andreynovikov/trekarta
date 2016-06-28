package mobi.maptrek.util;

import com.skedgo.converter.TimezoneMapper;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Helper class to quickly calculate sunrise and sunset time for specified location.
 * http://williams.best.vwh.net/sunrise_sunset_algorithm.htm
 */
public class SunriseSunset {
    @SuppressWarnings("unused")
    private static final double ASTRONOMICAL = 108d;
    @SuppressWarnings("unused")
    private static final double NAUTICAL = 102d;
    @SuppressWarnings("unused")
    private static final double CIVIL = 96d;
    private static final double OFFICIAL = 90.8333d; // 90deg 50'

    @SuppressWarnings("FieldCanBeLocal")
    private static double D2R = Math.PI / 180;
    @SuppressWarnings("FieldCanBeLocal")
    private static double R2D = 180 / Math.PI;

    private Calendar calendar;
    @SuppressWarnings("FieldCanBeLocal")
    private double zenith = OFFICIAL;
    private int N;
    private double lngHour;
    private double latRad;
    private double tzOffset;

    public SunriseSunset() {
        calendar = Calendar.getInstance();
        // 1. first calculate the day of the year
        N = calendar.get(Calendar.DAY_OF_YEAR);
        tzOffset = (calendar.get(Calendar.ZONE_OFFSET) + calendar.get(Calendar.DST_OFFSET)) * 1d / 3600000;
    }

    public void setLocation(double latitude, double longitude) {
        String timeZoneId = TimezoneMapper.latLngToTimezoneString(latitude, longitude);
        TimeZone timeZone = TimeZone.getTimeZone(timeZoneId);
        tzOffset = timeZone.getOffset(calendar.getTimeInMillis()) * 1d / 3600000;

        latRad = Math.toRadians(latitude);
        // 2a. convert the longitude to hour value
        lngHour = longitude / 15;
    }

    /**
     * Returns sunrise or sunset time in UTC. If sun never rises returns Double.MAX_VALUE,
     * if sun never sets, returns Double.MIN_VALUE.
     *
     * @param sunrise Indicates what to calculate - sunrise or sunset
     */
    public double compute(boolean sunrise) {
        // 2b. calculate an approximate time
        double t = N + (((sunrise ? 6 : 18) - lngHour) / 24);
        // 3. calculate the Sun's mean anomaly
        double M = 0.9856 * t - 3.289;
        double radM = M * D2R;
        // 4. calculate the Sun's true longitude
        double L = M + (1.916 * Math.sin(radM)) + (0.020 * Math.sin(2 * radM)) + 282.634;
        L = adjustDegrees(L);
        // 5a. calculate the Sun's right ascension
        double RA = R2D * Math.atan(0.91764 * Math.tan(L * D2R));
        RA = adjustDegrees(RA);
        // 5b. right ascension value needs to be in the same quadrant as L
        double lQuadrant = Math.floor(L / 90) * 90;
        double rQuadrant = Math.floor(RA / 90) * 90;
        // 5c. right ascension value needs to be converted into hours
        RA = (RA + lQuadrant - rQuadrant) / 15;
        // 6. calculate the Sun's declination
        double sinDec = 0.39782 * Math.sin(L * D2R);
        double cosDec = Math.cos(Math.asin(sinDec));
        // 7a. calculate the Sun's local hour angle
        double cosH = (Math.cos(zenith * D2R) - (sinDec * Math.sin(latRad))) / (cosDec * Math.cos(latRad));
        if (cosH > 1)
            return Double.MAX_VALUE;
        if (cosH < -1)
            return Double.MIN_VALUE;
        // 7b. finish calculating H and convert into hours
        double H = R2D * Math.acos(cosH);
        if (sunrise)
            H = 360 - H;
        H = H / 15;
        // 8. calculate local mean time of rising/setting
        double T = H + RA - (0.06571 * t) - 6.622;
        // 9. adjust back to UTC
        return adjustTime(T - lngHour);
    }

    public boolean isDaytime(double now) {
        double sunrise = compute(true);
        double sunset = compute(false);
        // sun never sets
        if (sunrise == Double.MIN_VALUE || sunset == Double.MIN_NORMAL)
            return true;
        // sun never rises
        if (sunrise == Double.MAX_VALUE || sunset == Double.MAX_VALUE)
            return false;
        // sun sets after midnight
        if (sunrise > sunset)
            return !(now < sunrise && now > sunset);
        // sun sets before midnight
        return now < sunset && now > sunrise;
    }

    private static double adjustDegrees(double degrees) {
        if (degrees >= 360d)
            degrees -= 360d;
        if (degrees < 0)
            degrees += 360d;
        return degrees;
    }

    private static double adjustTime(double time) {
        if (time >= 24d)
            time -= 24d;
        if (time < 0)
            time += 24d;
        return time;
    }

    /**
     * Converts UTC time to local time and outputs it as string.
     */
    public CharSequence formatTime(double time) {
        return StringFormatter.timeR((int) (adjustTime(time + tzOffset) * 60));
    }

    public double getUtcOffset() {
        return tzOffset;
    }
}
