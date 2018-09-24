package org.sea9.android.secret;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TempAdaptor extends ListAdaptor<TempAdaptor.ViewHolder> {
	private List<String> dataKey;
	private Map<String, String> dataSet;

	static class ViewHolder extends RecyclerView.ViewHolder {
		View bkg;
		TextView key;
		TextView tag;
		ViewHolder(View v) {
			super(v);
			bkg = v;
			key = v.findViewById(R.id.item_name);
			tag = v.findViewById(R.id.item_tags);
		}
	}

	public interface Listener {
		void datSelectionMade(String content);
		void datSelectionCleared();
	}
	private Listener callback;

	TempAdaptor(Listener cb) {
		callback = cb;
		dataSet = TempData.Companion.get();
		dataKey = new ArrayList<>(dataSet.keySet());
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
		holder.key.setText(dataKey.get(position));
		holder.tag.setText("TEST DFLT XXXX YYYY");
	}

	@Override
	protected void clearContent() {
		callback.datSelectionCleared();
	}

	@Override
	protected void updateContent(int index) {
		if (index >= 0) {
			callback.datSelectionMade(dataSet.get(dataKey.get(selectedPos)));
		}
	}
}
