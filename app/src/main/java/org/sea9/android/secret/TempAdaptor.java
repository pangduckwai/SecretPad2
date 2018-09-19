package org.sea9.android.secret;

import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TempAdaptor extends RecyclerView.Adapter<TempAdaptor.ViewHolder> {
	private static final String EMPTY = "";

	private List<Map.Entry<String, String>> dataSet;

	private RecyclerView recyclerView;
	private ContentUpdater updater;
	private int selectedPos = -1;
	private int highlightColor;
	private Drawable itemBackground;

	@Override
	public void onAttachedToRecyclerView(@NonNull RecyclerView recycler) {
		super.onAttachedToRecyclerView(recycler);
		recyclerView = recycler;
	}

	// Provide a reference to the views for each data item
	// Complex data items may need more than one view per item, and
	// you provide access to all the views for a data item in a view holder
	static class ViewHolder extends RecyclerView.ViewHolder {
		View bkg;
		TextView num;
		TextView key;
		ViewHolder(View v) {
			super(v);
			bkg = v;
			num = v.findViewById(R.id.item_num);
			key = v.findViewById(R.id.item_name);
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
		// Highlight color
		highlightColor = ContextCompat.getColor(parent.getContext(), R.color.colorSelect);

		// Ripple background
		int[] attrs = new int[] { android.R.attr.selectableItemBackground };
		TypedArray ta = parent.getContext().obtainStyledAttributes(attrs);
		itemBackground = ta.getDrawable(0);
		ta.recycle();

		// create a new view
		View item = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_list, parent, false);

		// Click listener
		item.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				int pos = recyclerView.getChildLayoutPosition(v);
				if (pos == selectedPos) {
					selectedPos = -1;
					updater.update(EMPTY);
				} else {
					selectedPos = pos;
					updater.update(dataSet.get(selectedPos).getValue());
				}

				recyclerView.postDelayed(new Runnable() {
					@Override
					public void run() {
						notifyDataSetChanged();
					}
				}, 70);
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
			holder.bkg.setSelected(true);
			holder.bkg.setBackgroundColor(highlightColor);
		} else {
			holder.bkg.setSelected(false);
			holder.bkg.setBackground(itemBackground);
		}
		holder.num.setText(Integer.toString(position));
		holder.key.setText(dataSet.get(position).getKey());
	}

	// Return the size of your dataset (invoked by the layout manager)
	@Override
	public int getItemCount() {
		return dataSet.size();
	}
}
