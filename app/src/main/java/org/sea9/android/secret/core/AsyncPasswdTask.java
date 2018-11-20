package org.sea9.android.secret.core;

import android.os.AsyncTask;

import org.jetbrains.annotations.NotNull;
import org.sea9.android.secret.R;
import org.sea9.android.secret.crypto.CryptoUtils;
import org.sea9.android.secret.data.DbContract;
import org.sea9.android.secret.data.DbHelper;

import javax.crypto.BadPaddingException;

public class AsyncPasswdTask extends AsyncTask<char[], Void, char[]> {
	private ContextFragment caller;
	AsyncPasswdTask(ContextFragment ctx) {
		caller = ctx;
	}

	@Override
	protected void onPreExecute() {
		if (caller.getCallback() != null) caller.getCallback().setBusyState(true);
	}

	@Override
	protected char[] doInBackground(char[]... passwords) {
		if (passwords.length == 2) {
			try {
				int result = DbContract.Notes.Companion.passwd(caller.getDbHelper(), new DbHelper.Crypto() {
					@Override
					public char[] decrypt(@NotNull char[] input, @NotNull byte[] salt) {
						try {
							return CryptoUtils.decrypt(input, passwords[0], salt);
						} catch (BadPaddingException e) {
							throw new RuntimeException(e);
						}
					}

					@Override
					@NotNull
					public char[] encrypt(@NotNull char[] input, @NotNull byte[] salt) {
						try {
							return CryptoUtils.encrypt(input, passwords[1], salt);
						} catch (BadPaddingException e) {
							throw new RuntimeException(e);
						}
					}
				});

				for (int i = 0; i < passwords[0].length; i++)
					passwords[0][i] = 0;
				passwords[0] = null;

				if (result == 0) {
					return passwords[1];
				} else {
					for (int i = 0; i < passwords[1].length; i++)
						passwords[1][i] = 0;
					passwords[1] = null;
				}
			} catch (RuntimeException e) {
				if ((e.getCause() != null) && (e.getCause() instanceof BadPaddingException)) {
					if (caller.getCallback() != null)
						caller.getCallback().doNotify(String.format(caller.getString(R.string.msg_logon_fail), e.getMessage()), true);
				} else
					throw e;
			}
		}
		return null;
	}

	@Override
	protected void onPostExecute(char[] result) {
		if (result != null) {
			caller.setPassword(result);
			if (caller.getCallback() != null) caller.getCallback().doNotify(caller.getString(R.string.msg_passwd_changed), false);
		} else {
			if (caller.getCallback() != null) caller.getCallback().doNotify(caller.getString(R.string.msg_passwd_change_failed), false);
		}
		if (caller.getCallback() != null) caller.getCallback().setBusyState(false);
	}
}
