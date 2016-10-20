package com.ocwvar.darkpurple.Adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.ocwvar.darkpurple.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Project DarkPurple
 * Created by 区成伟
 * On 2016/10/14 14:46
 * File Location com.ocwvar.darkpurple.Adapters
 */

public final class EqualizerSavedAdapter extends RecyclerView.Adapter {

    private List<String> names;
    private OnEqualizerItemClickCallback callback;

    public EqualizerSavedAdapter(OnEqualizerItemClickCallback callback) {
        this.names = new ArrayList<>();
        this.callback = callback;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == 0) {
            return new ItemOptionViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_equalizer_option, parent, false));
        } else {
            return new ItemViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_equalizer_saved, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (position > 0) {
            final String name = names.get(position - 1);
            ItemViewHolder viewHolder = (ItemViewHolder) holder;
            viewHolder.title.setText(name);
        }
    }

    @Override
    public int getItemCount() {
        return names.size() + 1;
    }

    public void putNames(HashMap<String, int[]> hashMap) {
        for (String s : hashMap.keySet()) {
            names.add(s);
        }
        notifyDataSetChanged();
    }

    public void putName(String name) {
        names.add(0, name);
        notifyItemInserted(0);
    }

    public void remove(String name) {
        final int position = names.indexOf(name);
        if (position >= 0) {
            names.remove(position);
            notifyItemRemoved(position);
        }
    }

    public interface OnEqualizerItemClickCallback {

        void onAddItemClick();

        void onItemClick(String name);

        void onItemRemoveClick(String name);

    }

    private final class ItemOptionViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        ItemOptionViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (callback != null) {
                callback.onAddItemClick();
            }
        }

    }

    private final class ItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        TextView title;
        ImageView delete;

        ItemViewHolder(View itemView) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.item_equalizer_saved);
            delete = (ImageView) itemView.findViewById(R.id.item_equalizer_remove);
            delete.setOnClickListener(this);
            title.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (callback != null) {
                switch (v.getId()) {
                    case R.id.item_equalizer_saved:
                        callback.onItemClick(names.get(getAdapterPosition() - 1));
                        break;
                    case R.id.item_equalizer_remove:
                        callback.onItemRemoveClick(names.get(getAdapterPosition() - 1));
                        break;
                }
            }
        }

    }

}
