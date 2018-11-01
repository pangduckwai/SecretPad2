package org.sea9.android.secret.core;

import android.Manifest;
import android.animation.LayoutTransition;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.TextView;

import org.sea9.android.secret.compat.CompatLogonDialog;
import org.sea9.android.secret.data.NoteRecord;
import org.sea9.android.secret.details.DetailFragment;
import org.sea9.android.secret.R;
import org.sea9.android.secret.details.TagsAdaptor;
import org.sea9.android.secret.io.FileChooser;
import org.sea9.android.secret.io.FileChooserAdaptor;
import org.sea9.android.secret.ui.AboutDialog;
import org.sea9.android.secret.ui.LogonDialog;
import org.sea9.android.secret.ui.LogonDialog2;
import org.sea9.android.secret.ui.MessageDialog;
import org.sea9.android.secret.ui.PasswdDialog;

import java.io.File;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements
		ContextFragment.Callback,
		LogonDialog.Callback,
		LogonDialog2.Callback,
		CompatLogonDialog.Callback,
		PasswdDialog.Callback,
		FileChooser.Callback,
		DetailFragment.Callback,
		MessageDialog.Callback {
	public static final String TAG = "secret.main";
	private static final int READ_EXTERNAL_STORAGE_REQUEST = 17523;

	private View mainView;
	private ProgressBar progress;
	private FloatingActionButton fab;
	private ContextFragment ctxFrag;
	private RecyclerView recycler;
	private TextView content;
	private SearchView searchView;

	private int lastPos;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate");

		setContentView(R.layout.app_main);
		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		ctxFrag = (ContextFragment) getSupportFragmentManager().findFragmentByTag(ContextFragment.TAG);
		if (ctxFrag == null) {
			ctxFrag = new ContextFragment();
			getSupportFragmentManager().beginTransaction().add(ctxFrag, ContextFragment.TAG).commit();
		}

		mainView = findViewById(R.id.main_view);

		progress = findViewById(R.id.progressbar);

		fab = findViewById(R.id.fab);
		fab.setOnClickListener(view -> {
			if (!ctxFrag.isLogon()) {
				doNotify(getString(R.string.msg_not_logon), false);
			} else if (ctxFrag.isFiltered()) {
				doNotify(getString(R.string.msg_filter_active), false);
			} else if (ctxFrag.isBusy()) {
				doNotify(getString(R.string.msg_system_busy), false);
			} else {
				int position = ctxFrag.getAdaptor().getSelectedPosition();
				if (position >= 0) {
					recycler.smoothScrollToPosition(position); // Scroll list to the selected row
					NoteRecord record = ctxFrag.getAdaptor().getRecord(position);
					if (record != null) {
						String text = content.getText().toString();
						ctxFrag.getTagsAdaptor().selectTags(record.getTags());
						ctxFrag.clearUpdated();
						DetailFragment.getInstance(false, record, text).show(getSupportFragmentManager(), DetailFragment.TAG);
					}
				} else {
					ctxFrag.getAdaptor().clearSelection();
					ctxFrag.getAdaptor().notifyDataSetChanged();
					ctxFrag.getTagsAdaptor().selectTags(null);
					ctxFrag.clearUpdated();
					DetailFragment.getInstance(true, null, null).show(getSupportFragmentManager(), DetailFragment.TAG);
				}
			}
		});

		recycler = findViewById(R.id.recycler_list);
		content = findViewById(R.id.item_content);

		recycler.setHasFixedSize(true); // improve performance since content changes do not affect layout size of the RecyclerView
		recycler.setLayoutManager(new LinearLayoutManager(this)); // use a linear layout manager

		lastPos = -1; //Put it here instead of the ContextFragment deliberately so last selected position will reset after rotate
		content.setOnClickListener(view -> {
			int position = ctxFrag.getAdaptor().getSelectedPosition();
			if (position >= 0) {
				recycler.smoothScrollToPosition(position); // Scroll list to the selected row
				if (position == lastPos) {
					ctxFrag.getAdaptor().clearSelection();
					ctxFrag.getAdaptor().notifyDataSetChanged();
				} else {
					lastPos = position;
				}
			} else {
				if (lastPos >= 0) {
					ctxFrag.getAdaptor().selectRow(lastPos);
					ctxFrag.getAdaptor().notifyDataSetChanged();
					lastPos = -1;
				}
			}
		});
		content.setMovementMethod(new ScrollingMovementMethod());

		ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
			@Override
			public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder viewHolder1) {
				return false;
			}

			@Override
			public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
				if (ctxFrag.isFiltered()) return;

				final int position = viewHolder.getAdapterPosition();

				Bundle bundle = new Bundle();
				bundle.putInt(TAG, position);
				MessageDialog.Companion.getOkayCancelDialog(MSG_DIALOG_DELETE
						, String.format(Locale.getDefault(), getString(R.string.msg_confirm_delete), Integer.toString(position+1))
						, bundle)
					.show(getSupportFragmentManager(), MessageDialog.TAG);
			}
		});
		itemTouchHelper.attachToRecyclerView(recycler);
	}

	@Override
	protected void onResume() {
		super.onResume();

		ctxFrag = (ContextFragment) getSupportFragmentManager().findFragmentByTag(ContextFragment.TAG);
		if (ctxFrag == null) {
			ctxFrag = new ContextFragment();
			getSupportFragmentManager().beginTransaction().add(ctxFrag, ContextFragment.TAG).commit();
		}
		recycler.setAdapter(ctxFrag.getAdaptor());

		// If there is already a selection...
		int pos = ctxFrag.getAdaptor().getSelectedPosition();
		if (pos >= 0) {
			recycler.smoothScrollToPosition(pos); // Scroll list to the selected row
			ctxFrag.getAdaptor().selectDetails(pos);
		}

		if (!ctxFrag.isDbReady()) {
			Log.d(TAG, "onResume - DB not ready, initializing...");
			ctxFrag.initDb();
		} else if (!ctxFrag.isLogon()) {
			Log.d(TAG, "onResume - DB ready, logging in...");
			doLogon();
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		Log.d(TAG, "onPause");
		FragmentManager manager = getSupportFragmentManager();

		DialogFragment frag = (LogonDialog) manager.findFragmentByTag(LogonDialog.TAG);
		if (frag != null) frag.dismiss();

		frag = (LogonDialog2) manager.findFragmentByTag(LogonDialog2.TAG);
		if (frag != null) frag.dismiss();
	}

	@Override
	public void onStop() {
		super.onStop();
		Log.d(TAG, "onStop");
		ctxFrag.logoff();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		Log.d(TAG, "onNewIntent");
		handleIntent(intent);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		Log.d(TAG, "onCreateOptionsMenu");
		getMenuInflater().inflate(R.menu.menu_main, menu);

		SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
		if (searchManager != null) {
			MenuItem menuItem = menu.findItem(R.id.menu_search);
			searchView = (SearchView) menuItem.getActionView();
			searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

			LayoutTransition transit = new LayoutTransition();
			transit.setDuration(LayoutTransition.CHANGE_APPEARING, 0);
			((ViewGroup) searchView.findViewById(searchView.getContext().getResources()
					.getIdentifier("android:id/search_bar", null, null)))
					.setLayoutTransition(transit);

			if (ctxFrag.isFiltered()) {
				menuItem.expandActionView();
				searchView.setQuery(ctxFrag.getFilterQuery(), true);
				searchView.setIconified(false);
				searchView.clearFocus();
			}

			searchView.findViewById(searchView.getContext().getResources()
					.getIdentifier("android:id/search_close_btn", null, null))
					.setOnClickListener(view -> {
						searchView.setQuery(ContextFragment.EMPTY, false);
						searchView.setIconified(true);
						ctxFrag.clearFilter();
					});
		}

		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (!ctxFrag.isLogon() || ctxFrag.isFiltered() || ctxFrag.isBusy()) {
			menu.findItem(R.id.action_cleanup).setEnabled(false);
			menu.findItem(R.id.action_import).setEnabled(false);
			menu.findItem(R.id.action_export).setEnabled(false);
			menu.findItem(R.id.action_passwd).setEnabled(false);
		} else if (ctxFrag.getAdaptor().getItemCount() > 0) {
			menu.findItem(R.id.action_cleanup).setEnabled(true);
			menu.findItem(R.id.action_import).setEnabled(false);
			menu.findItem(R.id.action_export).setEnabled(true);
			menu.findItem(R.id.action_passwd).setEnabled(true);
		} else {
			menu.findItem(R.id.action_cleanup).setEnabled(true);
			menu.findItem(R.id.action_import).setEnabled(true);
			menu.findItem(R.id.action_export).setEnabled(false);
			menu.findItem(R.id.action_passwd).setEnabled(false);
		}

		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_cleanup:
				MessageDialog.Companion.getOkayCancelDialog(MSG_DIALOG_CLEANUP, getString(R.string.msg_confirm_delete_tags), null)
						.show(getSupportFragmentManager(), MessageDialog.TAG);
				break;

			case R.id.action_import:
				if (getApplicationContext().checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
					ActivityCompat.requestPermissions(this,
							new String[] { Manifest.permission.READ_EXTERNAL_STORAGE },
							READ_EXTERNAL_STORAGE_REQUEST);
				} else {
					ctxFrag.getFileAdaptor().setHasPermission(true);
					FileChooser.Companion.getInstance().show(getSupportFragmentManager(), FileChooser.TAG);
				}
				break;

			case R.id.action_export:
				MessageDialog.Companion.getOkayCancelDialog(MSG_DIALOG_EXPORT, getString(R.string.msg_confirm_export), null)
						.show(getSupportFragmentManager(), MessageDialog.TAG);
				break;
			case R.id.action_passwd:
				PasswdDialog.getInstance().show(getSupportFragmentManager(), PasswdDialog.TAG);
				break;
			case R.id.action_about:
				AboutDialog.Companion.getInstance().show(getSupportFragmentManager(), AboutDialog.TAG);
				break;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		switch (requestCode) {
			case READ_EXTERNAL_STORAGE_REQUEST:
				ctxFrag.getFileAdaptor().setHasPermission(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED);
				FileChooser.Companion.getInstance().show(getSupportFragmentManager(), FileChooser.TAG);
				break;
			default:
				super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		}
	}

	/*
	 * Common method to several Callback interfaces.
	 */
	public void doNotify(String message, boolean stay) {
		if (stay || (message.length() >= 70)) {
			MessageDialog.Companion.getInstance(MSG_DIALOG_NOTIFY, message, null).show(getSupportFragmentManager(), MessageDialog.TAG);
		} else {
			Snackbar.make(fab, message, Snackbar.LENGTH_LONG).show();
		}
	}

	/*
	 * Common method to several Callback interfaces.
	 */
	public void setBusyState(boolean isBusy) {
		ctxFrag.setBusy(isBusy);
		progress.setVisibility(isBusy ? View.VISIBLE : View.INVISIBLE);
	}

	private void handleIntent(Intent intent) {
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			String query = intent.getStringExtra(SearchManager.QUERY);
			if (query != null) {
				ctxFrag.applyFilter(query);
			}
			ctxFrag.getAdaptor().clearSelection();
			mainView.requestFocus();

		}
	}

	/*============================================================
	 * @see org.sea9.android.secret.main.ContextFragment.Callback
	 */
	@Override
	public void onLogoff() {
		DetailFragment frag = (DetailFragment) getSupportFragmentManager().findFragmentByTag(DetailFragment.TAG);
		if (frag != null) frag.dismissAllowingStateLoss();
	}

	@Override
	public void doLogon() {
		if (!ctxFrag.isDbEmpty()) {
			LogonDialog.getInstance().show(getSupportFragmentManager(), LogonDialog.TAG);
		} else {
			LogonDialog2.getInstance().show(getSupportFragmentManager(), LogonDialog2.TAG);
		}
	}

	@Override
	public void onRowSelectionChanged(String txt) {
		content.setText(txt);
		content.scrollTo(0, 0);
	}

	@Override
	public void onFilterCleared(int position) {
		if (position >= 0) recycler.smoothScrollToPosition(position);
	}

	@Override
	public void onDirectorySelected(File selected) {
		FileChooser frag = (FileChooser) getSupportFragmentManager().findFragmentByTag(FileChooser.TAG);
		if (frag != null) frag.setCurrentPath(selected.getPath());
	}

	@Override
	public void onFileSelected() {
		FileChooser frag = (FileChooser) getSupportFragmentManager().findFragmentByTag(FileChooser.TAG);
		if (frag != null) frag.dismissAllowingStateLoss();
	}

	@Override
	public void doCompatLogon() {
		new CompatLogonDialog().show(getSupportFragmentManager(), CompatLogonDialog.TAG);
	}

	@Override
	public void longPressed() {
		int position = ctxFrag.getAdaptor().getSelectedPosition();
		if (position >= 0) {
			NoteRecord record = ctxFrag.getAdaptor().getRecord(position);
			if (record != null) {
				String text = content.getText().toString();
				ctxFrag.getTagsAdaptor().selectTags(record.getTags());
				ctxFrag.clearUpdated();
				DetailFragment.getInstance(true, record, text).show(getSupportFragmentManager(), DetailFragment.TAG);
			}
		}
	}
	//============================================================

	/*========================================================
	 * @see org.sea9.android.secret.core.LogonDialog.Callback
	 */
	@Override
	public void onLogon(char[] value, boolean isNew) {
		if (value == null) {
			finish();
		} else {
			ctxFrag.onLogon(value, isNew);
		}
	}
	//========================================================

	/*=========================================================
	 * @see org.sea9.android.secret.core.PasswdDialog.Callback
	 */
	@Override
	public void onChangePassword(char[] oldValue, char[] newValue) {
		if ((oldValue != null) && (newValue != null)) {
			ctxFrag.onChangePassword(oldValue, newValue);
		}
	}
	//=========================================================

	/*==============================================================
	 * @see org.sea9.android.secret.core.CompatLogonDialog.Callback
	 */
	@Override
	public void onCompatLogon(char[] value, boolean smart) {
		if (value != null) {
			ctxFrag.importOldFormat(value, smart);
		}
	}
	//==============================================================

	/*======================================================
	 * @sea org.sea9.android.secret.io.FileChooser.Callback
	 */
	@Override @NonNull
	public FileChooserAdaptor getFileAdaptor() {
		return ctxFrag.getFileAdaptor();
	}
	//======================================================

	/*==============================================================
	 * @see org.sea9.android.secret.details.DetailFragment.Callback
	 */
	@Override
	public boolean isFiltered() {
		return ctxFrag.isFiltered();
	}

	@Override
	public boolean isUpdated() {
		return ctxFrag.isUpdated();
	}

	@Override
	public void dataUpdated() {
		ctxFrag.dataUpdated();
	}

	@Override @NonNull
	public TagsAdaptor getTagsAdaptor() {
		return ctxFrag.getTagsAdaptor();
	}

	@Override
	public void onAdd(String t) {
		int position = ctxFrag.getTagsAdaptor().insert(t);
		if (position >= 0) {
			DetailFragment fragment = (DetailFragment) getSupportFragmentManager().findFragmentByTag(DetailFragment.TAG);
			if (fragment != null) {
				fragment.onTagAddCompleted(position);
			}
		}
	}

	@Override
	public void onSave(boolean isNew, String k, String c, List<Long> t) {
		String msg;
		int position = -1;
		boolean stay = false;
		if (k.trim().length() <= 0) {
			msg = getString(R.string.msg_empty_key);
		} else if (isNew) {
			Long pid = ctxFrag.getAdaptor().insert(k, c, t);
			if (pid != null) {
				if (pid < 0) {
					msg = getString(R.string.msg_insert_duplicated);
					stay = true;
					pid *= -1;
				} else {
					msg = getString(R.string.msg_insert_okay);
				}
				position = ctxFrag.getAdaptor().selectRow(pid);
				if (position >= 0) {
					recycler.smoothScrollToPosition(position);
					ctxFrag.getAdaptor().notifyDataSetChanged();
				}
			} else {
				msg = getString(R.string.msg_insert_fail);
				stay = true;
			}
		} else {
			position = ctxFrag.getAdaptor().update(k, c, t);
			if (position >= 0) {
				recycler.smoothScrollToPosition(position);
				msg = getString(R.string.msg_update_okay);
			} else {
				msg = getString(R.string.msg_update_fail);
				stay = true;
			}
		}
		doNotify(String.format(Locale.getDefault(), msg, Integer.toString(position+1)), stay);
	}
	//==============================================================

	/*========================================================
	 * @see org.sea9.android.secret.ui.MessageDialog.Callback
	 */
	private static final int MSG_DIALOG_NOTIFY  = 90001;
	private static final int MSG_DIALOG_EXPORT  = 90002;
	private static final int MSG_DIALOG_DELETE  = 90003;
	private static final int MSG_DIALOG_CLEANUP = 90004;
	public static final int MSG_DIALOG_DISCARD  = 91005;
	public static final int MSG_DIALOG_LOG_FAIL = 92006;

	@Override
	public void neutral(DialogInterface dialog, int which, int reference, Bundle args) {
		switch (reference) {
			case MSG_DIALOG_NOTIFY:
				dialog.dismiss();
				break;
			case MSG_DIALOG_LOG_FAIL:
				finish();
				break;
		}
	}

	@Override
	public void positive(DialogInterface dialog, int which, int reference, Bundle args) {
		int del;
		String msg;
		switch (reference) {
			case MSG_DIALOG_EXPORT:
				ctxFrag.onExport(getExternalFilesDir(null));
				break;
			case MSG_DIALOG_DELETE:
				int position = args.getInt(TAG);
				del = ctxFrag.getAdaptor().delete(position);
				msg = (del >= 0) ? getString(R.string.msg_delete_okay) : getString(R.string.msg_delete_fail);
				doNotify(String.format(Locale.getDefault(), msg, Integer.toString(position+1)), false);
				break;
			case MSG_DIALOG_CLEANUP:
				del = ctxFrag.getTagsAdaptor().delete();
				msg = (del < 0) ?
						getString(R.string.msg_delete_tags_fail) :
						String.format(Locale.getDefault(), getString(R.string.msg_delete_tags_okay), Integer.toString(del));
				doNotify(msg, false);
				break;
			case MSG_DIALOG_DISCARD:
				DetailFragment frag = (DetailFragment) getSupportFragmentManager().findFragmentByTag(DetailFragment.TAG);
				if (frag != null) {
					frag.dismiss();
				}
				break;
		}
	}

	@Override
	public void negative(DialogInterface dialog, int which, int reference, Bundle args) {
		switch (reference) {
			case MSG_DIALOG_DELETE:
				if (recycler.getAdapter() != null)
					recycler.getAdapter().notifyDataSetChanged();
				break;
			case MSG_DIALOG_CLEANUP:
				doNotify(getString(R.string.msg_delete_tags_cancel), false);
				break;
		}
	}
}