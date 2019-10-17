/*
 * Copyright 2019 Andrey Novikov
 *
 * Derived from Python port by Max Smith
 * Original Smaz by Salvatore Sanfilippo
 *
 * BSD license per original C implementation at https://github.com/antirez/smaz
 *
 */

package mobi.maptrek.util;

public class Smaz {
    private static final String[] DECODE = {
            "+", "the", "e", "t", "a", "of", "o", "and", "i", "n", "s", "e ", "r", " th",
            "+t", "in", "he", "th", "h", "he ", "to", "https://", "l", "s ", "d", " a", "an",
            "er", "c", " o", "d ", "on", " of", "re", "of ", "t ", "www.", "is", "u", "at",
            "xn--", "n ", "or", "which", "f", "m", "as", "it", "that", "%", "was", "en",
            "about", " w", "es", " an", " i", "\r", "f ", "g", "p", "nd", " s", "nd ", "ed ",
            "w", "ed", "http://", "for", "te", "ing", "y ", "The", " c", "ti", "r ", "his",
            "st", " in", "ar", "nt", ",", " to", "y", "ng", " h", "with", "le", "al", "to ",
            "b", "ou", "be", "were", " b", "se", "o ", "ent", "ha", "ng ", "their", "_",
            "hi", "from", " f", "in ", "de", "ion", "me", "v", ".", "ve", "all", "re ",
            "ri", "ro", "is ", "co", "f t", "are", "ea", ". ", "her", " m", "er ", " p",
            "es ", "by", "they", "di", "ra", "ic", "not", "s, ", "d t", "at ", "ce", "la",
            "h ", "ne", "as ", "tio", "on ", "n t", "io", "we", " a ", "om", ", a", "s o",
            "ur", "li", "ll", "ch", "had", "this", "e t", "g ", ".ru", " wh", "ere",
            " co", "e o", "a ", "us", " d", "ss", ".org", ".html", "vk.com", " be", " e",
            "s a", "ma", "one", "t t", "or ", "but", "el", "so", "l ", "e s", "s,", "no",
            "ter", " wa", "iv", "ho", "e a", " r", "hat", "s t", "ns", "ch ", "wh", "tr",
            "ut", "/", "have", "ly ", "ta", " ha", " on", "tha", "-", " l", "ati", "en ",
            "pe", " re", "there", "ass", "si", " fo", "wa", "ec", "our", "who", "its", "z",
            "fo", "rs", "#", "ot", "un", "&", "im", "th ", "nc", "ate", "info", "ver", "ad",
            " we", "ly", "ee", " n", "id", " cl", "ac", "il", "www", "rt", " wi", "div",
            "e, ", " it", "whi", " ma", "ge", "x", "e c", "men", ".com"
    };

    public static final String[] WEBSITE_DECODE = {
            "https://www.", "https://", "http://www.", "http://", "www.", ".com", "n", ".de",
            "restaurant", "facebook", ".co.uk", "er", "/index.php", ".html", "en", "8", "D", "v",
            "in", ".org", "ar", "hotel", "st", "an", "%3D", "location", "ch", "or", "al", "es",
            "on", "at", "el", "it", "et", "e-", "%25", "il", "ur", "ol", "as", "re", "is", "au",
            ".gov", "ro", "am", "u", "y", ".ru", "apothek", ".pl", "co", "de", ".nl", "%26", "ic",
            ".fr", "un", "ho", "le", "us", "qr.", "?", "ad", "ed", "ag", "ac", "e/", "id", "a",
            "a-", "th", "A", "s/", "e", "to", "lo", "ap", "s-", "ri", "sp", "em", "ir", ".b",
            "club", ".htm", "ca", "im", "um", "/p", "ak", ".n", "/", "ut", "tr", "ab", "00", ".jp",
            "ik", "ie", "ul", ".php", "ne", "sk", "ig", "mo", "eb", ".uk", "av", "-b", "ec", "g",
            "9", "d", "om", "s", "-s", "6", "io", "m", "/s", "ay", "he", "/f", "-p", ".", ".dk",
            "bo", "gr", "-m", "ti", "w", "li", "a/", "j", "op", "qu", ".cz", "os", "br", "/c", "ot",
            "/m", "af", "ub", "10", "do", "ek", "20", "go", "-", "di", "pl", ".hu", "no", "z", "fi",
            "f", "ah", "-k", "-c", "/h", "up", "ff", "sh", "/w", "-d", "/b", "fo", "ok", "az", "pr",
            "eg", "ts", "od", "ck", "/k", "t-", ".ua", "ip", "ia", "i", "ge", "ud", "be", "-h", "2",
            "ep", "p", "C", "iv", ".s", "sb", "la", "me", "so", "eu", "S", "eh", "ma", "_", "iz",
            "ob", "lu", "l", "ow", "12", "-w", "ev", "b", "x", "fr", "si", "ov", "t/", "%2C", "B",
            "P", "d-", "c", "4", "k", "0", "1", "/d", "r", "ta", "7", "ty", "t", "o", "h", "ba",
            "5", "3", "xn--", "e.", "we", "t.", "te", "a.", "s.", "o.", "se", "ra", "i.", "u.",
            "ph", "ohn", "11", "-l", "wood"
    };

