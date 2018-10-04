package org.sea9.android.secret.main;

import android.animation.LayoutTransition;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
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

import org.sea9.android.secret.ContextFragment;
import org.sea9.android.secret.data.NoteRecord;
import org.sea9.android.secret.details.DetailFragment;
import org.sea9.android.secret.R;

import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements
		ContextFragment.Listener,
		DetailFragment.Listener {
	public static final String TAG = "secret.main";
	private static final String EMPTY = "";

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

		setContentView(R.layout.activity_list);
		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		ctxFrag = (ContextFragment) getSupportFragmentManager().findFragmentByTag(ContextFragment.TAG);
		if (ctxFrag == null) {
			ctxFrag = new ContextFragment();
			getSupportFragmentManager().beginTransaction().add(ctxFrag, ContextFragment.TAG).commit();
		}

		mainView = findViewById(R.id.main_view);

		fab = findViewById(R.id.fab);
		fab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (!ctxFrag.isFiltered()) {
					ctxFrag.prepareAdd();
				} else {
					Snackbar.make(view, getString(R.string.msg_filter_active), Snackbar.LENGTH_LONG).setAction("Action", null).show();
				}
			}
		});

		recycler = findViewById(R.id.recycler_list);
		content = findViewById(R.id.item_content);

		recycler.setHasFixedSize(true); // improve performance since content changes do not affect layout size of the RecyclerView
		recycler.setLayoutManager(new LinearLayoutManager(this)); // use a linear layout manager

		content.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				int pos = ctxFrag.getAdaptor().getSelectedPosition();
				if (pos >= 0) {
					recycler.smoothScrollToPosition(pos); // Scroll list to the selected row
					ctxFrag.queryData(pos);
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
				builder.setMessage(String.format(Locale.getDefault(),
						getString(R.string.msg_confirm_delete), Integer.toString(position+1)));
				builder.setPositiveButton(R.string.btn_okay, new DialogInterface.OnClickListener() {
					@Override public void onClick(DialogInterface arg0, int arg1) {
						Snackbar.make(recycler,
								String.format(Locale.getDefault(),
										getString(ctxFrag.deleteData(position) ? R.string.msg_delete_okay : R.string.msg_delete_fail),
										Integer.toString(position+1)),
								Snackbar.LENGTH_LONG).show();
					}
				});
				builder.setNegativeButton(R.string.btn_cancel, new DialogInterface.OnClickListener() {
					@Override public void onClick(DialogInterface arg0, int arg1) {
						if (recycler.getAdapter() != null)
							recycler.getAdapter().notifyDataSetChanged();
					}
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
			ctxFrag.datSelectionMade(pos);
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		Log.d(TAG, "onNewIntent");
		handleIntent(intent);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_list, menu);

		SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
		if (searchManager != null) {
			searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
			searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

			LayoutTransition transit = new LayoutTransition();
			transit.setDuration(LayoutTransition.CHANGE_APPEARING, 0);
			((ViewGroup) searchView.findViewById(searchView.getContext().getResources().getIdentifier("android:id/search_bar", null, null)))
					.setLayoutTransition(transit);

			searchView.findViewById(searchView.getContext().getResources().getIdentifier("android:id/search_close_btn", null, null))
					.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							searchView.setQuery(EMPTY, false);
							searchView.setIconified(true);
							ctxFrag.clearFilter();
						}
					});
		}

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_cleanup:
			AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
			builder.setMessage(getString(R.string.msg_confirm_delete_tags));
			builder.setPositiveButton(R.string.btn_okay, new DialogInterface.OnClickListener() {
				@Override public void onClick(DialogInterface arg0, int arg1) {
					Snackbar.make(recycler,
							getString(ctxFrag.deleteTags() ? R.string.msg_delete_tags_okay : R.string.msg_delete_tags_fail),
							Snackbar.LENGTH_LONG).show();
				}
			});
			builder.setNegativeButton(R.string.btn_cancel, new DialogInterface.OnClickListener() {
				@Override public void onClick(DialogInterface arg0, int arg1) {
					Snackbar.make(recycler, getString(R.string.msg_delete_tags_cancel), Snackbar.LENGTH_LONG).show();
				}
			});
			(builder.create()).show();
			break;
		case R.id.action_settings:
			Snackbar.make(getWindow().getDecorView(), "Changing settings", Snackbar.LENGTH_LONG).show(); //TODO TEMP
			break;
		case R.id.action_about:
			Snackbar.make(getWindow().getDecorView(), "About...", Snackbar.LENGTH_LONG).show(); //TODO TEMP
			break;
		}

		return super.onOptionsItemSelected(item);
	}

	private void handleIntent(Intent intent) {
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			String query = intent.getStringExtra(SearchManager.QUERY);
			if (query != null) {
				ctxFrag.applyFilter(query);
			}
			ctxFrag.clearSelection();
			mainView.requestFocus();

		}
	}

	/*=======================================================
	 * @see org.sea9.android.secret.ContextFragment.Listener
	 */
	@Override
	public void onRowSelectionMade(String txt) {
		content.setText(txt);
		fab.hide();
	}

	@Override
	public void onRowSelectionCleared() {
		content.setText(EMPTY);
		fab.show();
	}

	@Override
	public void onPrepareAddCompleted() {
		DetailFragment.getInstance(true, null).show(getSupportFragmentManager(), DetailFragment.TAG);
	}

	@Override
	public void onInsertDataCompleted(int position) {
		if (position >= 0) recycler.smoothScrollToPosition(position);
		Snackbar.make(recycler,
				String.format(
						Locale.getDefault(),
						getString((position >= 0) ? R.string.msg_insert_okay : R.string.msg_insert_fail),
						Integer.toString(position+1)),
				Snackbar.LENGTH_LONG).show();
	}

	@Override
	public void onUpdateDataCompleted(int position, String content) {
		if (position >= 0) recycler.smoothScrollToPosition(position);
		Snackbar.make(recycler,
				String.format(
						Locale.getDefault(),
						getString((position >= 0) ? R.string.msg_update_okay : R.string.msg_update_fail),
						Integer.toString(position+1)),
				Snackbar.LENGTH_LONG).show();
		onRowSelectionMade(content);
	}

	@Override
	public void onQueryDataCompleted(NoteRecord record) {
		DetailFragment.getInstance(false, record).show(getSupportFragmentManager(), DetailFragment.TAG);
	}

	@Override
	public void onFilterCleared(int position) {
		if (position >= 0) recycler.smoothScrollToPosition(position);
	}
	//=======================================================

	/*==============================================================
	 * @see org.sea9.android.secret.details.DetailFragment.Listener
	 */
	@Override
	public void onAdd(String t) {
		ctxFrag.addTag(t);
	}

	@Override
	public void onSave(boolean isNew, String k, String c, List<Integer> t) {
		if (isNew) {
			ctxFrag.insertData(k, c, t);
		} else {
			ctxFrag.updateData(k, c, t);
		}
	}
}