/*
 * Copyright 2024 Andrey Novikov
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

package mobi.maptrek.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import mobi.maptrek.R;
import mobi.maptrek.databinding.DialogTextInputBinding;

public class TextInputDialogFragment extends DialogFragment implements ClipboardManager.OnPrimaryClipChangedListener {
    private boolean mShowPasteButton;
    private TextInputDialogCallback mCallback;
    private ClipboardManager mClipboard;
    private DialogTextInputBinding viewBinding;

    public interface TextInputDialogCallback {
        void beforeTextChanged(CharSequence s, int start, int count, int after);

        void onTextChanged(CharSequence s, int start, int before, int count);

        void afterTextChanged(Editable s);

        void onTextInputPositiveClick(String id, String inputText);

        void onTextInputNegativeClick(String id);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mClipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mClipboard = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        onPrimaryClipChanged();
        mClipboard.addPrimaryClipChangedListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        mClipboard.removePrimaryClipChangedListener(this);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = requireArguments();
        String title = args.getString("title", "");
        String oldValue = args.getString("oldValue", "");
        boolean selectAllOnFocus = args.getBoolean("selectAllOnFocus", false);
        mShowPasteButton = args.getBoolean("showPasteButton", false);
        int inputType = args.getInt("inputType", InputType.TYPE_CLASS_TEXT);
        String hint = args.getString("hint", null);
        final String id = args.getString("id", null);

        final Activity activity = requireActivity();

        viewBinding = DialogTextInputBinding.inflate(getLayoutInflater());
        View rootView = viewBinding.getRoot();

        if (!"".equals(title)) {
            rootView.setPadding(
                    rootView.getPaddingLeft(),
                    0,
                    rootView.getPaddingRight(),
                    rootView.getPaddingBottom()
            );
        }

        viewBinding.textEdit.setInputType(inputType);
        viewBinding.textEdit.setText(oldValue);
        viewBinding.textEdit.setSelectAllOnFocus(selectAllOnFocus);
        viewBinding.textEdit.requestFocus();

        viewBinding.textEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (mCallback != null)
                    mCallback.beforeTextChanged(s, start, count, after);
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (mCallback != null)
                    mCallback.onTextChanged(s, start, before, count);
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (mCallback != null)
                    mCallback.afterTextChanged(s);
            }
        });

        if (hint != null)
            viewBinding.textWrapper.setHint(hint);

        if (mShowPasteButton) {
            viewBinding.pasteButton.setOnClickListener(v -> {
                if (mClipboard == null)
                    return;
                ClipData clipData = mClipboard.getPrimaryClip();
                if (clipData == null)
                    return;
                ClipData.Item item = clipData.getItemAt(0);
                CharSequence pasteData = item.getText();
                if (pasteData != null)
                    viewBinding.textEdit.setText(pasteData);
            });
            onPrimaryClipChanged();
        } else {
            viewBinding.pasteButton.setVisibility(View.GONE);
        }

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);
        dialogBuilder.setTitle(title);
        dialogBuilder.setPositiveButton(R.string.ok, (dialog, which) -> mCallback.onTextInputPositiveClick(id, viewBinding.textEdit.getText().toString()));
        dialogBuilder.setNegativeButton(R.string.cancel, (dialog, which) -> mCallback.onTextInputNegativeClick(id));
        dialogBuilder.setView(rootView);
        final AlertDialog alertDialog = dialogBuilder.create();
        Window window = alertDialog.getWindow();
        if (window != null)
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        return alertDialog;
    }

    @Override
    public void onPrimaryClipChanged() {
        if (!mShowPasteButton)
            return;
        int visibility = View.GONE;
        if (mClipboard.hasPrimaryClip()) {
            ClipDescription description = mClipboard.getPrimaryClipDescription();
            if (description != null && description.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN))
                visibility = View.VISIBLE;
        }
        viewBinding.pasteButton.setVisibility(visibility);
    }

    public void setDescription(@NonNull CharSequence text) {
        viewBinding.description.setVisibility(text.length() > 0 ? View.VISIBLE : View.GONE);
        viewBinding.description.setText(text);
    }

    public void setCallback(TextInputDialogCallback callback) {
        mCallback = callback;
    }

    /** @noinspection UnusedReturnValue, unused */
    public static class Builder {
        private String mTitle;
        private String mOldValue;
        private String mHint;
        private Boolean mSelectAllOnFocus;
        private Integer mInputType;
        private Boolean mShowPasteButton;
        private String mId;
        private TextInputDialogCallback mCallback;

        public Builder setTitle(String title) {
            mTitle = title;
            return this;
        }

        public Builder setOldValue(String oldValue) {
            mOldValue = oldValue;
            return this;
        }

        public Builder setHint(String hint) {
            mHint = hint;
            return this;
        }

        public Builder setSelectAllOnFocus(boolean selectAllOnFocus) {
            mSelectAllOnFocus = selectAllOnFocus;
            return this;
        }

        public Builder setShowPasteButton(boolean showPasteButton) {
            mShowPasteButton = showPasteButton;
            return this;
        }

        public Builder setInputType(int inputType) {
            mInputType = inputType;
            return this;
        }

        public Builder setId(String id) {
            mId = id;
            return this;
        }

        public Builder setCallback(TextInputDialogCallback callback) {
            mCallback = callback;
            return this;
        }

        public TextInputDialogFragment create() {
            TextInputDialogFragment dialogFragment = new TextInputDialogFragment();
            Bundle args = new Bundle();

            if (mTitle != null)
                args.putString("title", mTitle);
            if (mOldValue != null)
                args.putString("oldValue", mOldValue);
            if (mHint != null)
                args.putString("hint", mHint);
            if (mSelectAllOnFocus != null)
                args.putBoolean("selectAllOnFocus", mSelectAllOnFocus);
            if (mInputType != null)
                args.putInt("inputType", mInputType);
            if (mShowPasteButton != null)
                args.putBoolean("showPasteButton", mShowPasteButton);
            if (mId != null)
                args.putString("id", mId);
            dialogFragment.setCallback(mCallback);
            dialogFragment.setArguments(args);
            return dialogFragment;
        }
    }
}