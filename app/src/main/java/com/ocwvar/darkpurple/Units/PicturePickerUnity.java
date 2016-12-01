package com.ocwvar.darkpurple.Units;

import android.Manifest;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.ocwvar.darkpurple.R;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Random;

/**
 * Project AnHe_Business
 * Created by 区成伟
 * On 2016/11/8 16:40
 * File Location com.ocwvar.anhe_business.Units
 * 图像选择工具
 */

public class PicturePickerUnity extends AppCompatActivity implements View.OnClickListener {

    /**
     * 请求参数字段
     * <p>
     * Keys of request parameter
     */
    //是否需要裁剪
    public static final String ARG_NEED_CROP = "ac1";
    //是否需要压缩
    public static final String ARG_NEED_COMPRESS = "ac2";
    //只返回文件对象
    public static final String ARG_RETURN_FILE_ONLY = "ac3";
    //只返回位图对象
    public static final String ARG_RETURN_BITMAP_ONLY = "ac4";
    //返回位图和文件
    public static final String ARG_RETURN_BOTH = "ac5";
    //压缩比例
    public static final String ARG_COMPRESS_VALUE = "ac6";
    //裁剪宽度
    public static final String ARG_CROP_WIDTH = "ac7";
    //裁剪高度
    public static final String ARG_CROP_HEIGHT = "ac8";
    /**
     * 结果参数字段
     * <p>
     * keys of result parameter
     */
    //获取位图对象
    public static final String EXTRAS_BITMAP = "rs1";
    //获取图像文件对象
    public static final String EXTRAS_FILE = "rs2";
    //获取异常消息结果
    public static final String EXTRAS_EXCEPTION = "rs3";
    /**
     * 结果Action
     * <p>
     * Action in the result intent
     */
    public static final String ACTION_SUCCESS = "a1";
    public static final String ACTION_FAILED = "a2";
    /**
     * 内部请求码
     * <p>
     * Inner request code
     */
    private final int REQUEST_CAMERA = 301;
    private final int REQUEST_LOCAL = 302;
    private final int REQUEST_CUT = 303;
    /**
     * 临时使用的路径    必须保持有效性
     * <p>
     * Temporary path   MUST BE AVAILABLE
     */
    private final String TEMPSAVE_PATH = Environment.getExternalStorageDirectory().getPath() + "/temp.jpg";
    private final String TEMPSAVE_PATH2 = Environment.getExternalStorageDirectory().getPath() + "/temp2.jpg";
    /**
     * 异常消息文字
     * <p>
     * Text of exception
     */
    String ERROR_TEXT_COMPRESS_FILED;
    String ERROR_TEXT_FILE_FILED;
    String ERROR_TEXT_FILE_SAVE_FAILED;
    String ERROR_TEXT_PIC_INCURRECT;
    String ERROR_TEXT_BITMAP_TOOLAGER;
    String ERROR_TEXT_CAMERA_NO_DATA;
    String ERROR_TEXT_LOCAL_NO_DATA;
    String ERROR_TEXT_ARG;
    String ERROR_TEXT_UNKNOWN;
    /**
     * 显示的两个按钮
     */
    TextView fromLocal, fromCamera;
    /**
     * 操作参数
     * <p>
     * Operating parameter
     */
    private int CROP_WIDTH = 200;
    private int CROP_HEIGHT = 200;
    private int CROP_WIDTH_RATION = 1;
    private int CROP_HEIGHT_RATION = 1;
    private int COMPRESS_VALUE = 50;
    private boolean NEED_CROP = false;
    private boolean NEED_COMPRESS = false;
    private boolean RETURN_FILE_ONLY = false;
    private boolean RETURN_BITMAP_ONLY = false;
    private boolean RETURN_BOTH = false;

