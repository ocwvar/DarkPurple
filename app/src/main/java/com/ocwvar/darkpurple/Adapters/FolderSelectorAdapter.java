package com.ocwvar.darkpurple.Adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ocwvar.darkpurple.AppConfigs;
import com.ocwvar.darkpurple.R;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Project DarkPurple
 * Created by 区成伟
 * On 2016/8/29 10:38
 * File Location com.ocwvar.darkpurple.Adapters
 * 文件夹选择列表适配器
 */
public class FolderSelectorAdapter extends RecyclerView.Adapter {

    private List<File> folders;
    private Set<File> selectedFolder;
    private OnFolderSelectCallback callback;

    public FolderSelectorAdapter() {
        this.folders = new ArrayList<>();
        this.selectedFolder = new LinkedHashSet<>();
    }

    public void setCallback(OnFolderSelectCallback callback) {
        this.callback = callback;
    }

    public void addDatas(List<File> source) {
        this.folders.clear();
        if (source != null) {
            this.folders.addAll(source);
        }
        notifyDataSetChanged();
    }

    public Set<File> getSelectedPath() {
        return selectedFolder;
    }

    public int getSelectedPathCount() {
        return selectedFolder.size();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new FolderItemViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_folder, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        FolderItemViewHolder viewHolder = (FolderItemViewHolder) holder;
        final File folder = folders.get(position);
        viewHolder.name.setText(folder.getName());

        if (selectedFolder.contains(folder)) {
            viewHolder.itemView.setBackgroundColor(AppConfigs.Color.ToolBar_color);
        } else {
            viewHolder.itemView.setBackground(null);
        }
    }

    @Override
    public int getItemCount() {
        return folders.size();
    }

    public interface OnFolderSelectCallback {

        void onEnter(File path);

        void onSelectedFolderChanged();

    }

    private class FolderItemViewHolder extends RecyclerView.ViewHolder implements View.OnLongClickListener, View.OnClickListener {

        TextView name;

        FolderItemViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.textView);
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
        }

        @Override
        public boolean onLongClick(View view) {

            File folder = folders.get(getAdapterPosition());

            if (!selectedFolder.add(folder)) {
                selectedFolder.remove(folder);
            }

            notifyItemChanged(getAdapterPosition());
            if (callback != null) {
                callback.onSelectedFolderChanged();
            }
            return false;
        }

        @Override
        public void onClick(View view) {
            if (callback != null) {
                callback.onEnter(folders.get(getAdapterPosition()));
            }
        }

    }

}
