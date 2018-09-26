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
import java.util.Map;

public class ContextFragment extends Fragment implements ListAdaptor.Listener<TempViewHolder> {
	public static final String TAG = "secret.ctx_frag";

	private ListAdaptor<TempViewHolder> adaptor;
	public final ListAdaptor<TempViewHolder> getAdaptor() {
		return adaptor;
	}

	private List<String> dataKey;
	private Map<String, String> dataSet;

	private void init() {
		dataSet = TempData.Companion.get();
		dataKey = new ArrayList<>(dataSet.keySet());
		actionListeners = new ArrayList<>();
		adaptor = new ListAdaptor<>(this);
	}

	public final void queryData(int position) {
		if ((position >= 0) && (position < dataKey.size())) {
			//TODO Temp data schema
			String k = dataKey.get(position);
			String v = dataSet.get(k);
			for (Interaction listener : actionListeners) {
				listener.retrieved(k, v);
			}
		}
	}

	public final int insertData(String key, String val) {//TODO Temp data schema
		// TODO TEMP start...
		int cnt = dataKey.size();
		if (cnt > 0) {
			key = Integer.toString(Integer.parseInt(dataKey.get(dataKey.size() - 1)) + 1);
		} else {
			key = "1000";
		}
		val = "1" + key;
		// ... TEMP end
		int ret;
		if (dataSet.put(key, val) == null) {
			if (dataKey.add(key)) {
				ret = dataKey.size() - 1;
				adaptor.onItemInsert(ret);
			} else {
				ret = -2;
			}
		} else {
			ret = -1;
		}
		for (Interaction listener : actionListeners) {
			listener.added(ret);
		}
		return ret;
	}

	public final boolean deleteData(int position) {
		if ((position < 0) || (position >= dataKey.size())) {
			return false;
		} else {
			if (adaptor.isSelected(position)) {
				datSelectionCleared();
			}
			if (dataSet.remove(dataKey.remove(position)) != null) {
				adaptor.onItemDeleted(position);
				return true;
			} else {
				adaptor.notifyDataSetChanged();
				return false;
			}
		}
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "ContextFragment.onCreate");
		setRetainInstance(true);
		init();
	}

	/*===================================================
	 * @see org.sea9.android.secret.ListAdaptor.Listener
	 */
	private static final String EMPTY = "";

	@Override
	public int getItemCount() {
		return dataSet.size();
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
		holder.key.setText(dataKey.get(position));
		holder.tag.setText("TEST DFLT XXXX YYYY");
	}

	@Override
	public void datSelectionMade(int index) {
		if (index >= 0) {
			callback.rowSelectionMade();
			String txt = dataSet.get(dataKey.get(index));
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
		void retrieved(String k, String v); //TODO Temp data schema
	}
	private List<Interaction> actionListeners;
	public void addSelectListener(Interaction listener) {
		actionListeners.add(listener);
	}
}
