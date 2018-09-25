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
		selectListeners = new ArrayList<>();
		adaptor = new ListAdaptor<>(this);
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
		return R.layout.item_list;
	}

	@Override
	public TempViewHolder getHolder(View view) {
		return new TempViewHolder(view);
	}

	@Override
	public void populateList(TempViewHolder holder, int position) {
		if (adaptor.isSelected(position)) {
			holder.bkg.setSelected(true);
		} else {
			holder.bkg.setSelected(false);
		}
		holder.key.setText(dataKey.get(position));
		holder.tag.setText("TEST DFLT XXXX YYYY");
	}

	@Override
	public void datSelectionMade(int index) {
		if (index >= 0) {
			callback.rowSelectionMade();
			String txt = dataSet.get(dataKey.get(index));
			for (SelectListener listener : selectListeners) {
				listener.select(txt);
			}
		} else {
			datSelectionCleared();
		}
	}

	@Override
	public void datSelectionCleared() {
		callback.rowSelectionCleared();
		for (SelectListener listener : selectListeners) {
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
	 * Selection Listener interface to the list fragment
	 */
	public interface SelectListener {
		void select(String content);
	}
	private List<SelectListener> selectListeners;
	public void addSelectListener(SelectListener listener) {
		selectListeners.add(listener);
	}
}
