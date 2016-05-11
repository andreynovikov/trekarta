package mobi.maptrek.data.style;

public abstract class Style<T> {
    public String id;

    public abstract boolean isDefault();
    public abstract void copy(T style);
}
