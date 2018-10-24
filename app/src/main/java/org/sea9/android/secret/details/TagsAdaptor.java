package org.sea9.android.secret.details;

import android.database.SQLException;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import org.sea9.android.secret.R;
import org.sea9.android.secret.data.DbContract;
import org.sea9.android.secret.data.DbHelper;
import org.sea9.android.secret.data.TagRecord;

import java.util.ArrayList;
import java.util.List;

public class TagsAdaptor extends RecyclerView.Adapter<TagsAdaptor.ViewHolder> {
	private static final String TAG = "secret.tags_adaptor";

	private RecyclerView recyclerView;

	private List<Long> selectedIds = new ArrayList<>();
	final List<Long> getSelectedTags() {
		return new ArrayList<>(selectedIds);
	}
	private boolean isSelected(int position) {
		return (selectedIds.contains(cache.get(position).getPid()));
	}
	public void selectTags(List<TagRecord> list) {
		selectedIds.clear();
		if (list != null) {
			for (TagRecord tag : list) {
				selectedIds.add(tag.getPid());
			}
		}
	}

	private List<TagRecord> cache;

	public TagsAdaptor(Caller ctx) {
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

		select();
	}

	@Override @NonNull
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		Log.d(TAG, "onCreateViewHolder");

		TextView item = (TextView) LayoutInflater.from(parent.getContext()).inflate(R.layout.tag_item, parent, false);

		item.setOnClickListener(view -> {
			if (!caller.isFiltered()) {
				int position = recyclerView.getChildLayoutPosition(view);
				int index = selectedIds.indexOf(cache.get(position).getPid());
				if (index >= 0) {
					selectedIds.remove(index);
				} else {
					selectedIds.add(cache.get(position).getPid());
				}
				caller.dataUpdated();
				notifyDataSetChanged();
			}
		});

		return new ViewHolder(item);
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
		if (isSelected(position)) {
			holder.itemView.setSelected(true);
		} else {
			holder.itemView.setSelected(false);
		}

		holder.tag.setText(cache.get(position).getTag());
	}

	@Override
	public int getItemCount() {
		return cache.size();
	}
	//=====================================================

	/*======================
	 * Data access methods.
	 */
	private void select() {
		cache = DbContract.Tags.Companion.select(caller.getDbHelper());
	}

	public final int insert(String txt) {
		List<Long> tags = DbContract.Tags.Companion.search(caller.getDbHelper(), txt);
		long pid = -1;
		if (tags.size() > 0) {
			pid = tags.get(0);
		} else {
			TagRecord tag = DbContract.Tags.Companion.insert(caller.getDbHelper(), txt);
			if (tag != null) {
				pid = tag.getPid();
			}
			select();
		}

		int position = -1;
		if (pid >= 0) {
			for (int i = 0; i < cache.size(); i ++) {
				if (cache.get(i).getPid() == pid) {
					position = i;
					break;
				}
			}

			if ((position >= 0) && !isSelected(position)) { //Something wrong if position < 0...
				selectedIds.add(pid);
				caller.dataUpdated();
			}
			notifyDataSetChanged();
		}
		return position;
	}

	public final int delete() {
		try {
			return DbContract.Tags.Companion.delete(caller.getDbHelper());
		} catch (SQLException e) {
			return -1;
		}
	}
	//======================

	/*=============
	 * View holder
	 */
	static class ViewHolder extends RecyclerView.ViewHolder {
		TextView tag;
		ViewHolder(TextView v) {
			super(v);
			tag = v;
		}
	}
	//=============

	/*==========================================
	 * Access interface to the ContextFragment
	 */
	public interface Caller {
		DbHelper getDbHelper();
		boolean isFiltered();
		void dataUpdated();
	}
	private Caller caller;
}
