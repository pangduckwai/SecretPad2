package org.sea9.android.secret.core;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;

import org.sea9.android.secret.R;
import org.sea9.android.secret.crypto.CryptoUtils;

public class LogonDialog extends DialogFragment {
	public static final String TAG = "secret.logon_dialog";

	public static LogonDialog getInstance() {
		LogonDialog instance = new LogonDialog();
		instance.setCancelable(false);
		return instance;
	}

	private EditText txtPasswd;
	private Button btnLogon;

	@Override @Nullable
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.logon_dialog, container, false);

		txtPasswd = view.findViewById(R.id.password);
		txtPasswd.setOnEditorActionListener((v, actionId, event) -> {
			if (actionId == EditorInfo.IME_ACTION_DONE) {
				logon();
			}
			return false;
		});

		btnLogon = view.findViewById(R.id.logon);
		btnLogon.setOnClickListener(v -> logon());

		getDialog().setOnKeyListener((dialog, keyCode, event) -> {
			if (keyCode == KeyEvent.KEYCODE_BACK) {
				if (event.getAction() == KeyEvent.ACTION_UP) cancel();
				return true;
			} else {
				return false;
			}
		});

		Window window = getDialog().getWindow();
		if (window != null) window.requestFeature(Window.FEATURE_NO_TITLE);
		return view;
	}

	private boolean logon() {
		Editable txt = txtPasswd.getText();
		int len = txt.length();
		if (len <= 0) {
			Snackbar.make(btnLogon, getString(R.string.msg_passwd_needed), Snackbar.LENGTH_LONG).show();
		} else {
			char[] ret = new char[len];
			txt.getChars(0, len, ret, 0);
			txt.clear();
			callback.onLogon(CryptoUtils.convert(CryptoUtils.encode(CryptoUtils.hash(CryptoUtils.convert(ret)))));
			dismiss();
			return true;
		}
		return false;
	}

	private void cancel() {
		callback.onLogon(null);
		dismiss();
	}

	/*========================================
	 * Callback interface to the MainActivity
	 */
	public interface Callback {
		void onLogon(char[] value);
	}
	private Callback callback;

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		Log.d(TAG, "onAttach");
		try {
			callback = (Callback) context;
		} catch (ClassCastException e) {
			throw new ClassCastException(context.toString() + " missing implementation of LogonDialog.Callback");
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		Log.d(TAG, "onDetach");
		callback = null;
	}
}