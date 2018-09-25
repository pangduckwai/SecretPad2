package org.sea9.android.secret.temp;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import org.sea9.android.secret.R;

public class TempViewHolder extends RecyclerView.ViewHolder {
	public TextView key;
	public TextView tag;
	public TempViewHolder(View v) {
		super(v);
		key = v.findViewById(R.id.item_name);
		tag = v.findViewById(R.id.item_tags);
	}
}
