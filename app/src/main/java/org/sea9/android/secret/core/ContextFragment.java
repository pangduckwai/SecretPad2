package org.sea9.android.secret.core;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
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
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
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
	public static final String TAB = "\t";
	private static final String PLURAL = "s";
	static final String EMPTY = "";

	private long versionCode = -1;

	private DbHelper dbHelper;
	public final boolean isDbReady() {
		return ((dbHelper != null) && dbHelper.getReady());
	}
	public final void initDb() {
		new DbInitTask(this).execute();
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
		logoffTask = new LogoffTask(this);
		logoffTask.execute();
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
		new AppInitTask(this).execute();
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
			new AppInitTask(this).execute(); // Keep it here just in case DB somehow closed, normally won't reach here
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

	public final void onExport(File destination) {
		(new ExportTask(this)).execute(destination);
	}

	public final void onChangePassword(char[] oldPassword, char[] newPassword) {
		(new ChangePasswordTask(this)).execute(oldPassword, newPassword);
	}

	private boolean busy = false;
	public final boolean isBusy() { return busy; }
	public final void setBusy(boolean flag) { busy = flag; }

	/*========================================
	 * Callback interface to the MainActivity
	 */
	public interface Callback {
		void doNotify(String message);
		void setBusyState(boolean isBusy);
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
	/**
	 * Async initialize DB since the first call to getXxxDatabase() can be slow
	 */
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
	static class AppInitTask extends AsyncTask<Void, Void, Void> {
		private ContextFragment caller;
		AppInitTask(ContextFragment ctx) {
			caller = ctx;
		}

		@Override
		protected void onPreExecute() {
			caller.callback.setBusyState(true);
		}

		@Override
		protected Void doInBackground(Void... voids) {
			caller.getAdaptor().select();
			return null;
		}

		@Override
		protected void onPostExecute(Void nothing) {
			caller.getAdaptor().notifyDataSetChanged();
			caller.callback.setBusyState(false);
		}
	}

	/**
	 * Move logoff to a separate thread to introduce delay and allow it to be interrupted
	 */
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
			caller.onLogoff();
		}

		@Override
		protected void onCancelled(Void aVoid) {
			Log.d(TAG, "Logoff cancelled");
		}
	}

	/**
	 * Import notes from exports.
	 */
	static class ImportTask extends AsyncTask<File, Void, AsyncTaskResponse> {
		private static final int OLD_FORMAT_COLUMN_COUNT = 6;

		private ContextFragment caller;
		ImportTask(ContextFragment ctx) {
			caller = ctx;
		}

		@Override
		protected void onPreExecute() {
			caller.callback.setBusyState(true);
		}

		@Override
		protected AsyncTaskResponse doInBackground(File... files) {
			AsyncTaskResponse response = new AsyncTaskResponse();
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
						if ((row.length == OLD_FORMAT_COLUMN_COUNT) && (count == 0)) {
							Log.d(TAG, "Old file format");
							// Old Secret Pad format: ID, salt, category, title*, content*, modified
							cancel(true);
							caller.tempFile = files[0];
							response.setStatus(row.length).setMessage("Old file format");
							continue;
						} else {
							switch (row.length) {
								case 2:
									List<Long> tags = DbContract.Tags.Companion.search(caller.getDbHelper(), row[1]);
									if (tags.size() <= 0) {
										if (DbContract.Tags.Companion.insert(caller.getDbHelper(), row[1]) == null)
											response.setErrors("Insert tag " + row[1] + " failed (" + count + ")");
									}
									break;
								case 5:
									char[] ckey = caller.getDbHelper().getCrypto().decrypt(row[1].toCharArray(), CryptoUtils.decode(CryptoUtils.convert(row[0].toCharArray())));
									char[] cctn = caller.getDbHelper().getCrypto().decrypt(row[3].toCharArray(), CryptoUtils.decode(CryptoUtils.convert(row[2].toCharArray())));
									if ((ckey != null) && (cctn != null)) {
										if (DbContract.Notes.Companion.insert(caller.getDbHelper(), new String(ckey), new String(cctn), Long.parseLong(row[4])) != null)
											succd ++;
										else
											response.setErrors("Insert note failed (" + count + ")");
									}
									break;
								case 3:
									Long nid = Long.parseLong(row[1]);
									Long tid = Long.parseLong(row[2]);
									if (DbContract.NoteTags.Companion.insert(caller.getDbHelper(), nid, tid) < 0)
										response.setErrors("Insert note_tag failed (" + count + ")");
									break;
								default:
									break;
							}
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
		protected void onPostExecute(AsyncTaskResponse response) {
			if (response.getStatus() >= 0) {
				if ((response.getErrors() == null) || (response.getErrors().trim().length() <= 0))
					caller.callback.doNotify(
							String.format(caller.getString(R.string.msg_import_okay), response.getStatus(), response.getMessage(), (response.getStatus() > 1)? PLURAL :EMPTY)
					);
				else {
					String rspn = "Importing " + response.getMessage() + '\n' + response.getErrors();
					caller.callback.doNotify(rspn);
					Log.w(TAG, rspn);
				}
				caller.getAdaptor().select();
				caller.getAdaptor().notifyDataSetChanged();
			} else {
				caller.callback.doNotify(
						String.format(caller.getString(R.string.msg_import_fail), response.getMessage(), response.getStatus())
				);
			}
			caller.callback.setBusyState(false);
		}

		@Override
		protected void onCancelled(AsyncTaskResponse response) {
			if (response.getStatus() == OLD_FORMAT_COLUMN_COUNT) {
				caller.callback.doCompatLogon();
			} else {
				caller.callback.doNotify(response.getErrors());
				caller.callback.setBusyState(false);
			}
		}
	}

	/**
	 * Import notes in old format.
	 */
	static class ImportOldFormatTask extends AsyncTask<Void, Void, AsyncTaskResponse> {
		private static final String TAB = "\t";
		private static final int OLD_FORMAT_COLUMN_COUNT = 6;

		private ContextFragment caller;
		ImportOldFormatTask(ContextFragment ctx) {
			caller = ctx;
		}

		@Override
		protected AsyncTaskResponse doInBackground(Void... voids) {
			AsyncTaskResponse response = new AsyncTaskResponse();
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
								response.setErrors("Insert tag " + row[2] + " failed (" + count + ")");
						}

						long nid = -1;
						NoteRecord note = DbContract.Notes.Companion.insert(caller.getDbHelper(), title, cntnt, Long.parseLong(row[5]));
						if (note != null)
							nid = note.getPid();
						else
							response.setErrors("Insert note failed (" + count + ")");

						if (DbContract.NoteTags.Companion.insert(caller.getDbHelper(), nid, tid) < 0)
							response.setErrors("Insert note_tag failed (" + count + ")");
					} else {
						Log.w(TAG, "Invalid import format");
						cancel(true);
						continue;
					}
					count ++;
				}
				return response.setStatus(count);
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
		protected void onPostExecute(AsyncTaskResponse response) {
			if (response.getStatus() >= 0) {
				if ((response.getErrors() == null) || (response.getErrors().trim().length() <= 0))
					caller.callback.doNotify(
							String.format(caller.getString(R.string.msg_migrate_okay), response.getStatus(), caller.tempFile.getPath(), (response.getStatus() > 1)? PLURAL :EMPTY)
					);
				else {
					String rspn = "Importing " + caller.tempFile.getPath() + '\n' + response.getErrors();
					caller.callback.doNotify(rspn);
					Log.w(TAG, rspn);
				}
				caller.getAdaptor().select();
				caller.getAdaptor().notifyDataSetChanged();
			} else {
				caller.callback.doNotify(
						String.format(caller.getString(R.string.msg_migrate_fail), caller.tempFile.getPath(), response.getStatus())
				);
			}
			cleanUp();
		}

		@Override
		protected void onCancelled(AsyncTaskResponse response) {
			caller.callback.doNotify(
					String.format(caller.getString(R.string.msg_migrate_invalid), caller.tempFile.getPath(), response.getStatus())
			);
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
	static class ExportTask extends AsyncTask<File, Void, AsyncTaskResponse> {
		private static final String PATTERN_TIMESTAMP = "yyyyMMddHHmmss";
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
		protected AsyncTaskResponse doInBackground(File... files) {
			AsyncTaskResponse response = new AsyncTaskResponse();
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
					return response.setStatus(DbContract.Notes.Companion.export(caller.dbHelper, writer));
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
		protected void onPostExecute(AsyncTaskResponse response) {
			if (response.getStatus() < 0) {
				caller.callback.doNotify(String.format(caller.getString(R.string.msg_export_error), response.getStatus()));
			} else {
				caller.callback.doNotify(
						String.format(caller.getString(R.string.msg_export_okay), response.getStatus(), exportFileName, (response.getStatus() > 1)? PLURAL :EMPTY)
				);
			}
			caller.callback.setBusyState(false);
		}
	}

	/**
	 * Change password used to encrypt notes.
	 */
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
							caller.callback.doNotify(e.getMessage());
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
				caller.callback.doNotify(caller.getString(R.string.msg_passwd_changed));
			} else {
				caller.callback.doNotify(caller.getString(R.string.msg_passwd_change_failed));
			}
			caller.callback.setBusyState(false);
		}
	}
}