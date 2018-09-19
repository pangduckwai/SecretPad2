package org.sea9.android.secret;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public abstract class ListActivityAdaptor<H extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<H> {
//	protected int highlightColor;
//	protected Drawable itemBackground;
	private RecyclerView recyclerView;

	protected int selectedPos = -1;
	protected final boolean isSelected(int position) {
		return (selectedPos == position);
	}

	/**
	 * @return Return the list item layout id.
	 */
	protected abstract int getListItemLayout();

	/**
	 * @return Return the implementation of the ViewHolder.
	 */
	protected abstract H getHolder(View view);

	/**
	 * Update the list according to the data set.
	 * @param position position of the current item in the list.
	 */
	protected abstract void updateList(H holder, int position);

	/**
	 * Clear content.
	 */
	protected abstract void clearContent();

	/**
	 * Update content.
	 */
	protected abstract void updateContent(int index);

	@Override
	public final void onAttachedToRecyclerView(@NonNull RecyclerView recycler) {
		super.onAttachedToRecyclerView(recycler);
		recyclerView = recycler;
	}

	@Override @NonNull
	public final H onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		// Highlight color
//		highlightColor = ContextCompat.getColor(parent.getContext(), R.color.colorSelect);

		// Ripple background
//		int[] attrs = new int[] { android.R.attr.selectableItemBackground };
//		TypedArray ta = parent.getContext().obtainStyledAttributes(attrs);
//		itemBackground = ta.getDrawable(0);
//		ta.recycle();

		// create a new view
		View item = LayoutInflater.from(parent.getContext()).inflate(getListItemLayout(), parent, false);

		// Click listener
		item.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				int pos = recyclerView.getChildLayoutPosition(v);
				if (pos == selectedPos) {
					selectedPos = -1;
					clearContent();
				} else {
					selectedPos = pos;
					updateContent(pos);
				}
				notifyDataSetChanged();
			}
		});

		return getHolder(item);
	}

	@Override
	public final void onBindViewHolder(@NonNull H holder, int position) {
		updateList(holder, position);
	}
}
