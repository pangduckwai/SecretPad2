package org.sea9.android.secret.core;

import android.Manifest;
import android.animation.LayoutTransition;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;
import android.widget.TextView;

import org.sea9.android.secret.data.NoteRecord;
import org.sea9.android.secret.details.DetailFragment;
import org.sea9.android.secret.R;
import org.sea9.android.secret.io.FileChooser;

import java.io.File;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements
		ContextFragment.Callback,
		LogonDialog.Callback,
		DetailFragment.Callback {
	public static final String TAG = "secret.main";
	private static final String EMPTY = "";
	private static final int READ_EXTERNAL_STORAGE_REQUEST = 123;

	private View mainView;
	private FloatingActionButton fab;
	private ContextFragment ctxFrag;
	private RecyclerView recycler;
	private TextView content;
	private SearchView searchView;

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

		fab = findViewById(R.id.fab);
		fab.setOnClickListener(view -> {
			if (!ctxFrag.isLogon()) {
				doNotify(getString(R.string.msg_not_logon));
			} else if (ctxFrag.isFiltered()) {
				doNotify(getString(R.string.msg_filter_active));
			} else {
				ctxFrag.getAdaptor().clearSelection();
				ctxFrag.getAdaptor().notifyDataSetChanged();
				ctxFrag.getTagsAdaptor().selectTags(null);
				ctxFrag.clearUpdated();
				DetailFragment.getInstance(true, null, null).show(getSupportFragmentManager(), DetailFragment.TAG);
			}
		});

		recycler = findViewById(R.id.recycler_list);
		content = findViewById(R.id.item_content);

		recycler.setHasFixedSize(true); // improve performance since content changes do not affect layout size of the RecyclerView
		recycler.setLayoutManager(new LinearLayoutManager(this)); // use a linear layout manager

		content.setOnClickListener(view -> {
			int position = ctxFrag.getAdaptor().getSelectedPosition();
			if (position >= 0) {
				recycler.smoothScrollToPosition(position); // Scroll list to the selected row
				NoteRecord record = ctxFrag.getAdaptor().getRecord(position);
				String content = ((TextView) view).getText().toString();
				if (record != null) {
					ctxFrag.getTagsAdaptor().selectTags(record.getTags());
					ctxFrag.clearUpdated();
					DetailFragment.getInstance(false, record, content).show(getSupportFragmentManager(), DetailFragment.TAG);
				}
			}
		});

		ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
			@Override
			public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder viewHolder1) {
				return false;
			}

			@Override
			public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
				if (ctxFrag.isFiltered()) return;

				final int position = viewHolder.getAdapterPosition();

				AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
				builder.setMessage(
						String.format(Locale.getDefault(), getString(R.string.msg_confirm_delete), Integer.toString(position+1)));
				builder.setPositiveButton(R.string.btn_okay, (dialog, which) -> {
					int del = ctxFrag.getAdaptor().delete(position);
					String msg = (del >= 0) ?
							getString(R.string.msg_delete_okay) :
							getString(R.string.msg_delete_fail);
					Snackbar.make(recycler,
							String.format(Locale.getDefault(), msg, Integer.toString(position+1)),
							Snackbar.LENGTH_LONG).show();
				});
				builder.setNegativeButton(R.string.btn_cancel, (dialog, which) -> {
					if (recycler.getAdapter() != null)
						recycler.getAdapter().notifyDataSetChanged();
				});
				(builder.create()).show();

			}
		});
		itemTouchHelper.attachToRecyclerView(recycler);
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.d(TAG, "onResume");

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

		if (!ctxFrag.isLogon()) onInit();
	}

	@Override
	public void onPause() {
		super.onPause();
		Log.d(TAG, "onPause");
		FragmentManager manager = getSupportFragmentManager();

		DialogFragment frag = (LogonDialog) manager.findFragmentByTag(LogonDialog.TAG);
		if (frag != null) frag.dismiss();

		frag = (AboutDialog) manager.findFragmentByTag(AboutDialog.TAG);
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
		getMenuInflater().inflate(R.menu.menu_list, menu);

		SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
		if (searchManager != null) {
			searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
			searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

			LayoutTransition transit = new LayoutTransition();
			transit.setDuration(LayoutTransition.CHANGE_APPEARING, 0);
			((ViewGroup) searchView.findViewById(searchView.getContext().getResources()
					.getIdentifier("android:id/search_bar", null, null)))
					.setLayoutTransition(transit);

			searchView.findViewById(searchView.getContext().getResources()
					.getIdentifier("android:id/search_close_btn", null, null))
					.setOnClickListener(view -> {
						searchView.setQuery(EMPTY, false);
						searchView.setIconified(true);
						ctxFrag.clearFilter();
					});
		}

		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (!ctxFrag.isLogon() || ctxFrag.isFiltered()) {
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
			AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
			builder.setMessage(getString(R.string.msg_confirm_delete_tags));
			builder.setPositiveButton(R.string.btn_okay, (dialog, which) -> {
				int del = ctxFrag.getTagsAdaptor().delete();
				String msg = (del < 0) ?
						getString(R.string.msg_delete_tags_fail) :
						String.format(Locale.getDefault(), getString(R.string.msg_delete_tags_okay), Integer.toString(del));
				Snackbar.make(recycler, msg, Snackbar.LENGTH_LONG).show();
			});
			builder.setNegativeButton(R.string.btn_cancel, (dialog, which) -> Snackbar.make(recycler, getString(R.string.msg_delete_tags_cancel), Snackbar.LENGTH_LONG).show());
			(builder.create()).show();
			break;

		case R.id.action_import:
			if (getApplicationContext().checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
				ActivityCompat.requestPermissions(this,
						new String[] { Manifest.permission.READ_EXTERNAL_STORAGE },
						READ_EXTERNAL_STORAGE_REQUEST);
			} else {
				FileChooser.Companion.getInstance().show(getSupportFragmentManager(), FileChooser.TAG);
			}
			break;

		case R.id.action_export:
			Snackbar.make(getWindow().getDecorView(), "Exporting...", Snackbar.LENGTH_LONG).show(); //TODO TEMP
			break;
		case R.id.action_passwd:
			Snackbar.make(getWindow().getDecorView(), "Change password", Snackbar.LENGTH_LONG).show(); //TODO TEMP
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

	/*
	 * Common method to several Callback interfaces.
	 */
	public void doNotify(String message) {
		Snackbar.make(fab, message, Snackbar.LENGTH_LONG).show();
	}

	/*============================================================
	 * @see org.sea9.android.secret.main.ContextFragment.Callback
	 */
	@Override
	public void onInit() {
		LogonDialog.Companion.getInstance().show(getSupportFragmentManager(), LogonDialog.TAG);
	}

	@Override
	public void onLogoff() {
		DetailFragment frag = (DetailFragment) getSupportFragmentManager().findFragmentByTag(DetailFragment.TAG);
		if (frag != null) frag.dismissAllowingStateLoss();
	}

	@Override
	public void onRowSelectionMade(String txt) {
		content.setText(txt);
	}

	@Override
	public void onRowSelectionCleared() {
		content.setText(EMPTY);
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
	//============================================================

	/*========================================================
	 * @see org.sea9.android.secret.core.LogonDialog.Callback
	 */
	@Override
	public void onLogon(char[] value) {
		if (value == null) {
			finish();
		} else {
			ctxFrag.onLogon(value);
		}
	}
	//========================================================

	/*==============================================================
	 * @see org.sea9.android.secret.details.DetailFragment.Callback
	 */
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
		if (isNew) {
			Long pid = ctxFrag.getAdaptor().insert(k, c, t);
			if (pid != null) {
				if (pid < 0) {
					msg = getString(R.string.msg_insert_duplicated);
					pid *= -1;
				} else {
					msg = getString(R.string.msg_insert_okay);
				}
				position = ctxFrag.getAdaptor().selectRow(pid);
				if (position >= 0) recycler.smoothScrollToPosition(position);
			} else {
				msg = getString(R.string.msg_insert_fail);
			}
		} else {
			position = ctxFrag.getAdaptor().update(k, c, t);
			if (position >= 0) {
				recycler.smoothScrollToPosition(position);
				msg = getString(R.string.msg_update_okay);
			} else {
				msg = getString(R.string.msg_update_fail);
			}
		}
		Snackbar.make(recycler,
				String.format(Locale.getDefault(), msg, Integer.toString(position+1)),
				Snackbar.LENGTH_LONG).show();
	}
}