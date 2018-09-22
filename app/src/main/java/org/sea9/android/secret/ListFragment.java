package org.sea9.android.secret;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

/**
 * A placeholder fragment containing a simple view.
 */
public class ListFragment extends Fragment implements ContextFragment.SelectListener {
	public static final String TAG = "secret.list_frag";

	private ContextFragment ctxFrag;
	private RecyclerView recycler;
	private EditText content;

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		Log.d(TAG, "ListFragment.onCreateView");

		View ret = inflater.inflate(R.layout.fragment_list, container, false);
		recycler = ret.findViewById(R.id.recycler_list);
		content = ret.findViewById(R.id.item_content);

		recycler.setHasFixedSize(true); // improve performance since content changes do not affect layout size of the RecyclerView
		recycler.setLayoutManager(new LinearLayoutManager(this.getContext())); // use a linear layout manager

		content.addTextChangedListener(new TextWatcher() {
			@Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				Log.d(TAG, "beforeTextChanged " + s);
			}
			@Override public void onTextChanged(CharSequence s, int start, int before, int count) {
				Log.d(TAG, "onTextChanged " + s);
			}
			@Override public void afterTextChanged(Editable s) {
				Log.d(TAG, "afterTextChanged " + s);
			}
		});
		content.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					callback.gainFocus();

					recycler.postDelayed(new Runnable() {
						@Override
						public void run() {
							int pos = ctxFrag.getAdaptor().getSelectedPosition();
							if (pos >= 0) recycler.smoothScrollToPosition(pos); // Scroll list to the selected row
						}
					}, 500);
				}
			}
		});

		return ret;
	}

	@Override
	public void onResume() {
		super.onResume();
		Log.d(TAG, "ListFragment.onResume");

		FragmentManager manager = getFragmentManager();
		if (manager != null) {
			ctxFrag = (ContextFragment) manager.findFragmentByTag(ContextFragment.TAG);
			if (ctxFrag != null) {
				ctxFrag.addSelectListener(this);
				recycler.setAdapter(ctxFrag.getAdaptor()); //new TempAdaptor(ctxFrag.getDataSet(), this));
			}
		}
	}

	/*=============================================================
	 * @see org.sea9.android.secret.ContextFragment.SelectListener
	 */
	@Override
	public void select(String txt) {
		content.setText(txt);
	}

	/*==========================================
	 * Callback interface for the main activity
	 */
	public interface Listener {
		void gainFocus();
	}
	private Listener callback;

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		Log.d(TAG, "ListFragment.onAttach");
		try {
			callback = (Listener) context;
		} catch (ClassCastException e) {
			throw new ClassCastException(context.toString() + " missing implementation of ListFragment.Listener");
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		Log.d(TAG, "ListFragment.onDetach");
		callback = null;
	}
}