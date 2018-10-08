package org.sea9.android.secret.main;

import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.sea9.android.secret.R;

public class NotesAdaptor extends RecyclerView.Adapter<NotesViewHolder> {
	private static final String TAG = "secret.list_adaptor";

	private RecyclerView recyclerView;

	private int selectedPos = -1;
	public final boolean isSelected(int position) {
		return (selectedPos == position);
	}
	public final int getSelectedPosition() { return selectedPos; }
	public final void clearSelection() {
		selectedPos = -1;
		callback.clearRvRow();
	}
	public final void selectRow(int position) {
		selectedPos = position;
	}

	public final long getAdapterPositionId(int position) {
		NotesViewHolder holder = (NotesViewHolder) recyclerView.findViewHolderForAdapterPosition(position);
		if (holder != null) {
			return holder.pid;
		} else {
			return -1;
		}
	}

	/*
	 * Call notify the recyclerView for the newly inserted row, remember the current row, and
	 * highlight the current row by callback to the Activity.
	 * @param position position of the inserted row in the recycler.
	 * @param pid SQLite ID of the newly inserted row.
	 */
	public final void onItemInsert(int position) {
		if (position >= 0) {
			notifyItemInserted(position);
			selectedPos = position;
			callback.selectRvRow(position);
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

	public NotesAdaptor(Listener callback) {
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
		callback.prepareRvData();
	}

	@Override @NonNull
	public final NotesViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, int viewType) {
		Log.d(TAG, "onCreateViewHolder");

		// create a new view
		View item = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item, parent, false);

		// Click listener
		item.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				int position = recyclerView.getChildLayoutPosition(view);
				if (position == selectedPos) {
					clearSelection();
				} else {
					selectedPos = position;
					callback.selectRvRow(position);
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

		return new NotesViewHolder(item);
	}

	@Override
	public final void onBindViewHolder(@NonNull NotesViewHolder holder, int position) {
		callback.populateRv(holder, position);
	}

	@Override
	public int getItemCount() {
		return callback.getRvItemCount();
	}
	//=====================================================

	/*============================================
	 * Callback interface to the context fragment
	 */
	public interface Listener {
		void prepareRvData();
		int getRvItemCount();
		void populateRv(NotesViewHolder holder, int position);
		void selectRvRow(int position);
		void clearRvRow();
	}
	private Listener callback;
}
