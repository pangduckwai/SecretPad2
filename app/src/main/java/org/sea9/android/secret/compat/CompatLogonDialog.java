package org.sea9.android.secret.compat;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.Checkable;
import android.widget.EditText;

import org.sea9.android.secret.R;

public class CompatLogonDialog extends DialogFragment {
	public static final String TAG = "secret.compatlogon_dialog";

	private EditText txtPasswd;
	private CheckBox chkbSmart;

	@Override @NonNull
	public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
		Activity activity = getActivity();
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		if (activity != null) {
			LayoutInflater inflater = activity.getLayoutInflater();
			@SuppressLint("InflateParams") View view = inflater.inflate(R.layout._compat_logon, null);
			txtPasswd = view.findViewById(R.id.password);
			txtPasswd.setOnEditorActionListener((v, actionId, event) -> {
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					logon();
				}
				return false;
			});

			chkbSmart = view.findViewById(R.id.smart);

			builder.setView(view)
					.setPositiveButton(R.string.btn_logon, (dialog, id) -> logon())
					.setNegativeButton(R.string.btn_cancel, (dialog, id) -> CompatLogonDialog.this.getDialog().cancel());
		}
		return builder.create();
	}

	private void logon() {
		Editable txt = txtPasswd.getText();
		int len = txt.length();
		if (len <= 0) {
			callback.onCompatLogon(null, false);
		} else {
			char[] ret = new char[len];
			txt.getChars(0, len, ret, 0);
			txt.clear();
			callback.onCompatLogon(ret, chkbSmart.isChecked());
		}
		dismiss();
	}

	/*========================================
	 * Callback interface to the MainActivity
	 */
	public interface Callback {
		void onCompatLogon(char[] value, boolean smart);
	}
	private Callback callback;

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		Log.d(TAG, "onAttach");
		try {
			callback = (Callback) context;
		} catch (ClassCastException e) {
			throw new ClassCastException(context.toString() + " missing implementation of CompatLogonDialog.Callback");
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		Log.d(TAG, "onDetach");
		callback = null;
	}
}
