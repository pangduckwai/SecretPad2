package org.sea9.android.secret.core;

import android.annotation.SuppressLint;
import android.content.Context;
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

import javax.crypto.BadPaddingException;

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
		for (int i = 0; i < shown.size(); i ++) {
			if (pid == shown.get(i).getPid()) {
				return i;
			}
		}
		return -1;
	}

	final void selectRow(int position) {
		selectedPos = position;
		selectDetails(position);
	}

	private List<NoteRecord> shown;
	private List<NoteRecord> cache;
	final NoteRecord getRecord(int position) {
		if ((position >= 0) && (position < shown.size()))
			return shown.get(position);
		else
			return null;
	}
	final void filterRecords(String query) {
		// Do this since using removeif() got an UnsupportedOperationException
		List<NoteRecord> filtered = cache.stream().filter(p -> {
			if (p.getKey().toLowerCase().contains(query)) {
				return true;
			} else
				return (p.getTagNames() != null) && p.getTagNames().contains(query);
		}).collect(Collectors.toList());
		if (filtered != null) {
			shown = filtered;
		}
	}
	final void clearFilter() {
		shown = cache;
	}
	final void clearRecords() {
		shown.clear();
		cache.clear();
	}

	NotesAdaptor(Caller ctx) {
		caller = ctx;
		shown = new ArrayList<>();
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
			if (caller.isBusy()) return;
			int position = recyclerView.getChildLayoutPosition(view);
			if (position == selectedPos) {
				clearSelection();
			} else {
				selectRow(position);
			}
			notifyDataSetChanged();
		});

		item.setOnLongClickListener(view -> {
			if (!caller.isFiltered() && !caller.isBusy()) {
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

		NoteRecord record = shown.get(position);
		holder.key.setText(record.getKey());

		String tagNames = record.getTagNames();
		if (tagNames != null) {
			holder.tag.setText(record.getTagNames());
		}
	}

	@Override
	public int getItemCount() {
		return shown.size();
	}
	//=====================================================

	/*======================
	 * Data access methods.
	 */
	final void populateCache() {
		try {
			cache = DbContract.Notes.Companion.select(caller.getDbHelper());

			// Build the concatenated tag string
			StringBuilder buff = new StringBuilder();
			for (NoteRecord record : cache) {
				List<Long> tags = record.getTags();
				if ((tags != null) && (tags.size() > 0)) {
					buff.setLength(0);
					buff.append(caller.getTag(tags.get(0)));
					for (int i = 1; i < tags.size(); i ++)
						buff.append(SPACE).append(caller.getTag(tags.get(i)));
					record.setTagNames(buff.toString());
				}
			}

			shown = cache;
		} catch (RuntimeException e) {
			if ((e.getCause() != null) && (e.getCause() instanceof BadPaddingException)) {
				String msg = String.format(caller.getContext().getString(R.string.msg_logon_fail), e.getMessage());
				caller.getCallback().doNotify(MainActivity.MSG_DIALOG_LOG_FAIL, msg, true);
			} else
				throw e;
		}
		if (cache == null) {
			cache = new ArrayList<>();
			shown = new ArrayList<>();
		}
	}

	final void selectDetails(int position) { // Retrieve detail of the selected row
		if (caller.isBusy()) return;
		if ((position >= 0) && (position < shown.size())) {
			String content = DbContract.Notes.Companion.select(caller.getDbHelper(), shown.get(position).getPid());
			if (content != null) {
				caller.updateContent(content);
			}
		}
	}

	final int delete(int position) {
		int ret = -1;
		if ((position >= 0) && (position < shown.size())) {
			ret = DbContract.Notes.Companion.delete(caller.getDbHelper(), shown.get(position).getPid());
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
		boolean isBusy();
		void longPressed();
		void updateContent(String content);
		void onLogoff();
		String getTag(long tid);
		Context getContext();
		ContextFragment.Callback getCallback(); //TODO : simplify this Caller by getting the callback directly?
	}
	private Caller caller;
}
