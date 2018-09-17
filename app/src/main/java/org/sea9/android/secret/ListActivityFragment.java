package org.sea9.android.secret;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import org.jetbrains.annotations.NotNull;

/**
 * A placeholder fragment containing a simple view.
 */
public class ListActivityFragment extends Fragment {
//	private RecyclerView.Adapter mAdapter;

	@Override
	public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View ret = inflater.inflate(R.layout.fragment_list, container, false);
		RecyclerView mRecyclerView = ret.findViewById(R.id.recycler_list);

		// use this setting to improve performance if you know that changes
		// in content do not change the layout size of the RecyclerView
		mRecyclerView.setHasFixedSize(true);

		// use a linear layout manager
		RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this.getContext());
		mRecyclerView.setLayoutManager(mLayoutManager);

		// specify an adapter (see also next example)
		String[] myDataset = {
				"0000", "0001", "0002", "0003", "0004", "0005", "0006", "0007", "0008", "0009",
				"0010", "0011", "0012", "0013", "0014", "0015", "0016", "0017", "0018", "0019",
				"0020", "0021", "0022", "0023", "0024", "0025", "0026", "0027", "0028", "0029"
		};
		mRecyclerView.setAdapter(new TempAdaptor(myDataset));

		return ret;
	}
}