package com.ocwvar.picturePicker;

import android.Manifest;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import com.ocwvar.darkpurple.R;
import com.ocwvar.picturePicker.Adapters.FileObjectAdapter;
import com.ocwvar.picturePicker.Units.PathManager;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Random;

/**
 * Project AnHe_Business
 * Created by 区成伟
 * On 2016/11/8 16:40
 * File Location com.ocwvar.anhe_business.Units
 * 图像选择工具
 */

public class PicturePickerUnity extends AppCompatActivity
        implements FileObjectAdapter.OnFileItemClickCallback,
        Scanner.ScannerResultCallback,
        View.OnClickListener {

    /**
     * 结果参数字段
     * <p>
     * keys of result parameter
     */
    //获取位图对象
    public static final String EXTRAS_BITMAP = "rs1";
    //获取图像文件对象
    public static final String EXTRAS_FILE = "rs2";
    //获取图像文件位置URI
    public static final String EXTRAS_URI = "rs3";
    /**
     * 请求参数字段
     * <p>
     * Keys of request parameter
     */
    //是否需要裁剪
    private static final String ARG_NEED_CROP = "ac1";
    //是否需要压缩
    private static final String ARG_NEED_COMPRESS = "ac2";
    //只返回文件对象
    private static final String ARG_RETURN_FILE_ONLY = "ac3";
    //只返回位图对象
    private static final String ARG_RETURN_BITMAP_ONLY = "ac4";
    //返回位图和文件
    private static final String ARG_RETURN_BOTH = "ac5";
    //压缩比例
    private static final String ARG_COMPRESS_VALUE = "ac6";
    //裁剪宽度
    private static final String ARG_CROP_WIDTH = "ac7";
    //裁剪高度
    private static final String ARG_CROP_HEIGHT = "ac8";
    //保存路径
    private static final String ARG_SAVE_PATH = "ac9";
    //保存文件名称
    private static final String ARG_SAVE_NAME = "ac10";
    //只返回URI位置
    private static final String ARG_RETURN_URI_ONLY = "ac11";
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
     * 显示当前界面的显示文字
     */
    TextView currentLevelShower;
    /**
     * 显示上一级文件夹名字
     */
    TextView upLevelShower;
    /**
     * 列表显示的适配器
     */
    FileObjectAdapter adapter;
    /**
     * 搜索器
     */
    Scanner scanner;
    /**
     * 路径管理器
     */
    PathManager pathManager;
    /**
     * 显示列表
     */
    RecyclerView recyclerView;
    /**
     * 异常消息文字
     * <p>
     * Text of exception
     */
    private String ERROR_TEXT_COMPRESS_FILED;
    private String ERROR_TEXT_FILE_FILED;
    private String ERROR_TEXT_FILE_SAVE_FAILED;
    private String ERROR_TEXT_PIC_INCURRECT;
    private String ERROR_TEXT_BITMAP_TOOLAGER;
    private String ERROR_TEXT_CAMERA_NO_DATA;
    private String ERROR_TEXT_LOCAL_NO_DATA;
    private String ERROR_TEXT_ARG;
    private String ERROR_TEXT_UNKNOWN;
    private String ERROR_TEXT_MOVEFAILED;
    private String ERROR_TEXT_OOM;
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
    private boolean RETURN_URI_ONLY = false;
    private boolean RETURN_BOTH = false;
    private String CUSTOM_SAVE_PATH = null;
    private String CUSTOM_SAVE_NAME = null;
    /**
     * 图像文件保存路径
     * <p>
     * The folder of file objects
     */
    private String SAVE_PATH = Environment.getExternalStorageDirectory().getPath() + "/Picker/";
    /**
     * 是否需要修正三星手机导致的图像旋转问题
     */
    private boolean needFixAngle = false;
    /**
     * 当前是否正在处理图像
     * 当此变量为True时 , 不可以处理下一张图像
     */
    private boolean pictureProgressing = false;

    /**
     * 对话框储存容器
     */
    private WeakReference<AlertDialog> dialogContainer = new WeakReference<>(null);

    @Override
    @SuppressWarnings("ResultOfMethodCallIgnored")
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent() == null || getIntent().getExtras() == null) {
            onFinalStep(null, null, null, ERROR_TEXT_ARG);
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
            RETURN_URI_ONLY = request.getBoolean(ARG_RETURN_URI_ONLY, false);
            RETURN_BOTH = request.getBoolean(ARG_RETURN_BOTH, false);
            CUSTOM_SAVE_PATH = request.getString(ARG_SAVE_PATH, null);
            CUSTOM_SAVE_NAME = request.getString(ARG_SAVE_NAME, null);
        }

        ERROR_TEXT_COMPRESS_FILED = getString(R.string.ERROR_TEXT_COMPRESS_FILED);
        ERROR_TEXT_FILE_FILED = getString(R.string.ERROR_TEXT_FILE_FILED);
        ERROR_TEXT_FILE_SAVE_FAILED = getString(R.string.ERROR_TEXT_FILE_SAVE_FAILED);
        ERROR_TEXT_PIC_INCURRECT = getString(R.string.ERROR_TEXT_PIC_INCURRECT);
        ERROR_TEXT_BITMAP_TOOLAGER = getString(R.string.ERROR_TEXT_BITMAP_TOOLAGER);
        ERROR_TEXT_CAMERA_NO_DATA = getString(R.string.ERROR_TEXT_CAMERA_NO_DATA);
        ERROR_TEXT_LOCAL_NO_DATA = getString(R.string.ERROR_TEXT_LOCAL_NO_DATA);
        ERROR_TEXT_ARG = getString(R.string.ERROR_TEXT_ARG);
        ERROR_TEXT_UNKNOWN = getString(R.string.ERROR_TEXT_UNKNOWN);
        ERROR_TEXT_MOVEFAILED = getString(R.string.ERROR_TEXT_MOVEFAILED);
        ERROR_TEXT_OOM = getString(R.string.ERROR_TEXT_OOM);

        if (Build.VERSION.SDK_INT >= 21) {
            //设置状态栏和导航栏颜色
            final Window window = getWindow();
            window.setBackgroundDrawable(new ColorDrawable(Color.rgb(255, 255, 255)));
            window.setNavigationBarColor(Color.rgb(180, 180, 180));
            window.setStatusBarColor(Color.argb(100, 255, 9, 50));
        }

        setContentView(R.layout.picture_select_buildint_gallery);
        adapter = new FileObjectAdapter(this, getApplicationContext());
        scanner = new Scanner(this);
        pathManager = new PathManager();

        upLevelShower = (TextView) findViewById(R.id.textView_up_folder_name);
        currentLevelShower = (TextView) findViewById(R.id.textView_current_level);
        findViewById(R.id.button_up_folder).setOnClickListener(this);
        recyclerView = (RecyclerView) findViewById(R.id.recycleView);
        recyclerView.setLayoutManager(new GridLayoutManager(PicturePickerUnity.this, 3, GridLayoutManager.VERTICAL, false));
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(adapter);

        //计算裁剪长宽的最大公约数
        final int maxCommonDivisor = maxCommonDivisor(CROP_WIDTH, CROP_HEIGHT);
        CROP_WIDTH_RATION = CROP_WIDTH / maxCommonDivisor;
        CROP_HEIGHT_RATION = CROP_HEIGHT / maxCommonDivisor;

        //清除旧的临时文件
        new File(TEMPSAVE_PATH).delete();
        new File(TEMPSAVE_PATH2).delete();

        scanner.scanFiles(pathManager.getCurrentPath(), PicturePickerUnity.this);
    }

    /**
     * 点击文件夹回调
     *
     * @param fileObject 文件对象
     */
    @Override
    public void onFolderClick(Scanner.FileObject fileObject) {
        scanner.scanFiles(pathManager.addPath(fileObject.getPath()), PicturePickerUnity.this);
    }

    /**
     * 点击图像对象回调
     *
     * @param fileObject 文件对象
     */
    @Override
    public void onFileClick(Scanner.FileObject fileObject) {
        if (pictureProgressing) {
            return;
        } else {
            pictureProgressing = true;
        }
        showMessageDialog(false, getString(R.string.simple_loading), null, null);

        if (NEED_CROP) {
            final Uri uri = FileProvider.getUriForFile(PicturePickerUnity.this, PicturePickerUnity.this.getApplicationContext().getPackageName() + ".provider", new File(fileObject.getPath()));
            cropImageFromURI(uri);
        } else {
            handleCompressAndSave(new File(fileObject.getPath()), null);
        }
    }

    /**
     * 点击选项按钮
     *
     * @param optionType 选项功能
     */
    @Override
    public void onOptionClick(FileObjectAdapter.OptionTypes optionType) {
        switch (optionType) {
            case 最近图像:
                scanner.scanFiles(pathManager.addPath("recent"), PicturePickerUnity.this);
                break;
            case 使用其他图库:
                showMessageDialog(false, getString(R.string.simple_loading), null, null);
                requestPickFromLocal();
                break;
            case 使用相机:
                if (showCameraTips()) {
                    showMessageDialog(false, getString(R.string.simple_loading), null, null);
                    requestPickFromCamera();
                }
                break;
        }
    }

    /**
     * 获取到扫描结果
     * <p>
     * 显示上一级目录名称
     * 显示当前目录名称
     * 往文件展示适配器中添加数据对象
     *
     * @param fileObjects  文件对象列表
     * @param currentLevel 当前的目录
     */
    @Override
    public void onScanCompleted(@NonNull ArrayList<Scanner.FileObject> fileObjects, String currentLevel) {
        //显示当前目录名称
        currentLevelShower.setText(String.format("%s%s", getString(R.string.CURRENT), reduceLongString(currentLevel)));

        //显示上一级目录的名称
        final String upFolderName = pathManager.getUpPath();
        if (upFolderName == null) {
            upLevelShower.setText(R.string.NOUP);
        } else if (upFolderName.equals("main")) {
            upLevelShower.setText(R.string.MAIN);
        } else if (upFolderName.equals("recent")) {
            upLevelShower.setText(R.string.RECENT);
        } else {
            upLevelShower.setText(reduceLongString(upFolderName));
        }

        //往对象列表适配器中添加数据
        adapter.putSource(fileObjects);
    }

    /**
     * 获取扫描结果失败
     */
    @Override
    public void onScanFailed() {

    }

    /**
     * 控件点击控制
     * <p>
     * 操作有：
     * 返回上一级目录
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_up_folder:
                //点击上一级按钮
                scanner.scanFiles(pathManager.popPath(), PicturePickerUnity.this);
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
                onFinalStep(null, null, null, ERROR_TEXT_UNKNOWN);
                break;
        }

    }

    /**
     * 最终获取到图像的时候
     *
     * @param bitmap 得到的Bitmap
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void onFinalStep(Bitmap bitmap, File file, Uri uri, String exceptionMessage) {

        dismissDialog();

        final Intent result = new Intent();

        //当需要返回URI时
        if (uri != null) {
            result.putExtra(EXTRAS_URI, uri);
            result.setData(uri);
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

        //当文件对象需要移动到用户指定位置时
        if (!TextUtils.isEmpty(CUSTOM_SAVE_PATH) && (RETURN_BOTH || RETURN_FILE_ONLY) && file != null) {
            file = moveFile2Folder(file, CUSTOM_SAVE_PATH, CUSTOM_SAVE_NAME);
            if (file == null) {
                exceptionMessage = ERROR_TEXT_MOVEFAILED;
            }
        }

        //将错误信息显示在界面内的对话框
        if (!TextUtils.isEmpty(exceptionMessage)) {
            showMessageDialog(true, exceptionMessage, getString(R.string.simple_done), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            pictureProgressing = false;
            return;
        }

        //传递文件对象
        result.putExtra(EXTRAS_FILE, file);
        //设置返回Intent
        setResult(100, result);

        //清除临时文件
        new File(TEMPSAVE_PATH).delete();
        new File(TEMPSAVE_PATH2).delete();

        //结束页面,完成操作
        finish();
    }

    /**
     * 处理较长的String为短String
     *
     * @param string 长String
     * @return 短String，不足长度的，直接返回原有数据
     */
    private
    @Nullable
    String reduceLongString(@NonNull String string) {
        if (TextUtils.isEmpty(string) || string.length() <= 20) {
            return string;
        }

        final int reduceLength = string.length() - 20;
        return "…" + string.substring(reduceLength, string.length());
    }

    /**
     * 计算最大公约数
     *
     * @param width  要裁剪的宽度
     * @param height 要裁剪的高度
     * @return 宽高之比
     */
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

    /**
     * 显示相机使用提示，如果不是第一次使用，则不显示
     */
    private boolean showCameraTips() {
        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(PicturePickerUnity.this);
        if (sp.getBoolean("isFirstUseCamera", true)) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(PicturePickerUnity.this);
            builder.setMessage(getString(R.string.cameraTips));
            builder.setCancelable(false);
            builder.setPositiveButton(R.string.simple_done, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    final SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(PicturePickerUnity.this).edit();
                    editor.putBoolean("isFirstUseCamera", false);
                    editor.apply();

                    dialog.dismiss();
                }
            });
            builder.show();
            return false;
        } else {
            return true;
        }
    }

    /**
     * 移动文件到指定位置
     *
     * @param file   要移动的文件对象
     * @param toPath 要移动到的位置
     * @param toName 移动后的文件名称
     * @return 新的文件对象
     */
    private File moveFile2Folder(File file, String toPath, String toName) {
        if (file == null || !file.exists() || TextUtils.isEmpty(toPath) || TextUtils.isEmpty(toName)) {
            return null;
        } else {

            if (toPath.charAt(toPath.length() - 1) != '/') {
                //如果目的路径的最后一位不是 "/" 则帮其补上
                toPath = toPath + "/";
            }

            try {
                final File targetFolder = new File(toPath);
                if (targetFolder.exists() || targetFolder.mkdirs()) {
                    //如果目标目录已存在，或者创建目录成功

                    //创建新的目的文件对象
                    final File targetFile = new File(toPath + toName);
                    if (file.renameTo(targetFile)) {
                        //如果位置复制成功，则直接返回目的文件对象
                        return targetFile;
                    } else {
                        //移动失败，则返回NULL
                        return null;
                    }
                } else {
                    //无法创建目标目录
                    return null;
                }
            } catch (Exception e) {
                //发生文件操作异常
                return null;
            }
        }
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
        final Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        final Uri uri = FileProvider.getUriForFile(PicturePickerUnity.this, PicturePickerUnity.this.getApplicationContext().getPackageName() + ".provider", new File(TEMPSAVE_PATH));
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        intent.setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
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
                final Uri uri = FileProvider.getUriForFile(PicturePickerUnity.this, PicturePickerUnity.this.getApplicationContext().getPackageName() + ".provider", savedFile);
                cropImageFromURI(uri);
            } else {
                //如果不需要剪裁,则直接调到最后部分
                handleCompressAndSave(savedFile, null);
            }

        } else {
            //文件无效 , 操作失败
            onFinalStep(null, null, null, ERROR_TEXT_CAMERA_NO_DATA);
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
            }
            intent.putExtra("return-data", true);
            intent.putExtra("scale", true);
            intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
            intent.putExtra("noFaceDetection", true);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(tempFile));
            startActivityForResult(intent, REQUEST_LOCAL);
        } catch (Exception e) {
            tempFile = null;
            onFinalStep(null, null, null, ERROR_TEXT_ARG);
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
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            startActivityForResult(intent, REQUEST_CUT);
        } catch (Exception e) {
            tempFile = null;
            onFinalStep(null, null, null, ERROR_TEXT_PIC_INCURRECT);
        }
    }

    /**
     * 处理本地选取图片后的结果
     *
     * @param intent 返回的结果 Intent
     */
    private void handlePickFromLocal(Intent intent) {
        if (intent == null) {
            onFinalStep(null, null, null, ERROR_TEXT_LOCAL_NO_DATA);
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
            onFinalStep(null, null, null, ERROR_TEXT_LOCAL_NO_DATA);
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
            onFinalStep(null, null, null, ERROR_TEXT_LOCAL_NO_DATA);
        } else if (!intent.hasExtra("data") && intent.getData() != null) {
            //返回来的是 Uri 路径 , 解析后直接加载即可 , 其文件路径为我们在请求Intent中设定的路径 TEMPSAVE_PATH2
            final Bitmap bitmap = BitmapFactory.decodeFile(TEMPSAVE_PATH2);
            if (bitmap != null) {
                handleCompressAndSave(null, bitmap);
            } else {
                onFinalStep(null, null, null, ERROR_TEXT_LOCAL_NO_DATA);
            }
        } else if (intent.hasExtra("data")) {
            handleCompressAndSave(null, (Bitmap) intent.getParcelableExtra("data"));
        } else {
            onFinalStep(null, null, null, ERROR_TEXT_LOCAL_NO_DATA);
        }
    }

    /**
     * 处理压缩图片并储存到本地
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void handleCompressAndSave(File handleFile, Bitmap handleBitmap) {
        Bitmap bitmap = null;

        if (handleFile != null) {
            try {
                bitmap = BitmapFactory.decodeFile(handleFile.getPath());
            } catch (OutOfMemoryError e) {
                bitmap = null;
                onFinalStep(null, null, null, ERROR_TEXT_OOM);
                return;
            }
        } else if (handleBitmap != null) {
            bitmap = handleBitmap;
        }

        /**
         *  修正图像旋转问题
         */
        if (bitmap != null && needFixAngle) {
            Matrix matrix = new Matrix();
            matrix.postRotate((float) 90.0);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        }

        if (bitmap != null) {
            //如果能获取到图像,则代表这个对象是可用的

            //储存最终文件的位置
            File saveFile = new File(SAVE_PATH);
            saveFile.mkdirs();
            saveFile = new File(SAVE_PATH + getRandomFileName());
            final Uri fileUri = Uri.fromFile(saveFile);

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

                            onFinalStep(null, saveFile, null, null);
                        } else if (RETURN_BITMAP_ONLY) {
                            //如果仅需要返回位图对象,则需要把储存的对象删除

                            onFinalStep(BitmapFactory.decodeFile(saveFile.getPath()), null, null, null);
                            saveFile.delete();
                        } else if (RETURN_URI_ONLY) {
                            //如果仅需要返回文件URI

                            onFinalStep(null, null, fileUri, null);
                        } else if (RETURN_BOTH) {
                            //都返回的情况下
                            onFinalStep(BitmapFactory.decodeFile(saveFile.getPath()), saveFile, fileUri, null);
                        }
                    } else {
                        onFinalStep(null, null, null, ERROR_TEXT_COMPRESS_FILED);
                    }

                } catch (Exception e) {
                    //产生异常则全部返回 null
                    bitmap.recycle();
                    onFinalStep(null, null, null, ERROR_TEXT_FILE_FILED);
                }

            } else {
                //不需要压缩
                try {

                    if (RETURN_BITMAP_ONLY) {
                        //仅返回位图对象
                        onFinalStep(bitmap, null, null, null);
                    } else {
                        //否则就是需要图像文件的 位图+文件 或 仅文件

                        //创建新文件
                        saveFile.createNewFile();
                        //创建输出流
                        final FileOutputStream fileOutputStream = new FileOutputStream(saveFile, false);

                        if (bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream)) {
                            //保存成功
                            if (RETURN_FILE_ONLY) {
                                bitmap.recycle();
                                onFinalStep(null, saveFile, null, null);
                            } else if (RETURN_URI_ONLY) {
                                onFinalStep(null, null, fileUri, null);
                            } else {
                                onFinalStep(bitmap, saveFile, fileUri, null);
                            }
                        } else {
                            bitmap.recycle();
                            onFinalStep(null, null, null, ERROR_TEXT_FILE_SAVE_FAILED);
                        }
                    }

                } catch (Exception e) {
                    //产生异常则全部返回 null
                    bitmap.recycle();
                    onFinalStep(null, null, null, ERROR_TEXT_FILE_FILED);
                }

            }
        } else {
            onFinalStep(null, null, null, ERROR_TEXT_PIC_INCURRECT);
        }
    }

    /**
     * 显示信息对话框
     *
     * @param canBeCancel    是否允许取消
     * @param message        显示的信息
     * @param doneButtonText 按钮文字，传入空则不显示按钮
     * @param listener       按钮回调，传入空则不显示按钮
     */
    private void showMessageDialog(boolean canBeCancel, String message, String doneButtonText, DialogInterface.OnClickListener listener) {
        AlertDialog dialog = dialogContainer.get();
        if (dialog == null) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(PicturePickerUnity.this);
            dialog = builder.create();
            dialogContainer = new WeakReference<>(dialog);
        }

        dialog.setMessage(message);
        dialog.setCancelable(canBeCancel);
        dialog.setCanceledOnTouchOutside(canBeCancel);
        if (!TextUtils.isEmpty(doneButtonText) && listener != null) {
            dialog.setButton(DialogInterface.BUTTON_POSITIVE, doneButtonText, listener);
        }

        dialog.show();
    }

    /**
     * 使信息对话框消失
     */
    private void dismissDialog() {
        final AlertDialog alertDialog = dialogContainer.get();
        if (alertDialog != null && alertDialog.isShowing()) {
            alertDialog.dismiss();
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
        private boolean arg_returnUri = false;
        private boolean arg_returnBoth = false;

        private int arg_compressValue = 60;
        private int arg_cropWidth = 200;
        private int arg_cropHeight = 200;

        private String arg_savePath = null;
        private String arg_saveName = null;

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
         * @param returnUri 是否只返回图像位置Uri
         * @return 构建器本身
         */
        public Builder returnUri(boolean returnUri) {
            arg_returnUri = returnUri;
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
         * @param path 保存File对象至指定路径
         * @param name 文件的储存名称
         * @return 构建器本身
         */
        public Builder setFileSavePathAndName(@NonNull String path, @NonNull String name) {
            arg_savePath = path;
            arg_saveName = name;
            return this;
        }

        /**
         * 启动图像选择器
         *
         * @param activity              activity 对象
         * @param requestCode           请求码
         * @param permissionRequestCode 权限请求码
         */
        public void startPickerNow_ACTIVITY(@NonNull Activity activity, int requestCode, int permissionRequestCode) {

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
            request.putExtra(ARG_RETURN_URI_ONLY, arg_returnUri);
            request.putExtra(ARG_RETURN_BOTH, arg_returnBoth);

            request.putExtra(ARG_COMPRESS_VALUE, arg_compressValue);
            request.putExtra(ARG_CROP_WIDTH, arg_cropWidth);
            request.putExtra(ARG_CROP_HEIGHT, arg_cropHeight);

            request.putExtra(ARG_SAVE_PATH, arg_savePath);
            request.putExtra(ARG_SAVE_NAME, arg_saveName);

            resetDefaultValues();

            activity.startActivityForResult(request, requestCode);

        }

        /**
         * 启动图像选择器
         *
         * @param fragment              fragment 对象
         * @param requestCode           请求码
         * @param permissionRequestCode 权限请求码
         */
        public void startPickerNow_FRAGMENT(@NonNull Fragment fragment, int requestCode, int permissionRequestCode) {

            if (fragment.getContext() == null) {
                return;
            }

            if (Build.VERSION.SDK_INT >= 23) {

                final FragmentActivity activity = fragment.getActivity();

                if (activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED || activity.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {

                    //如果系统版本为 Android 6.0+，同时应用的权限不完整，则一次性全部获取
                    fragment.getActivity().requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA}, permissionRequestCode);
                    Toast.makeText(fragment.getContext(), R.string.ERROR_TEXT_PERMISSION, Toast.LENGTH_SHORT).show();
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

            final Intent request = new Intent(fragment.getContext(), PicturePickerUnity.class);
            request.putExtra(ARG_NEED_CROP, arg_needCrop);
            request.putExtra(ARG_NEED_COMPRESS, arg_needCompress);

            request.putExtra(ARG_RETURN_FILE_ONLY, arg_returnFile);
            request.putExtra(ARG_RETURN_BITMAP_ONLY, arg_returnBitmap);
            request.putExtra(ARG_RETURN_URI_ONLY, arg_returnUri);
            request.putExtra(ARG_RETURN_BOTH, arg_returnBoth);

            request.putExtra(ARG_COMPRESS_VALUE, arg_compressValue);
            request.putExtra(ARG_CROP_WIDTH, arg_cropWidth);
            request.putExtra(ARG_CROP_HEIGHT, arg_cropHeight);

            request.putExtra(ARG_SAVE_PATH, arg_savePath);
            request.putExtra(ARG_SAVE_NAME, arg_saveName);

            resetDefaultValues();

            fragment.startActivityForResult(request, requestCode);

        }

        /**
         * 请求之前恢复默认的参数
         */
        private void resetDefaultValues() {
            arg_needCrop = false;
            arg_needCompress = false;

            arg_returnFile = true;
            arg_returnBitmap = false;
            arg_returnUri = false;
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
