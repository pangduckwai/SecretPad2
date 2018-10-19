package org.sea9.android.secret.io;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.sea9.android.secret.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileChooserAdaptor extends RecyclerView.Adapter<FileChooserAdaptor.ViewHolder> {
	private static final String TAG = "secret.files_adaptor";

	private RecyclerView recyclerView;

	private List<File> cache;

	public FileChooserAdaptor(Listener ctx) {
		callback = ctx;
		cache = new ArrayList<>();
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
		return null;
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {

	}

	@Override
	public int getItemCount() {
		return cache.size();
	}

	/*=============
	 * View holder
	 */
	static class ViewHolder extends RecyclerView.ViewHolder {
		ImageView dirc;
		ImageView file;
		TextView name;
		TextView time;
		TextView size;
		ViewHolder(View v) {
			super(v);
			dirc = v.findViewById(R.id.icon_dir);
			file = v.findViewById(R.id.icon_file);
			name = v.findViewById(R.id.file_name);
			time = v.findViewById(R.id.modify_time);
			size = v.findViewById(R.id.file_size);
		}
	}
	//=============

	/*============================================
	 * Callback interface to the context fragment
	 */
	public interface Listener {
		void selected(File selected);
	}
	private Listener callback;
}
