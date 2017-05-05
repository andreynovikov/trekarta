package mobi.maptrek.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import mobi.maptrek.R;

public class TextInputDialogFragment extends DialogFragment implements ClipboardManager.OnPrimaryClipChangedListener {
    private boolean mShowPasteButton;
    private TextInputDialogCallback mCallback;
    private ClipboardManager mClipboard;
    private ImageButton mPasteButton;
    private TextView mDescription;

    public interface TextInputDialogCallback {
        void beforeTextChanged(CharSequence s, int start, int count, int after);

        void onTextChanged(CharSequence s, int start, int before, int count);

        void afterTextChanged(Editable s);

        void onTextInputPositiveClick(String id, String inputText);

        void onTextInputNegativeClick(String id);
    }

    @Override
    public void onAttach(Context context) {
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

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        String title = args.getString("title", "");
        String oldValue = args.getString("oldValue", "");
        boolean selectAllOnFocus = args.getBoolean("selectAllOnFocus", false);
        mShowPasteButton = args.getBoolean("showPasteButton", false);
        int inputType = args.getInt("inputType", InputType.TYPE_CLASS_TEXT);
        String hint = args.getString("hint", null);
        final String id = args.getString("id", null);

        final Activity activity = getActivity();

        @SuppressLint("InflateParams")
        View dialogView = activity.getLayoutInflater().inflate(R.layout.dialog_text_input, null);
        final EditText textEdit = (EditText) dialogView.findViewById(R.id.textEdit);

        textEdit.setInputType(inputType);
        if (!"".equals(oldValue))
            textEdit.setText(oldValue);
        textEdit.setSelectAllOnFocus(selectAllOnFocus);
        textEdit.requestFocus();

        textEdit.addTextChangedListener(new TextWatcher() {
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

        if (hint != null) {
            TextInputLayout textInputLayout = (TextInputLayout) dialogView.findViewById(R.id.textWrapper);
            textInputLayout.setHint(hint);
        }

        if (mShowPasteButton) {
            mPasteButton = (ImageButton) dialogView.findViewById(R.id.pasteButton);
            mPasteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mClipboard == null)
                        return;
                    ClipData.Item item = mClipboard.getPrimaryClip().getItemAt(0);
                    CharSequence pasteData = item.getText();
                    if (pasteData != null)
                        textEdit.setText(pasteData);
                }
            });
            onPrimaryClipChanged();
        }

        mDescription = (TextView) dialogView.findViewById(R.id.description);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);
        dialogBuilder.setTitle(title);
        dialogBuilder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mCallback.onTextInputPositiveClick(id, textEdit.getText().toString());
            }
        });
        dialogBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mCallback.onTextInputNegativeClick(id);
            }
        });
        dialogBuilder.setView(dialogView);
        final AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        return alertDialog;
    }

    @Override
    public void onPrimaryClipChanged() {
        if (!mShowPasteButton)
            return;
        int visibility = View.GONE;
        if (mClipboard.hasPrimaryClip() && mClipboard.getPrimaryClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN))
            visibility = View.VISIBLE;
        mPasteButton.setVisibility(visibility);
    }

    public void setDescription(@NonNull CharSequence text) {
        mDescription.setVisibility(text.length() > 0 ? View.VISIBLE : View.GONE);
        mDescription.setText(text);
    }

    public void setCallback(TextInputDialogCallback callback) {
        mCallback = callback;
    }

    public static class Builder {
        private String mTitle;
        private String mOldValue;
        private String mHint;
        private Boolean mSelectAllOnFocus;
        private Integer mInputType;
        private Boolean mShowPasteButton;
        private String mId;
        private TextInputDialogCallback mCallbacks;

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

        public Builder setCallbacks(TextInputDialogCallback callbacks) {
            mCallbacks = callbacks;
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
            dialogFragment.setCallback(mCallbacks);
            dialogFragment.setArguments(args);
            return dialogFragment;
        }
    }
}