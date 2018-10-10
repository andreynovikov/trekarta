package org.oscim.layers.tile.vector.labeling;

import org.oscim.renderer.bucket.SymbolItem;
import org.oscim.utils.pool.Pool;

final class SymbolPool extends Pool<SymbolItem> {
    Symbol releaseAndGetNext(Symbol l) {
        if (l.item != null)
            l.item = SymbolItem.pool.release(l.item);

        // drop references
        l.item = null;
        Symbol ret = (Symbol) l.next;

        // ignore warning
        super.release(l);
        return ret;
    }

    @Override
    protected Symbol createItem() {
        return new Symbol();
    }
}