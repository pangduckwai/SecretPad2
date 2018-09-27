package org.sea9.android.secret;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;

import java.util.ArrayList;

public class DetailFragment extends DialogFragment {
	public static final String TAG = "secret.dialog_frag";
	public static final String KEY = "secret.key";
	public static final String CTN = "secret.content";

	private ContextFragment ctxFrag;
	private RecyclerView tagList;
	private EditText editKey;
	private EditText editCtn;

	public static DetailFragment getInstance(DataRecord record) {
		DetailFragment dialog = new DetailFragment();
		dialog.setCancelable(false);

		Bundle args = new Bundle();
		args.putString(KEY, record.getKey());
		args.putString(CTN, record.getContent());
		args.putIntegerArrayList(TAG, (ArrayList<Integer>) record.getTags());
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

		tagList.setHasFixedSize(true);
		tagList.setLayoutManager(new LinearLayoutManager(this.getContext()));

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

		Bundle args = getArguments();
		if (args != null) {
			editKey.setText(args.getString(KEY));
			editCtn.setText(args.getString(CTN));
		}

		FragmentManager manager = getFragmentManager();
		if (manager != null) {
			ctxFrag = (ContextFragment) manager.findFragmentByTag(ContextFragment.TAG);
			if (ctxFrag != null) {
				TagsAdaptor adaptor = ctxFrag.getTagsAdaptor();
				adaptor.prepare(args.getIntegerArrayList(TAG));
				tagList.setAdapter(adaptor);
			}
		}
	}
}
