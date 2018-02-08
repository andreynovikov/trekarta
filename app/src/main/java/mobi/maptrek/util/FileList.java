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

/**
 * Recursive file listing under a specified directory.
 *
 * @author Andrey Novikov
 */
package mobi.maptrek.util;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class FileList {
    /**
     * Recursively walk a directory tree and return a List of all
     * files found; the List is sorted using File.compareTo().
     *
     * @param startingDir root directory, must be valid directory which can be read.
     * @param filter      <code>FilenameFilter</code> to filter files.
     * @return <code>List</code> containing found <code>File</code> objects or empty <code>List</code> otherwise.
     */

    static public List<File> getFileListing(final File startingDir, final FilenameFilter filter) {
        List<File> result = getFileListingNoSort(startingDir, filter);
        Collections.sort(result);
        return result;
    }

    static private List<File> getFileListingNoSort(final File startingDir, final FilenameFilter filter) {
        List<File> result = new ArrayList<>();

        // find files
        File[] files = startingDir.listFiles(filter);
        if (files != null)
            result.addAll(Arrays.asList(files));

        // go deeper
        DirFileFilter dirFilter = new DirFileFilter();
        File[] dirs = startingDir.listFiles(dirFilter);
        if (dirs != null) {
            for (File dir : dirs) {
                List<File> deeperList = getFileListingNoSort(dir, filter);
                result.addAll(deeperList);
            }
        }

        // if nothing find may be it empty and can be deleted,
        // current logic does not remove nested empty dirs
        //noinspection ConstantConditions
        if (startingDir.isDirectory() && (files == null || files.length == 0) && (dirs == null || dirs.length == 0)) {
            String[] items = startingDir.list();
            if (items == null || items.length == 0)
                //noinspection ResultOfMethodCallIgnored
                startingDir.delete();
        }

        return result;
    }

}
