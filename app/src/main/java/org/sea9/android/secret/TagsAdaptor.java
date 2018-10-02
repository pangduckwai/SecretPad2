package org.sea9.android.secret;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TagsAdaptor extends RecyclerView.Adapter<TagsAdaptor.ViewHolder> {
	private static final String TAG = "secret.tags_adaptor";

	private RecyclerView recyclerView;

	private List<Integer> selectedPos = new ArrayList<>();
	public final List<Integer> getSelectedPosition() {
		List<Integer> ret = new ArrayList<>(selectedPos);
		Collections.sort(ret);
		return ret;
	}
	public final void selectTag(int index) {
		if ((index >= 0) && (index < getItemCount())) {
			selectedPos.add(index);
			callback.selectionChanged();
			notifyDataSetChanged();
		}
	}

	static class ViewHolder extends RecyclerView.ViewHolder {
		TextView tag;
		ViewHolder(TextView v) {
			super(v);
			tag = v;
		}
	}

	public TagsAdaptor(TagsAdaptor.Listener callback) {
		this.callback = callback;
	}

	public void prepare(List<Integer> sel) {
		selectedPos.clear();
		if (sel != null) selectedPos.addAll(sel);
	}

	@Override
	public final void onAttachedToRecyclerView(@NonNull RecyclerView recycler) {
		super.onAttachedToRecyclerView(recycler);
		Log.d(TAG, "onAttachedToRecyclerView");
		recyclerView = recycler;
	}

	@Override @NonNull
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		Log.d(TAG, "onCreateViewHolder");

		TextView item = (TextView) LayoutInflater.from(parent.getContext()).inflate(R.layout.tag_item, parent, false);

		item.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				int pos = recyclerView.getChildLayoutPosition(v);
				if (selectedPos.contains(pos)) {
					selectedPos.remove(Integer.valueOf(pos));
				} else {
					selectedPos.add(pos);
				}
				callback.selectionChanged();
				notifyDataSetChanged();
			}
		});

		return new ViewHolder(item);
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
		if (selectedPos.contains(position)) {
			holder.itemView.setSelected(true);
		} else {
			holder.itemView.setSelected(false);
		}
		holder.tag.setText(callback.getTag(position));
	}

	@Override
	public int getItemCount() {
		return callback.getTagsCount();
	}

	/*============================================
	 * Callback interface to the context fragment
	 */
	public interface Listener {
		String getTag(int position);
		int getTagsCount();
		void selectionChanged();
	}
	private TagsAdaptor.Listener callback;
}
