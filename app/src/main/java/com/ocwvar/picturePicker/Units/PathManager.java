package com.ocwvar.picturePicker.Units;

import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.util.ArrayList;

/**
 * Project PicturePicker
 * Created by 区成伟
 * On 2017/1/23 22:21
 * File Location com.ocwvar.picturepicker.Picker
 * 路径操作
 */

public class PathManager {

    private ArrayList<String> pathSet;

    public PathManager() {
        this.pathSet = new ArrayList<>();
        this.pathSet.add(0, "recent");
        this.pathSet.add(1, "main");
    }

    /**
     * @return 栈顶路径
     */
    public String getCurrentPath() {
        return this.pathSet.get(0);
    }

    /**
     * 添加路径，进入下一个路径时调用
     *
     * @param path 路径
     * @return 栈顶路径
     */
    public String addPath(String path) {
        if (!TextUtils.isEmpty(path)) {
            if (!pathSet.contains(path)) {
                pathSet.add(0, path);
            } else {
                return null;
            }
        }
        return getCurrentPath();
    }

    /**
     * 移除栈顶路径，返回上一级路径时调用
     *
     * @return 栈顶路径
     */
    public String popPath() {
        if (pathSet.size() == 1) {
            //必须保证栈底部是 "main"
            return getCurrentPath();
        } else {
            //否则移除栈顶路径后，返回栈顶路径
            pathSet.remove(0);
            return getCurrentPath();
        }
    }

    /**
     * @return 上一级目录，无上一级目录时返回NULL
     */
    public
    @Nullable
    String getUpPath() {
        try {
            return pathSet.get(1);
        } catch (Exception e) {
            return null;
        }
    }

}
