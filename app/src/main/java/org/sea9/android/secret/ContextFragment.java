package org.sea9.android.secret;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class ContextFragment extends Fragment implements TempAdaptor.Listener {
	public static final String TAG = "secret.ctx_frag";

	public interface SelectListener {
		void select(String content);
	}
	private List<SelectListener> selectListeners;
	public void addSelectListener(SelectListener listener) {
		selectListeners.add(listener);
	}

	private TempAdaptor adaptor;
	public final TempAdaptor getAdaptor() {
		return adaptor;
	}

	private void init() {
		selectListeners = new ArrayList<>();
		adaptor = new TempAdaptor(this);
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "ContextFragment.onCreate");
		setRetainInstance(true);
		init();
	}

	/*===================================================
	 * @see org.sea9.android.secret.TempAdaptor.Listener
	 */
	private static final String EMPTY = "";

	@Override
	public void datSelectionMade(String txt) {
		callback.rowSelectionMade();
		for (SelectListener listener : selectListeners) {
			listener.select(txt);
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

	/*==========================================
	 * Callback interface for the main activity
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
}
