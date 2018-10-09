package org.sea9.android.secret.main;

import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.sea9.android.secret.R;
import org.sea9.android.secret.data.DbContract;
import org.sea9.android.secret.data.DbHelper;
import org.sea9.android.secret.data.NoteRecord;
import org.sea9.android.secret.data.TagRecord;

import java.util.ArrayList;
import java.util.List;

public class NotesAdaptor extends RecyclerView.Adapter<NotesAdaptor.ViewHolder> {
	private static final String TAG = "secret.list_adaptor";
	private static final String SPACE = " ";

	private RecyclerView recyclerView;

	private int selectedPos = -1;
	private boolean isSelected(int position) {
		return (selectedPos == position);
	}
	public final int getSelectedPosition() { return selectedPos; }
//	public final void clearSelection() {
//		selectedPos = -1;
//		callback.clearRvRow();
//	}
//	public final void selectRow(int position) {
//		selectedPos = position;
//	}

	private List<NoteRecord> dataset;
	public final NoteRecord getRecord(int position) {
		if ((position >= 0) && (position < dataset.size()))
			return dataset.get(position);
		else
			return null;
	}

//	public final long getAdapterPositionId(int position) {
//		NotesViewHolder holder = (NotesViewHolder) recyclerView.findViewHolderForAdapterPosition(position);
//		if (holder != null) {
//			return holder.pid;
//		} else {
//			return -1;
//		}
//	}

	/*
	 * Call notify the recyclerView for the newly inserted row, remember the current row, and
	 * highlight the current row by callback to the Activity.
	 * @param position position of the inserted row in the recycler.
	 * @param pid SQLite ID of the newly inserted row.
	 */
//	public final void onItemInsert(int position) {
//		if (position >= 0) {
//			notifyItemInserted(position);
//			selectedPos = position;
//			callback.selectRvRow(position);
//		}
//	}
//
//	public final void onItemDeleted(int position) {
//		if ((position >= 0) && (position <= getItemCount())) {
//			notifyItemRemoved(position);
//			if (position < selectedPos) {
//				selectedPos --;
//			}
//		}
//	}

	public NotesAdaptor(Listener ctx) {
		callback = ctx;
		dataset = new ArrayList<>();
	}

	/*=====================================================
	 * @see android.support.v7.widget.RecyclerView.Adapter
	 */
	@Override
	public final void onAttachedToRecyclerView(@NonNull RecyclerView recycler) {
		super.onAttachedToRecyclerView(recycler);
		Log.d(TAG, "onAttachedToRecyclerView");
		recyclerView = recycler;

		onInit();
	}

	@Override @NonNull
	public final ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, int viewType) {
		Log.d(TAG, "onCreateViewHolder");

		// create a new view
		View item = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item, parent, false);

		// Click listener
		item.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				int position = recyclerView.getChildLayoutPosition(view);
				if (position == selectedPos) {
					selectedPos = -1;
					callback.selectRvRow(null);
				} else {
					selectedPos = position;
					onSelectRvRow(position);
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

		return new ViewHolder(item);
	}

	@Override
	public final void onBindViewHolder(@NonNull ViewHolder holder, int position) {
		if (isSelected(position)) {
			holder.itemView.setSelected(true);
		} else {
			holder.itemView.setSelected(false);
		}

		NoteRecord record = dataset.get(position);
		holder.key.setText(record.getKey());

		List<TagRecord> tags = record.getTags();
		if ((tags != null) && tags.size() > 0) {
			StringBuilder buff = new StringBuilder(tags.get(0).getTag());
			Log.w(TAG, "################ " + tags.get(0).getTag());
			for (int i = 1; i < tags.size(); i ++) {
				Log.w(TAG, "!!!!!!!!!!!!!!!! " + tags.get(i).getTag());
				buff.append(SPACE).append(tags.get(i).getTag());
			}
			holder.tag.setText(buff.toString());
		}
	}

	@Override
	public int getItemCount() {
		return dataset.size();
	}
	//=====================================================

	/*=====================================================================
	 * Data access methods. TODO: maybe need to move to a separate thread?
	 */
	private void onInit() {
		dataset = DbContract.Notes.Companion.select(callback.getDbHelper());
		for (NoteRecord record : dataset) {
			List<TagRecord> tags = DbContract.NoteTags.Companion.select(callback.getDbHelper(), record.getPid());
			record.setTags(tags);
		}
	}

	public void onSelectRvRow(int position) {
		String content = DbContract.Notes.Companion.select(callback.getDbHelper(), dataset.get(position).getPid());
		if (content != null) {
			callback.selectRvRow(content);
		}
	}
	//=====================================================================

	/*=============
	 * View holder
	 */
	static class ViewHolder extends RecyclerView.ViewHolder {
		public TextView key;
		public TextView tag;
		ViewHolder(View v) {
			super(v);
			key = v.findViewById(R.id.item_name);
			tag = v.findViewById(R.id.item_tags);
		}
	}
	//=============

	/*============================================
	 * Callback interface to the context fragment
	 */
	public interface Listener {
//		void retrieveRvData();
//		int getRvItemCount();
//		void populateRv(ViewHolder holder, int position);
		DbHelper getDbHelper();
		void selectRvRow(String content);
	}
	private Listener callback;
}
