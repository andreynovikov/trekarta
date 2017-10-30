package mobi.maptrek.util;

import java.util.StringTokenizer;

public class StringUtils {
    public static String capitalizeFirst(String s) {
        if (s == null || s.length() == 0)
            return "";
        char first = s.charAt(0);
        if (Character.isUpperCase(first))
            return s;
        else
            return Character.toUpperCase(first) + s.substring(1);
    }

    public static String capitalize(String line)
    {
        StringTokenizer token =new StringTokenizer(line);
        String CapLine="";
        while(token.hasMoreTokens())
        {
            String tok = token.nextToken();
            CapLine += Character.toUpperCase(tok.charAt(0))+ tok.substring(1)+" ";
        }
        return CapLine.substring(0,CapLine.length()-1);
    }
}
