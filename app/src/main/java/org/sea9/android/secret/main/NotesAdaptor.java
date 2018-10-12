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
	private static final String TAG = "secret.notes_adaptor";
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
		for (int i = 0; i < cache.size(); i ++) {
			if (key.equals(cache.get(i).getKey())) {
				selectRow(i);
				idx = i;
				break;
			}
		}
		return idx;
	}

	private List<NoteRecord> cache;
	public final NoteRecord getRecord(int position) {
		if ((position >= 0) && (position < cache.size()))
			return cache.get(position);
		else
			return null;
	}
	public final void filterRecord(String query) {
		refresh();

		// Do this since using removeif() got an UnsupportedOperationException
		List<NoteRecord> filtered = cache.stream().filter(p -> {
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
			cache = filtered;
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

	public NotesAdaptor(Listener ctx) {
		callback = ctx;
		cache = new ArrayList<>();
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

		NoteRecord record = cache.get(position);
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
		return cache.size();
	}
	//=====================================================

	/*======================
	 * Data access methods.
	 */
	public void refresh() {
		cache = DbContract.Notes.Companion.select(callback.getDbHelper());
		for (NoteRecord record : cache) {
			List<TagRecord> tags = DbContract.NoteTags.Companion.select(callback.getDbHelper(), record.getPid());
			record.setTags(tags);
		}
	}

	public int update(String k, String c, List<Long> t) {
		int position = 0, ret = -1;
		while (position < cache.size()) {
			if (cache.get(position).getKey().equals(k)) {
				ret = DbContract.Notes.Companion.update(callback.getDbHelper(), cache.get(position).getPid(), c, t);
				break;
			}
			position ++;
		}

		if (ret >= 0) {
			refresh();
			notifyItemChanged(position);
			callback.updateContent(c);
			return position;
		} else
			return ret;
	}

	int delete(int position) {
		int ret = -1;
		if ((position >= 0) && (position < cache.size())) {
			if (isSelected(position)) {
				clearSelection();
			}

			ret = DbContract.Notes.Companion.delete(callback.getDbHelper(), cache.get(position).getPid());
			if (ret >= 0) {
				refresh();
				notifyItemRemoved(position);
				if (position < selectedPos) {
					selectedPos --;
				}
			} else {
				notifyDataSetChanged();
			}
		}
		return ret;
	}

	/**
	 * Retrieve detail of the selected row.
	 * @param position position selected on the recyclerView.
	 */
	void onRowSelected(int position) {
		if ((position >= 0) && (position < cache.size())) {
			String content = DbContract.Notes.Companion.select(callback.getDbHelper(), cache.get(position).getPid());
			if (content != null) {
				callback.updateContent(content);
			}
		}
	}
	//======================

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
