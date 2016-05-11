package mobi.maptrek.io;

import java.io.File;
import java.io.FilenameFilter;

public class DataFilenameFilter implements FilenameFilter {

    @Override
    public boolean accept(final File dir, final String filename) {
        String lc = filename.toLowerCase();
        return lc.endsWith(TrackManager.EXTENSION) || lc.endsWith(GPXManager.EXTENSION) || lc.endsWith(KMLManager.EXTENSION);
    }

}