    /**
     * 图像文件保存路径
     * <p>
     * The folder of file objects
     */
    private String SAVE_PATH = Environment.getExternalStorageDirectory().getPath() + "/Picker/";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent() == null || getIntent().getExtras() == null) {
            onFinalStep(null, null, ERROR_TEXT_ARG);
            return;
        } else {
            setTitle(null);
            final Bundle request = getIntent().getExtras();
            CROP_WIDTH = request.getInt(ARG_CROP_WIDTH, 0);
            CROP_HEIGHT = request.getInt(ARG_CROP_HEIGHT, 0);
            COMPRESS_VALUE = request.getInt(ARG_COMPRESS_VALUE, 50);
            NEED_CROP = request.getBoolean(ARG_NEED_CROP, false);
            NEED_COMPRESS = request.getBoolean(ARG_NEED_COMPRESS, false);
            RETURN_BITMAP_ONLY = request.getBoolean(ARG_RETURN_BITMAP_ONLY, false);
            RETURN_FILE_ONLY = request.getBoolean(ARG_RETURN_FILE_ONLY, false);
            RETURN_BOTH = request.getBoolean(ARG_RETURN_BOTH, false);
        }

        setContentView(R.layout.picture_select_unity);

        ERROR_TEXT_COMPRESS_FILED = getString(R.string.ERROR_TEXT_COMPRESS_FILED);
        ERROR_TEXT_FILE_FILED = getString(R.string.ERROR_TEXT_FILE_FILED);
        ERROR_TEXT_FILE_SAVE_FAILED = getString(R.string.ERROR_TEXT_FILE_SAVE_FAILED);
        ERROR_TEXT_PIC_INCURRECT = getString(R.string.ERROR_TEXT_PIC_INCURRECT);
        ERROR_TEXT_BITMAP_TOOLAGER = getString(R.string.ERROR_TEXT_BITMAP_TOOLAGER);
        ERROR_TEXT_CAMERA_NO_DATA = getString(R.string.ERROR_TEXT_CAMERA_NO_DATA);
        ERROR_TEXT_LOCAL_NO_DATA = getString(R.string.ERROR_TEXT_LOCAL_NO_DATA);
        ERROR_TEXT_ARG = getString(R.string.ERROR_TEXT_ARG);
        ERROR_TEXT_UNKNOWN = getString(R.string.ERROR_TEXT_UNKNOWN);

        fromLocal = (TextView) findViewById(R.id.picture_selector_fromLocal);
        fromCamera = (TextView) findViewById(R.id.picture_selector_fromCamera);

        fromLocal.setOnClickListener(this);
        fromCamera.setOnClickListener(this);

        final int maxCommonDivisor = maxCommonDivisor(CROP_WIDTH, CROP_HEIGHT);
        CROP_WIDTH_RATION = CROP_WIDTH / maxCommonDivisor;
        CROP_HEIGHT_RATION = CROP_HEIGHT / maxCommonDivisor;

    }

    private int maxCommonDivisor(int width, int height) {
        if (width < height) {// 保证m>n,若m<n,则进行数据交换
            int temp = width;
            width = height;
            height = temp;
        }
        if (width % height == 0) {// 若余数为0,返回最大公约数
            return height;
        } else { // 否则,进行递归,把n赋给m,把余数赋给n
            return maxCommonDivisor(height, width % height);
        }
    }

    @Override
    public void onClick(View clickedView) {
        switch (clickedView.getId()) {
            case R.id.picture_selector_fromCamera:
                requestPickFromCamera();
                break;
            case R.id.picture_selector_fromLocal:
                requestPickFromLocal();
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_LOCAL:
                handlePickFromLocal(data);
                break;
            case REQUEST_CUT:
                handleCropFromPic(resultCode, data);
                break;
            case REQUEST_CAMERA:
                handlePickFromCamera();
                break;
            default:
                onFinalStep(null, null, ERROR_TEXT_UNKNOWN);
                break;
        }

    }

    /**
     * 最终获取到图像的时候
     *
     * @param bitmap 得到的Bitmap
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void onFinalStep(Bitmap bitmap, File file, String exceptionMessage) {

        final Intent result = new Intent();

        //设置返回的Action 是成功或者失败状态
        if (!TextUtils.isEmpty(exceptionMessage)) {
            result.setAction(ACTION_FAILED);
        } else {
            result.setAction(ACTION_SUCCESS);
        }

        //当Bitmap对象大于1Mb的时候不能使用Intent来传递
        if (bitmap != null && bitmap.getByteCount() < 1024 * 1024) {
            result.putExtra(EXTRAS_BITMAP, bitmap);
        } else if (bitmap != null) {
            //不能传递的时候传递错误信息以及回收位图对象
            if (TextUtils.isEmpty(exceptionMessage)) {
                exceptionMessage = ERROR_TEXT_BITMAP_TOOLAGER;
            }
            bitmap.recycle();
        }

        //传递文件对象
        result.putExtra(EXTRAS_FILE, file);
        //传递错误信息
        result.putExtra(EXTRAS_EXCEPTION, exceptionMessage);
        //设置返回Intent
        setResult(100, result);

        //清除临时文件
        new File(TEMPSAVE_PATH).delete();
        new File(TEMPSAVE_PATH2).delete();

        //结束页面,完成操作
        finish();
    }

    /**
     * 获取随机文件名
     *
     * @return 文件名称
     */
    private String getRandomFileName() {
        final String headText = "OCPicture_";
        final Random random = new Random(System.currentTimeMillis());
        return headText + String.valueOf(random.nextInt()) + String.valueOf(random.nextInt() * random.nextInt(3)) + ".jpg";
    }

    /**
     * 启动摄像头获取图像
     */
    private void requestPickFromCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(TEMPSAVE_PATH)));
        startActivityForResult(intent, REQUEST_CAMERA);
    }

    /**
     * 处理拍照后得到的数据
     */
    private void handlePickFromCamera() {
        /**
         * 拍照后得到照片对象会保存在我们请求中的指定路径内 TEMPSAVE_PATH .
         * 如果设置了带数据返回 "return-data" 则会在Intent中返回一个Bitmap对象
         * 但一般非常不建议这么使用 , 因为一般这个Bitmap对象会很大
         */

        File savedFile = new File(TEMPSAVE_PATH);

        if (savedFile.exists() && savedFile.length() > 0) {
            //如果文件是有效的

            if (NEED_CROP) {
                //如果需要剪裁,则直接调到剪裁部分
                cropImageFromURI(Uri.fromFile(savedFile));
            } else {
                //如果不需要剪裁,则直接调到最后部分
                handleCompressAndSave(savedFile, null);
            }

        } else {
            //文件无效 , 操作失败
            onFinalStep(null, null, ERROR_TEXT_CAMERA_NO_DATA);
        }

    }

    /**
     * 启动图片选取界面
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void requestPickFromLocal() {
        File tempFile = new File(TEMPSAVE_PATH);
        try {
            tempFile.createNewFile();
            Intent intent = new Intent(Intent.ACTION_PICK, null);
            intent.setType("image/*");
            if (NEED_CROP) {
                intent.putExtra("crop", "true");
                if (CROP_HEIGHT * CROP_WIDTH > 0) {
                    intent.putExtra("aspectX", CROP_WIDTH_RATION);
                    intent.putExtra("aspectY", CROP_HEIGHT_RATION);
                    intent.putExtra("outputX", CROP_WIDTH);
                    intent.putExtra("outputY", CROP_HEIGHT);
                }
            } else {
                intent.putExtra("crop", "false");
            }
            intent.putExtra("return-data", true);
            intent.putExtra("scale", true);
            intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
            intent.putExtra("noFaceDetection", true);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(tempFile));
            startActivityForResult(intent, REQUEST_LOCAL);
        } catch (Exception e) {
            tempFile = null;
            onFinalStep(null, null, ERROR_TEXT_ARG);
        }
    }

    /**
     * 照相后的图像剪裁
     *
     * @param uri 图像Uri
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void cropImageFromURI(Uri uri) {
        File tempFile = new File(TEMPSAVE_PATH2);
        try {
            tempFile.createNewFile();
            Intent intent = new Intent("com.android.camera.action.CROP");
            intent.setDataAndType(uri, "image/*");
            if (NEED_CROP) {
                intent.putExtra("crop", "true");
                if (CROP_HEIGHT * CROP_WIDTH > 0) {
                    intent.putExtra("aspectX", CROP_WIDTH_RATION);
                    intent.putExtra("aspectY", CROP_HEIGHT_RATION);
                    intent.putExtra("outputX", CROP_WIDTH);
                    intent.putExtra("outputY", CROP_HEIGHT);
                }
            } else {
                intent.putExtra("crop", "false");
            }
            intent.putExtra("return-data", true);
            intent.putExtra("scale", true);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(tempFile));
            startActivityForResult(intent, REQUEST_CUT);
        } catch (Exception e) {
            tempFile = null;
            onFinalStep(null, null, ERROR_TEXT_PIC_INCURRECT);
        }
    }

    /**
     * 处理本地选取图片后的结果
     *
     * @param intent 返回的结果 Intent
     */
    private void handlePickFromLocal(Intent intent) {
        if (intent == null) {
            onFinalStep(null, null, ERROR_TEXT_LOCAL_NO_DATA);
        } else if (intent.getData() != null) {
            //从图库有返回一个 Uri 路径

            //解析出本地路径
            final PickUriUtils pickUriUtils = new PickUriUtils();
            final String resultPath = pickUriUtils.getPath(this, intent.getData());

            //最后进行储存
            handleCompressAndSave(new File(resultPath), null);

        } else if (intent.hasExtra("data")) {
            //从图库中返回一个 Bitmap 位图对象
            /**
             * 这个方式虽然直接 , 但是对内存使用并不友好 , 有可能得到的 Bitmap 对象很大
             * 但是有的图像软件裁剪后不会返回 Uri 数据 , 只会返回剪裁后的 Bitmap 对象
             *
             * 比如: 快图浏览
             */

            handleCompressAndSave(null, (Bitmap) intent.getParcelableExtra("data"));
        } else if (new File(TEMPSAVE_PATH).exists()) {
            //没有数据返回 , 检查请求文件路径
            /**
             * 有可能虽然Intent没有返回路径 , 但是可能会已经把我们要的图像处理完成后存放到我们当初指定的位置内了
             * 有的手机内置图库不会返回路径 同时也不会返回Bitmap数据 , 即使设置了 "return-data", true
             *
             * 比如: 华为荣耀 SCL-CL00 , OS: EMUI 3.1
             *
             */
            handleCompressAndSave(new File(TEMPSAVE_PATH), null);
        } else {
            onFinalStep(null, null, ERROR_TEXT_LOCAL_NO_DATA);
        }
    }

    /**
     * 处理剪裁后的数据
     *
     * @param resultCode 返回来的 resultCode
     * @param intent     返回来的 intent
     */
    private void handleCropFromPic(int resultCode, Intent intent) {
        if (resultCode == 0) {
            onFinalStep(null, null, ERROR_TEXT_LOCAL_NO_DATA);
        } else if (!intent.hasExtra("data") && intent.getData() != null) {
            //返回来的是 Uri 路径 , 解析后直接加载即可 , 其文件路径为我们在请求Intent中设定的路径 TEMPSAVE_PATH2
            final Bitmap bitmap = BitmapFactory.decodeFile(TEMPSAVE_PATH2);
            if (bitmap != null) {
                handleCompressAndSave(null, bitmap);
            } else {
                onFinalStep(null, null, ERROR_TEXT_LOCAL_NO_DATA);
            }
        } else if (intent.hasExtra("data")) {
            handleCompressAndSave(null, (Bitmap) intent.getParcelableExtra("data"));
        } else {
            onFinalStep(null, null, ERROR_TEXT_LOCAL_NO_DATA);
        }
    }

    /**
     * 处理压缩图片并储存到本地
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void handleCompressAndSave(File handleFile, Bitmap handleBitmap) {
        final Bitmap bitmap;

        if (handleFile != null) {
            bitmap = BitmapFactory.decodeFile(handleFile.getPath());
        } else if (handleBitmap != null) {
            bitmap = handleBitmap;
        } else {
            bitmap = null;
        }

        if (bitmap != null) {
            //如果能获取到图像,则代表这个对象是可用的

            //储存最终文件的位置
            File saveFile = new File(SAVE_PATH);
            saveFile.mkdirs();
            saveFile = new File(SAVE_PATH + getRandomFileName());

            if (NEED_COMPRESS) {
                //如果需要压缩
                if (COMPRESS_VALUE > 100) COMPRESS_VALUE = 100;   //修正不合法的压缩值
                else if (COMPRESS_VALUE < 0) COMPRESS_VALUE = 0;

                try {
                    //创建新文件
                    saveFile.createNewFile();
                    //创建输出流
                    final FileOutputStream fileOutputStream = new FileOutputStream(saveFile, false);
                    //压缩并储存到本地
                    if (bitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESS_VALUE, fileOutputStream)) {
                        //压缩储存成功的状态

                        //回收旧的位图对象
                        bitmap.recycle();

                        if (RETURN_FILE_ONLY) {
                            //如果仅需要返回文件对象,则直接返回保存的文件对象

                            onFinalStep(null, saveFile, null);
                        } else if (RETURN_BITMAP_ONLY) {
                            //如果仅需要返回位图对象,则需要把储存的对象删除
                            onFinalStep(BitmapFactory.decodeFile(saveFile.getPath()), null, null);
                            saveFile.delete();
                        } else if (RETURN_BOTH) {
                            //都返回的情况下
                            onFinalStep(BitmapFactory.decodeFile(saveFile.getPath()), saveFile, null);
                        }
                    } else {
                        onFinalStep(null, null, ERROR_TEXT_COMPRESS_FILED);
                    }

                } catch (Exception e) {
                    //产生异常则全部返回 null
                    bitmap.recycle();
                    onFinalStep(null, null, ERROR_TEXT_FILE_FILED);
                }

            } else {
                //不需要压缩
                try {

                    if (RETURN_BITMAP_ONLY) {
                        //仅返回位图对象
                        onFinalStep(bitmap, null, null);
                    } else {
                        //否则就是需要图像文件的 位图+文件 或 仅文件

                        //创建新文件
                        saveFile.createNewFile();
                        //创建输出流
                        final FileOutputStream fileOutputStream = new FileOutputStream(saveFile, false);

                        if (bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)) {
                            //保存成功
                            if (RETURN_FILE_ONLY) {
                                bitmap.recycle();
                                onFinalStep(null, saveFile, null);
                            } else {
                                onFinalStep(bitmap, saveFile, null);
                            }
                        } else {
                            bitmap.recycle();
                            onFinalStep(null, null, ERROR_TEXT_FILE_SAVE_FAILED);
                        }
                    }

                } catch (Exception e) {
                    //产生异常则全部返回 null
                    bitmap.recycle();
                    onFinalStep(null, null, ERROR_TEXT_FILE_FILED);
                }

            }
        } else {
            onFinalStep(null, null, ERROR_TEXT_PIC_INCURRECT);
        }
    }

    /**
     * 图像选择器的请求构建器
     */
    public static class Builder {

        private boolean arg_needCrop = false;
        private boolean arg_needCompress = false;

        private boolean arg_returnFile = true;
        private boolean arg_returnBitmap = false;
        private boolean arg_returnBoth = false;

        private int arg_compressValue = 60;
        private int arg_cropWidth = 200;
        private int arg_cropHeight = 200;

        /**
         * @param needCrop 是否需要剪裁
         * @return 构建器本身
         */
        public Builder needCrop(boolean needCrop) {
            arg_needCrop = needCrop;
            return this;
        }

        /**
         * @param needCompress 是否需要图像压缩    0(最差画质) ~ 100(最好画质)
         * @return 构建器本身
         */
        public Builder needCompress(boolean needCompress) {
            arg_needCompress = needCompress;
            return this;
        }

        /**
         * @param returnFile 是否只返回图像文件
         * @return 构建器本身
         */
        public Builder returnFile(boolean returnFile) {
            arg_returnFile = returnFile;
            return this;
        }

        /**
         * @param returnBitmap 是否只返回图像位图
         * @return 构建器本身
         */
        public Builder returnBitmap(boolean returnBitmap) {
            arg_returnBitmap = returnBitmap;
            return this;
        }

        /**
         * @param returnBoth 是否同时返回图像位图与文件
         * @return 构建器本身
         */
        public Builder returnBoth(boolean returnBoth) {
            arg_returnBoth = returnBoth;
            return this;
        }

        /**
         * @param compressValue 图像压缩程度
         * @return 构建器本身
         */
        public Builder setCompressValue(int compressValue) {
            arg_compressValue = compressValue;
            return this;
        }

        /**
         * @param cropWidth 剪裁宽度
         * @return 构建器本身
         */
        public Builder setCropWidth(int cropWidth) {
            arg_cropWidth = cropWidth;
            return this;
        }

        /**
         * @param cropHeight 剪裁高度
         * @return 构建器本身
         */
        public Builder setCropHeight(int cropHeight) {
            arg_cropHeight = cropHeight;
            return this;
        }

        /**
         * 启动图像选择器
         *
         * @param activity              context 对象
         * @param requestCode           请求码
         * @param permissionRequestCode 权限请求码
         */
        public void startPickerNow(Activity activity, int requestCode, int permissionRequestCode) {

            if (Build.VERSION.SDK_INT >= 23) {

                if (activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED || activity.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {

                    //如果系统版本为 Android 6.0+，同时应用的权限不完整，则一次性全部获取
                    activity.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA}, permissionRequestCode);
                    Toast.makeText(activity, R.string.ERROR_TEXT_PERMISSION, Toast.LENGTH_SHORT).show();
                    return;

                }

            }

            if (arg_returnBitmap && arg_returnFile) {
                arg_returnBoth = true;
                arg_returnBitmap = false;
                arg_returnFile = false;
            }

            if (arg_compressValue > 100) arg_compressValue = 100;
            if (arg_compressValue < 0) arg_compressValue = 0;
            if (arg_cropHeight <= 0) arg_cropHeight = 200;
            if (arg_cropWidth <= 0) arg_cropWidth = 200;

            final Intent request = new Intent(activity, PicturePickerUnity.class);
            request.putExtra(ARG_NEED_CROP, arg_needCrop);
            request.putExtra(ARG_NEED_COMPRESS, arg_needCompress);

            request.putExtra(ARG_RETURN_FILE_ONLY, arg_returnFile);
            request.putExtra(ARG_RETURN_BITMAP_ONLY, arg_returnBitmap);
            request.putExtra(ARG_RETURN_BOTH, arg_returnBoth);

            request.putExtra(ARG_COMPRESS_VALUE, arg_compressValue);
            request.putExtra(ARG_CROP_WIDTH, arg_cropWidth);
            request.putExtra(ARG_CROP_HEIGHT, arg_cropHeight);

            resetDefaultValues();

            activity.startActivityForResult(request, requestCode);

        }

        /**
         * 请求之前恢复默认的参数
         */
        private void resetDefaultValues() {
            arg_needCrop = false;
            arg_needCompress = false;

            arg_returnFile = true;
            arg_returnBitmap = false;
            arg_returnBoth = false;

            arg_compressValue = 60;
            arg_cropWidth = 200;
            arg_cropHeight = 200;
        }

    }

    private class PickUriUtils {

        /**
         * Get a file path from a Uri. This will get the the path for Storage Access
         * Framework Documents, as well as the _data field for the MediaStore and
         * other file-based ContentProviders.
         *
         * @param context The context.
         * @param uri     The Uri to query.
         * @author paulburke
         */
        String getPath(final Context context, final Uri uri) {

            // DocumentProvider
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && DocumentsContract.isDocumentUri(context, uri)) {
                // ExternalStorageProvider
                if (isExternalStorageDocument(uri)) {
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(":");
                    final String type = split[0];

                    if ("primary".equalsIgnoreCase(type)) {
                        return Environment.getExternalStorageDirectory() + "/" + split[1];
                    }

                    // TODO handle non-primary volumes
                }
                // DownloadsProvider
                else if (isDownloadsDocument(uri)) {

                    final String id = DocumentsContract.getDocumentId(uri);
                    final Uri contentUri = ContentUris.withAppendedId(
                            Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                    return getDataColumn(context, contentUri, null, null);
                }
                // MediaProvider
                else if (isMediaDocument(uri)) {
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(":");
                    final String type = split[0];

                    Uri contentUri = null;
                    if ("image".equals(type)) {
                        contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                    } else if ("video".equals(type)) {
                        contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                    } else if ("audio".equals(type)) {
                        contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                    }

                    final String selection = "_id=?";
                    final String[] selectionArgs = new String[]{
                            split[1]
                    };

                    return getDataColumn(context, contentUri, selection, selectionArgs);
                }
            }
            // MediaStore (and general)
            else if ("content".equalsIgnoreCase(uri.getScheme())) {
                return getDataColumn(context, uri, null, null);
            }
            // File
            else if ("file".equalsIgnoreCase(uri.getScheme())) {
                return uri.getPath();
            }

            return null;
        }

        /**
         * Get the value of the data column for this Uri. This is useful for
         * MediaStore Uris, and other file-based ContentProviders.
         *
         * @param context       The context.
         * @param uri           The Uri to query.
         * @param selection     (Optional) Filter used in the query.
         * @param selectionArgs (Optional) Selection arguments used in the query.
         * @return The value of the _data column, which is typically a file path.
         */
        String getDataColumn(Context context, Uri uri, String selection,
                             String[] selectionArgs) {

            Cursor cursor = null;
            final String column = "_data";
            final String[] projection = {
                    column
            };

            try {
                cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                        null);
                if (cursor != null && cursor.moveToFirst()) {
                    final int column_index = cursor.getColumnIndexOrThrow(column);
                    return cursor.getString(column_index);
                }
            } finally {
                if (cursor != null)
                    cursor.close();
            }
            return null;
        }


        /**
         * @param uri The Uri to check.
         * @return Whether the Uri authority is ExternalStorageProvider.
         */
        boolean isExternalStorageDocument(Uri uri) {
            return "com.android.externalstorage.documents".equals(uri.getAuthority());
        }

        /**
         * @param uri The Uri to check.
         * @return Whether the Uri authority is DownloadsProvider.
         */
        boolean isDownloadsDocument(Uri uri) {
            return "com.android.providers.downloads.documents".equals(uri.getAuthority());
        }

        /**
         * @param uri The Uri to check.
         * @return Whether the Uri authority is MediaProvider.
         */
        boolean isMediaDocument(Uri uri) {
            return "com.android.providers.media.documents".equals(uri.getAuthority());
        }

    }

}
