package org.sea9.android.secret.main;

import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class NotesAdaptor<H extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<H> {
	private static final String TAG = "secret.list_adaptor";

	private RecyclerView recyclerView;

	private int selectedPos = -1;
	public final boolean isSelected(int position) {
		return (selectedPos == position);
	}
	public final int getSelectedPosition() { return selectedPos; }
	public final void clearSelection() {
		selectedPos = -1;
		callback.datSelectionCleared();
	}
	public final void selectRow(int position) {
		selectedPos = position;
	}

	public final void onItemInsert(int position) {
		if (position >= 0) {
			notifyItemInserted(position);
			selectedPos = position;
			callback.datSelectionMade(position);
		}
	}

	public final void onItemDeleted(int position) {
		if ((position >= 0) && (position <= getItemCount())) {
			notifyItemRemoved(position);
			if (position < selectedPos) {
				selectedPos --;
			}
		}
	}

	public NotesAdaptor(Listener<H> callback) {
		this.callback = callback;
	}

	/*=====================================================
	 * @see android.support.v7.widget.RecyclerView.Adapter
	 */
	@Override
	public final void onAttachedToRecyclerView(@NonNull RecyclerView recycler) {
		super.onAttachedToRecyclerView(recycler);
		Log.d(TAG, "onAttachedToRecyclerView");
		recyclerView = recycler;
	}

	@Override @NonNull
	public final H onCreateViewHolder(@NonNull final ViewGroup parent, int viewType) {
		Log.d(TAG, "onCreateViewHolder");

		// create a new view
		View item = LayoutInflater.from(parent.getContext()).inflate(callback.getListItemLayoutId(), parent, false);

		// Click listener
		item.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				int pos = recyclerView.getChildLayoutPosition(v);
				if (pos == selectedPos) {
					clearSelection();
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
				Snackbar.make(parent, "Long pressed...", Snackbar.LENGTH_LONG).show(); //TODO TEMP
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
	//=====================================================

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
