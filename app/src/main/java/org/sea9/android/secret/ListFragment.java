package org.sea9.android.secret;

import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * A placeholder fragment containing a simple view.
 */
public class ListFragment extends Fragment implements ContextFragment.SelectListener {
	private ContextFragment ctxFrag;

	private RecyclerView recycler;
	private TextView content;

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View ret = inflater.inflate(R.layout.fragment_list, container, false);
		recycler = ret.findViewById(R.id.recycler_list);
		content = ret.findViewById(R.id.item_content);

		// use this setting to improve performance if you know that changes
		// in content do not select the layout size of the RecyclerView
		recycler.setHasFixedSize(true);

		// use a linear layout manager
		recycler.setLayoutManager(new LinearLayoutManager(this.getContext()));

		return ret;
	}

	@Override
	public void onResume() {
		super.onResume();
		// specify an adapter
		FragmentManager manager = getFragmentManager();
		if (manager != null) {
			ctxFrag = (ContextFragment) manager.findFragmentByTag(ContextFragment.TAG);
			if (ctxFrag != null) {
				ctxFrag.addSelectListener(this);
				recycler.setAdapter(ctxFrag.getAdaptor()); //new TempAdaptor(ctxFrag.getDataSet(), this));
			}
		}
	}

	/*=============================================================
	 * @see org.sea9.android.secret.ContextFragment.SelectListener
	 */
	@Override
	public void select(String txt) {
		content.setText(txt);
	}
}