package mobi.maptrek.maps.maptrek;

import org.oscim.tiling.ITileDataSource;
import org.oscim.tiling.source.UrlTileDataSource;
import org.oscim.tiling.source.UrlTileSource;

public class MapTrekUrlTileSource extends UrlTileSource {

    private final static String DEFAULT_URL = "http://maptrek.mobi:3579/all";
    private final static String DEFAULT_PATH = "/{Z}/{X}/{Y}.mvt";

    public static class Builder<T extends Builder<T>> extends UrlTileSource.Builder<T> {

        public Builder() {
            super(DEFAULT_URL, DEFAULT_PATH, 8, 17);
        }

        public MapTrekUrlTileSource build() {
            return new MapTrekUrlTileSource(this);
        }
    }

    @SuppressWarnings("rawtypes")
    public static Builder<?> builder() {
        return new Builder();
    }

    protected MapTrekUrlTileSource(Builder<?> builder) {
        super(builder);
    }

    public MapTrekUrlTileSource() {
        this(builder());
    }

    public MapTrekUrlTileSource(String urlString) {
        this(builder().url(urlString));
    }

    @Override
    public ITileDataSource getDataSource() {
        return new UrlTileDataSource(this, new MapboxTileDecoder(), getHttpEngine());
    }
}
