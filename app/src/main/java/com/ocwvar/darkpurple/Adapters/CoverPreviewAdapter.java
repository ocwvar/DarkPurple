package com.ocwvar.darkpurple.Adapters;

import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.ocwvar.darkpurple.AppConfigs;
import com.ocwvar.darkpurple.Bean.CoverPreviewBean;
import com.ocwvar.darkpurple.R;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

/**
 * Project DarkPurple
 * Created by 区成伟
 * On 2016/9/15 10:54
 * File Location com.ocwvar.darkpurple.Adapters
 * 歌曲封面预览列表适配器
 */
public class CoverPreviewAdapter extends RecyclerView.Adapter {

    final ArrayList<CoverPreviewBean> list;
    final Drawable loadingRes;

    private OnPreviewClickCallback callback;

    public CoverPreviewAdapter() {
        list = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= 21) {
            loadingRes = AppConfigs.ApplicationContext.getDrawable(R.drawable.ic_picture_loading);
        } else {
            loadingRes = AppConfigs.ApplicationContext.getResources().getDrawable(R.drawable.ic_picture_loading);
        }
    }

    public void addDatas(ArrayList<CoverPreviewBean> source) {
        list.clear();
        if (source != null) {
            list.addAll(source);
        }
        notifyDataSetChanged();
    }

    public void setCallback(OnPreviewClickCallback callback) {
        this.callback = callback;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == 0){
            return new CoverReCoverViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recover_cover,parent,false));
        }else {
            return new CoverPreviewViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cover_preview, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (position != 0){
            CoverPreviewViewHolder viewHolder = (CoverPreviewViewHolder) holder;
            CoverPreviewBean bean = list.get(position-1);

            viewHolder.album.setText(bean.getAlbumName());
            viewHolder.cover.setImageDrawable(loadingRes);
            if (!TextUtils.isEmpty(bean.getArtworkUrl60())) {
                Picasso.with(AppConfigs.ApplicationContext).load(Uri.parse(bean.getArtworkUrl60())).resize(320, 320).into(viewHolder.cover);
            } else {
                Picasso.with(AppConfigs.ApplicationContext).load(Uri.parse(bean.getArtworkUrl100())).resize(200, 200).into(viewHolder.cover);
            }
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public interface OnPreviewClickCallback {

        void onRecoverCover();

        void onPreviewClick(CoverPreviewBean coverPreviewBean);

    }

    class CoverReCoverViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public CoverReCoverViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (callback != null){
                callback.onRecoverCover();
            }
        }
    }

    class CoverPreviewViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        ImageView cover;
        TextView album;

        public CoverPreviewViewHolder(View itemView) {
            super(itemView);
            cover = (ImageView) itemView.findViewById(R.id.textView_cover_preview_image);
            album = (TextView) itemView.findViewById(R.id.textView_cover_preview_name);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (callback != null) {
                callback.onPreviewClick(list.get(getAdapterPosition()-1));
            }
        }

    }

}
