package org.sea9.android.secret.main;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import org.sea9.android.secret.R;

public class NotesViewHolder extends RecyclerView.ViewHolder {
	public Long pid;
	public TextView key;
	public TextView tag;
	public NotesViewHolder(View v) {
		super(v);
		key = v.findViewById(R.id.item_name);
		tag = v.findViewById(R.id.item_tags);
	}
}
