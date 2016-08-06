package com.ocwvar.darkpurple.Adapters;

import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ocwvar.darkpurple.AppConfigs;
import com.ocwvar.darkpurple.R;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by 区成伟
 * Package: com.ocwvar.darkpurple.Adapters
 * Data: 2016/8/2 14:46
 * Project: DarkPurple
 * 歌曲文件夹列表适配器
 */
public class MusicFolderAdapter extends RecyclerView.Adapter {

    private ArrayList<String> paths;
    private OnPathChangedCallback callback;

    public MusicFolderAdapter(OnPathChangedCallback callback) {
        paths = new ArrayList<>();
        if (AppConfigs.MusicFolders != null){
            Collections.addAll(paths, AppConfigs.MusicFolders);
        }
        this.callback = callback;
    }

    public ArrayList<String> getPaths() {
        return paths;
    }

    /**
     * 添加一条数据到头部
     * @param path  路径数据
     */
    public void addPath(String path){
        if (!TextUtils.isEmpty(path)){
            if (path.length() > 1 ){
                //如果路径长度大于 1 则排除了 " / "目录的状态 , 我们只需要把字符串最后的 "/" 去掉即可 , 如果不存在则不操作
                char[] chars = path.toCharArray();
                if (chars[chars.length-1] == '/' ){
                    path = path.substring(0,path.length()-1);
                    chars = null;
                }
            }

            if (paths.contains(path)){
                //如果已经添加过的则放弃
                callback.onAddedFailed();
                return;
            }

            paths.add( 0 , path );
            notifyItemInserted(0);
            callback.onAddedPath();
        }else {
            callback.onAddedFailed();
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new PathViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_path,parent,false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        PathViewHolder pathViewHolder = (PathViewHolder) holder;
        String path = paths.get(position);
        pathViewHolder.textView.setText(path);
    }

    @Override
    public int getItemCount() {
        return paths.size();
    }

    class PathViewHolder extends RecyclerView.ViewHolder implements View.OnLongClickListener {

        TextView textView;

        public PathViewHolder(View itemView) {
            super(itemView);
            itemView.setOnLongClickListener(this);
            textView = (TextView)itemView.findViewById(R.id.textView);
        }

        @Override
        public boolean onLongClick(View view) {
            paths.remove(getAdapterPosition());
            notifyItemRemoved(getAdapterPosition());
            callback.onRemovedPath();
            return false;
        }
    }

    public interface OnPathChangedCallback{

        void onRemovedPath();

        void onAddedPath();

        void onAddedFailed();
    }

}
