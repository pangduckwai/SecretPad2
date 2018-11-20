package org.sea9.android.secret.ui;

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
import org.sea9.android.secret.core.ContextFragment;
import org.sea9.android.secret.crypto.CryptoUtils;

import java.util.Arrays;

public class PasswdDialog extends DialogFragment {
	public static final String TAG = "secret.change_password";

	public static PasswdDialog getInstance() {
		PasswdDialog instance = new PasswdDialog();
		instance.setCancelable(false);
		return instance;
	}

	private EditText txtCurrnt;
	private EditText txtPasswd;
	private EditText txtConfrm;
	private Button btnLogon;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.passwd_dialog, container, false);

		txtCurrnt = view.findViewById(R.id.old_password);

		txtPasswd = view.findViewById(R.id.new_password);

		txtConfrm = view.findViewById(R.id.new_confirm);
		txtConfrm.setOnEditorActionListener((v, actionId, event) -> {
			if (actionId == EditorInfo.IME_ACTION_DONE) {
				change();
			}
			return false;
		});

		btnLogon = view.findViewById(R.id.logon);
		btnLogon.setOnClickListener(v -> change());

		getDialog().setOnKeyListener((dialog, keyCode, event) -> {
			if ((keyCode == KeyEvent.KEYCODE_BACK) && (event.getAction() == KeyEvent.ACTION_UP)) {
				cancel();
				return true;
			} else {
				return false;
			}
		});

		Window window = getDialog().getWindow();
		if (window != null) window.requestFeature(Window.FEATURE_NO_TITLE);
		return view;
	}

	private void change() {
		Editable txt0 = txtCurrnt.getText();
		int len0 = txt0.length();
		Editable txt1 = txtPasswd.getText();
		int len1 = txt1.length();
		Editable txt2 = txtConfrm.getText();
		int len2 = txt2.length();

		if (len0 <= 0) {
			Snackbar.make(btnLogon, getString(R.string.msg_passwd_current), Snackbar.LENGTH_LONG).show();
		} else if (len1 <= 0) {
			Snackbar.make(btnLogon, getString(R.string.msg_passwd_needed), Snackbar.LENGTH_LONG).show();
		} else if (len2 != len1) {
			Snackbar.make(btnLogon, getString(R.string.msg_passwd_mismatch), Snackbar.LENGTH_LONG).show();
		} else if (!ContextFragment.POLICY.matcher(txt2).matches()) {
			callback.doNotify(getString(R.string.msg_passwd_simple), true);
		} else {
			char[] c1 = new char[len1];
			char[] c2 = new char[len2];
			txt1.getChars(0, len1, c1, 0);
			txt2.getChars(0, len2, c2, 0);
			if (!Arrays.equals(c1, c2)) {
				txt2.clear();
				Snackbar.make(btnLogon, getString(R.string.msg_passwd_mismatch), Snackbar.LENGTH_LONG).show();
			} else {
				char[] c0 = new char[len0];
				txt0.getChars(0, len0, c0, 0);
				txt0.clear();
				txt1.clear();
				txt2.clear();
				callback.onChangePassword(
						  CryptoUtils.convert(CryptoUtils.encode(CryptoUtils.hash(CryptoUtils.convert(c0))))
						, CryptoUtils.convert(CryptoUtils.encode(CryptoUtils.hash(CryptoUtils.convert(c1))))
				);
				dismiss();
			}
		}
	}

	private void cancel() {
		Editable txt0 = txtCurrnt.getText();
		Editable txt1 = txtPasswd.getText();
		Editable txt2 = txtConfrm.getText();
		txt0.clear();
		txt1.clear();
		txt2.clear();
		callback.onChangePassword(null, null);
		dismiss();
	}

	/*========================================
	 * Callback interface to the MainActivity
	 */
	public interface Callback {
		void doNotify(String message, boolean stay);
		void onChangePassword(char[] oldValue, char[] newValue);
	}
	private Callback callback;

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		Log.d(TAG, "onAttach");
		try {
			callback = (Callback) context;
		} catch (ClassCastException e) {
			throw new ClassCastException(context.toString() + " missing implementation of PasswdDialog.Callback");
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		Log.d(TAG, "onDetach");
		callback = null;
	}
}
