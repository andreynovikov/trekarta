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

package mobi.maptrek.dialogs;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.Spannable;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import mobi.maptrek.R;
import mobi.maptrek.util.JosmCoordinatesParser;

public class CoordinatesInput extends DialogFragment {
    private int mColorTextPrimary;
    private int mColorDarkBlue;
    private int mColorRed;
    private final String mLineSeparator = System.getProperty("line.separator");

    private CoordinatesInputDialogCallback mCallback;
    private AlertDialog mDialog;

    public interface CoordinatesInputDialogCallback {
        void onTextInputPositiveClick(String id, String inputText);

        void onTextInputNegativeClick(String id);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        mColorTextPrimary = context.getColor(R.color.textColorPrimary);
        mColorDarkBlue = context.getColor(R.color.darkBlue);
        mColorRed = context.getColor(R.color.red);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        String title = args.getString("title", "");
        final String id = args.getString("id", null);

        final Activity activity = getActivity();

        @SuppressLint("InflateParams")
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_coordinates_input, null);
        final EditText textEdit = dialogView.findViewById(R.id.coordinatesEdit);

        textEdit.requestFocus();

        textEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 0)
                    return;
                String[] lines = s.toString().split(mLineSeparator);
                int offset = 0;
                ForegroundColorSpan[] spans = s.getSpans(0, s.length(), ForegroundColorSpan.class);
                if (spans != null && spans.length > 0) {
                    Log.e("CID", "L: " + spans.length);
                    for (ForegroundColorSpan span : spans)
                        s.removeSpan(span);
                }
                for (String line : lines) {
                    try {
                        JosmCoordinatesParser.Result result = JosmCoordinatesParser.parseWithResult(line);
                        s.setSpan(
                                new ForegroundColorSpan(mColorDarkBlue),
                                offset,
                                offset + result.offset,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        s.setSpan(
                                new ForegroundColorSpan(mColorTextPrimary),
                                offset + result.offset,
                                s.length(),
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    } catch (IllegalArgumentException e) {
                        s.setSpan(
                                new ForegroundColorSpan(mColorRed),
                                offset,
                                s.length(),
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                    offset += line.length() + mLineSeparator.length();
                }
            }
        });

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);
        dialogBuilder.setTitle(title);
        dialogBuilder.setPositiveButton(R.string.ok, (dialog, which) -> mCallback.onTextInputPositiveClick(id, textEdit.getText().toString()));
        dialogBuilder.setNegativeButton(R.string.cancel, (dialog, which) -> mCallback.onTextInputNegativeClick(id));
        dialogBuilder.setNeutralButton(R.string.explain, null);
        dialogBuilder.setView(dialogView);
        mDialog = dialogBuilder.create();
        mDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        // Workaround to prevent dialog dismissing
        mDialog.setOnShowListener(dialogInterface -> {
            // Hide keyboard
            mDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(mDialog.getWindow().getDecorView().getWindowToken(), 0);
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage(R.string.msgCoordinatesInputExplanation);
                builder.setPositiveButton(R.string.ok, null);
                AlertDialog dialog = builder.create();
                dialog.show();
            });
        });
        return mDialog;
    }

    public void setCallback(CoordinatesInputDialogCallback callback) {
        mCallback = callback;
    }

    public static class Builder {
        private String mTitle;
        private String mId;
        private CoordinatesInputDialogCallback mCallbacks;

        public Builder setTitle(String title) {
            mTitle = title;
            return this;
        }

        public Builder setId(String id) {
            mId = id;
            return this;
        }

        public Builder setCallbacks(CoordinatesInputDialogCallback callbacks) {
            mCallbacks = callbacks;
            return this;
        }

        public CoordinatesInput create() {
            CoordinatesInput dialogFragment = new CoordinatesInput();
            Bundle args = new Bundle();

            if (mTitle != null)
                args.putString("title", mTitle);
            if (mId != null)
                args.putString("id", mId);
            dialogFragment.setCallback(mCallbacks);
            dialogFragment.setArguments(args);
            return dialogFragment;
        }
    }
}