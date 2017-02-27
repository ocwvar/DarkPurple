package com.ocwvar.picturePicker.Adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.ocwvar.darkpurple.R;
import com.ocwvar.picturePicker.Scanner;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

/**
 * Project PicturePicker
 * Created by 区成伟
 * On 2017/1/23 12:30
 * File Location com.ocwvar.picturepicker.Picker.BuildInGallery
 * 文件对象列表适配器
 */

public class FileObjectAdapter extends RecyclerView.Adapter {

    private final int COLOR_PNG;
    private final int COLOR_JPG;
    private final int COLOR_BMP;
    private final int COLOR_FOLDER;
    private ArrayList<Scanner.FileObject> fileObjects;
    private OnFileItemClickCallback callback;
    private boolean otherGalleryAvailable = true;
    private boolean cameraAvailable = true;

    @SuppressWarnings("deprecation")
    public FileObjectAdapter(OnFileItemClickCallback callback, Context context) {
        this.callback = callback;
        this.fileObjects = new ArrayList<>();
        this.COLOR_JPG = context.getResources().getColor(R.color.JPG);
        this.COLOR_PNG = context.getResources().getColor(R.color.PNG);
        this.COLOR_BMP = context.getResources().getColor(R.color.BMP);
        this.COLOR_FOLDER = context.getResources().getColor(R.color.FOLDER);
    }

    /**
     * 添加数据
     *
     * @param source 数据源
     */
    public void putSource(ArrayList<Scanner.FileObject> source) {
        fileObjects.clear();
        if (source != null && source.size() > 0) {
            fileObjects.addAll(source);
        }

        notifyDataSetChanged();
    }

    /**
     * 设置是否可以使用其他图库选项
     *
     * @param available 是否允许使用其他图库
     */
    public void setOptionOfOtherGallery(boolean available) {
        this.otherGalleryAvailable = available;
        notifyDataSetChanged();
    }

    /**
     * 设置是否可以使用相机选项
     *
     * @param available 是否允许使用相机拍照
     */
    public void setOptionOfCamera(boolean available) {
        this.cameraAvailable = available;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == 0) {
            return new OtherGalleryViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_option_other_gallery, parent, false));
        } else if (viewType == 1) {
            return new CameraViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_option_camera, parent, false));
        } else if (viewType == 2) {
            return new RecentViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_option_recent, parent, false));
        } else {
            return new FileObjectViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file_object, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof FileObjectViewHolder) {
            final FileObjectViewHolder viewHolder = (FileObjectViewHolder) holder;
            final Scanner.FileObject fileObject = fileObjects.get(position - 3);

            if (fileObject.isFolder()) {
                viewHolder.type.setText(R.string.FILE_TYPE_FOLDER);
            } else {
                viewHolder.type.setText(fileObject.getType());
            }

            viewHolder.name.setText(fileObject.getName());

            if (!fileObject.isFolder()) {
                //显示的是图像
                switch (fileObject.getType()) {
                    case "JPG":
                    case "JPEG":
                        viewHolder.typeBG.setBackgroundColor(COLOR_JPG);
                        break;
                    case "PNG":
                        viewHolder.typeBG.setBackgroundColor(COLOR_PNG);
                        break;
                    case "BMP":
                        viewHolder.typeBG.setBackgroundColor(COLOR_BMP);
                        break;
                }

                Picasso.with(viewHolder.itemView.getContext())
                        .load("file://" + fileObject.getPath())
                        .config(Bitmap.Config.RGB_565)
                        .resize(200, 200)
                        .centerCrop()
                        .placeholder(R.drawable.ic_action_image_loading)
                        .error(R.drawable.ic_action_image_failed)
                        .into(viewHolder.cover);
            } else {
                //显示的目录
                viewHolder.cover.setImageResource(R.drawable.ic_folder);
                viewHolder.typeBG.setBackgroundColor(COLOR_FOLDER);
            }

        } else if (holder instanceof OtherGalleryViewHolder) {
            final OtherGalleryViewHolder viewHolder = (OtherGalleryViewHolder) holder;
            if (!otherGalleryAvailable) {
                viewHolder.itemView.setAlpha(0.5f);
            } else {
                viewHolder.itemView.setAlpha(1f);
            }
        } else if (holder instanceof CameraViewHolder) {
            final CameraViewHolder viewHolder = (CameraViewHolder) holder;
            if (!cameraAvailable) {
                viewHolder.itemView.setAlpha(0.5f);
            } else {
                viewHolder.itemView.setAlpha(1f);
            }
        }
    }

    @Override
    public int getItemCount() {
        return fileObjects.size() + 3;
    }

    /**
     * 选项操作类型
     */
    public enum OptionTypes {
        使用其他图库,
        使用相机,
        最近图像
    }

    public interface OnFileItemClickCallback {

        void onFolderClick(Scanner.FileObject fileObject);

        void onFileClick(Scanner.FileObject fileObject);

        void onOptionClick(OptionTypes optionType);

    }

    private class FileObjectViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        final TextView name;
        final TextView type;
        final View typeBG;
        final ImageView cover;

        FileObjectViewHolder(View itemView) {
            super(itemView);

            name = (TextView) itemView.findViewById(R.id.item_file_name);
            type = (TextView) itemView.findViewById(R.id.item_file_type);
            cover = (ImageView) itemView.findViewById(R.id.item_file_cover);
            typeBG = itemView.findViewById(R.id.item_file_type_color);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (callback == null) {
                return;
            }

            final Scanner.FileObject fileObject = fileObjects.get(getAdapterPosition() - 3);
            if (fileObject.isFolder()) {
                callback.onFolderClick(fileObject);
            } else {
                callback.onFileClick(fileObject);
            }

        }
    }

    private class OtherGalleryViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        OtherGalleryViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (callback == null || !otherGalleryAvailable) {
                return;
            }

            callback.onOptionClick(OptionTypes.使用其他图库);
        }
    }

    private class CameraViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        CameraViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (callback == null || !cameraAvailable) {
                return;
            }

            callback.onOptionClick(OptionTypes.使用相机);
        }
    }

    private class RecentViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        RecentViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (callback == null || !cameraAvailable) {
                return;
            }

            callback.onOptionClick(OptionTypes.最近图像);
        }
    }

}
