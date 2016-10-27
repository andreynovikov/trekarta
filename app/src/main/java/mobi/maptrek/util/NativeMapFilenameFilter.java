package mobi.maptrek.util;

import java.io.File;
import java.io.FilenameFilter;

public class NativeMapFilenameFilter implements FilenameFilter {
    @Override
    public boolean accept(final File dir, final String filename) {
        String lc = filename.toLowerCase();
        return lc.endsWith(".map") || lc.endsWith(".enqueue");
    }
}
