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

package mobi.maptrek.fragments;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.view.ActionProvider;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;

/**
 * Description of a single menu item that the user can select.
 */
public class PanelMenuItem implements MenuItem {
    /**
     * Default value for {@link PanelMenuItem#id MenuItem.id} indicating that no
     * identifier value is set. All other values (including those below -1)
     * are valid.
     */
    public static final int HEADER_ID_UNDEFINED = -1;

    /**
     * Identifier for this header, to correlate with a new list when
     * it is updated. The default value is {@link PanelMenuItem#HEADER_ID_UNDEFINED}, meaning no id.
     *
     * @attr ref R.styleable#MenuItem_id
     */
    private int id = HEADER_ID_UNDEFINED;

    /**
     * Title of the header that is shown to the user.
     *
     * @attr ref R.styleable#MenuItem_title
     */
    private CharSequence title;

    /**
     * Optional icon to show for this item.
     *
     * @attr ref R.styleable#MenuItem_icon
     */
    private Drawable icon;

    /**
     * Optional check state. Item is checkable if not null.
     *
     * @attr ref R.styleable#MenuItem_checkable
     */
    private Boolean checked;

    /**
     * Optional action view.
     *
     * @attr ref R.styleable#MenuItem_actionLayout
     */
    private View actionView;

    /**
     * Optional intent.
     *
     * @attr ref R.styleable#MenuItem_intent
     */
    private Intent intent;

    private Context mContext;

    public PanelMenuItem(Context context) {
        mContext = context;
    }

    public PanelMenuItem setItemId(int id) {
        this.id = id;
        return this;
    }

    @Override
    public int getItemId() {
        return id;
    }

    @Override
    public int getGroupId() {
        return 0;
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public PanelMenuItem setTitle(CharSequence title) {
        this.title = title;
        return this;
    }

    @Override
    public PanelMenuItem setTitle(@StringRes int title) {
        this.title = mContext.getString(title);
        return this;
    }

    @Override
    public CharSequence getTitle() {
        return title;
    }

    @Override
    public PanelMenuItem setTitleCondensed(CharSequence title) {
        //FIXME Unimplemented
        return this;
    }

    @Override
    public CharSequence getTitleCondensed() {
        return null;
    }

    @Override
    public PanelMenuItem setIcon(Drawable icon) {
        this.icon = icon;
        return this;
    }

    @Override
    public PanelMenuItem setIcon(@DrawableRes int iconRes) {
        this.icon = mContext.getDrawable(iconRes);
        return this;
    }

    @Override
    public Drawable getIcon() {
        return icon;
    }

    @Override
    public PanelMenuItem setIntent(Intent intent) {
        this.intent = intent;
        return this;
    }

    @Override
    public Intent getIntent() {
        return intent;
    }

    @Override
    public PanelMenuItem setShortcut(char numericChar, char alphaChar) {
        //FIXME Unimplemented
        return this;
    }

    @Override
    public PanelMenuItem setNumericShortcut(char numericChar) {
        //FIXME Unimplemented
        return this;
    }

    @Override
    public char getNumericShortcut() {
        return 0;
    }

    @Override
    public PanelMenuItem setAlphabeticShortcut(char alphaChar) {
        //FIXME Unimplemented
        return this;
    }

    @Override
    public char getAlphabeticShortcut() {
        return 0;
    }

    @Override
    public PanelMenuItem setCheckable(boolean checkable) {
        if (checkable)
            checked = Boolean.FALSE;
        return this;
    }

    @Override
    public boolean isCheckable() {
        return checked != null;
    }

    @Override
    public PanelMenuItem setChecked(boolean checked) {
        this.checked = checked;
        return this;
    }

    @Override
    public boolean isChecked() {
        return checked != null && checked;
    }

    @Override
    public PanelMenuItem setVisible(boolean visible) {
        //FIXME Unimplemented
        return this;
    }

    @Override
    public boolean isVisible() {
        return true;
    }

    @Override
    public PanelMenuItem setEnabled(boolean enabled) {
        //FIXME Unimplemented
        return this;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean hasSubMenu() {
        return false;
    }

    @Override
    public SubMenu getSubMenu() {
        return null;
    }

    @Override
    public PanelMenuItem setOnMenuItemClickListener(OnMenuItemClickListener menuItemClickListener) {
        return this;
    }

    @Override
    public ContextMenu.ContextMenuInfo getMenuInfo() {
        //FIXME Unimplemented
        return null;
    }

    @Override
    public void setShowAsAction(int actionEnum) {
        //FIXME Unimplemented
    }

    @Override
    public PanelMenuItem setShowAsActionFlags(int actionEnum) {
        //FIXME Unimplemented
        return this;
    }

    @Override
    public PanelMenuItem setActionView(View view) {
        this.actionView = view;
        return this;
    }

    @Override
    public PanelMenuItem setActionView(int resId) {
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.actionView = inflater.inflate(resId, null);
        return this;
    }

    @Override
    public View getActionView() {
        return actionView;
    }

    @Override
    public PanelMenuItem setActionProvider(ActionProvider actionProvider) {
        //FIXME Unimplemented
        return this;
    }

    @Override
    public ActionProvider getActionProvider() {
        return null;
    }

    @Override
    public boolean expandActionView() {
        return false;
    }

    @Override
    public boolean collapseActionView() {
        return false;
    }

    @Override
    public boolean isActionViewExpanded() {
        return false;
    }

    @Override
    public PanelMenuItem setOnActionExpandListener(OnActionExpandListener listener) {
        //FIXME Unimplemented
        return this;
    }
}
