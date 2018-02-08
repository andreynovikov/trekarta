/*
 * Copyright 2018 Andrey Novikov
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileUtils {
    public static String unusable = "*+~|<>!?\\/:";

    /**
     * Replace illegal characters in a filename with "_" Illegal characters: : \
     * / * ? | < >
     *
     * @param name Proposed file name
     * @return sanitized string
     */
    public static String sanitizeFilename(String name) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            if (unusable.indexOf(name.charAt(i)) > -1)
                sb.append("_");
            else
                sb.append(name.charAt(i));
        }
        return sb.toString();
    }

    public static void copyFile(File from, File to) throws IOException {
        InputStream in = new FileInputStream(from);
        copyStreamToFile(in, to);
    }

    public static void copyStreamToFile(InputStream in, File to) throws IOException {
        OutputStream out = new FileOutputStream(to);

        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1)
            out.write(buffer, 0, read);
        in.close();

        out.flush();
        out.close();
    }
}