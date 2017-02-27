package com.ocwvar.picturePicker;

import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.ocwvar.darkpurple.R;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Project PicturePicker
 * Created by 区成伟
 * On 2017/1/23 11:11
 * File Location com.ocwvar.picturepicker.Picker.BuildInGallery
 * 对象扫描器
 */

public class Scanner {

    private ScannerResultCallback callback;

    public Scanner(ScannerResultCallback callback) {
        this.callback = callback;
    }

    /**
     * 搜索文件数据
     *
     * @param folderPath 路径
     * @param context    可用的Context
     */
    public void scanFiles(String folderPath, Context context) {
        if (TextUtils.isEmpty(folderPath)) {
            return;
        }

        if (folderPath.equals("recent")) {
            //最近图像
            new ScanRecentImages(callback, context).execute();
        } else if (folderPath.equals("main")) {
            //根目录，返回内存卡和内置储存路径
            final ArrayList<FileObject> fileObjects = new ArrayList<>();
            final FileObject sdCardPath = new FileObject();
            sdCardPath.setFolder(true);
            sdCardPath.setPath(Environment.getExternalStorageDirectory().getPath());
            sdCardPath.setName(context.getString(R.string.EXStorage));
            fileObjects.add(sdCardPath);
            callback.onScanCompleted(fileObjects, context.getString(R.string.MAIN));
        } else {
            //文件图像
            new ScanFilesImages(folderPath, callback, context).execute();
        }
    }

    /**
     * 扫描结果回调接口
     */
    public interface ScannerResultCallback {

        /**
         * 获取到扫描结果
         *
         * @param fileObjects  文件对象列表
         * @param currentLevel 当前的目录
         */
        void onScanCompleted(@NonNull ArrayList<FileObject> fileObjects, String currentLevel);

        /**
         * 扫描失败
         */
        void onScanFailed();

    }

    /**
     * 文件对象
     */
    public class FileObject {

        private boolean isFolder;
        private String path;
        private String name;
        private String type;
        private String length;

        public boolean isFolder() {
            return isFolder;
        }

        private void setFolder(boolean folder) {
            isFolder = folder;
        }

        public String getPath() {
            return path;
        }

        private void setPath(String path) {
            this.path = path;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        private void setType(String type) {
            this.type = type;
        }

        public String getLength() {
            return length;
        }

        private void setLength(String length) {
            this.length = length;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            FileObject that = (FileObject) o;

            return path.equals(that.path);

        }

        @Override
        public int hashCode() {
            return path.hashCode();
        }
    }

    /**
     * 近期图像扫描器
     */
    private class ScanRecentImages extends AsyncTask<Integer, Void, ArrayList<FileObject>> {

        private ProgressDialog progressDialog;
        private ScannerResultCallback callback;
        private Context context;

        ScanRecentImages(@NonNull ScannerResultCallback callback, @NonNull Context context) {
            this.callback = callback;
            this.context = context;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(context);
            progressDialog.setMessage(context.getString(R.string.LOADING_SCANNING_RECENT));
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        protected ArrayList<FileObject> doInBackground(Integer... params) {
            try {
                return scanRecentCore(context, 30);
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        protected void onPostExecute(ArrayList<FileObject> fileObjects) {
            super.onPostExecute(fileObjects);
            progressDialog.dismiss();
            if (fileObjects == null) {
                callback.onScanFailed();
            } else {
                callback.onScanCompleted(fileObjects, context.getString(R.string.RECENT));
            }
        }

        /**
         * 搜索最近图像核心
         *
         * @param context    可使用的Context
         * @param countLimit 获取的最大数量
         * @return 获取成功则返回图像文件列表，如果获取失败，则返回NULL
         */
        private
        @Nullable
        ArrayList<FileObject> scanRecentCore(@NonNull Context context, int countLimit) {
            final Cursor cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null, null, null, MediaStore.Images.Media.DATE_TAKEN + " DESC");
            if (cursor != null && cursor.getCount() > 0) {
                final ArrayList<FileObject> fileObjects = new ArrayList<>();
                while (cursor.moveToNext() && fileObjects.size() <= countLimit) {
                    //文件名称
                    final String name = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME));
                    //文件大小
                    final String size = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.SIZE));
                    //文件路径
                    final String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                    //从文件路径中获取文件类型（可能为无后缀，则返回NULL）
                    String type;
                    try {
                        final String[] strings = path.split("\\.");
                        type = strings[strings.length - 1].toUpperCase();
                    } catch (Exception e) {
                        type = null;
                    }

                    final FileObject fileObject = new FileObject();
                    fileObject.setFolder(false);
                    fileObject.setLength(size);
                    fileObject.setName(name);
                    fileObject.setPath(path);
                    fileObject.setType(type);

                    fileObjects.add(fileObject);
                }

