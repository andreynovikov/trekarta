/*
 * Copyright 2019 Andrey Novikov
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

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// https://wiki.openstreetmap.org/wiki/Key:opening_hours
public class OpeningHoursLocalizer {
    private static Pattern fullPattern = Pattern.compile("\\b(mo|tu|we|th|fr|sa|su|jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec|ph|sh|week|sunrise|sunset|off|open|closed)\\b", Pattern.CASE_INSENSITIVE);

    private static final HashMap<String, String> mTranslations;
    static {
        mTranslations = new HashMap<>();
        mTranslations.put("mo", "пн");
        mTranslations.put("tu", "вт");
        mTranslations.put("we", "ср");
        mTranslations.put("th", "чт");
        mTranslations.put("fr", "пт");
        mTranslations.put("sa", "сб");
        mTranslations.put("su", "вс");

        mTranslations.put("jan", "янв");
        mTranslations.put("feb", "фев");
        mTranslations.put("mar", "мар");
        mTranslations.put("apr", "апр");
        mTranslations.put("may", "май");
        mTranslations.put("jun", "июн");
        mTranslations.put("jul", "июл");
        mTranslations.put("aug", "авг");
        mTranslations.put("sep", "сен");
        mTranslations.put("oct", "окт");
        mTranslations.put("nov", "ноя");
        mTranslations.put("dec", "дек");

        mTranslations.put("ph", "выходные");
        mTranslations.put("sh", "шк.праздники");
        mTranslations.put("week", "неделя");
        mTranslations.put("sunrise", "восход");
        mTranslations.put("sunset", "закат");
        mTranslations.put("off", "не работает");
        mTranslations.put("open", "открыто");
        mTranslations.put("closed", "закрыто");
    }

    public static String localize(String hours, int language) {
        if (language == 643) {
            if ("24/7".equals(hours))
                return "круглосуточно";
            Matcher m = fullPattern.matcher(hours);
            StringBuffer sb = new StringBuffer();
            while (m.find()) {
                m.appendReplacement(sb, mTranslations.get(m.group(1).toLowerCase()));
            }
            m.appendTail(sb);
            return sb.toString();
        } else {
            if ("24/7".equals(hours))
                return "round-the-clock";
            else
                return hours;
        }
    }
}
