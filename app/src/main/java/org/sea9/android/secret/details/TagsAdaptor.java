package org.sea9.android.secret.details;

import android.database.SQLException;
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
import org.sea9.android.secret.data.TagRecord;

import java.util.ArrayList;
import java.util.List;

public class TagsAdaptor extends RecyclerView.Adapter<TagsAdaptor.ViewHolder> {
	private static final String TAG = "secret.tags_adaptor";

	private RecyclerView recyclerView;

	private List<Long> selectedIds = new ArrayList<>();
	final List<Long> getSelectedTags() {
		return new ArrayList<>(selectedIds);
		//Collections.sort(ret);
		//return ret;
	}
	private boolean isSelected(int position) {
		return (selectedIds.contains(dataset.get(position).getPid()));
	}
	public void selectTags(List<TagRecord> list) {
		selectedIds.clear();
		if (list != null) {
			for (TagRecord tag : list) {
				selectedIds.add(tag.getPid());
			}
		}
	}
//	public final void selectTag(int index) {
//		if ((index >= 0) && (index < getItemCount())) {
//			selectedPos.add(index);
//			callback.dataUpdated();
//			notifyDataSetChanged();
//		}
//	}

	private List<TagRecord> dataset;

	public TagsAdaptor(TagsAdaptor.Listener ctx) {
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
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		Log.d(TAG, "onCreateViewHolder");

		TextView item = (TextView) LayoutInflater.from(parent.getContext()).inflate(R.layout.tag_item, parent, false);

		item.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!callback.isFiltered()) {
					int position = recyclerView.getChildLayoutPosition(v);
					if (isSelected(position)) {
						selectedIds.remove(position);
					} else {
						selectedIds.add(dataset.get(position).getPid());
					}
					callback.dataUpdated();
					notifyDataSetChanged();
				}
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

		holder.tag.setText(dataset.get(position).getTag());
	}

	@Override
	public int getItemCount() {
		return dataset.size();
	}
	//=====================================================

	/*=====================================================================
	 * Data access methods. TODO: maybe need to move to a separate thread?
	 */
	private void refresh() {
		dataset = DbContract.Tags.Companion.select(callback.getDbHelper());
	}

	public final int create(String txt) {
		List<Long> tags = DbContract.Tags.Companion.search(callback.getDbHelper(), txt);
		long pid = -1;
		if (tags.size() > 0) {
			pid = tags.get(0);
		} else {
			TagRecord tag = DbContract.Tags.Companion.insert(callback.getDbHelper(), txt);
			if (tag != null) {
				pid = tag.getPid();
			}
			refresh();
		}

		int position = -1;
		if (pid >= 0) {
			for (int i = 0; i < dataset.size(); i ++) {
				if (dataset.get(i).getPid() == pid) {
					position = i;
					break;
				}
			}

			if ((position >= 0) && !isSelected(position)) { //Something wrong it position < 0...
				selectedIds.add(pid);
				callback.dataUpdated();
			}
			notifyDataSetChanged();
		}
		return position;
	}

	public final int delete() {
		try {
			return DbContract.Tags.Companion.delete(callback.getDbHelper());
		} catch (SQLException e) {
			return -1;
		}
	}
	//=====================================================================

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

	/*============================================
	 * Callback interface to the context fragment
	 */
	public interface Listener {
		DbHelper getDbHelper();
//		TagRecord getTag(int position);
//		int getTagsCount();
//		void selectionChanged();
		boolean isFiltered();
		void dataUpdated();
	}
	private TagsAdaptor.Listener callback;
}
