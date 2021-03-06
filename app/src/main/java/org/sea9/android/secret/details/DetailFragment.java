package org.sea9.android.secret.details;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.sea9.android.secret.core.ContextFragment;
import org.sea9.android.secret.R;
import org.sea9.android.secret.core.MainActivity;
import org.sea9.android.secret.data.NoteRecord;
import org.sea9.android.secret.ui.MessageDialog;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class DetailFragment extends DialogFragment {
	public static final String TAG = "secret.detail_dialog";
	public static final String NID = "secret.nid";
	public static final String KEY = "secret.key";
	public static final String CTN = "secret.content";
	public static final String MOD = "secret.modified";
	public static final String TGS = "secret.tags";
	private static final String EMPTY = "";

	private RecyclerView tagList;
	private ProgressBar progress;
	private EditText editKey;
	private EditText editCtn;
	private EditText editTag;
	private ImageButton bttnAdd;
	private ImageButton bttnSav;
	private TextView textNid;
	private boolean isNew;
	private List<Long> orgnTags;

	public static DetailFragment getInstance(boolean isNew, NoteRecord record, String content) {
		DetailFragment dialog = new DetailFragment();
		dialog.setCancelable(false);

		Bundle args = new Bundle();
		args.putBoolean(TAG, isNew);
		if (record != null) {
			args.putLong(NID, record.getPid());
			args.putString(KEY, record.getKey());
			args.putString(CTN, content);
			args.putLong(MOD, record.getModified());
			if (record.getTags() != null) {
				long[] tags = new long[record.getTags().size()];
				for (int i = 0; i < tags.length; i ++)
					tags[i] = record.getTags().get(i);
				args.putLongArray(TGS, tags);
			}
		}
		dialog.setArguments(args);

		return dialog;
	}

	@SuppressLint("SetTextI18n")
	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		Log.d(TAG, "onCreateView");

		View view = inflater.inflate(R.layout.detail_dialog, container, false);

		tagList = view.findViewById(R.id.edit_tags);
		progress = view.findViewById(R.id.progressbar);
		editKey = view.findViewById(R.id.edit_key);
		editCtn = view.findViewById(R.id.edit_content);
		editTag = view.findViewById(R.id.edit_tag);
		bttnAdd = view.findViewById(R.id.tag_add);
		bttnSav = view.findViewById(R.id.dtl_save);
		textNid = view.findViewById(R.id.note_id);
		TextView textMod = view.findViewById(R.id.modify_time);

		SimpleDateFormat formatter = new SimpleDateFormat(ContextFragment.PATTERN_DATE, Locale.getDefault());
		Bundle args = getArguments();
		if (args != null) {
			isNew = args.getBoolean(TAG);
			editCtn.setText(args.getString(CTN));
			editKey.setText(args.getString(KEY));
			textNid.setText(Long.toString(args.getLong(NID)));

			long mod = args.getLong(MOD);
			textMod.setText(formatter.format((mod > 0) ? new Date(mod) : new Date()));

			long[] tags = args.getLongArray(TGS);
			if (tags != null)
				orgnTags = Arrays.stream(tags).boxed().collect(Collectors.toList());
			else
				orgnTags = null;
		}

		tagList.setHasFixedSize(true);
		tagList.setLayoutManager(new LinearLayoutManager(getContext()));

		String title = String.format(getString(R.string.value_details), getString(isNew ? R.string.value_details_new : R.string.value_details_edit));
		((TextView) view.findViewById(R.id.content_title)).setText(title);

		editKey.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if (isNew) {
					callback.dataUpdated();
				}
			}

			@Override
			public void afterTextChanged(Editable s) { }
		});

		editCtn.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				callback.dataUpdated();
			}

			@Override
			public void afterTextChanged(Editable s) { }
		});

		bttnAdd.setOnClickListener(v -> {
			Editable e = editTag.getText();
			if (e != null) {
				String t = e.toString();
				if (t.trim().length() > 0) {
					callback.onAdd(t);
				}
			}
		});

		bttnSav.setOnClickListener(v -> {
			if (isUpdated() && (tagList.getAdapter() != null)) {
				Long i = (textNid.getText() != null) ? Long.parseLong(textNid.getText().toString()) : -1;
				String k = (editKey.getText() != null) ? editKey.getText().toString() : EMPTY;
				String c = (editCtn.getText() != null) ? editCtn.getText().toString() : EMPTY;

				if (k.isEmpty()) {
					closeKeyboard();
					Snackbar.make(bttnSav, getString(R.string.msg_empty_key), Snackbar.LENGTH_LONG).show();
				} else
					callback.onSave(isNew, i, k, c, callback.getTagsAdaptor().getSelectedTags());
			} else
				dismiss();
		});

		view.findViewById(R.id.dtl_cancel).setOnClickListener(v -> close());

		getDialog().setOnKeyListener((dialog, keyCode, event) -> {
			if ((keyCode == KeyEvent.KEYCODE_BACK) && (event.getAction() == KeyEvent.ACTION_UP)) {
				close();
				return true;
			} else {
				return false;
			}
		});

		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		Log.d(TAG, "onResume");

		if (!isNew) {
			editKey.setFilters(new InputFilter[] {(src, start, end, dst, dstart, dend) -> dst.subSequence(dstart, dend)});
		}

		tagList.setAdapter(callback.getTagsAdaptor());
		if (callback.isFiltered()) {
			editTag.setEnabled(false);
			bttnAdd.setEnabled(false);
			bttnSav.setEnabled(false);
			editCtn.setFilters(new InputFilter[]{(src, start, end, dst, dstart, dend) -> dst.subSequence(dstart, dend)});
		} else {
			editCtn.setFilters(new InputFilter[]{});
		}
	}

	private void close() {
		if (!callback.isFiltered() && isUpdated()) {
			DialogFragment d = MessageDialog.Companion.getOkayCancelDialog(MainActivity.MSG_DIALOG_DISCARD, getString(R.string.msg_discard_changes), null);
			FragmentManager m = getFragmentManager();
			if (m != null)
				d.show(m, MessageDialog.TAG);
			else
				d.getDialog().show();
		} else {
			dismiss();
		}
	}

	private boolean isUpdated() {
		if (callback.isUpdated()) return true;
		if (callback.isTagsUpdated()) {
			if (orgnTags == null)
				return true;
			else
				return !(callback.getTagsAdaptor().getSelectedTags().equals(orgnTags));
		}
		return false;
	}

	private void closeKeyboard() {
		View root = getView();
		if (root != null) {
			Context context = getContext();
			View focused = root.findFocus();
			if ((focused != null) && (context != null)) {
				InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
				if (imm != null)
					imm.hideSoftInputFromWindow(focused.getWindowToken(), 0);
			}
		}
	}

	/*========================================
	 * Callback interface to the MainActivity
	 */
	public interface Callback {
		boolean isFiltered();
		boolean isUpdated();
		void dataUpdated();
		boolean isTagsUpdated();
		TagsAdaptor getTagsAdaptor();
		void onAdd(String t);
		void onSave(boolean isNew, Long i, String k, String c, List<Long> t);
	}
	private Callback callback;

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		Log.d(TAG, "onAttach");
		try {
			callback = (Callback) context;
		} catch (ClassCastException e) {
			throw new ClassCastException(context.toString() + " missing implementation of DetailFragment.Callback");
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		Log.d(TAG, "onDetach");
		callback = null;
	}
	//=========================================

	public void onTagAddCompleted(int position) {
		editTag.setText(EMPTY);
		tagList.smoothScrollToPosition(position);
	}

	public void setBusyState(boolean isBusy) {
		progress.setVisibility(isBusy ? View.VISIBLE : View.INVISIBLE);
	}
}
