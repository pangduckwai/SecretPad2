package org.sea9.android.secret.core;

import android.app.Activity;
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

import org.sea9.android.secret.R;
import org.sea9.android.secret.compat.CompatCryptoUtils;
import org.sea9.android.secret.crypto.CryptoUtils;
import org.sea9.android.secret.data.DbContract;
import org.sea9.android.secret.data.DbHelper;
import org.sea9.android.secret.data.NoteRecord;
import org.sea9.android.secret.data.TagRecord;
import org.sea9.android.secret.details.TagsAdaptor;
import org.sea9.android.secret.io.FileChooserAdaptor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import javax.crypto.BadPaddingException;

public class ContextFragment extends Fragment implements
		DbHelper.Caller,
		NotesAdaptor.Caller,
		TagsAdaptor.Caller,
		FileChooserAdaptor.Caller,
		Filterable, Filter.FilterListener {
	public static final String TAG = "secret.ctx_frag";
	static final String EMPTY = "";
	private static final String PURAL = "s";

	private DbHelper dbHelper;
	public final boolean isDbReady() {
		return ((dbHelper != null) && dbHelper.getReady());
	}
	public final void initDb() {
		new DbInitTask().execute(this);
	}
	@Override public final DbHelper getDbHelper() {
		if (isDbReady())
			return dbHelper;
		else
			throw new RuntimeException("Database not ready");
	}

	private NotesAdaptor adaptor;
	public final NotesAdaptor getAdaptor() {
		return adaptor;
	}

	private TagsAdaptor tagsAdaptor;
	public final TagsAdaptor getTagsAdaptor() {
		return tagsAdaptor;
	}

	private FileChooserAdaptor fileAdaptor;
	public final FileChooserAdaptor getFileAdaptor() { return fileAdaptor; }

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate");
		setRetainInstance(true);

		adaptor = new NotesAdaptor(this);
		tagsAdaptor = new TagsAdaptor(this);
		fileAdaptor = new FileChooserAdaptor(this);

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
		if (dbHelper != null) {
			dbHelper.close();
			dbHelper = null;
		}
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

	public final boolean isDbEmpty() {
		int count = DbContract.Notes.Companion.count(dbHelper);
		if (count < 0) {
			throw new RuntimeException("Failed counting number of rows in the Notes table");
		} else
			return (count == 0);
	}

	private LogoffTask logoffTask;

	/**
	 * Initiate the logoff process.
	 */
	public final void logoff() {
		logoffTask = new LogoffTask();
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
		new AppInitTask().execute(this);
	}

	/**
	 * Called by the logoff async task after logged off.
	 */
	public void onLogoff() {
		Log.d(TAG, "onLogoff");
		callback.onLogoff();
	}
	//=============================

	/*===================================================
	 * @see org.sea9.android.secret.data.DbHelper.Caller
	 */
	@Override
	public void onReady() {
		Log.d(TAG, "DbHelper.Caller.onReady");
		if (!isLogon()) {
			Activity activity = getActivity();
			if (activity != null) activity.runOnUiThread(() -> callback.doLogon());
		} else {
			new AppInitTask().execute(this); // Keep it here just in case DB somehow closed, normally won't reach here
		}
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
	@Override
	public final char[] decrypt(@NonNull char[] input, @NonNull byte[] salt) {
		try {
			return CryptoUtils.decrypt(input, password, salt);
		} catch (BadPaddingException e) {
			Log.i(TAG, e.getMessage(), e);
			callback.doNotify(e.getMessage());
			return null;
		}
	}
	//===================================================

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
			callback.onRowSelectionChanged(content);
		else
			callback.onRowSelectionChanged(EMPTY);
	}
	//================================================

	/*===========================================================
	 * @see org.sea9.android.secret.io.FileChooserAdaptor.Caller
	 */
	@Override
	public void directorySelected(File selected) {
		Log.d(TAG, "Directory selected: " + selected.getName());
		callback.onDirectorySelected(selected);
	}

	@Override
	public void fileSelected(File selected) {
		Log.d(TAG, "File selected: " + selected.getName());
		callback.onFileSelected();
		(new ImportTask(this)).execute(selected);
	}

	private File tempFile;
	private char[] tempPassword;
	public void importOldFormat(char[] value) {
		if (tempFile != null) {
			tempPassword = value;
			(new ImportOldFormatTask(this)).execute();
		}
	}
	//===========================================================

	/*=========================================
	 * Callback interface to the main activity
	 */
	public interface Callback {
		void doNotify(String message);
		void doLogon();
		void onLogoff();
		void onRowSelectionChanged(String content);
		void onFilterCleared(int position);
		void onDirectorySelected(File selected);
		void onFileSelected();
		void doCompatLogon();
	}
	private Callback callback;

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		Log.d(TAG, "onAttach");
		try {
			callback = (Callback) context;
		} catch (ClassCastException e) {
			throw new ClassCastException(context.toString() + " missing implementation of ContextFragment.Callback");
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

	/**
	 * Import notes from exports.
	 */
	static class ImportTask extends AsyncTask<File, Void, Integer> {
		private ContextFragment caller = null;
		private static final String TAB = "\t";
		private static final int OLD_FORMAT_COLUMN_COUNT = 6;

		ImportTask(ContextFragment ctx) {
			caller = ctx;
		}

		@Override
		protected Integer doInBackground(File... files) {
			if (files.length > 0) {
				String line;
				String row[];
				int count = 0;
				BufferedReader reader = null;
				try {
					reader = new BufferedReader(new FileReader(files[0]));
					while ((line = reader.readLine()) != null) {
						if (isCancelled()) break;

						row = line.split(TAB);
						if ((row.length == OLD_FORMAT_COLUMN_COUNT) && (count == 0)) {
							// Old Secret Pad format:
							// ID, salt, category, title*, content*, modified
							cancel(true);
							caller.tempFile = files[0];
							count = row.length;
							continue;
						} else {
							// TODO check column count!!!
							Log.w(TAG, row.length + " columns found"); //TODO TEMP
						}
						count ++;
					}
					return count;
				} catch (FileNotFoundException e) {
					Log.w(TAG, e);
					return -1;
				} catch (IOException e) {
					Log.w(TAG, e);
					return -2;
				} finally {
					if (reader != null) {
						try {
							reader.close();
						} catch (IOException e) {
							Log.d(TAG, e.getMessage());
						}
					}
				}
			}
			return -3;
		}

		@Override
		protected void onPostExecute(Integer integer) {
			Log.w(TAG, "ImportTask.onPostExecute " + integer); //TODO TEMP
		}

		@Override
		protected void onCancelled(Integer integer) {
			Log.w(TAG, "ImportTask.onCancelled " + integer); //TODO TEMP
			if (integer == OLD_FORMAT_COLUMN_COUNT) {
				caller.callback.doCompatLogon();
			}
		}
	}

	/**
	 * Import notes in old format.
	 */
	static class ImportOldFormatTask extends AsyncTask<Void, Void, Integer> {
		private ContextFragment caller = null;
		private static final String TAB = "\t";
		private static final int OLD_FORMAT_COLUMN_COUNT = 6;

		private StringBuilder errors;
		ImportOldFormatTask(ContextFragment ctx) {
			caller = ctx;
			errors = new StringBuilder(EMPTY);
		}

		@Override
		protected Integer doInBackground(Void... voids) {
			String line;
			String row[];
			int count = 0;
			BufferedReader reader = null;
			try {
				reader = new BufferedReader(new FileReader(caller.tempFile));
				while ((line = reader.readLine()) != null) {
					if (isCancelled()) break;

					row = line.split(TAB);
					if (row.length == OLD_FORMAT_COLUMN_COUNT) {
						// ID, salt, category, title*, content*, modified
						byte[] salt = CryptoUtils.decode(CryptoUtils.convert(row[1].toCharArray()));
						String title = new String(CompatCryptoUtils.decrypt(row[3].toCharArray(), caller.tempPassword, salt));
						String cntnt = new String(CompatCryptoUtils.decrypt(row[4].toCharArray(), caller.tempPassword, salt));

						long tid = -1;
						List<Long> tags = DbContract.Tags.Companion.search(caller.getDbHelper(), row[2]);
						if (tags.size() > 0)
							tid = tags.get(0);
						else {
							TagRecord tag = DbContract.Tags.Companion.insert(caller.getDbHelper(), row[2]);
							if (tag != null)
								tid = tag.getPid();
							else
								errors.append('\n').append("Insert tag ").append(row[2]).append(" failed (").append(count).append(")");
						}

						long nid = -1;
						NoteRecord note = DbContract.Notes.Companion.insert(caller.getDbHelper(), title, cntnt, Long.parseLong(row[5]));
						if (note != null)
							nid = note.getPid();
						else
							errors.append('\n').append("Insert note failed (").append(count).append(")");

						DbContract.NoteTags.Companion.insert(caller.getDbHelper(), nid, tid);
					} else {
						Log.w(TAG, "Invalid import format");
						cancel(true);
						continue;
					}
					count ++;
				}
				return count;
			} catch (FileNotFoundException e) {
				Log.w(TAG, e);
				return -1;
			} catch (IOException e) {
				Log.w(TAG, e);
				return -2;
			} catch (RuntimeException e) {
				Log.w(TAG, e);
				return -3;
			} finally {
				if (reader != null) {
					try {
						reader.close();
					} catch (IOException e) {
						Log.d(TAG, e.getMessage());
					}
				}
			}
		}

		@Override
		protected void onPostExecute(Integer integer) {
			if (integer >= 0) {
				if (errors.toString().trim().length() <= 0)
					caller.callback.doNotify(
							String.format(caller.getString(R.string.msg_migrate_okay), integer, caller.tempFile.getPath(), (integer > 1)?PURAL:EMPTY)
					);
				else {
					caller.callback.doNotify("Importing " + caller.tempFile.getPath() + errors.toString());
					Log.w(TAG, "Importing " + caller.tempFile.getPath() + errors.toString());
				}
				caller.getAdaptor().select();
				caller.getAdaptor().notifyDataSetChanged();
			} else {
				caller.callback.doNotify(
						String.format(caller.getString(R.string.msg_migrate_fail), caller.tempFile.getPath(), integer)
				);
			}
			cleanUp();
		}

		@Override
		protected void onCancelled(Integer integer) {
			caller.callback.doNotify(
					String.format(caller.getString(R.string.msg_migrate_invalid), caller.tempFile.getPath(), integer)
			);
			cleanUp();
		}

		private void cleanUp() {
			for (int i = 0; i < caller.tempPassword.length; i++)
				caller.tempPassword[i] = 0;
			caller.tempPassword = null;
			caller.tempFile = null;
		}
	}
}