                cursor.close();
                return fileObjects;
            } else if (cursor != null && cursor.getCount() == 0) {
                //如果数据库中没有图像，则返回空列表对象
                cursor.close();
                return new ArrayList<>();
            } else {
                //无法得到Cursor对象
                return null;
            }
        }

    }

    /**
     * 文件图像扫描器
     */
    private class ScanFilesImages extends AsyncTask<Integer, Void, ArrayList<FileObject>> {

        private ProgressDialog progressDialog;
        private ScannerResultCallback callback;
        private String folderPath;
        private Context context;

        ScanFilesImages(@NonNull String folderPath, @NonNull ScannerResultCallback callback, @NonNull Context context) {
            this.callback = callback;
            this.context = context;
            this.folderPath = folderPath;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(context);
            progressDialog.setMessage(context.getString(R.string.LOADING_SCANNING_FILES));
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        protected ArrayList<FileObject> doInBackground(Integer... params) {
            try {
                return scanFilesCore(folderPath, context);
            } catch (Exception e) {
                return null;
            }
        }

        /**
         * 搜索文件操作核心
         *
         * @param folderPath 文件夹路径
         * @param context    可用的Context
         * @return 获取成功则返回图像文件列表，如果获取失败，则返回NULL
         */
        private
        @Nullable
        ArrayList<FileObject> scanFilesCore(@NonNull String folderPath, @NonNull Context context) {
            if (folderPath.charAt(folderPath.length() - 1) != '/') {
                //将路径合法化
                folderPath = folderPath + "/";
            }

            final ArrayList<FileObject> fileObjects = new ArrayList<>();

            /**
             * 获取文件集合
             */
            final File[] files = new File(folderPath).listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return pathname.isDirectory() || isImageFile(pathname.getName());
                }
            });

            for (File file : files) {
                final FileObject fileObject = new FileObject();
                fileObject.setLength(String.valueOf(file.length()));
                fileObject.setFolder(file.isDirectory());
                fileObject.setName(file.getName());
                fileObject.setPath(file.getPath());
                if (!fileObject.isFolder()) {
                    fileObject.setType(getFileTypeName(fileObject.getName()));
                }
                fileObjects.add(fileObject);
            }

            Collections.sort(fileObjects, new Comparator<FileObject>() {
                @Override
                public int compare(FileObject o1, FileObject o2) {
                    if (o1.isFolder() && o2.isFolder()) {
                        return o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase());
                    } else if (o1.isFolder() && !o2.isFolder()) {
                        return -1;
                    } else {
                        return 1;
                    }
                }
            });

            return fileObjects;
        }

        /**
         * 通过文件名后缀检查是否为图像文件
         *
         * @param fileName 文件名称
         * @return 是否为图像文件
         */
        private boolean isImageFile(String fileName) {
            switch (getFileTypeName(fileName)) {
                case "PNG":
                case "JPG":
                case "JPEG":
                case "BMP":
                    return true;
                default:
                    return false;
            }
        }

        /**
         * 获取文件后缀名
         *
         * @param fileName 文件名称
         * @return 后缀名，无法解析返回 "" 字符串
         */
        private String getFileTypeName(String fileName) {
            try {
                final String[] strings = fileName.split("\\.");
                return strings[strings.length - 1].toUpperCase();
            } catch (Exception e) {
                return "";
            }
        }

        @Override
        protected void onPostExecute(ArrayList<FileObject> fileObjects) {
            super.onPostExecute(fileObjects);
            progressDialog.dismiss();

            if (fileObjects == null) {
                callback.onScanFailed();
            } else {
                callback.onScanCompleted(fileObjects, folderPath);
            }
        }
    }

}
