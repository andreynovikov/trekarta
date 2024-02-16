/*
 * Copyright 2012 osmdroid authors:
 * Copyright 2012 Nicolas Gramlich
 * Copyright 2012 Theodore Hong
 * Copyright 2012 Fred Eisele
 * 
 * Copyright 2013 Hannes Janetzek
 *
 * This file is part of the OpenScienceMap project (http://www.opensciencemap.org).
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
 */

package mobi.maptrek.layers.marker;

import org.oscim.core.Point;
import org.oscim.layers.Layer;
import org.oscim.map.Map;

/**
 * Draws a list of {@link MarkerItem} as markers to a map. The item with the
 * lowest index is drawn as last and therefore the 'topmost' marker. It also
 * gets checked for onTap first. This class is generic, because you then you get
 * your custom item-class passed back in onTap(). << TODO
 */
public abstract class MarkerLayer<Item extends MarkerItem> extends Layer {

	protected final MarkerRenderer mMarkerRenderer;
	protected Item mFocusedItem;
    protected int mFocusColor;

    /**
	 * Method by which subclasses create the actual Items. This will only be
	 * called from populate() we'll cache them for later use.
	 */
	protected abstract Item createItem(int i);

	/**
	 * The number of items in this overlay.
	 */
	public abstract int size();

	@SuppressWarnings("unchecked")
	public MarkerLayer(Map map, MarkerSymbol defaultSymbol, float scale, int outlineColor) {
		super(map);

		mMarkerRenderer = new MarkerRenderer((MarkerLayer<MarkerItem>) this, defaultSymbol, scale, outlineColor);
		mRenderer = mMarkerRenderer;
	}

	public void setOutlineColor(int color) {
		mMarkerRenderer.setOutlineColor(color);
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		mMarkerRenderer.update();
	}

	/**
	 * Utility method to perform all processing on a new ItemizedOverlay.
	 * Subclasses provide Items through the createItem(int) method. The subclass
	 * should call this as soon as it has data, before anything else gets
	 * called.
	 */
	protected final void populate() {
		mMarkerRenderer.populate(size());
	}

	/**
	 * TODO
	 * If the given Item is found in the overlay, force it to be the current
	 * focus-bearer. Any registered {link ItemizedLayer#OnFocusChangeListener}
	 * will be notified. This does not move the map, so if the Item isn't
	 * already centered, the user may get confused. If the Item is not found,
	 * this is a no-op. You can also pass null to remove focus.
	 * 
	 * @param item
	 */
	public void setFocus(Item item) {
		mFocusedItem = item;
		mMarkerRenderer.update();
		mMap.updateMap(true);
	}

    public void setFocus(Item item, int color) {
        mFocusedItem = item;
        mFocusColor = color;
        mMarkerRenderer.update();
        mMap.updateMap(true);
    }

    /**
	 * @return the currently-focused item, or null if no item is currently
	 *         focused.
	 */
	public Item getFocus() {
		return mFocusedItem;
	}

	public void setTitlesEnabled(boolean titlesEnabled) {
		mMarkerRenderer.setTitlesEnabled(titlesEnabled);
	}
}
