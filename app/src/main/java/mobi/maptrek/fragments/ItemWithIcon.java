package mobi.maptrek.fragments;

public class ItemWithIcon {
    public final String text;
    public final int icon;

    public ItemWithIcon(String text, Integer icon) {
        this.text = text;
        this.icon = icon;
    }

    @Override
    public String toString() {
        return text;
    }
}
