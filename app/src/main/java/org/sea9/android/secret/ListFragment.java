package org.sea9.android.secret;

import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
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
public class ListFragment extends Fragment implements ContextFragment.Interaction {
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

		content.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				int pos = ctxFrag.getAdaptor().getSelectedPosition();
				if (pos >= 0) {
					recycler.smoothScrollToPosition(pos); // Scroll list to the selected row
					ctxFrag.queryData(pos);
				}
			}
		});

		ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
			@Override
			public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder viewHolder1) {
				return false;
			}

			@Override
			public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
				final int position = viewHolder.getAdapterPosition();
				FragmentActivity activity = getActivity();
				if (activity != null) {
					AlertDialog.Builder builder = new AlertDialog.Builder(activity);
					builder.setMessage(String.format(Locale.getDefault(),
							getString(R.string.msg_confirm_delete), Integer.toString(position+1)));
					builder.setPositiveButton(R.string.btn_okay, new DialogInterface.OnClickListener() {
						@Override public void onClick(DialogInterface arg0, int arg1) {
							Snackbar.make(recycler,
									String.format(Locale.getDefault(),
											getString(ctxFrag.deleteData(position) ? R.string.msg_delete_okay : R.string.msg_delete_fail),
											Integer.toString(position+1)),
									Snackbar.LENGTH_LONG).show();
						}
					});
					builder.setNegativeButton(R.string.btn_cancel, new DialogInterface.OnClickListener() {
						@Override public void onClick(DialogInterface arg0, int arg1) {
							if (recycler.getAdapter() != null)
								recycler.getAdapter().notifyDataSetChanged();
						}
					});
					(builder.create()).show();
				}
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
				ctxFrag.addInteractListener(this);
				recycler.setAdapter(ctxFrag.getAdaptor());
			}
		}
	}

	/*=============================================================
	 * @see org.sea9.android.secret.ContextFragment.Interaction
	 */
	@Override
	public void select(String txt) {
		content.setText(txt);
	}

	@Override
	public void added(int position) {
		if (position >= 0) recycler.smoothScrollToPosition(position);
		Snackbar.make(recycler,
				String.format(
						Locale.getDefault(),
						getString((position >= 0) ? R.string.msg_insert_okay : R.string.msg_insert_fail),
						Integer.toString(position+1)),
				Snackbar.LENGTH_LONG).show();
	}

	@Override
	public void changed(int position) {
		if (position >= 0) recycler.smoothScrollToPosition(position);
		Snackbar.make(recycler,
				String.format(
						Locale.getDefault(),
						getString((position >= 0) ? R.string.msg_update_okay : R.string.msg_update_fail),
						Integer.toString(position+1)),
				Snackbar.LENGTH_LONG).show();
	}

	@Override
	public void retrieved(DataRecord record) {
		FragmentManager manager = getFragmentManager();
		if (manager != null) DetailFragment.getInstance(false, record).show(manager, DetailFragment.TAG);
	}
}