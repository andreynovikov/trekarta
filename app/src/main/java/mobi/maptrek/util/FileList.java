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
        if (startingDir.isDirectory() && files.length == 0 && dirs.length == 0 && startingDir.list().length == 0)
            //noinspection ResultOfMethodCallIgnored
            startingDir.delete();

        return result;
    }

}
