package org.sea9.android.secret;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

public class ContextFragment extends Fragment {
	public static final String TAG = "secret.ctx_frag";

	private TempAdaptor adaptor;
	public final TempAdaptor getAdaptor(TempAdaptor.Listener cb) {
		adaptor.setCallback(cb);
		return adaptor;
	}

	private void init() {
		adaptor = new TempAdaptor();
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
			throw new ClassCastException(context.toString() + " missing implementation of ContextFragment.Listener");
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
	}
}
