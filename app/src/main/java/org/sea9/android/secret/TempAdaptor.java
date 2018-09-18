package org.sea9.android.secret;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TempAdaptor extends RecyclerView.Adapter<TempAdaptor.ViewHolder> {
	private List<Map.Entry<String, String>> dataSet;

	private RecyclerView recyclerView;
	private ContentUpdater updater;
	private int selectedPos = -1;

	@Override
	public void onAttachedToRecyclerView(@NonNull RecyclerView recycler) {
		super.onAttachedToRecyclerView(recycler);
		recyclerView = recycler;
	}

	// Provide a reference to the views for each data item
	// Complex data items may need more than one view per item, and
	// you provide access to all the views for a data item in a view holder
	static class ViewHolder extends RecyclerView.ViewHolder {
		// each data item is just a string in this case
		TextView key;
		ViewHolder(TextView v) {
			super(v);
			key = v;
		}
	}

	// Provide a suitable constructor (depends on the kind of dataset)
	TempAdaptor(Map<String, String> data, ContentUpdater updtr) {
		dataSet = new ArrayList<>(data.entrySet());
		updater = updtr;
	}

	// Create new views (invoked by the layout manager)
	@Override @NonNull
	public TempAdaptor.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		// create a new view
		TextView item = (TextView) LayoutInflater.from(parent.getContext()).inflate(R.layout.item_list, parent, false);

		item.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				int pos = recyclerView.getChildLayoutPosition(v);
				if (pos == selectedPos) {
					selectedPos = -1;
					updater.update("");
				} else {
					selectedPos = pos;
					updater.update(dataSet.get(selectedPos).getValue());
				}
				notifyDataSetChanged();
			}
		});

		return new ViewHolder(item);
	}

	// Replace the contents of a view (invoked by the layout manager)
	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
		// - get element from your dataset at this position
		// - replace the contents of the view with that element
		if (selectedPos == position) {
			holder.key.setSelected(true);
		} else {
			holder.key.setSelected(false);
		}
		holder.key.setText(dataSet.get(position).getKey());
	}

	// Return the size of your dataset (invoked by the layout manager)
	@Override
	public int getItemCount() {
		return dataSet.size();
	}
}
