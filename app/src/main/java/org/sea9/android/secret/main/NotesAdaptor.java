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
import java.util.stream.Collectors;

public class NotesAdaptor extends RecyclerView.Adapter<NotesAdaptor.ViewHolder> {
	private static final String TAG = "secret.list_adaptor";
	private static final String SPACE = " ";

	private RecyclerView recyclerView;

	private int selectedPos = -1;
	private boolean isSelected(int position) {
		return (selectedPos == position);
	}
	public final int getSelectedPosition() { return selectedPos; }
	public final void clearSelection() {
		selectedPos = -1;
		callback.updateContent(null);
	}

	public final void selectRow(int position) {
		selectedPos = position;
		onRowSelected(position);
	}
	public final int selectRow(String key) {
		int idx = -1;
		for (int i = 0; i < dataset.size(); i ++) {
			if (key.equals(dataset.get(i).getKey())) {
				selectRow(i);
				idx = i;
				break;
			}
		}
		return idx;
	}

	private List<NoteRecord> dataset;
	public final NoteRecord getRecord(int position) {
		if ((position >= 0) && (position < dataset.size()))
			return dataset.get(position);
		else
			return null;
	}
	public final void filterRecord(String query) {
		refresh();

		// Do this since using removeif() got an UnsupportedOperationException
		List<NoteRecord> filtered = dataset.stream().filter(p -> {
			if (p.getKey().toLowerCase().contains(query)) {
				return true;
			} else if (p.getTags() != null) {
				for (TagRecord tag : p.getTags()) {
					if (tag.getTag().toLowerCase().contains(query)) return true;
				}
			}
			return false;
		}).collect(Collectors.toList());
		if (filtered != null) {
			dataset = filtered;
		}
	}

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
//			callback.updateContent(position);
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

		refresh();
	}

	@Override @NonNull
	public final ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, int viewType) {
		Log.d(TAG, "onCreateViewHolder");

		// create a new view
		View item = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item, parent, false);

		// Click listener
		item.setOnClickListener(view -> {
			int position = recyclerView.getChildLayoutPosition(view);
			if (position == selectedPos) {
				clearSelection();
			} else {
				selectRow(position);
			}
			notifyDataSetChanged();
		});

		item.setOnLongClickListener(view -> {
			Snackbar.make(parent, "Long pressed...", Snackbar.LENGTH_LONG).show(); //TODO TEMP
			return false;
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
			for (int i = 1; i < tags.size(); i ++) {
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
	public void refresh() {
		dataset = DbContract.Notes.Companion.select(callback.getDbHelper());
		for (NoteRecord record : dataset) {
			List<TagRecord> tags = DbContract.NoteTags.Companion.select(callback.getDbHelper(), record.getPid());
			record.setTags(tags);
		}
	}

	public int delete(int position) {
//		if ((position >= 0) && (position < dataset.size())) {
//			return DbContract.Notes.Companion.delete(callback.getDbHelper(), dataset.get(position).getPid());
//		}
		return -1;
	}

	/**
	 * Retrieve detail of the selected row.
	 * @param position position selected on the recyclerView.
	 */
	public void onRowSelected(int position) {
		if ((position >= 0) && (position < dataset.size())) {
			String content = DbContract.Notes.Companion.select(callback.getDbHelper(), dataset.get(position).getPid());
			if (content != null) {
				callback.updateContent(content);
			}
		}
	}
	//=====================================================================

	/*=============
	 * View holder
	 */
	static class ViewHolder extends RecyclerView.ViewHolder {
		TextView key;
		TextView tag;
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
		DbHelper getDbHelper();
		void updateContent(String content);
	}
	private Listener callback;
}
