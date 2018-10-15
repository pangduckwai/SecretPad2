package org.sea9.android.secret;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.widget.Filter;
import android.widget.Filterable;

import org.sea9.android.secret.data.DbContract;
import org.sea9.android.secret.data.DbHelper;
import org.sea9.android.secret.data.NoteRecord;
import org.sea9.android.secret.details.TagsAdaptor;
import org.sea9.android.secret.main.NotesAdaptor;

public class ContextFragment extends Fragment implements
		DbHelper.Listener,
		NotesAdaptor.Listener,
		TagsAdaptor.Listener,
		Filterable,
		Filter.FilterListener {
	public static final String TAG = "secret.ctx_frag";
	private static final String EMPTY = "";

	private DbHelper dbHelper;
	@Override public final DbHelper getDbHelper() {
		if ((dbHelper == null) || !dbHelper.getReady()) throw new RuntimeException("Database not ready");
		return dbHelper;
	}

	private NotesAdaptor adaptor;
	public final NotesAdaptor getAdaptor() {
		return adaptor;
	}

	private TagsAdaptor tagsAdaptor;
	public final TagsAdaptor getTagsAdaptor() {
		return tagsAdaptor;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate");
		setRetainInstance(true);

		adaptor = new NotesAdaptor(this);
		tagsAdaptor = new TagsAdaptor(this);

		updated = false;
		filtered = false;

		//TODO TEMP >>>>>>>>>>>>
		dbHelper = new DbHelper(this);
		dbHelper.getWritableDatabase().execSQL(DbContract.SQL_CONFIG);
		(new org.sea9.android.secret.data.DbTest()).run(this);
		//TODO TEMP <<<<<<<<<<<<
		new DbInitTask().execute(this); //Initial DB async since the first call to getXxxDatabase() can be slow
	}

	@Override
	public void onDestroy() {
		Log.d(TAG, "onDestroy");
		//TODO TEMP >>>>>>>>>>>>
		org.sea9.android.secret.data.DbTest.cleanup(this);
		//TODO TEMP <<<<<<<<<<<<
		dbHelper.close();
		super.onDestroy();
	}

	private boolean updated;
	public final boolean isUpdated() { return updated; }
	public final void clearUpdated() {  updated = false; }
	@Override public final void dataUpdated() { updated = true; }

	/*=====================================================
	 * @see org.sea9.android.secret.data.DbHelper.Listener
	 */
	@Override
	public void onReady() {
		Log.d(TAG, "DbHelper.Listener.onReady");
		new AppInitTask().execute(this);
	}
	//=====================================================

	/*================================
	 * @see android.widget.Filterable
	 */
	private boolean filtered;

	@Override public boolean isFiltered() {
		return filtered;
	}

	@Override
	public void onFilterComplete(int count) {
		adaptor.notifyDataSetChanged();
	}

	public void applyFilter(String query) {
		getFilter().filter(query, this);
	}

	public void clearFilter() {
		if (isFiltered()) {
			NoteRecord r = null;
			int pos = adaptor.getSelectedPosition();
			if (pos >= 0) r = adaptor.getRecord(pos);

			filtered = false;
			adaptor.refresh();
			adaptor.notifyDataSetChanged();
			if (r != null) {
				callback.onFilterCleared(adaptor.selectRow(r.getKey()));
			}
		}
	}

	@Override @SuppressWarnings("unchecked")
	public Filter getFilter() {
		return new Filter() {
			@Override
			protected FilterResults performFiltering(CharSequence constraint) {
				FilterResults results = new FilterResults();
				String query = constraint.toString().trim().toLowerCase();
				if (query.length() <= 0) {
					results.values = EMPTY;
				} else {
					results.values = query;
				}
				return results;
			}

			@Override
			protected void publishResults(CharSequence constraint, FilterResults results) {
				adaptor.filterRecord((String) results.values);
				filtered = true;
			}
		};
	}
	//================================

	/*================================================
	 * @see org.sea9.android.secret.main.NotesAdaptor
	 */
	public void updateContent(String content) {
		if (content != null)
			callback.onRowSelectionMade(content);
		else
			callback.onRowSelectionCleared();
	}
	//================================================

	/*=========================================
	 * Callback interface to the main activity
	 */
	public interface Listener {
		void onRowSelectionMade(String content);
		void onRowSelectionCleared();
		void onFilterCleared(int position);
	}
	private Listener callback;

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		Log.d(TAG, "onAttach");
		try {
			callback = (Listener) context;
		} catch (ClassCastException e) {
			throw new ClassCastException(context.toString() + " missing implementation of ContextFragment.Listener");
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		Log.d(TAG, "onDetach");
		callback = null;
	}
	//=========================================

	static class DbInitTask extends AsyncTask<ContextFragment, Void, Void> {
		@Override
		protected Void doInBackground(ContextFragment... fragments) {
			if ((fragments.length > 0) && (fragments[0].getContext() != null)) {
				fragments[0].dbHelper = new DbHelper(fragments[0]);
				fragments[0].dbHelper.getWritableDatabase().execSQL(DbContract.SQL_CONFIG);
			}
			return null;
		}
	}

	static class AppInitTask extends AsyncTask<ContextFragment, Void, Void> {
		@Override
		protected Void doInBackground(ContextFragment... fragments) {
			if ((fragments.length > 0) && (fragments[0].getContext() != null)) {
				fragments[0].getAdaptor().refresh();
			}
			return null;
		}
	}

//	/*=========================================
//	 * Callback interface to the detail dialog
//	 */
//	public interface DetailListener {
//		void onTagAddCompleted(int position);
//	}
//	private DetailListener detailListener;
//	public final void setDetailListener(DetailListener listener) { detailListener = listener; }
//	//=========================================
}
