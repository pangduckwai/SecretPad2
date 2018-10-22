package org.sea9.android.secret.core;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import org.sea9.android.secret.R;
import org.sea9.android.secret.crypto.CryptoUtils;

import java.util.List;

public class LogonDialog2 extends DialogFragment {
	public static final String TAG = "secret.logon_dialog";

	public static LogonDialog2 getInstance() {
		LogonDialog2 instance = new LogonDialog2();
		instance.setCancelable(false);
		return instance;
	}

	private EditText txtPasswd;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		return super.onCreateView(inflater, container, savedInstanceState);
	}

	private boolean logon(View view) {
		Editable txt = txtPasswd.getText();
		int len = txt.length();
		if (len <= 0) {
			Snackbar.make(view, getString(R.string.msg_passwd_needed), Snackbar.LENGTH_LONG).show();
			return false;
		} else {
			char[] ret = new char[len];
			txt.getChars(0, len, ret, 0);
			txt.clear();
			callback.onLogon(CryptoUtils.convert(CryptoUtils.encode(CryptoUtils.hash(CryptoUtils.convert(ret)))));
			dismiss();
			return true;
		}
	}

	private void cancel() {
		callback.onLogon(null);
		dismiss();
	}

	/*=========================================
	 * Callback interface to the main activity
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
