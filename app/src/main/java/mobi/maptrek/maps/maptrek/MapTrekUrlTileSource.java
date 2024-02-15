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

package mobi.maptrek.maps.maptrek;

import org.oscim.tiling.ITileDataSource;
import org.oscim.tiling.source.UrlTileDataSource;
import org.oscim.tiling.source.UrlTileSource;

public class MapTrekUrlTileSource extends UrlTileSource {

    private static final String DEFAULT_URL = "http://maptrek.mobi:3579/all";
    private static final String DEFAULT_PATH = "/{Z}/{X}/{Y}.mvt";

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
        return new UrlTileDataSource(this, new MapTrekTileDecoder(), getHttpEngine());
    }
}
