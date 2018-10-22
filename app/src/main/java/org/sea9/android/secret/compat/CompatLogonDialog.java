package org.sea9.android.secret.compat;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;

import org.sea9.android.secret.R;

public class CompatLogonDialog extends DialogFragment {
	@Override @NonNull
	public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
		Activity activity = getActivity();
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		if (activity != null) {
			LayoutInflater inflater = activity.getLayoutInflater();

			builder.setView(inflater.inflate(R.layout._compat_logon, null))
					.setPositiveButton(R.string.btn_logon, (dialog, id) -> {
						// sign in the user ...
					})
					.setNegativeButton(R.string.btn_cancel, (dialog, id) -> CompatLogonDialog.this.getDialog().cancel());
		}
		return builder.create();
	}
}
