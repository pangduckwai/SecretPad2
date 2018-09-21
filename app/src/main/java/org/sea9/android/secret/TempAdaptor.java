package org.sea9.android.secret;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TempAdaptor extends ListAdaptor<TempAdaptor.ViewHolder> {
	private static final String EMPTY = "";

	private List<Map.Entry<String, String>> dataSet;

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

	public interface Listener {
		void update(String content);
	}

	private Listener callback;
	public void setCallback(Listener cb) {
		callback = cb;
	}

	TempAdaptor() {
		dataSet = new ArrayList<>(TempData.Companion.get().entrySet());
	}

	@Override
	public int getItemCount() {
		return dataSet.size();
	}

	@Override
	protected int getListItemLayout() {
		return R.layout.item_list;
	}

	@Override
	protected ViewHolder getHolder(View view) {
		return new ViewHolder(view);
	}

	@Override
	protected void updateList(ViewHolder holder, int position) {
		if (isSelected(position)) {
			holder.bkg.setSelected(true);
		} else {
			holder.bkg.setSelected(false);
		}
		holder.num.setText(String.format("R%d", position));
		holder.key.setText(dataSet.get(position).getKey());
	}

	@Override
	protected void clearContent() {
		callback.update(EMPTY);
	}

	@Override
	protected void updateContent(int index) {
		if (index >= 0) {
			callback.update(dataSet.get(selectedPos).getValue());
		}
	}
}
