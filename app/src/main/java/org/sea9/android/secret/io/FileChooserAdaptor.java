package org.sea9.android.secret.io;

import android.annotation.SuppressLint;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.sea9.android.secret.R;

import java.io.File;
import java.io.FileFilter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FileChooserAdaptor extends RecyclerView.Adapter<FileChooserAdaptor.ViewHolder> implements FileFilter {
	private static final String TAG = "secret.files_adaptor";
	private static final String PATTERN_DATE = "yyyy-MM-dd HH:mm:ss";
	private static final String FILE_EXT = ".txt";
	private static final String FILE_PARENT = "..";

	private boolean ready;

	private RecyclerView recyclerView;

	private int selectedPos = -1;
	private boolean isSelected(int position) {
		return (selectedPos == position);
	}
	final int getSelectedPosition() { return selectedPos; }

	private String currentPath;
	final String getCurrentPath() { return currentPath; }

	private List<FileRecord> cache;

	public FileChooserAdaptor(Caller ctx) {
		caller = ctx;
		cache = new ArrayList<>();

		String state = Environment.getExternalStorageState();
		ready = (Environment.MEDIA_MOUNTED.equals(state) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state));
	}

	/*=====================================================
	 * @see android.support.v7.widget.RecyclerView.Adapter
	 */
	@Override
	public void onAttachedToRecyclerView(@NonNull RecyclerView recycler) {
		super.onAttachedToRecyclerView(recycler);
		Log.d(TAG, "onAttachedToRecyclerView");
		recyclerView = recycler;
	}

	@Override @NonNull
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View item = LayoutInflater.from(parent.getContext()).inflate(R.layout.filechooser_item, parent, false);
		item.setOnClickListener(view -> {
			int position = recyclerView.getChildLayoutPosition(view);
			if (position == selectedPos) {
				//Un-select
				selectedPos = -1;
			} else {
				//Select
				FileRecord selected = cache.get(position);
				if (selected.isDirectory()) {
					selectedPos = -1;
					if (selected.getPath().equals(FILE_PARENT)) {
						Log.w(TAG, currentPath);
						select((new File(currentPath)).getParent());
					} else {
						select(selected.getPath());
					}
				} else {
					selectedPos = position;
					caller.selected(new File(selected.getPath()));
				}
			}
			notifyDataSetChanged();
		});

		return new ViewHolder(item);
	}

	@SuppressLint("SetTextI18n")
	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
		if (isSelected(position)) {
			holder.itemView.setSelected(true);
		} else {
			holder.itemView.setSelected(false);
		}

		SimpleDateFormat formatter = new SimpleDateFormat(PATTERN_DATE, Locale.getDefault());
		FileRecord selected = cache.get(position);
		if (selected.isDirectory()) {
			holder.iconDir.setVisibility(View.VISIBLE);
			holder.iconFile.setVisibility(View.GONE);
		} else {
			holder.iconDir.setVisibility(View.GONE);
			holder.iconFile.setVisibility(View.VISIBLE);
		}
		holder.name.setText(selected.getName());
		holder.time.setText(formatter.format(selected.getModified()));
		holder.size.setText(Long.toString(selected.getSize()));
	}

	@Override
	public int getItemCount() {
		return cache.size();
	}
	//=====================================================

	/*======================
	 * Data access methods.
	 */
	final void select(String current) {
		currentPath = current;
		if (ready) {
			File curr = new File(current);
			if (curr.exists()) {
				if (curr.isDirectory()) {
					// TODO !!!!!!!!!!! Check permission here!!!!!!!!
					File[] list = curr.listFiles(this);
					cache = new ArrayList<>(list.length + 1);
					cache.add(new FileRecord(FILE_PARENT, FILE_PARENT, new Date(), 0, true));
					for (File record : list) {
						cache.add(new FileRecord(
								  record.getPath()
								, record.getName()
								, new Date(record.lastModified())
								, record.length()
								, record.isDirectory()));
					}
				} else {
					cache = new ArrayList<>(); //Should not reach here
				}
			}
		}

	}

	@Override
	public boolean accept(File pathname) {
		return pathname.exists() && (pathname.isDirectory() || pathname.getName().toLowerCase().endsWith(FILE_EXT));
	}
	//======================

	/*=============
	 * View holder
	 */
	static class ViewHolder extends RecyclerView.ViewHolder {
		ImageView iconDir;
		ImageView iconFile;
		TextView name;
		TextView time;
		TextView size;
		ViewHolder(View v) {
			super(v);
			iconDir = v.findViewById(R.id.icon_dir);
			iconFile = v.findViewById(R.id.icon_file);
			name = v.findViewById(R.id.file_name);
			time = v.findViewById(R.id.modify_time);
			size = v.findViewById(R.id.file_size);
		}
	}
	//=============

	/*============================================
	 * Callback interface to the context fragment
	 */
	public interface Caller {
		void selected(File selected);
	}
	private Caller caller;
}
