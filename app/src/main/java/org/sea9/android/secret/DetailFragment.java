package org.sea9.android.secret;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;

public class DetailFragment extends DialogFragment {
	public static final String TAG = "secret.dialog_frag";
	public static final String CTN = "secret.content";

//	private ContextFragment ctxFrag;
	private RecyclerView tagList;
	private EditText editKey;
	private EditText editCtn;

	public static DetailFragment getInstance(String k, String v) {
		DetailFragment dialog = new DetailFragment();
		dialog.setCancelable(false);

		Bundle args = new Bundle();
		args.putString(TAG, k);
		args.putString(CTN, v);
		dialog.setArguments(args);

		return dialog;
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		Log.d(TAG, "DetailFragment.onCreateView");

		View view = inflater.inflate(R.layout.dialog_detail, container, false);

		tagList = view.findViewById(R.id.edit_tags);
		editKey = view.findViewById(R.id.edit_key);
		editCtn = view.findViewById(R.id.edit_content);

		view.findViewById(R.id.dtl_save).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.d(TAG, "SAVED!");
			}
		});

		view.findViewById(R.id.dtl_cancel).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});

		Window w = getDialog().getWindow();
		if (w != null) w.requestFeature(Window.FEATURE_NO_TITLE);
		getDialog().setOnKeyListener(new DialogInterface.OnKeyListener() {
			@Override
			public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_BACK) {
					dismiss();
					return true;
				} else {
					return false;
				}
			}
		});

		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		Log.d(TAG, "DetailFragment.onResume");

//		FragmentManager manager = getFragmentManager();
//		if (manager != null) {
//			ctxFrag = (ContextFragment) manager.findFragmentByTag(ContextFragment.TAG);
//		}
		Bundle args = getArguments();
		if (args != null) {
			editKey.setText(args.getString(TAG));
			editCtn.setText(args.getString(CTN));
		}
	}
}
