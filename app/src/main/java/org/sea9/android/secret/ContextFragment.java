package org.sea9.android.secret;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.View;
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

public class ContextFragment extends Fragment implements
		NotesAdaptor.Listener,
		TagsAdaptor.Listener,
		Filterable,
		Filter.FilterListener {
	public static final String TAG = "secret.ctx_frag";

	private DbHelper dbHelper;

	private List<NoteRecord> filteredList;

	private List<TagRecord> tagList;
	public List<Integer> getSelectedTags(NoteRecord record) {
		List<Integer> ret = new ArrayList<>();
		List<TagRecord> tags = record.getTags();
		if (tags != null) {
			for (int idx = 0; idx < tagList.size(); idx++) {
				if (tags.contains(tagList.get(idx))) ret.add(idx);
			}
		}
		return ret;
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

		dataList = retrieveNotes();
		filteredList = null;
		tagList = retrieveTags();

		adaptor = new NotesAdaptor(this);
		tagsAdaptor = new TagsAdaptor(this);
		test();
	}

	@Override
	public void onDestroy() {
		Log.d(TAG, "onDestroy");
		dbHelper.close();
		super.onDestroy();
	}

	public final void clearSelection() {
		adaptor.clearSelection();
		clearRvRow();
	}

	//TODO TEMP >>>>>>>>>>>>
	private void test() {
		new org.sea9.android.secret.data.DbTest(getContext(), this, dbHelper, true);
	}
	//TODO TEMP <<<<<<<<<<<<

	/*=========================================
	 * DAO API
	 */
//	public final List<NoteRecord> retrieveNotes() {
//		List<NoteRecord> notes = DbContract.Notes.Companion.select(dbHelper);
//		for (NoteRecord note : notes) {
//			DbContract.NoteTags.Companion.select(dbHelper, note);
//		}
//		return notes;
//	}
	public final String retrieveNote(NoteRecord rec) {
		return DbContract.Notes.Companion.select(dbHelper, rec.getPid());
	}
	public final List<TagRecord> retrieveTags() {
		return DbContract.Tags.Companion.select(dbHelper);
	}
	public final NoteRecord createNote(String k, String c, List<Integer> t) {
		NoteRecord added = DbContract.Notes.Companion.insert(dbHelper, k, c);
		List<TagRecord> tags = new ArrayList<>();
		if (added != null) {
			for (Integer idx : t) {
				long tid = DbContract.NoteTags.Companion.insert(dbHelper, added.getPid(), tagList.get(idx).getPid());
				if (tid >= 0) {
					tags.add(new TagRecord(tid, ));
				} else {
					return null;
				}
			}
			added.setTags(tags);
		}
		return added;
	}
	//=========================================

	/*=======================================================
	 * Data maintenance APIs - query, insert, update, delete
	 */
	private boolean updated = false;
	public final boolean isUpdated() { return updated; }
	public final void detailUpdated() { updated = true; }

	public final void prepareAdd() {
		tagsAdaptor.prepare(null);
		updated = false;
		callback.onPrepareAddCompleted();
	}

	public final void queryData(int position) {
		if ((position >= 0) && (position < getData().size())) {
			NoteRecord rec = getData().get(position);
			tagsAdaptor.prepare(getSelectedTags(rec));
			updated = false; // Reset detail dialog updated flag every time opening it
			callback.onQueryDataCompleted(rec);
		}
	}

	public final void insertData(String k, String c, List<Integer> t) {
		if (isFiltered()) return;
		NoteRecord rec = createNote(k, c, t);
		int idx = -1;
		if (rec != null) {
			dataList = retrieveNotes();
			for (idx = 0; idx < dataList.size(); idx ++) {
				if (dataList.get(idx).getPid() == rec.getPid()) {
					adaptor.onItemInsert(idx, rec.getPid());
					break;
				}
			}
			if (idx >= dataList.size()) idx = -1;
		}
		callback.onInsertDataCompleted(idx);
	}

	// TODO TEMP using memory, will use SQLite
	public final void updateData(String k, String c, List<Integer> t) {
//		if (isFiltered()) return;
//		DataRecord rec;
//		int ret = 0;
//		while (ret < dataList.size()) {
//			rec = dataList.get(ret);
//			if (rec.getKey().equals(k)) {
//				rec.setContent(c);
//				rec.getTags().clear();
//				rec.getTags().addAll(t);
//				if (dataList.set(ret, rec) != null) {
//					adaptor.notifyItemChanged(ret);
//					break;
//				}
//			}
//			ret ++;
//		}
//		if (ret >= dataList.size()) ret = -1;
//		callback.onUpdateDataCompleted(ret, c);
	}

	// TODO TEMP using memory, will use SQLite
	public final boolean deleteData(int position) {
//		if (isFiltered()) return true;
//		if ((position < 0) || (position >= dataList.size())) {
//			return false;
//		} else {
//			if (adaptor.isSelected(position)) {
//				clearRvRow();
//			}
//			if (dataList.remove(position) != null) {
//				adaptor.onItemDeleted(position);
//				return true;
//			} else {
//				adaptor.notifyDataSetChanged();
				return false;
//			}
//		}
	}
	//=======================================================

	/*=======================================================
	 * Tag maintenance APIs - add, delete
	 */
	public final void addTag(String t) {
		int index = -1;

		// TODO TEMP using memory, will use SQLite
		for (int i = 0; i < tagList.size(); i ++) {
			if (t.toLowerCase().equals(tagList.get(i).getTag().toLowerCase())) {
				index = i;
				break;
			}
		}

//		if (index < 0) {
//			// Tag not found, can add to list
//			if (tagList.add(t)) {
//				index = tagList.size() - 1;
//				tagsAdaptor.notifyItemInserted(index);
//			}
//		}
//
//		tagsAdaptor.selectTag(index);
//		if (detailListener != null) detailListener.onTagAddCompleted(index);
	}

	/**
	 * Delete unused tags.
	 */
	public final boolean deleteTags() {
		//TODO - Implement when switched to SQLite
		Log.i(TAG, "TEMP - Deleting unused Tags");
		return false;
	}
	//=======================================================

	/*================================
	 * @see android.widget.Filterable
	 */
	private List<NoteRecord> getData() {
		if (filteredList == null) {
			return dataList;
		} else {
			return filteredList;
		}
	}
	public boolean isFiltered() {
		return (filteredList != null);
	}
	public void applyFilter(String query) {
		getFilter().filter(query, this);
	}
	@Override
	public void onFilterComplete(int count) {
		adaptor.notifyDataSetChanged();
	}
	public void clearFilter() {
		if (filteredList != null) {
			int idx = -1, pos = getAdaptor().getSelectedPosition();
			if (pos >= 0) {
				String k = filteredList.get(pos).getKey();
				for (int i = 0; i < dataList.size(); i ++) {
					if (k.equals(dataList.get(i).getKey())) {
						adaptor.selectRow(i);
						idx = i;
						break;
					}
				}
			}
			filteredList.clear();
			filteredList = null;
			adaptor.notifyDataSetChanged();
			callback.onFilterCleared(idx);
		}
	}

	@Override @SuppressWarnings("unchecked")
	public Filter getFilter() {
		return new Filter() {
			@Override
			protected FilterResults performFiltering(CharSequence constraint) {
				FilterResults results = new FilterResults();
				String query = constraint.toString().trim().toLowerCase();
//				if (query.length() <= 0) {
//					results.values = dataList;
//				} else {
//					List<NoteRecord> rslt = new ArrayList<>();
//					for (NoteRecord item : dataList) {
//						// Contents stay encrypted, so cannot be searched, keys are decrypted into memory
//						if (item.getKey().toLowerCase().contains(query)) {
//							rslt.add(item);
//						} else {
//							for (Long tag : item.getTags()) {
//								if (tagList.get(tag).getTag().toLowerCase().contains(query)) {
//									rslt.add(item);
//									break;
//								}
//							}
//						}
//					}
//					results.values = rslt;
//				}
				return results;
			}

			@Override
			protected void publishResults(CharSequence constraint, FilterResults results) {
				filteredList = (List<NoteRecord>) results.values;
			}
		};
	}
	//================================

	/*========================================================
	 * @see org.sea9.android.secret.main.NotesAdaptor.Listener
	 */
	private static final String EMPTY = "";
	private static final String SPACE = " ";

	private List<NoteRecord> dataList;
	private NotesAdaptor adaptor;
	public final NotesAdaptor getAdaptor() {
		return adaptor;
	}

	/*
	 * Prepare initial data for the recyclerView, retrieved from the database.
	 */
	@Override
	public void prepareRvData() {
//		dataList = DbContract.Notes.Companion.select(dbHelper);
//		for (NoteRecord note : dataList) {
//			DbContract.NoteTags.Companion.select(dbHelper, note);
//		}
	}

	@Override
	public int getRvItemCount() {
		return getData().size();
	}

	@Override
	public void populateRv(NotesViewHolder holder, int position) {
		NoteRecord rec = dataList.get(position);
//		holder.itemView.setSelected(isSelected);

//		NoteRecord rec = getData().get(position);
//		List<Long> tag = rec.getTags();
//		StringBuilder tags = new StringBuilder((tag.size() > 0) ? tagList.get(tag.get(0)) : EMPTY);
//		for (int i = 1; i < tag.size(); i ++)
//			tags.append(SPACE).append(tagList.get(tag.get(i)));
//		holder.key.setText(rec.getKey());
//		holder.tag.setText(tags.toString());
	}

	@Override
	public void selectRvRow(int position) {
		if (position >= 0) {
			long pid = adaptor.getAdapterPositionId(position);
			if (pid >= 0) {
				String content = DbContract.Notes.Companion.select(dbHelper, pid);
				callback.onRowSelectionMade((content != null) ? content : EMPTY);
			}
		} else {
			clearRvRow(); // Should not be possible to reach here
		}
	}

	@Override
	public void clearRvRow() {
		callback.onRowSelectionCleared();
	}
	//========================================================

	/*===========================================================
	 * @see org.sea9.android.secret.details.TagsAdaptor.Listener
	 */
	@Override
	public String getTag(int position) {
		return tagList.get(position).getTag();
	}

	@Override
	public int getTagsCount() {
		return tagList.size();
	}

	@Override
	public void selectionChanged() {
		detailUpdated();
	}
	//===========================================================

	/*=========================================
	 * Callback interface to the main activity
	 */
	public interface Listener {
		void onRowSelectionMade(String content);
		void onRowSelectionCleared();
		void onPrepareAddCompleted();
		void onInsertDataCompleted(int position);
		void onUpdateDataCompleted(int position, String content);
		void onQueryDataCompleted(NoteRecord record);
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
