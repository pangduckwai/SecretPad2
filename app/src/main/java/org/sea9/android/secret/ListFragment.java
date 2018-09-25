package org.sea9.android.secret;

import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Locale;

/**
 * A placeholder fragment containing a simple view.
 */
public class ListFragment extends Fragment implements ContextFragment.SelectListener {
	public static final String TAG = "secret.list_frag";

	private ContextFragment ctxFrag;
	private RecyclerView recycler;
	private TextView content;

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		Log.d(TAG, "ListFragment.onCreateView");

		View ret = inflater.inflate(R.layout.fragment_list, container, false);
		recycler = ret.findViewById(R.id.recycler_list);
		content = ret.findViewById(R.id.item_content);

		recycler.setHasFixedSize(true); // improve performance since content changes do not affect layout size of the RecyclerView
		recycler.setLayoutManager(new LinearLayoutManager(this.getContext())); // use a linear layout manager

//		content.addTextChangedListener(new TextWatcher() {
//			@Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {
//				Log.d(TAG, "beforeTextChanged " + s);
//			}
//			@Override public void onTextChanged(CharSequence s, int start, int before, int count) {
//				Log.d(TAG, "onTextChanged " + s);
//			}
//			@Override public void afterTextChanged(Editable s) {
//				Log.d(TAG, "afterTextChanged " + s);
//			}
//		});
//		content.setOnFocusChangeListener(new View.OnFocusChangeListener() {
//			@Override
//			public void onFocusChange(View v, boolean hasFocus) {
//				if (hasFocus) {
//					callback.gainFocus();
//
//					recycler.postDelayed(new Runnable() {
//						@Override
//						public void run() {
//							int pos = ctxFrag.getAdaptor().getSelectedPosition();
//							if (pos >= 0) recycler.smoothScrollToPosition(pos); // Scroll list to the selected row
//						}
//					}, 500);
//				}
//			}
//		});
		content.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				int pos = ctxFrag.getAdaptor().getSelectedPosition();
				if (pos >= 0) recycler.smoothScrollToPosition(pos); // Scroll list to the selected row
				// TODO Open detail dialog
			}
		});

		ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
			@Override
			public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder viewHolder1) {
				return false;
			}

			@Override
			public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
				int position = viewHolder.getAdapterPosition();
				Snackbar.make(recycler,
						String.format(Locale.getDefault(),
								getString(ctxFrag.deleteData(position) ? R.string.msg_delete_okay : R.string.msg_delete_fail),
								Integer.toString(position+1)),
						Snackbar.LENGTH_LONG).show();
			}
		});
		itemTouchHelper.attachToRecyclerView(recycler);

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

	@Override
	public void added(int position) {
		if (position >= 0) recycler.smoothScrollToPosition(position);
		Snackbar.make(recycler,
				String.format(Locale.getDefault(),
						getString((position >= 0) ? R.string.msg_insert_okay : R.string.msg_insert_fail),
						Integer.toString(position+1)),
				Snackbar.LENGTH_LONG).show();
	}
//	/*==========================================
//	 * Callback interface for the main activity
//	 */
//	public interface Listener {
//		void gainFocus();
//	}
//	private Listener callback;
//
//	@Override
//	public void onAttach(Context context) {
//		super.onAttach(context);
//		Log.d(TAG, "ListFragment.onAttach");
//		try {
//			callback = (Listener) context;
//		} catch (ClassCastException e) {
//			throw new ClassCastException(context.toString() + " missing implementation of ListFragment.Listener");
//		}
//	}
//
//	@Override
//	public void onDetach() {
//		super.onDetach();
//		Log.d(TAG, "ListFragment.onDetach");
//		callback = null;
//	}
}