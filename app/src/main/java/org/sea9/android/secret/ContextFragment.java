package org.sea9.android.secret;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.View;

import org.sea9.android.secret.temp.TempData;
import org.sea9.android.secret.temp.TempViewHolder;

import java.util.ArrayList;
import java.util.List;

public class ContextFragment extends Fragment implements
		ListAdaptor.Listener<TempViewHolder>,
		TagsAdaptor.Listener {
	public static final String TAG = "secret.ctx_frag";

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "ContextFragment.onCreate");
		setRetainInstance(true);
		init();
	}

	private List<DataRecord> dataList;
	private List<String> tagList;

	private ListAdaptor<TempViewHolder> adaptor;
	public final ListAdaptor<TempViewHolder> getAdaptor() {
		return adaptor;
	}

	private TagsAdaptor tagsAdaptor;
	public final TagsAdaptor getTagsAdaptor() {
		return tagsAdaptor;
	}

	private void init() {
		dataList = TempData.Companion.data();
		tagList = TempData.Companion.tags();
		actionListeners = new ArrayList<>();
		adaptor = new ListAdaptor<>(this);
		tagsAdaptor = new TagsAdaptor(this);
	}

	/*=======================================================
	 * Data maintenance APIs - query, insert, update, delete
	 */
	public final void queryData(int position) {
		if ((position >= 0) && (position < dataList.size())) {
			for (Interaction listener : actionListeners) {
				listener.retrieved(dataList.get(position));
			}
		}
	}

	// TODO TEMP
	public final int insertData(String k, String c, List<Integer> t) {
		int ret;
		DataRecord rec = new DataRecord(k, c, t);
		if (dataList.add(rec)) {
			ret = dataList.size() - 1;
			adaptor.onItemInsert(ret);
		} else {
			ret = -1;
		}
		for (Interaction listener : actionListeners) {
			listener.added(ret);
		}
		return ret;
	}

	// TODO TEMP
	public final int updateData(String k, String c, List<Integer> t) {
		DataRecord rec;
		int ret = 0;
		while (ret < dataList.size()) {
			rec = dataList.get(ret);
			if (rec.getKey().equals(k)) {
				rec.setContent(c);
				rec.getTags().clear();
				rec.getTags().addAll(t);
				if (dataList.set(ret, rec) != null) {
					adaptor.notifyItemChanged(ret);
					break;
				}
			}
			ret ++;
		}
		if (ret >= dataList.size()) ret = -1;
		for (Interaction listener : actionListeners) {
			listener.changed(ret);
			listener.select(c);
		}
		return ret;
	}

	// TODO TEMP
	public final boolean deleteData(int position) {
		if ((position < 0) || (position >= dataList.size())) {
			return false;
		} else {
			if (adaptor.isSelected(position)) {
				datSelectionCleared();
			}
			if (dataList.remove(position) != null) {
				adaptor.onItemDeleted(position);
				return true;
			} else {
				adaptor.notifyDataSetChanged();
				return false;
			}
		}
	}
	//=======================================================

	/*===================================================
	 * @see org.sea9.android.secret.ListAdaptor.Listener
	 */
	private static final String EMPTY = "";
	private static final String SPACE = " ";

	@Override
	public int getItemCount() {
		return dataList.size();
	}

	@Override
	public int getListItemLayoutId() {
		return R.layout.list_item;
	}

	@Override
	public TempViewHolder getHolder(View view) {
		return new TempViewHolder(view);
	}

	@Override
	public void populateList(TempViewHolder holder, int position) {
		if (adaptor.isSelected(position)) {
			holder.itemView.setSelected(true);
		} else {
			holder.itemView.setSelected(false);
		}

		DataRecord rec = dataList.get(position);
		List<Integer> tag = rec.getTags();
		StringBuilder tags = new StringBuilder((tag.size() > 0) ? tagList.get(tag.get(0)) : EMPTY);
		for (int i = 1; i < tag.size(); i ++)
			tags.append(SPACE).append(tagList.get(tag.get(i)));
		holder.key.setText(rec.getKey());
		holder.tag.setText(tags.toString());
	}

	@Override
	public void datSelectionMade(int index) {
		if (index >= 0) {
			callback.rowSelectionMade();
			String txt = dataList.get(index).getContent();
			for (Interaction listener : actionListeners) {
				listener.select(txt);
			}
		} else {
			datSelectionCleared(); // Should not be possible to reach here
		}
	}

	@Override
	public void datSelectionCleared() {
		callback.rowSelectionCleared();
		for (Interaction listener : actionListeners) {
			listener.select(EMPTY);
		}
	}
	//===================================================

	/*===================================================
	 * @see org.sea9.android.secret.TagsAdaptor.Listener
	 */
	@Override
	public String getTag(int position) {
		return tagList.get(position);
	}

	@Override
	public int getTagsCount() {
		return tagList.size();
	}
	//===================================================

	/*=========================================
	 * Callback interface to the main activity
	 */
	public interface Listener {
		void rowSelectionMade();
		void rowSelectionCleared();
	}
	private Listener callback;

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		Log.d(TAG, "ContextFragment.onAttach");
		try {
			callback = (Listener) context;
		} catch (ClassCastException e) {
			throw new ClassCastException(context.toString() + " missing implementation of ContextFragment.Listener");
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		Log.d(TAG, "ContextFragment.onDetach");
		callback = null;
	}
	//=========================================

	/*===================================================
	 * Interaction interface to the list fragment
	 */
	public interface Interaction {
		void select(String content);
		void added(int position);
		void changed(int position);
		void retrieved(DataRecord record);
	}
	private List<Interaction> actionListeners;
	public void addSelectListener(Interaction listener) {
		actionListeners.add(listener);
	}
}
