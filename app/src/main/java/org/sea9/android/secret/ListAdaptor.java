package org.sea9.android.secret;

import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class ListAdaptor<H extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<H> {
	private static final String TAG = "secret.list_adaptor";

	private RecyclerView recyclerView;

	private int selectedPos = -1;
	public final boolean isSelected(int position) {
		return (selectedPos == position);
	}
	public final int getSelectedPosition() { return selectedPos; }

	public final void onItemDeleted(int position) {
		notifyItemRemoved(position);
		if ((position >= 0) && (position < selectedPos)) {
			selectedPos --;
		}
	}

	public ListAdaptor(Listener<H> callback) {
		this.callback = callback;
	}

	@Override
	public final void onAttachedToRecyclerView(@NonNull RecyclerView recycler) {
		super.onAttachedToRecyclerView(recycler);
		Log.d(TAG, "ListAdaptor.onAttachedToRecyclerView");
		recyclerView = recycler;
	}

	@Override @NonNull
	public final H onCreateViewHolder(@NonNull final ViewGroup parent, int viewType) {
		Log.d(TAG, "ListAdaptor.onCreateViewHolder");

		// create a new view
		View item = LayoutInflater.from(parent.getContext()).inflate(callback.getListItemLayoutId(), parent, false);

		// Click listener
		item.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				int pos = recyclerView.getChildLayoutPosition(v);
				if (pos == selectedPos) {
					selectedPos = -1;
					callback.datSelectionCleared();
				} else {
					selectedPos = pos;
					callback.datSelectionMade(pos);
				}
				notifyDataSetChanged();
			}
		});

		item.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				Snackbar.make(parent, "Long pressed...", Snackbar.LENGTH_LONG).setAction("Action", null).show();
				return false;
			}
		});

		return callback.getHolder(item);
	}

	@Override
	public final void onBindViewHolder(@NonNull H holder, int position) {
		callback.populateList(holder, position);
	}

	@Override
	public int getItemCount() {
		return callback.getItemCount();
	}

	/*============================================
	 * Callback interface to the context fragment
	 */
	public interface Listener<H> {
		int getItemCount();
		int getListItemLayoutId(); // Return the list item layout id.
		H getHolder(View view);
		void populateList(H holder, int position);
		void datSelectionMade(int index);
		void datSelectionCleared();
	}
	private Listener<H> callback;
}
