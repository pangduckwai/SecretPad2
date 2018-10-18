package org.sea9.android.secret.core;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.Filter;
import android.widget.Filterable;

import org.jetbrains.annotations.NotNull;
import org.sea9.android.secret.crypto.CryptoUtils;
import org.sea9.android.secret.data.DbContract;
import org.sea9.android.secret.data.DbHelper;
import org.sea9.android.secret.data.NoteRecord;
import org.sea9.android.secret.details.TagsAdaptor;

import javax.crypto.BadPaddingException;

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
	}

	@Override
	public void onResume() {
		super.onResume();
		Log.d(TAG, "onResume");
		cancelLogoff();
	}

	@Override
	public void onDestroy() {
		Log.d(TAG, "onDestroy");
		if (dbHelper != null) dbHelper.close();
		cancelLogoff();
		super.onDestroy();
	}

	/*=========================================
	 * Handle unsaved updates in detail dialog
	 */
	private boolean updated;
	public final boolean isUpdated() { return updated; }
	public final void clearUpdated() {  updated = false; }
	@Override public final void dataUpdated() { updated = true; }
	//=========================================

	/*=============================
	 * Handle logon related stuffs
	 */
	private char[] password = null;

	public final boolean isLogon() {
		return (password != null);
	}

	private LogoffTask logoffTask;

	/**
	 * Initiate the logoff process.
	 */
	public final void logoff() {
		logoffTask = new ContextFragment.LogoffTask();
		logoffTask.execute(this);
	}

	/*
	 * Called by the logoff async task to do the actual logoff steps.
	 */
	private void doLogoff() {
		for (int i = 0; i < password.length; i++)
			password[i] = 0;
		password = null;

		adaptor.clearRecords();

		FragmentActivity activity = getActivity();
		if (activity != null) {
			activity.runOnUiThread(() -> adaptor.clearSelection());
		}
	}

	private void cancelLogoff() {
		if (logoffTask != null) logoffTask.cancel(true);
	}

	/**
	 * Called after logon.
	 * @param value Hash value of the password.
	 */
	public void onLogon(char[] value) {
		password = value;
		new ContextFragment.DbInitTask().execute(this);
	}

	/**
	 * Called by the logoff async task after logged off.
	 */
	public void onLogoff() {
		Log.d(TAG, "onLogoff");
		callback.onLogoff();
	}
	//=============================

	/*=====================================================
	 * @see org.sea9.android.secret.data.DbHelper.Listener
	 */
	@Override
	public void onReady() {
		Log.d(TAG, "DbHelper.Listener.onReady");
		new AppInitTask().execute(this);
	}

	/**
	 * Put the method here because the password is held here.
	 * @param input data to be encrypted.
	 * @param salt salt used for this encryption.
	 * @return the encrypted data.
	 */
	@Override @NonNull
	public final char[] encrypt(@NonNull char[] input, @NonNull byte[] salt) {
		try {
			return CryptoUtils.encrypt(input, password, salt);
		} catch (BadPaddingException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Put the method here because the password is held here.
	 * @param input data to be decrypted.
	 * @param salt salt use for this decryption.
	 * @return the decrypted data.
	 */
	@Override @NonNull
	public final char[] decrypt(@NonNull char[] input, @NonNull byte[] salt) {
		try {
			return CryptoUtils.decrypt(input, password, salt);
		} catch (BadPaddingException e) {
			throw new RuntimeException(e);
		}
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
			adaptor.select();
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
				adaptor.filterRecords((String) results.values);
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
		void onLogoff();
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
	 * Async initialize DB since the first call to getXxxDatabase() can be slow
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

	/**
	 * Invoke a separate thread to read the database after DB init to avoid an IllegalStateException
	 * which complained 'getDatabase' is called recursively.
	 */
	static class AppInitTask extends AsyncTask<ContextFragment, Void, ContextFragment> {
		@Override
		protected ContextFragment doInBackground(ContextFragment... fragments) {
			if ((fragments.length > 0) && (fragments[0].getContext() != null)) {
				fragments[0].getAdaptor().select();
			}
			return fragments[0];
		}

		@Override
		protected void onPostExecute(ContextFragment ctx) {
			ctx.getAdaptor().notifyDataSetChanged();
		}
	}

	/**
	 * Move logoff to a separate thread to introduce delay and allow it to be interrupted
	 */
	static class LogoffTask extends AsyncTask<ContextFragment, Void, ContextFragment> {
		@Override
		protected ContextFragment doInBackground(ContextFragment... fragments) {
			if ((fragments.length > 0) && (fragments[0].getContext() != null)) {
				if (fragments[0].isLogon()) {
					try {
						Thread.sleep(500);
						if (!isCancelled()) {
							fragments[0].doLogoff();
						}
					} catch (InterruptedException e) {
						Log.i(TAG, e.getMessage());
					}
				}
			}
			return fragments[0];
		}

		@Override
		protected void onPostExecute(ContextFragment fragment) {
			fragment.onLogoff();
		}

		@Override
		protected void onCancelled() {
			Log.d(TAG, "Logoff cancelled");
		}
	}
}
