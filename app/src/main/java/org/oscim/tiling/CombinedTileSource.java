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

package org.oscim.tiling;

import org.oscim.backend.canvas.Bitmap;
import org.oscim.core.MapElement;
import org.oscim.layers.tile.MapTile;

public class CombinedTileSource extends TileSource {
    private static final byte MAP_FILE_MIN_ZOOM = 8;

    private TileSource mMapFileSource;
    private TileSource mUrlSource;

    public CombinedTileSource(TileSource mapFileSource, TileSource urlSource) {
        super(0, 17);
        mMapFileSource = mapFileSource;
        mUrlSource = urlSource;
    }

    @Override
    public ITileDataSource getDataSource() {
        return new CombinedDataSource(mMapFileSource.getDataSource(), mUrlSource.getDataSource());
    }

    @Override
    public OpenResult open() {
        mMapFileSource.open();
        return mUrlSource.open();
    }

    @Override
    public void close() {
        mMapFileSource.close();
        mUrlSource.close();
    }

    class CombinedDataSource implements ITileDataSource {
        private ITileDataSource mMapFileDataSource;
        private ITileDataSource mUrlDataSource;

        public CombinedDataSource(ITileDataSource mapFileDataSource, ITileDataSource urlDataSource) {
            mMapFileDataSource = mapFileDataSource;
            mUrlDataSource = urlDataSource;
        }

        @Override
        public void query(MapTile tile, ITileDataSink mapDataSink) {
            if (tile.zoomLevel < MAP_FILE_MIN_ZOOM) {
                mUrlDataSource.query(tile, mapDataSink);
            } else {
                ProxyTileDataSink proxyDataSink = new ProxyTileDataSink(mapDataSink);
                mMapFileDataSource.query(tile, proxyDataSink);
                if (proxyDataSink.result != QueryResult.SUCCESS)
                    mUrlDataSource.query(tile, proxyDataSink);

                mapDataSink.completed(proxyDataSink.result);
            }
        }

        @Override
        public void dispose() {
            mMapFileDataSource.dispose();
            mUrlDataSource.dispose();
        }

        @Override
        public void cancel() {
            mMapFileDataSource.cancel();
            mUrlDataSource.cancel();
        }
    }

    class ProxyTileDataSink implements ITileDataSink {
        ITileDataSink mapDataSink;
        QueryResult result;
        boolean hasElements;

        public ProxyTileDataSink(ITileDataSink mapDataSink) {
            this.mapDataSink = mapDataSink;
            hasElements = false;
        }

        @Override
        public void process(MapElement element) {
            mapDataSink.process(element);
            hasElements = true;
        }

        @Override
        public void setTileImage(Bitmap bitmap) {
            // There should be no bitmaps in vector data sources
            // but we will put it here for convenience
            mapDataSink.setTileImage(bitmap);
        }

        @Override
        public void completed(QueryResult result) {
            this.result = result;
        }
    }
}
