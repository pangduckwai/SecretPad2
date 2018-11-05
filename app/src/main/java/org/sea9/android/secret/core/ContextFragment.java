package org.sea9.android.secret.core;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
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
import org.sea9.android.secret.details.TagsAdaptor;
import org.sea9.android.secret.io.FileChooserAdaptor;
import org.sea9.android.secret.ui.MessageDialog;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
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
	public static final String TAB = "\t";
	private static final String PLURAL = "s";
	static final String EMPTY = "";

	private long versionCode = -1;

	private DbHelper dbHelper;
	public final boolean isDbReady() {
		return ((dbHelper != null) && dbHelper.getReady());
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

	/**
	 * Called when logging on.
	 * @param value Hash value of the password.
	 */
	public void onLogon(char[] value, boolean isNew) {
		password = value;
		if (!isNew)
			new DbReadTask(this).execute(-1L);
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
			new DbReadTask(this).execute(-1L); // Keep it here just in case DB somehow closed, normally won't reach here
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
			String msg = String.format(getString(R.string.msg_logon_fail), e.getMessage());
			DialogFragment d = MessageDialog.Companion.getInstance(MainActivity.MSG_DIALOG_LOG_FAIL, msg, null);
			FragmentManager m = getFragmentManager();
			if (m != null)
				d.show(m, MessageDialog.TAG);
			else
				callback.doNotify(msg, true);
			return null;
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
			NoteRecord r = null;
			int pos = adaptor.getSelectedPosition();
			if (pos >= 0) r = adaptor.getRecord(pos);

			filterQuery = null;
			long pid = -1;
			if (r != null) pid = r.getPid();
			new DbReadTask(this).execute(pid);
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
				adaptor.filterRecords(filterQuery);
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

	public void longPressed() {
		callback.longPressed();
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
	public void importOldFormat(char[] value, boolean smart) {
		if (tempFile != null) {
			tempPassword = value;
			SmartConverter converter = null;
			if (smart) {
				converter = SmartConverter.getInstance();
			}
			(new ImportOldFormatTask(this, converter)).execute();
		}
	}
	//===========================================================

	/*========================================
	 * Callback interface to the MainActivity
	 */
	public interface Callback {
		void doNotify(String message, boolean stay);
		void setBusyState(boolean isBusy);
		void doLogon();
		void onLoggedOff();
		void onRowSelectionChanged(String content);
		void onFilterCleared(int position);
		void onDirectorySelected(File selected);
		void onFileSelected();
		void doCompatLogon();
		void longPressed();
	}
	private Callback callback;

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
		new DbInitTask(this).execute();
	}
	static class DbInitTask extends AsyncTask<Void, Void, Void> {
		private ContextFragment caller;
		DbInitTask(ContextFragment ctx) {
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
	 * Invoke a separate thread to read the database after DB init to avoid an IllegalStateException
	 * which complained 'getDatabase' is called recursively.
	 */
	static class DbReadTask extends AsyncTask<Long, Void, Long> {
		private ContextFragment caller;
		DbReadTask(ContextFragment ctx) {
			caller = ctx;
		}

		@Override
		protected void onPreExecute() {
			caller.callback.setBusyState(true);
		}

		/**
		 * Note: the long passing in, and then passing to post-execute, is the note id
		 * to be selected automatically after DB read. Give -1 to skip the auto select.
		 * @param positions input parameter pass in via AsyncTask.execute(...).
		 * @return the first element of the input parameter.
		 */
		@Override
		protected Long doInBackground(Long... positions) {
			caller.getAdaptor().select();
			if ((positions != null) && (positions.length > 0)) {
				return positions[0];
			} else {
				return -1L;
			}
		}

		@Override
		protected void onPostExecute(Long pid) {
			caller.getAdaptor().notifyDataSetChanged();
			if (pid >= 0) {
				caller.callback.onFilterCleared(caller.adaptor.selectRow(pid));
			}
			caller.callback.setBusyState(false);
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
			return caller.getTagsAdaptor().delete();
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
					caller.getAdaptor().select();
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

	private static final int OLD_FORMAT_COLUMN_COUNT = 6;
	/**
	 * Import notes from exports.
	 */
	static class ImportTask extends AsyncTask<File, Void, Response> {
		private ContextFragment caller;
		ImportTask(ContextFragment ctx) {
			caller = ctx;
		}

		@Override
		protected void onPreExecute() {
			caller.callback.setBusyState(true);
		}

		@Override
		protected Response doInBackground(File... files) {
			Response response = new Response();
			if (files.length > 0) {
				String line;
				String row[];
				int count = 0;
				int succd = 0;
				BufferedReader reader = null;
				try {
					reader = new BufferedReader(new FileReader(files[0]));
					while ((line = reader.readLine()) != null) {
						if (isCancelled()) {
							return response;
						}

						row = line.split(TAB);
						if (count == 0) { // First row in the import data file
							if (row.length == OLD_FORMAT_COLUMN_COUNT) {
								// Old Secret Pad format: ID, salt, category, title*, content*, modified
								// Need to exit the task, ask for old password, and run import again...
								Log.d(TAG, "Old file format");
								cancel(true);
								caller.tempFile = files[0];
								response.setStatus(row.length).setMessage("Old file format");
								continue;
							} else if (row.length != 3) { // First row has 3 columns: App name, version and export time
								Log.d(TAG, "Invalid file format");
								cancel(true);
								response.setStatus(-4).setErrors("Invalid file format");
								continue;
							}
						} else if (row.length >= DbContract.Notes.EXPORT_FORMAT_MIN_COLUMN) {
							long ret = DbContract.Notes.Companion.doImport(caller.getDbHelper(), new DbHelper.Crypto() {
								@Override
								public char[] decrypt(@NotNull char[] input, @NotNull byte[] salt) {
									try {
										return CryptoUtils.decrypt(input, caller.password, salt);
									} catch (BadPaddingException e) {
										Log.i(TAG, e.getMessage(), e);
										caller.callback.doNotify(String.format(caller.getString(R.string.msg_logon_fail), e.getMessage()), true);
										return null;
									}
								}

								@Override @NotNull
								public char[] encrypt(@NotNull char[] input, @NotNull byte[] salt) {
									try {
										return CryptoUtils.encrypt(input, caller.password, salt);
									} catch (BadPaddingException e) {
										throw new RuntimeException(e);
									}
								}
							}, row);
							if (ret >= 0)
								succd ++;
							else if (ret < -1) {
								return response.setStatus(-4);
							}
						} else {
							response.setErrors("Invalid file format at row " + count);
						}
						count ++;
					}
					return response.setStatus(succd).setMessage(files[0].getPath());
				} catch (FileNotFoundException e) {
					Log.w(TAG, e);
					return response.setStatus(-3).setErrors(e.getMessage());
				} catch (IOException e) {
					Log.w(TAG, e);
					return response.setStatus(-2).setErrors(e.getMessage());
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
			return response;
		}

		@Override
		protected void onPostExecute(Response response) {
			if (response.getStatus() >= 0) {
				if ((response.getErrors() == null) || (response.getErrors().trim().length() <= 0))
					caller.callback.doNotify(
							String.format(caller.getString(R.string.msg_import_okay), response.getStatus(), response.getMessage(), (response.getStatus() > 1)? PLURAL :EMPTY),
							false
					);
				else {
					String rspn = "Importing " + response.getMessage() + '\n' + response.getErrors();
					caller.callback.doNotify(rspn, true);
					Log.w(TAG, rspn);
				}
				caller.getAdaptor().select();
				caller.getAdaptor().notifyDataSetChanged();
			} else if (response.getStatus() != -4) { // -4 already handled
				caller.callback.doNotify(
						String.format(caller.getString(R.string.msg_import_fail), response.getMessage(), response.getStatus()),
						true
				);
			}
			caller.callback.setBusyState(false);
		}

		@Override
		protected void onCancelled(Response response) {
			if (response.getStatus() == OLD_FORMAT_COLUMN_COUNT) {
				caller.callback.doCompatLogon();
			} else {
				caller.callback.doNotify(response.getErrors(), true);
			}
			caller.callback.setBusyState(false);
		}
	}

	/**
	 * Import notes in old format.
	 */
	static class ImportOldFormatTask extends AsyncTask<Void, Void, Response> {
		private ContextFragment caller;
		private SmartConverter converter;
		ImportOldFormatTask(ContextFragment ctx, SmartConverter cvr) {
			caller = ctx;
			converter = cvr;
		}

		@Override
		protected void onPreExecute() {
			caller.callback.setBusyState(true);
		}

		@Override
		protected Response doInBackground(Void... voids) {
			Response response = new Response();
			String line;
			String old[];
			int count = 0;
			int succd = 0;
			BufferedReader reader = null;
			try {
				reader = new BufferedReader(new FileReader(caller.tempFile));
				while ((line = reader.readLine()) != null) {
					old = line.split(TAB);
					if (old.length == OLD_FORMAT_COLUMN_COUNT) {
						// Old format: ID, salt, category, title*, content*, modified
						int ret = DbContract.Notes.Companion.doOldImport(caller.getDbHelper(), new DbHelper.Crypto() {
							@Override
							public char[] decrypt(@NotNull char[] input, @NotNull byte[] salt) {
								try {
									return CompatCryptoUtils.decrypt(input, caller.tempPassword, salt);
								} catch (RuntimeException e) {
									Log.i(TAG, e.getMessage(), e);
									caller.callback.doNotify(String.format(caller.getString(R.string.msg_logon_fail), e.getMessage()), true);
									return null;
								}
							}

							@Override @NotNull
							public char[] encrypt(@NotNull char[] input, @NotNull byte[] salt) {
								try {
									return CryptoUtils.encrypt(input, caller.password, salt);
								} catch (BadPaddingException e) {
									throw new RuntimeException(e);
								}
							}
						}, old, converter);
						if (ret >= 0)
							succd ++;
						else if (ret < -1) {
							return response.setStatus(-4);
						}
					} else {
						response.setErrors("Invalid file format at row " + count);
					}
					count ++;
				}
				return response.setStatus(succd);
			} catch (FileNotFoundException e) {
				Log.w(TAG, e);
				return response.setStatus(-3).setErrors(e.getMessage());
			} catch (IOException e) {
				Log.w(TAG, e);
				return response.setStatus(-2).setErrors(e.getMessage());
			} catch (RuntimeException e) {
				Log.w(TAG, e);
				return response.setStatus(-1).setErrors(e.getMessage());
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
		protected void onPostExecute(Response response) {
			if (response.getStatus() >= 0) {
				if ((response.getErrors() == null) || (response.getErrors().trim().length() <= 0))
					caller.callback.doNotify(
							String.format(caller.getString(R.string.msg_migrate_okay), response.getStatus(), caller.tempFile.getPath(), (response.getStatus() > 1)? PLURAL :EMPTY),
							false
					);
				else {
					String rspn = "Importing " + caller.tempFile.getPath() + '\n' + response.getErrors();
					caller.callback.doNotify(rspn, true);
					Log.w(TAG, rspn);
				}
				caller.getAdaptor().select();
				caller.getAdaptor().notifyDataSetChanged();
			} else if (response.getStatus() != -4) { //-4 already handled
				caller.callback.doNotify(
						String.format(caller.getString(R.string.msg_migrate_fail), caller.tempFile.getPath(), response.getStatus()),
						true
				);
			}
			cleanUp();
		}

		private void cleanUp() {
			for (int i = 0; i < caller.tempPassword.length; i++)
				caller.tempPassword[i] = 0;
			caller.tempPassword = null;
			caller.tempFile = null;
			caller.callback.setBusyState(false);
		}
	}

	/**
	 * Export notes in encrypted format.
	 */
	public final void onExport(File destination) {
		(new ExportTask(this)).execute(destination);
	}
	static class ExportTask extends AsyncTask<File, Void, Response> {
		private static final String PATTERN_TIMESTAMP = "yyyyMMddHHmm";
		private static final String EXPORT_SUFFIX = ".txt";
		private ContextFragment caller;
		private String exportFileName;

		ExportTask(ContextFragment ctx) {
			caller = ctx;
			SimpleDateFormat formatter = new SimpleDateFormat(PATTERN_TIMESTAMP, Locale.getDefault());
			Context context = caller.getContext();
			if (context != null) {
				exportFileName = context.getString(R.string.value_export) + formatter.format(new Date()) + EXPORT_SUFFIX;
			}
		}

		@Override
		protected void onPreExecute() {
			caller.callback.setBusyState(true);
		}

		@Override
		protected Response doInBackground(File... files) {
			Response response = new Response();
			if ((files.length > 0) && (files[0].isDirectory())) {
				File export = new File(files[0], exportFileName);
				if (export.exists()) {
					Log.w(TAG, "File " + export.getPath() + " already exists");
					return response.setStatus(-3);
				}

				PrintWriter writer = null;
				try {
					writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(export)));
					writer.println(caller.getString(R.string.app_name) + TAB + caller.versionCode + TAB + (new Date()).getTime()); //Header
					return response.setStatus(DbContract.Notes.Companion.doExport(caller.dbHelper, writer));
				} catch (FileNotFoundException e) {
					Log.w(TAG, e);
					return response.setStatus(-2);
				} finally {
					if (writer != null) {
						writer.flush();
						writer.close();
					}
				}
			}
			return response;
		}

		@Override
		protected void onPostExecute(Response response) {
			if (response.getStatus() < 0) {
				caller.callback.doNotify(String.format(caller.getString(R.string.msg_export_error), response.getStatus()), true);
			} else {
				caller.callback.doNotify(
						String.format(caller.getString(R.string.msg_export_okay), response.getStatus(), exportFileName, (response.getStatus() > 1)? PLURAL :EMPTY),
						false
				);
			}
			caller.callback.setBusyState(false);
		}
	}

	/**
	 * Change password used to encrypt notes.
	 */
	public final void onChangePassword(char[] oldPassword, char[] newPassword) {
		(new ChangePasswordTask(this)).execute(oldPassword, newPassword);
	}
	static class ChangePasswordTask extends AsyncTask<char[], Void, char[]> {
		private ContextFragment caller;
		ChangePasswordTask(ContextFragment ctx) {
			caller = ctx;
		}

		@Override
		protected void onPreExecute() {
			caller.callback.setBusyState(true);
		}

		@Override
		protected char[] doInBackground(char[]... passwords) {
			if (passwords.length == 2) {
				int result = DbContract.Notes.Companion.passwd(caller.dbHelper, new DbHelper.Crypto() {
					@Override
					public char[] decrypt(@NotNull char[] input, @NotNull byte[] salt) {
						try {
							return CryptoUtils.decrypt(input, passwords[0], salt);
						} catch (BadPaddingException e) {
							Log.i(TAG, e.getMessage(), e);
							caller.callback.doNotify(String.format(caller.getString(R.string.msg_logon_fail), e.getMessage()), true);
							return null;
						}
					}

					@Override @NotNull
					public char[] encrypt(@NotNull char[] input, @NotNull byte[] salt) {
						try {
							return CryptoUtils.encrypt(input, passwords[1], salt);
						} catch (BadPaddingException e) {
							throw new RuntimeException(e);
						}
					}
				});

				for (int i = 0; i < passwords[0].length; i++)
					passwords[0][i] = 0;
				passwords[0] = null;

				if (result == 0) {
					return passwords[1];
				} else {
					for (int i = 0; i < passwords[1].length; i++)
						passwords[1][i] = 0;
					passwords[1] = null;
				}
			}
			return null;
		}

		@Override
		protected void onPostExecute(char[] result) {
			if (result != null) {
				caller.password = result;
				caller.callback.doNotify(caller.getString(R.string.msg_passwd_changed), false);
			} else {
				caller.callback.doNotify(caller.getString(R.string.msg_passwd_change_failed), false);
			}
			caller.callback.setBusyState(false);
		}
	}

	static class Response {
		private int status;
		private String message;
		private String errors;

		Response() {
			status = -1;
			message = null;
			errors = null;
		}

		final int getStatus() { return status; }
		final Response setStatus(int status) {
			this.status = status;
			return this;
		}

		final String getMessage() { return message; }
		final Response setMessage(String message) {
			this.message = message;
			return this;
		}

		final String getErrors() { return errors; }
		final Response setErrors(String error) {
			if (errors == null) {
				errors = error;
			} else {
				errors += '\n' + error;
			}
			return this;
		}
	}
}