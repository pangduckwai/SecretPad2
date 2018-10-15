package org.sea9.android.secret.core;

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
		DbHelper tempHelper = new DbHelper(new DbHelper.Listener() {
			@Override
			public void onReady() {
				Log.w(TAG, "DB Test finished");
			}
			@org.jetbrains.annotations.Nullable
			@Override
			public Context getContext() {
				return ContextFragment.this.getContext();
			}
		});
		tempHelper.getWritableDatabase().execSQL(DbContract.SQL_CONFIG);
		(new org.sea9.android.secret.data.DbTest()).run(tempHelper);
		//TODO TEMP <<<<<<<<<<<<
//		new DbInitTask().execute(this);
	}

	@Override
	public void onResume() {
		super.onResume();
		callback.onInit();
	}

	@Override
	public void onDestroy() {
		Log.d(TAG, "onDestroy");
		//TODO TEMP >>>>>>>>>>>>
		Context context = getContext();
		if ((context != null) && (dbHelper != null))
			org.sea9.android.secret.data.DbTest.cleanup(context, dbHelper);
		//TODO TEMP <<<<<<<<<<<<
		if (dbHelper != null) dbHelper.close();
		super.onDestroy();
	}

	private boolean updated;
	public final boolean isUpdated() { return updated; }
	public final void clearUpdated() {  updated = false; }
	@Override public final void dataUpdated() { updated = true; }

	/**
	 * Called after logon.
	 * @param value Hash value of the password.
	 */
	public void onLogon(char[] value) {
		new ContextFragment.DbInitTask().execute(this);
	}

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
		void onInit();
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

	/*====================================
	 * Worker running on separate threads
	 */

	/**
	 * //Async initialize DB since the first call to getXxxDatabase() can be slow
	 */
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

	static class AppInitTask extends AsyncTask<ContextFragment, Void, ContextFragment> {
		@Override
		protected ContextFragment doInBackground(ContextFragment... fragments) {
			if ((fragments.length > 0) && (fragments[0].getContext() != null)) {
				fragments[0].getAdaptor().refresh();
			}
			return fragments[0];
		}

		@Override
		protected void onPostExecute(ContextFragment ctx) {
			ctx.getAdaptor().notifyDataSetChanged();
		}
	}
}
