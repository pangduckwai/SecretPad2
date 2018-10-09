package org.sea9.android.secret;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.widget.Filter;
import android.widget.Filterable;

import org.sea9.android.secret.data.DbContract;
import org.sea9.android.secret.data.DbHelper;
import org.sea9.android.secret.data.NoteRecord;
import org.sea9.android.secret.data.TagRecord;
import org.sea9.android.secret.details.TagsAdaptor;
import org.sea9.android.secret.main.NotesAdaptor;
import org.sea9.android.secret.main.NotesViewHolder;

import java.util.ArrayList;
import java.util.List;

public class ContextFragment extends Fragment
		implements
		NotesAdaptor.Listener,
		TagsAdaptor.Listener
//		Filterable,
//		Filter.FilterListener
{
	public static final String TAG = "secret.ctx_frag";
	private static final String EMPTY = "";

	private DbHelper dbHelper;
	@Override public final DbHelper getDbHelper() {
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

		Context context = getContext();
		if (context != null) dbHelper = new DbHelper(context);

		adaptor = new NotesAdaptor(this);
		tagsAdaptor = new TagsAdaptor(this);
		test();
	}

	@Override
	public void onDestroy() {
		Log.d(TAG, "onDestroy");
		org.sea9.android.secret.data.DbTest.cleanup(getContext(), dbHelper);
		dbHelper.close();
		super.onDestroy();
	}

//	public final void clearSelection() {
//		adaptor.clearSelection();
//		clearRvRow();
//	}

	//TODO TEMP >>>>>>>>>>>>
	private void test() {
		new org.sea9.android.secret.data.DbTest(getContext(), this, dbHelper, false);
	}
	//TODO TEMP <<<<<<<<<<<<

	/*=======================================================
	 * Data maintenance APIs - query, insert, update, delete
	 */
	private boolean updated = false;
	public final boolean isUpdated() { return updated; }
	@Override public final void dataUpdated() { updated = true; }

	public final void prepareAdd() {
		updated = false;
		tagsAdaptor.refreshSelection(null);
		callback.onPrepareAddCompleted();
	}

	/*================================
	 * @see android.widget.Filterable
	 */
//	private List<NoteRecord> getData() {
//		if (filteredList == null) {
//			return dataList;
//		} else {
//			return filteredList;
//		}
//	}
	@Override public boolean isFiltered() {
//		return (filteredList != null);
		return false;
	}
//	public void applyFilter(String query) {
//		getFilter().filter(query, this);
//	}
//	@Override
//	public void onFilterComplete(int count) {
//		adaptor.notifyDataSetChanged();
//	}
//	public void clearFilter() {
//		if (filteredList != null) {
//			int idx = -1, pos = getAdaptor().getSelectedPosition();
//			if (pos >= 0) {
//				String k = filteredList.get(pos).getKey();
//				for (int i = 0; i < dataList.size(); i ++) {
//					if (k.equals(dataList.get(i).getKey())) {
//						adaptor.selectRow(i);
//						idx = i;
//						break;
//					}
//				}
//			}
//			filteredList.clear();
//			filteredList = null;
//			adaptor.notifyDataSetChanged();
//			callback.onFilterCleared(idx);
//		}
//	}
//
//	@Override @SuppressWarnings("unchecked")
//	public Filter getFilter() {
//		return new Filter() {
//			@Override
//			protected FilterResults performFiltering(CharSequence constraint) {
//				FilterResults results = new FilterResults();
//				String query = constraint.toString().trim().toLowerCase();
////				if (query.length() <= 0) {
////					results.values = dataList;
////				} else {
////					List<NoteRecord> rslt = new ArrayList<>();
////					for (NoteRecord item : dataList) {
////						// Contents stay encrypted, so cannot be searched, keys are decrypted into memory
////						if (item.getKey().toLowerCase().contains(query)) {
////							rslt.add(item);
////						} else {
////							for (Long tag : item.getTags()) {
////								if (tagList.get(tag).getTag().toLowerCase().contains(query)) {
////									rslt.add(item);
////									break;
////								}
////							}
////						}
////					}
////					results.values = rslt;
////				}
//				return results;
//			}
//
//			@Override
//			protected void publishResults(CharSequence constraint, FilterResults results) {
//				filteredList = (List<NoteRecord>) results.values;
//			}
//		};
//	}
	//================================

	/*================================================
	 * @see org.sea9.android.secret.main.NotesAdaptor
	 */
	public void selectRvRow(String content) {
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
		void onPrepareAddCompleted();
//		void onInsertDataCompleted(int position);
//		void onUpdateDataCompleted(int position, String content);
//		void onQueryDataCompleted(NoteRecord record);
//		void onFilterCleared(int position);
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

	/*=========================================
	 * Callback interface to the detail dialog
	 */
	public interface DetailListener {
		void onTagAddCompleted(int position);
	}
	private DetailListener detailListener;
	public final void setDetailListener(DetailListener listener) { detailListener = listener; }
	//=========================================
}
