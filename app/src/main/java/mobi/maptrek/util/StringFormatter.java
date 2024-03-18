/*
 * Copyright 2024 Andrey Novikov
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package mobi.maptrek.util;

import android.annotation.SuppressLint;

import org.oscim.core.GeoPoint;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.coords.MGRSCoord;
import gov.nasa.worldwind.geom.coords.UPSCoord;
import gov.nasa.worldwind.geom.coords.UTMCoord;

public class StringFormatter
{
	// http://code.google.com/p/android/issues/detail?id=2626
	final static DecimalFormat coordDegFormat = new DecimalFormat("#0.000000", new DecimalFormatSymbols(Locale.ENGLISH));
	final static DecimalFormat coordIntFormat = new DecimalFormat("00", new DecimalFormatSymbols(Locale.ENGLISH));
	final static DecimalFormat coordMinFormat = new DecimalFormat("00.0000", new DecimalFormatSymbols(Locale.ENGLISH));
	final static DecimalFormat coordSecFormat = new DecimalFormat("00.000", new DecimalFormatSymbols(Locale.ENGLISH));
	
	final static DecimalFormat timeFormat = new DecimalFormat("00");

	public static int coordinateFormat = 0;
	
	public static double distanceFactor = 1d;
	public static String distanceAbbr = "km";
	public static double distanceShortFactor = 1d;
	public static String distanceShortAbbr = "m";
	
	public static String precisionFormat = "%.0f";
	public static float speedFactor = 1f;
	public static String speedAbbr = "m/s";

	public static String elevationFormat = "%.0f";
	public static float elevationFactor = 1f;
	public static String elevationAbbr = "m";

	public static String angleFormat = "%.0f";
	public static double angleFactor = 1d;
	public static String angleAbbr = "deg";
	public static String angleLeft = "L";
	public static String angleRight = "R";

	//FIXME Should localize:
	public static String secondAbbr = "sec";
	public static String minuteAbbr = "min";
	public static String hourAbbr = "h";

	@SuppressLint("DefaultLocale")
	public static String distanceHP(final double distance)
	{
		double dist = distance / 1000 * distanceFactor;
		long rdist = (long) dist;
		double fractional = (dist - rdist) * 1000 / distanceFactor;
		long rfrac = (long) (fractional * distanceShortFactor + 0.5);
		if (rdist > 0 && rfrac > 0) {
			return String.format("%d %s %d %s", rdist, distanceAbbr, rfrac, distanceShortAbbr);
		} else if (rdist > 0) {
			return String.format("%d %s", rdist, distanceAbbr);
		} else {
			return String.format("%d %s", rfrac, distanceShortAbbr);
		}
	}

	public static String distanceH(final double distance)
	{
		return distanceH(distance, 2000);
	}

	public static String distanceH(double distance, int threshold)
	{
		String[] dist = distanceC(distance, threshold);
		return dist[0] + " " + dist[1];
	}

	public static String distanceH(double distance, String format)
	{
		return distanceH(distance, format, 2000);
	}

	public static String distanceH(double distance, String format, int threshold)
	{
		String[] dist = distanceC(distance, format, threshold);
		return dist[0] + " " + dist[1];
	}

	public static String[] distanceC(final double distance)
	{
		return distanceC(distance, 2000);
	}

	public static String[] distanceC(final double distance, int threshold)
	{
		return distanceC(distance, precisionFormat, threshold);
	}

	public static String[] distanceC(final double distance, final String format, int threshold)
	{
		double dist = distance * distanceShortFactor;
		String distunit = distanceShortAbbr;
		if (Math.abs(dist) > threshold) {
			dist = dist / distanceShortFactor / 1000 * distanceFactor;
			distunit = distanceAbbr;
		}

		return new String[] {String.format(distanceAbbr.equals(distunit) ? format : "%.0f", dist), distunit};
	}

	public static String speedH(final float speed)
	{
		return speedC(speed) + " " + speedAbbr;
	}

	public static String speedC(final float speed)
	{
		return String.format(precisionFormat, speed * speedFactor);
	}

	public static String elevationH(final float elevation)
	{
		return elevationH(elevation, elevationFormat);
	}

	public static String elevationH(final float elevation, final String format)
	{
		return elevationC(elevation, format) + " " + elevationAbbr;
	}

	public static String elevationC(final float elevation)
	{
		return elevationC(elevation, elevationFormat);
	}

	public static String elevationC(final float elevation, final String format)
	{
		return String.format(format, elevation * elevationFactor);
	}

	public static String angleH(final double angle)
	{
		if (angleFactor == 1f)
		{
			// Special case for degrees: use symbol instead of abbreviation
			return String.format(angleFormat, angle)+"°";
		}
		else
		{
			return angleC(angle) + " " + angleAbbr;
		}
	}

	public static String angleT(final double angle)
	{
		String value = angleC(Math.abs(angle));
		if ("0".equals(value))
			return value;
		return (angle < 0 ? angleLeft : angleRight) + value;
	}

	public static String angleC(final double angle)
	{
		return String.format(angleFormat, angle / angleFactor);
	}

	public static String coordinate(double coordinate)
	{
		return coordinate(coordinateFormat, coordinate);
	}

	public static String coordinate(int format, double coordinate)
	{
		switch (format)
		{
			case 0:
			{
				return coordDegFormat.format(coordinate);
			}
			case 1:
			{
				final double sign = Math.signum(coordinate);
				final double coord = Math.abs(coordinate);
				final int degrees = (int) Math.floor(coord);
				final double minutes = (coord - degrees) * 60;
				return coordIntFormat.format(sign*degrees) + "° "
						+ coordMinFormat.format(minutes) + "'";
			}
			case 2:
			{
				final double sign = Math.signum(coordinate);
				final double coord = Math.abs(coordinate);
				final int degrees = (int) Math.floor(coord);
				final double min = (coord - degrees) * 60;
				final int minutes = (int) Math.floor(min);
				final double seconds = (min - minutes) * 60;
				return coordIntFormat.format(sign*degrees) + "° "
						+ coordIntFormat.format(minutes) + "' "
						+ coordSecFormat.format(seconds) + "\"";
			}
		}
		return String.valueOf(coordinate);
	}

	public static String coordinates(GeoPoint point) {
		return coordinates(" ", point.getLatitude(), point.getLongitude());
	}

	/**
	 * Formats coordinates according to currently selected format as one string with specified delimiter between coordinates (if applicable).
	 * @param delimiter Delimiter between latitude and longitude
	 * @param latitude Latitude
	 * @param longitude Longitude
	 * @return string representation of coordinates
	 */
	public static String coordinates(String delimiter, double latitude, double longitude)
	{
		return coordinates(coordinateFormat, delimiter, latitude, longitude);
	}

	public static String coordinates(int format, String delimiter, double latitude, double longitude)
	{
		switch (format)
		{
			case 0:
			case 1:
			case 2:
			{
				return coordinate(format, latitude) + delimiter + coordinate(format, longitude);
			}
			case 3:
			{
				try
				{
					Angle lat = Angle.fromDegrees(latitude);
					Angle lon = Angle.fromDegrees(longitude);
					if (latitude < 84 && latitude > -80)
						return UTMCoord.fromLatLon(lat, lon).toString();
					else
						return UPSCoord.fromLatLon(lat, lon).toString();
				}
				catch (IllegalArgumentException e)
				{
					e.printStackTrace();
					break;
				}
				
			}
			case 4:
			{
				try
				{
					Angle lat = Angle.fromDegrees(latitude);
					Angle lon = Angle.fromDegrees(longitude);
					return MGRSCoord.fromLatLon(lat, lon).toString();
				}
				catch (IllegalArgumentException e)
				{
					e.printStackTrace();
					break;
				}
			}
		}
		// On any error fall back to default lat/lon coordinates format
		return coordDegFormat.format(latitude) + delimiter + coordDegFormat.format(longitude);
	}

	public static String bearingSimpleH(double bearing)
	{
		if (bearing <  22 || bearing >= 338) return "↑"; // N
		if (bearing <  67) return "↗"; // NE
		if (bearing < 112) return "→"; // E
		if (bearing < 158) return "↘"; // SE
		if (bearing < 202) return "↓"; // S
		if (bearing < 248) return "↙"; // SW
		if (bearing < 292) return "←"; // W
		if (bearing < 338) return "↖"; // NW
		return ".";
	}

	/**
	 * Formats time period in four ways:<br/>
	 * "< 1 min" - for 1 minute<br/>
	 * "12 min" - for period less than 1 hour<br/>
	 * "1:53 min" - for period more than 1 hour<br/>
	 * "> 24 h" - for period more than 1 day
	 *
	 * @param minutes time in minutes
	 * @return Time period
	 */
	public static String timeH(int minutes)
	{
		String[] time = timeC(minutes);
		return time[0] + " " + time[1];
	}

	/**
	 * Formats time period in four ways:<br/>
	 * "< 1 min" - for 1 minute<br/>
	 * "12 min" - for period less than 1 hour<br/>
	 * "1:53 min" - for period more than 1 hour<br/>
	 * "> 24 h" - for period more than 1 day
	 * 
	 * @param minutes time in minutes
	 * @return Time period
	 */
	public static String[] timeC(int minutes)
	{
		int min = minutes;

		if (min <= 1)
			return new String[] {"< 1", minuteAbbr};

		if (min < 60)
			return new String[] {String.format(Locale.getDefault(), "%d", min), minuteAbbr};

		int hour = (int) Math.floor(min / 60d);
		if (hour > 23)
			return new String[] {"> 24", hourAbbr};

		min = min - hour * 60;
		return new String[] {String.format(Locale.getDefault(), "%2d:%02d", hour, min), minuteAbbr};
	}

	/**
	 * Formats time period in three ways:<br/>
	 * "12 sec" - for period less than 1 minute<br/>
	 * "34 min" - for period more than 1 minute<br/>
	 * "> 40 min" - for period more than timeout (where 40 is timeout)
	 * 
	 * @param seconds time period in seconds
	 * @param timeout timeout in seconds 
	 * @return Time period
	 */
	public static String[] timeCP(int seconds, int timeout)
	{
		boolean t = seconds > timeout;

		System.err.print("CP " + seconds + " " + timeout);
		if (seconds <= 59)
		{
			if (t)
				return new String[] {"> " + timeout, secondAbbr};
			else
				return new String[] {String.valueOf(seconds), secondAbbr};
		}
		int min = (int) Math.floor(seconds / 60d);
		if (t)
		{
			min = (int) Math.floor(timeout / 60d);
			return new String[] {"> " + min, minuteAbbr};
		}
		else
			return new String[] {String.valueOf(min), minuteAbbr};
	}

	public static String timeR(int minutes)
	{
        int hour = 0;
        int min = minutes;

        if (min > 59)
		{
			hour = (int) Math.floor(min / 60d);
			min = min - hour * 60;
		}
		if (hour > 99)
		{
			return "--:--";
		}

		return String.format(Locale.getDefault(), "%02d:%02d", hour, min);
	}

    /**
     * Formats time offset (with sign)
     */
    public static String timeO(int minutes) {
        boolean minus = false;

        if (minutes < 0) {
            minus = true;
            minutes = -minutes;
        }

        int hour = 0;
        int min = minutes;

        if (min > 59) {
            hour = (int) Math.floor(min / 60d);
            min = min - hour * 60;
        }

        return String.format(Locale.getDefault(), "%s%d:%02d", (minus ? "-" : "+"), hour, min);
    }
}
