package org.sea9.android.secret.core;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;
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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class NotesAdaptor extends RecyclerView.Adapter<NotesAdaptor.ViewHolder> {
	private static final String TAG = "secret.notes_adaptor";
	private static final String SPACE = " ";

	private RecyclerView recyclerView;

	private int selectedPos = -1;
	final boolean isSelected(int position) {
		return (selectedPos == position);
	}
	final int getSelectedPosition() { return selectedPos; }
	final void clearSelection() {
		selectedPos = -1;
		caller.updateContent(null);
	}
	final int findSelectedPosition(long pid) {
		for (int i = 0; i < cache.size(); i ++) {
			if (pid == cache.get(i).getPid()) {
				return i;
			}
		}
		return -1;
	}

	final void selectRow(int position) {
		selectedPos = position;
		selectDetails(position);
	}

	private List<NoteRecord> cache;
	final NoteRecord getRecord(int position) {
		if ((position >= 0) && (position < cache.size()))
			return cache.get(position);
		else
			return null;
	}
	final void filterRecords(String query) {
		populateCache();

		// Do this since using removeif() got an UnsupportedOperationException
		List<NoteRecord> filtered = cache.stream().filter(p -> {
			if (p.getKey().toLowerCase().contains(query)) {
				return true;
			} else if (p.getTags() != null) {
				for (Long tid : p.getTags()) {
					if (caller.getTag(tid).toLowerCase().contains(query)) return true;
				}
			}
			return false;
		}).collect(Collectors.toList());
		if (filtered != null) {
			cache = filtered;
		}
	}
	final void clearRecords() {
		cache.clear();
	}

	NotesAdaptor(Caller ctx) {
		caller = ctx;
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

	@SuppressLint("ClickableViewAccessibility")
	@Override @NonNull
	public final ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, int viewType) {
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
			if (!caller.isFiltered()) {
				int position = recyclerView.getChildLayoutPosition(view);
				selectRow(position);
				caller.longPressed();
				notifyDataSetChanged();
				return true;
			} else
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

		List<Long> tags = record.getTags();
		if ((tags != null) && tags.size() > 0) {
			StringBuilder buff = new StringBuilder(caller.getTag(tags.get(0)));
			for (int i = 1; i < tags.size(); i ++) {
				buff.append(SPACE).append(caller.getTag(tags.get(i)));
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
	final void populateCache() {
		cache = DbContract.Notes.Companion.select(caller.getDbHelper());
		if (cache == null) {
			cache = new ArrayList<>();
		}
	}

	final void selectDetails(int position) { // Retrieve detail of the selected row
		if ((position >= 0) && (position < cache.size())) {
			String content = DbContract.Notes.Companion.select(caller.getDbHelper(), cache.get(position).getPid());
			if (content != null) {
				caller.updateContent(content);
			}
		}
	}

	final int delete(int position) {
		int ret = -1;
		if ((position >= 0) && (position < cache.size())) {
			ret = DbContract.Notes.Companion.delete(caller.getDbHelper(), cache.get(position).getPid());
			if (ret >= 0) {
				if (position < selectedPos) {
					selectedPos --;
				}
			}
		}
		return ret;
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

	/*=========================================
	 * Access interface to the ContextFragment
	 */
	public interface Caller {
		DbHelper getDbHelper();
		boolean isFiltered();
		void longPressed();
		void updateContent(String content);
		void onLogoff();
		String getTag(long tid);
	}
	private Caller caller;
}
