package org.sea9.android.secret.details;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageButton;

import org.sea9.android.secret.ContextFragment;
import org.sea9.android.secret.R;
import org.sea9.android.secret.data.NoteRecord;

import java.util.List;

public class DetailFragment extends DialogFragment implements ContextFragment.DetailListener {
	public static final String TAG = "secret.dialog_frag";
	public static final String KEY = "secret.key";
	public static final String CTN = "secret.content";
	private static final String EMPTY = "";

	private ContextFragment ctxFrag;
	private RecyclerView tagList;
	private EditText editKey;
	private EditText editCtn;
	private EditText editTag;
	private ImageButton bttnAdd;
	private ImageButton bttnSav;
	private boolean isNew;

	public static DetailFragment getInstance(boolean isNew, NoteRecord record) {
		DetailFragment dialog = new DetailFragment();
		dialog.setCancelable(false);

		Bundle args = new Bundle();
		args.putBoolean(TAG, isNew);
		if (record != null) {
			args.putString(KEY, record.getKey());
			args.putString(CTN, ""); //TODO Get content from db
		}
		dialog.setArguments(args);

		return dialog;
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		Log.d(TAG, "onCreateView");

		View view = inflater.inflate(R.layout.dialog_detail, container, false);

		tagList = view.findViewById(R.id.edit_tags);
		editKey = view.findViewById(R.id.edit_key);
		editCtn = view.findViewById(R.id.edit_content);
		editTag = view.findViewById(R.id.edit_tag);
		bttnAdd = view.findViewById(R.id.tag_add);
		bttnSav = view.findViewById(R.id.dtl_save);

		Bundle args = getArguments();
		if (args != null) {
			isNew = args.getBoolean(TAG);
			editCtn.setText(args.getString(CTN));
			editKey.setText(args.getString(KEY));
		}

		tagList.setHasFixedSize(true);
		tagList.setLayoutManager(new LinearLayoutManager(this.getContext()));

		editKey.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if ((before != 0) || (count != 0)) {
					if (ctxFrag != null) ctxFrag.detailUpdated();
				}
			}

			@Override
			public void afterTextChanged(Editable s) { }
		});

		editCtn.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) { }

			@Override
			public void afterTextChanged(Editable s) {
				if (ctxFrag != null) ctxFrag.detailUpdated();
			}
		});

		bttnAdd.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Editable e = editTag.getText();
				if (e != null) {
					String t = e.toString();
					if (t.trim().length() > 0) {
						callback.onAdd(t);
					}
				}
			}
		});

		bttnSav.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if ((ctxFrag != null) && ctxFrag.isUpdated() && (tagList.getAdapter() != null)) {
					String k = (editKey.getText() != null) ? editKey.getText().toString() : EMPTY;
					String c = (editCtn.getText() != null) ? editCtn.getText().toString() : EMPTY;
					callback.onSave(isNew, k, c, ((TagsAdaptor) tagList.getAdapter()).getSelectedPosition());
				}
				dismiss();
			}
		});

		view.findViewById(R.id.dtl_cancel).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				close();
			}
		});

		Window w = getDialog().getWindow();
		if (w != null) w.requestFeature(Window.FEATURE_NO_TITLE);
		getDialog().setOnKeyListener(new DialogInterface.OnKeyListener() {
			@Override
			public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_BACK) {
					close();
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
		Log.d(TAG, "onResume");

		if (!isNew) {
			editKey.setFilters(new InputFilter[] { new InputFilter() {
				public CharSequence filter(CharSequence src, int start, int end, Spanned dst, int dstart, int dend) {
					return dst.subSequence(dstart, dend);
				}
			}});
		}

		FragmentManager manager = getFragmentManager();
		if (manager != null) {
			ctxFrag = (ContextFragment) manager.findFragmentByTag(ContextFragment.TAG);
			if (ctxFrag != null) {
				tagList.setAdapter(ctxFrag.getTagsAdaptor());
				ctxFrag.setDetailListener(this);

				if (ctxFrag.isFiltered()) {
					editTag.setEnabled(false);
					bttnAdd.setEnabled(false);
					bttnSav.setEnabled(false);
					editCtn.setFilters(new InputFilter[] { new InputFilter() {
						public CharSequence filter(CharSequence src, int start, int end, Spanned dst, int dstart, int dend) {
							return dst.subSequence(dstart, dend);
						}
					}});
				} else {
					editCtn.setFilters(new InputFilter[] {});
				}
			}
		}
	}

	private void close() {
		if ((ctxFrag != null) && !ctxFrag.isFiltered() && ctxFrag.isUpdated()) {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				AlertDialog.Builder builder = new AlertDialog.Builder(activity);
				builder.setMessage(R.string.msg_discard_changes);
				builder.setPositiveButton(R.string.btn_okay, new DialogInterface.OnClickListener() {
					@Override public void onClick(DialogInterface arg0, int arg1) {
						dismiss();
					}
				});
				builder.setNegativeButton(R.string.btn_cancel, new DialogInterface.OnClickListener() {
					@Override public void onClick(DialogInterface arg0, int arg1) { }
				});
				(builder.create()).show();
			}
		} else {
			dismiss();
		}
	}

	/*=========================================
	 * Callback interface to the main activity
	 */
	public interface Listener {
		void onAdd(String t);
		void onSave(boolean isNew, String k, String c, List<Integer> t);
	}
	private Listener callback;

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		Log.d(TAG, "onAttach");
		try {
			callback = (Listener) context;
		} catch (ClassCastException e) {
			throw new ClassCastException(context.toString() + " missing implementation of DetailFragment.Listener");
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		Log.d(TAG, "onDetach");
		callback = null;
	}
	//=========================================

	/*=============================================================
	 * @see org.sea9.android.secret.ContextFragment.DetailListener
	 */
	@Override
	public void onTagAddCompleted(int position) {
		editTag.setText(EMPTY);
		tagList.smoothScrollToPosition(position);
	}
}