    public static final String[] OPENING_HOURS_DECODE = {
            ":00", "Mo-Fr 0", "Mo-Su ", "Mo-Sa 0", "; Sa 0", ":30-1",
            "; Jan 1,Apr 19,May 1,Jun 24,Jul 5,Jul 24,Oct 12,Dec 24,Dec 25,Dec 31 ", "Mo-Fr 1",
            "; Jan 01,Apr 19,May 01,Jun 24,Jul 05,Jul 24,Oct 12,Dec 24,Dec 25,Dec 31 off",
            "-", "; Su ", ":30", "24/7", "Sa 1", "1", "; PH off", "; Sa-Su ", "Mo-Th ",
            "sunrise-sunset", "Fr 0", "; Su,PH off", "; ", "-1", " ", "Sa 0", "Mo-", " off", "Tu-",
            "Th 0", "-2", "PH closed", "Sa,Su", "09", ", ", "08", "Mo,Tu,Th", ",", "Su", "Fr", "10",
            "-0", "We", " appointment", "sunset-sunrise", "3:59", "Mo", ":45", "Th", "closed",
            ":15", ",1", "07", "11", "Tu", ",PH", "Apr-Oct", "mo-fr", "Sa", "off", "Apr-Sep", "12",
            "06", "Dec 2", "t", "Nov-Mar", "Oct-Mar", "9:", "May-Sep", ";", " open", "Oct", " - ",
            "Jul-Aug", "(", "Jun-Aug", ": ", "PH", "6", " 9", "Sep", "Mar", "00", "sunset",
            "dusk-dawn", "V", "Apr", "Public Holi", "H", "unknown", "May", "c", "Jan", "Nov", " 8",
            "sunrise", "Dec 31", "Bank Holi", "05", " || ", "o", "day", "Closed", "f", "S", "g",
            "bank holi", "Jun", "_", "h", "e", "5 ", ".", "-Dec", " 01", ":20", "AM to", ":40", "5",
            "17", "on call", "see timetable", "i", "-Feb", "4", "easter", "5-", "A", "Aug",
            "dawn-dusk", " -", "7", "13", "T", "16", "pleas", "14", "d", "2:", "mo-sa", " 7", ":50",
            "F", "n", "r", "er", "tu-fr", "18", "mo-s", "|", "M", "15", " hours", " 31", "call us",
            "Jul", "Off", "30", "20", "P", "en", "-53/2", "Dec", " to", "on", "week", ":35", " a",
            ":55", "B", "in", "19", "[1]", "a", " OFF", "un", " 6", "04", "pm", "e ", "ur", "PM",
            "0", "Feb", "p.m.", "es", ":25", " n", "b", "22", "am", "[", "8", "ch", ")", "So", "su",
            "21", "sa", "D", "edi", "through", "tag", "[1,3]", "dusk", "t ", "Do", "p", "SH", "2",
            " by", "]", "Di", "m", "ar", "th", "l", "k", "or", "+", "/", "Ph", "L", " 5", "v", "d ",
            "seas", "23", "03", "AM", "24", "an", "y", ":", "fr", "ti", "s ", "[2]", "[2,4]", "3",
            "st", "w", "N", "i ", "ly", "-3", "s", "n-", "[3]", "-dawn", "O", ". ", "at", "9", "u"
    };

    public static final String[] PHONE_DECODE = {
            "+49", "+3", "22", "00", "+441", "33", "12", "55", "14", "17", "88", "13", "90", "42",
            "15", "16", "77", "32", "49", "+4", "18", "66", "19", "02", "34", "35", "62", "10",
            "44", "78", "52", "72", "36", "30", "99", "50", "80", "82", "70", "39", "60", "40",
            "38", "75", "79", "74", "11", "76", "56", "89", "86", "46", "59", "54", "84", "92",
            "85", "37", "64", "31", "69", "58", "09", "73", "04", "+8", "53", "48", "06", "45",
            "+6", "47", "20", "68", "57", "96", "08", "98", "05", "26", "24", "94", "87", "+7",
            "51", "03", "71", "65", "25", "43", "29", "07", "83", "67", "63", "+5", "+9", "95",
            "81", "41", "21", "93", "28", "97", "61", "23", "91", "+1", "01", "27", "+2", "+0",
            "1+", "5+", ";", "4+", "6+", "3+", "0+", "9+", "8+", "7+", ",", "3", "4", "1", "2",
            "0", "5", "9", "8", "6", "7", "+"
    };

    /**
     * Returns decoded text from the input_str using the SMAZ algorithm by default
     *
     * @param input_str encoded text
     * @return decoded text
     */
    public static String decompress(String input_str) {
        return decompress(input_str, DECODE);
    }

    /**
     * Returns decoded text from the input_str using custom SMAZ codebook
     *
     * @param input_str        encoded text
     * @param decompress_table codebook array
     * @return decoded text
     */
    public static String decompress(String input_str, String[] decompress_table) {
        if (input_str == null)
            return null;

        int input_str_len = input_str.length();
        StringBuilder output = new StringBuilder();
        int pos = 0;
        while (pos < input_str_len) {
            char ch = input_str.charAt(pos);
            pos += 1;
            int z;
            if (ch < 254) {
                // Code table entry
                output.append(decompress_table[ch]);
                z = 1;
            } else {
                char next_byte = input_str.charAt(pos);
                pos += 1;
                if (254 == ch) {
                    // Verbatim byte
                    output.append(next_byte);
                    z = 2;
                } else { // 255 == ch:
                    // Verbatim string
                    int end_pos = pos + next_byte + 1;
                    if (end_pos > input_str_len) {
                        throw new IndexOutOfBoundsException("Invalid input to decompress - buffer overflow");
                    }
                    for (int j = 0; j < end_pos; j++) {
                        output.append(input_str.charAt(pos + j));
                    }
                    pos = end_pos;
                    z = 3;
                }
            }
        }
        return output.toString();
    }

}
