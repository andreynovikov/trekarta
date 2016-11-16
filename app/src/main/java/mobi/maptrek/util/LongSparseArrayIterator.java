package mobi.maptrek.util;

import android.util.LongSparseArray;

import java.util.ListIterator;
import java.util.NoSuchElementException;

public final class LongSparseArrayIterator<E> implements ListIterator<E> {

    private final LongSparseArray<E> array;
    private int cursor;
    private boolean cursorNowhere;

    /**
     * @param array to iterate over.
     * @return A ListIterator on the elements of the SparseArray. The elements
     * are iterated in the same order as they occur in the SparseArray.
     * {@link #nextIndex()} and {@link #previousIndex()} return a
     * SparseArray index, not a key!
     */
    public static <E> ListIterator<E> iterate(LongSparseArray<E> array) {
        return new LongSparseArrayIterator<>(array, -1);
    }

    private LongSparseArrayIterator(LongSparseArray<E> array, int location) {
        this.array = array;
        if (location < 0) {
            cursor = -1;
            cursorNowhere = true;
        } else if (location < array.size()) {
            cursor = location;
            cursorNowhere = false;
        } else {
            cursor = array.size() - 1;
            cursorNowhere = true;
        }
    }

    @Override
    public boolean hasNext() {
        return cursor < array.size() - 1;
    }

    @Override
    public boolean hasPrevious() {
        return cursorNowhere && cursor >= 0 || cursor > 0;
    }

    @Override
    public int nextIndex() {
        if (hasNext()) {
            return array.indexOfKey(array.keyAt(cursor + 1));
        } else {
            throw new NoSuchElementException();
        }
    }

    @Override
    public int previousIndex() {
        if (hasPrevious()) {
            if (cursorNowhere) {
                return array.indexOfKey(array.keyAt(cursor));
            } else {
                return array.indexOfKey(array.keyAt(cursor - 1));
            }
        } else {
            throw new NoSuchElementException();
        }
    }

    @Override
    public E next() {
        if (hasNext()) {
            if (cursorNowhere) {
                cursorNowhere = false;
            }
            cursor++;
            return array.valueAt(cursor);
        } else {
            throw new NoSuchElementException();
        }
    }

    @Override
    public E previous() {
        if (hasPrevious()) {
            if (cursorNowhere) {
                cursorNowhere = false;
            } else {
                cursor--;
            }
            return array.valueAt(cursor);
        } else {
            throw new NoSuchElementException();
        }
    }

    @Override
    public void add(E object) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void remove() {
        if (!cursorNowhere) {
            array.remove(array.keyAt(cursor));
            cursorNowhere = true;
            cursor--;
        } else {
            throw new IllegalStateException();
        }
    }

    @Override
    public void set(E object) {
        if (!cursorNowhere) {
            array.setValueAt(cursor, object);
        } else {
            throw new IllegalStateException();
        }
    }
}