package mobi.maptrek.data.source;

import mobi.maptrek.io.TrackManager;

public class FileDataSource extends MemoryDataSource {
    public String path;
    // Native format helper data
    public long propertiesOffset;

    @Override
    public boolean isNativeTrack() {
        return path != null && path.endsWith(TrackManager.EXTENSION);
    }
}
