package org.sea9.android.secret.core;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.SQLException;
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
import org.sea9.android.secret.R;
import org.sea9.android.secret.compat.CompatCryptoUtils;
import org.sea9.android.secret.compat.SmartConverter;
import org.sea9.android.secret.crypto.CryptoUtils;
import org.sea9.android.secret.data.DbContract;
import org.sea9.android.secret.data.DbHelper;
import org.sea9.android.secret.data.NoteRecord;
import org.sea9.android.secret.data.TagRecord;
import org.sea9.android.secret.details.TagsAdaptor;
import org.sea9.android.secret.io.FileChooserAdaptor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.crypto.BadPaddingException;

public class ContextFragment extends Fragment implements
		DbHelper.Caller, DbHelper.Crypto,
		NotesAdaptor.Caller,
		TagsAdaptor.Caller,
		FileChooserAdaptor.Caller,
		Filterable, Filter.FilterListener {
	public static final String TAG = "secret.ctx_frag";
	public static final String PATTERN_DATE = "yyyy-MM-dd HH:mm:ss";
	static final String NEWLINE = "\n";
	static final String PLURAL = "s";
	static final String EMPTY = "";

	long versionCode = -1;

	private DbHelper dbHelper;
	public final boolean isDbReady() {
		return ((dbHelper != null) && dbHelper.getReady());
	}
	@Override public final DbHelper getDbHelper() {
		if (isDbReady())
			return dbHelper;
		else {
			throw new RuntimeException("Database not ready");
		}
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
		filterQuery = null;
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

	/*=====================
	 * Handle app settings
	 */
	static final String SETTING_SORTBY = "setting.sortBy";
	static final int SETTING_SORTBY_KEY = 0;
	static final int SETTING_SORTBY_TAG = 1;

	private int sortBy = SETTING_SORTBY_TAG;
	public final int getSortBy() { return sortBy; }
	public final void setSortBy(int sort) { sortBy = sort; }

	public final void doSort(SharedPreferences pref, int sort) {
		long pid = -1;
		int pos = adaptor.getSelectedPosition();
		if (pos >= 0) {
			NoteRecord r = adaptor.getRecord(pos);
			if (r != null) pid = r.getPid();
		}
		SharedPreferences.Editor editor = pref.edit();
		editor.putInt(SETTING_SORTBY, sort);
		editor.apply();
		sortBy = sort;
		adaptor.sortCache();
		adaptor.notifyDataSetChanged();
		if (pid >= 0) {
			int position = adaptor.findSelectedPosition(pid);
			if (position >= 0) {
				adaptor.selectRow(position);
				callback.onScrollToPosition(position);
			}
		}
	}
	//=====================

	/*=============================
	 * Handle logon related stuffs
	 */
	private char[] password = null;
	final void setPassword(char[] pwd) { password = pwd; }

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

	/**
	 * Called when logging on.
	 * @param value Hash value of the password.
	 */
	public void onLogon(char[] value, boolean isNew) {
		password = value;
		if (!isNew)
			new AsyncDbReadTask(this).execute(-1L);
		else
			callback.setBusyState(false);
	}

	/**
	 * Called by the logoff async task after logged off.
	 */
	private void onLoggedOff() {
		Log.d(TAG, "onLoggedOff");
		callback.onLoggedOff();
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
			new AsyncDbReadTask(this).execute(-1L); // Keep it here just in case DB somehow closed, normally won't reach here
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
			throw new RuntimeException(e);
		}
	}
	//===================================================

	/*================================
	 * @see android.widget.Filterable
	 */
	private String filterQuery;
	public final String getFilterQuery() { return filterQuery; }

	@Override
	public boolean isFiltered() {
		return (filterQuery != null);
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
			long pid = -1;
			filterQuery = null;
			int pos = adaptor.getSelectedPosition();
			if (pos >= 0) {
				NoteRecord r = adaptor.getRecord(pos);
				if (r != null) pid = r.getPid();
			}

			adaptor.clearFilter();
			adaptor.notifyDataSetChanged();
			if (pid >= 0) {
				int position = adaptor.findSelectedPosition(pid);
				if (position >= 0) {
					adaptor.selectRow(position);
					callback.onScrollToPosition(position);
				}
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
				filterQuery = (String) results.values;
				adaptor.filterRecords();
			}
		};
	}
	//================================

	/*=======================================================
	 * @see org.sea9.android.secret.main.NotesAdaptor.Caller
	 */
	@Override
	public String getTag(long tid) {
		String ret = getTagsAdaptor().getTag(tid);
		if (ret == null)
			return EMPTY;
		else
			return ret;
	}
	//=======================================================

	/*===========================================================
	 * @see org.sea9.android.secret.io.FileChooserAdaptor.Caller
	 */
	@Override
	public void directorySelected(@NonNull File selected) {
		Log.d(TAG, "Directory selected: " + selected.getName());
		callback.onDirectorySelected(selected);
	}

	@Override
	public void fileSelected(@NonNull File selected) {
		Log.d(TAG, "File selected: " + selected.getName());
		callback.onFileSelected();
		(new AsyncImportTask(this)).execute(selected);
	}

	private File tempFile;
	final File getTempFile() { return tempFile; }
	final void setTempFile(File temp) { tempFile = temp; }
	private char[] tempPassword;
	final void cleanUp() {
		for (int i = 0; i < tempPassword.length; i++)
			tempPassword[i] = 0;
		tempPassword = null;
		tempFile = null;
	}

	public final void importOldFormat(char[] value, boolean smart) {
		if (tempFile != null) {
			tempPassword = value;
			SmartConverter converter = null;
			if (smart) {
				converter = SmartConverter.getInstance();
			}
			(new AsyncImportOldTask(this, new DbHelper.Crypto() {
					@Override
					public char[] decrypt(@NotNull char[] input, @NotNull byte[] salt) {
						try {
							return CompatCryptoUtils.decrypt(input, tempPassword, salt);
						} catch (RuntimeException e) {
							throw new RuntimeException(e);
						}
					}

					@Override @NotNull
					public char[] encrypt(@NotNull char[] input, @NotNull byte[] salt) {
						try {
							return CryptoUtils.encrypt(input, password, salt);
						} catch (BadPaddingException e) {
							throw new RuntimeException(e);
						}
					}
				}, converter)).execute();
		}
	}
	//===========================================================

	/*========================================
	 * Callback interface to the MainActivity
	 */
	public interface Callback {
		void doNotify(int reference, String message, boolean stay);
		void doNotify(String message, boolean stay);
		void setBusyState(boolean isBusy);
		void doLogon();
		void onLoggedOff();
		void onRowSelectionChanged(String content);
		void onScrollToPosition(int position);
		void onDirectorySelected(File selected);
		void onFileSelected();
		void doCompatLogon();
		void longPressed();
		void onTagAdded(int position);
		void onNoteSaved(boolean successful);
	}
	Callback callback; //TODO TEMP until move this to kotlin, then should be private
	public final Callback getCallback() { return callback; }

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		Log.d(TAG, "onAttach");
		try {
			callback = (Callback) context;
			versionCode = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
		} catch (ClassCastException e) {
			throw new ClassCastException(context.toString() + " missing implementation of ContextFragment.Callback");
		} catch (PackageManager.NameNotFoundException e) {
			versionCode = -2;
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
	private boolean busy = false;
	public final boolean isBusy() { return busy; }
	public final void setBusy(boolean flag) { busy = flag; }

	/**
	 * Async initialize DB since the first call to getXxxDatabase() can be slow
	 */
	public final void onInitDb() {
		new AsyncDbInitTask(this).execute();
	}

	/**
	 * Create a thread to initiate the DB. Cannot move to a separate class, because the point is to
	 * run the constructor of the db helper in this separate thread.
	 */
	static class AsyncDbInitTask extends AsyncTask<Void, Void, Void> {
		private ContextFragment caller;
		AsyncDbInitTask(ContextFragment ctx) {
			caller = ctx;
		}

		@Override
		protected void onPreExecute() {
			caller.callback.setBusyState(true);
		}

		@Override
		protected Void doInBackground(Void... voids) {
			caller.dbHelper = new DbHelper(caller, caller);
			caller.dbHelper.getWritableDatabase().execSQL(DbContract.SQL_CONFIG);
			return null;
		}
	}

	/**
	 * Move logoff to a separate thread to introduce delay and allow it to be interrupted
	 */
	private LogoffTask logoffTask;
	public final void onLogoff() {
		logoffTask = new LogoffTask(this);
		logoffTask.execute();
	}
	private void cancelLogoff() {
		if (logoffTask != null) logoffTask.cancel(true);
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
	static class LogoffTask extends AsyncTask<Void, Void, Void> {
		private ContextFragment caller;
		LogoffTask(ContextFragment ctx) {
			caller = ctx;
		}

		@Override
		protected Void doInBackground(Void... voids) {
			if (caller.isLogon()) {
				try {
					Thread.sleep(500);
					if (!isCancelled()) {
						caller.doLogoff();
					}
				} catch (InterruptedException e) {
					Log.i(TAG, e.getMessage());
				}
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void aVoid) {
			caller.onLoggedOff();
		}

		@Override
		protected void onCancelled(Void aVoid) {
			Log.d(TAG, "Logoff cancelled");
		}
	}

	/**
	 * Clean-up unused tags.
	 */
	public final void onCleanUp() {
		new CleanUpTask(this).execute();
	}
	static class CleanUpTask extends AsyncTask<Void, Void, Integer> {
		private ContextFragment caller;
		CleanUpTask(ContextFragment ctx) {
			caller = ctx;
		}

		@Override
		protected void onPreExecute() {
			caller.callback.setBusyState(true);
		}

		@Override
		protected Integer doInBackground(Void... voids) {
			try {
				return DbContract.Tags.Companion.delete(caller.getDbHelper());
			} catch (SQLException e) {
				return -1;
			}
		}

		@Override
		protected void onPostExecute(Integer result) {
			caller.callback.setBusyState(false);
			String msg;
			if ((result == null) || (result < 0)) {
				msg = caller.getString(R.string.msg_delete_tags_fail);
			} else {
				msg = String.format(Locale.getDefault(), caller.getString(R.string.msg_delete_tags_okay), Integer.toString(result));
			}
			caller.callback.doNotify(msg, false);
		}
	}

	/**
	 * Delete a note.
	 */
	public final void onDeleteNote(int position) {
		if ((position >= 0) && (position < getAdaptor().getItemCount())) {
			//if (isSelected(position)) {
			if (getAdaptor().isSelected(position)) {
				getAdaptor().clearSelection();
			}
			new DeleteNoteTask(this).execute(position);
		}
	}
	static class DeleteNoteTask extends AsyncTask<Integer, Void, int[]> {
		private ContextFragment caller;
		DeleteNoteTask(ContextFragment ctx) {
			caller = ctx;
		}

		@Override
		protected void onPreExecute() {
			caller.callback.setBusyState(true);
		}

		@Override
		protected int[] doInBackground(Integer... positions) {
			if (positions.length > 0) {
				int[] ret = { positions[0], -1 };
				ret[1] = caller.getAdaptor().delete(positions[0]);
				if (ret[1] >= 0) {
					caller.getAdaptor().populateCache();
				}
				return ret;
			}
			return null;
		}

		@Override
		protected void onPostExecute(int[] response) {
			caller.callback.setBusyState(false);
			String msg = caller.getString(R.string.msg_delete_error);
			boolean stay = true;
			if ((response != null) && (response.length == 2)) {
				if (response[1] < 0) {
					caller.getAdaptor().notifyDataSetChanged();
					msg = String.format(Locale.getDefault(), caller.getString(R.string.msg_delete_fail), Integer.toString(response[0] + 1));
				} else {
					caller.getAdaptor().notifyItemRemoved(response[0]);
					msg = String.format(Locale.getDefault(), caller.getString(R.string.msg_delete_okay), Integer.toString(response[0] + 1));
					stay = false;
				}
			}
			caller.callback.doNotify(msg, stay);
		}
	}

	/**
	 * Add a Tag.
	 */
	public final void onAddTag(String tag) {
		new AddTagTask(this).execute(tag);
	}
	static class AddTagTask extends AsyncTask<String, Void, Long> {
		private ContextFragment caller;
		AddTagTask(ContextFragment ctx) {
			caller = ctx;
		}

		@Override
		protected void onPreExecute() {
			caller.callback.setBusyState(true);
		}

		@Override
		protected Long doInBackground(String... inputs) {
			Long pid = -1L;
			if ((inputs.length > 0) && (inputs[0] != null)) {
				List<Long> tags = DbContract.Tags.Companion.search(caller.getDbHelper(), inputs[0]);
				if (tags.size() > 0) {
					pid = tags.get(0);
				} else {
					TagRecord tag = DbContract.Tags.Companion.insert(caller.getDbHelper(), inputs[0]);
					if (tag != null) {
						pid = tag.getPid();
					}
					caller.getTagsAdaptor().populateCache();
				}
			}
			return pid;
		}

		@Override
		protected void onPostExecute(Long pid) {
			caller.callback.setBusyState(false);
			if ((pid != null) && (pid >= 0)) {
				int position = caller.getTagsAdaptor().onInserted(pid);
				if (position >= 0) {
					caller.callback.onTagAdded(position);
				}
			}
		}
	}

	/**
	 * Save a note.
	 */
	public final void onSaveNote(boolean isNew, Long i, String k, String c, List<Long> t) {
		if (isNew)
			new InsertNoteTask(this).execute(new NoteRecord(i, k, c, t, null, -1));
		else
			new UpdateNoteTask(this).execute(new NoteRecord(i, k, c, t, null, -1));
	}
	static class InsertNoteTask extends AsyncTask<NoteRecord, Void, NoteRecord> {
		private ContextFragment caller;
		InsertNoteTask(ContextFragment ctx) {
			caller = ctx;
		}

		@Override
		protected void onPreExecute() {
			caller.callback.setBusyState(true);
		}

		@Override
		protected NoteRecord doInBackground(NoteRecord... records) {
			if ((records.length > 0) && (records[0] != null)) {
				String c = records[0].getContent();
				List<Long> t = records[0].getTags();
				if (t == null) t = new ArrayList<>();
				Long pid = DbContract.Notes.Companion.insert(caller.getDbHelper(), records[0].getKey(), (c == null) ? EMPTY : c, t);

				if (pid != null) {
					if (pid >= 0) caller.getAdaptor().populateCache(); // Refresh the cache
					return new NoteRecord(pid, records[0].getKey(), c, t, null, -1);
				}
			}
			return null;
		}

		@Override
		protected void onPostExecute(NoteRecord note) {
			caller.callback.setBusyState(false);
			String msg = caller.getString(R.string.msg_insert_fail);
			boolean stay = true;
			boolean successful = false;
			if (note != null) {
				long pid = note.getPid();
				if (pid < 0) {
					msg = caller.getString(R.string.msg_insert_duplicated);
					pid *= -1;
				} else {
					msg = String.format(Locale.getDefault(), caller.getString(R.string.msg_insert_okay), note.getKey());
					stay = false;
					successful = true;
				}

				int position = caller.getAdaptor().findSelectedPosition(pid);
				if (position >= 0) {
					caller.callback.onScrollToPosition(position);
					if (successful) {
						caller.getAdaptor().notifyDataSetChanged();
						caller.getAdaptor().selectRow(position);
					}
				} else {
					caller.getAdaptor().notifyDataSetChanged();
				}
			}
			caller.callback.doNotify(msg, stay);
			caller.callback.onNoteSaved(successful);
		}
	}
	static class UpdateNoteTask extends AsyncTask<NoteRecord, Void, NoteRecord> {
		private ContextFragment caller;
		UpdateNoteTask(ContextFragment ctx) {
			caller = ctx;
		}

		@Override
		protected void onPreExecute() {
			caller.callback.setBusyState(true);
		}

		@Override
		protected NoteRecord doInBackground(NoteRecord... records) {
			if ((records.length > 0) && (records[0] != null)) {
				Long pid = records[0].getPid();
				String c = records[0].getContent();
				List<Long> t = records[0].getTags();
				if (t == null) t = new ArrayList<>();
				int ret = DbContract.Notes.Companion.update(caller.getDbHelper(), pid, (c == null) ? EMPTY : c, t);

				if (ret >= 0) {
					caller.getAdaptor().populateCache(); // Refresh the cache
					return records[0];
				} else {
					return new NoteRecord(-1 * pid, records[0].getKey(), c, t, null, -1);
				}
			}
			return null;
		}

		@Override
		protected void onPostExecute(NoteRecord note) {
			caller.callback.setBusyState(false);
			String msg = caller.getString(R.string.msg_update_error);
			boolean stay = true;
			boolean successful = false;
			if (note != null) {
				long pid = note.getPid();
				if (pid < 0) {
					msg = String.format(Locale.getDefault(), caller.getString(R.string.msg_update_fail), note.getKey());
					pid *= -1;
				} else {
					msg = String.format(Locale.getDefault(), caller.getString(R.string.msg_update_okay), note.getKey());
					stay = false;
					successful = true;
				}

				int position = caller.getAdaptor().findSelectedPosition(pid);
				if (position >= 0) {
					caller.callback.onScrollToPosition(position);
					if (successful) {
						// Not using notifyItemChanged(position) because update may change sort order if sort by tags
						caller.getAdaptor().notifyDataSetChanged();
						caller.getAdaptor().selectRow(position);
					}
				} else {
					caller.getAdaptor().notifyDataSetChanged();
				}
			}
			caller.callback.doNotify(msg, stay);
			caller.callback.onNoteSaved(successful);
		}
	}

	/**
	 * Export notes in encrypted format.
	 */
	public final void onExport(File destination) {
		(new AsyncExportTask(this)).execute(destination);
	}

	/**
	 * Change password used to encrypt notes.
	 */
	public final void onChangePassword(char[] oldPassword, char[] newPassword) {
		(new AsyncPasswdTask(this)).execute(oldPassword, newPassword);
	}
}