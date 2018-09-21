package org.sea9.android.secret;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import java.util.Map;

public class ContextFragment extends Fragment {
	public static final String TAG = "secret.ctx_frag";

	private Map<String, String> dataSet;
	public final Map<String, String> getDataSet() { return dataSet; }

	private void init() {
		dataSet = TempData.Companion.get();
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		init();
	}

	public interface Listener {
		void doSearch(String query);
	}
	private Listener callback;

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		try {
			callback = (Listener) context;
		} catch (ClassCastException e) {
			throw new ClassCastException(context.toString() + " missing implementation of ListActivityFragment.Listener");
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
	}
}
