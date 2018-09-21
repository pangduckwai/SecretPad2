package org.sea9.android.secret;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * A placeholder fragment containing a simple view.
 */
public class ListActivityFragment extends Fragment implements ContentUpdater {
	private ContextFragment ctxFrag;

	private RecyclerView recycler;
	private TextView content;

	@Override
	public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View ret = inflater.inflate(R.layout.fragment_list, container, false);
		recycler = ret.findViewById(R.id.recycler_list);
		content = ret.findViewById(R.id.item_content);

		// use this setting to improve performance if you know that changes
		// in content do not change the layout size of the RecyclerView
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
			if (ctxFrag != null)
				recycler.setAdapter(new TempAdaptor(ctxFrag.getDataSet(), this));
		}
	}

	@Override
	public void update(String txt) {
		content.setText(txt);
	}
}