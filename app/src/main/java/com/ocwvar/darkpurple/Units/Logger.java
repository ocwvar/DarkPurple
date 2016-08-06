package com.ocwvar.darkpurple.Units;

import android.os.Environment;
import android.util.Log;

/**
 * Created by 区成伟
 * Package: com.ocwvar.darkpurple.Units
 * Data: 2016/7/9 12:15
 * Project: DarkPurple
 * 调试器
 */
public class Logger {

    private final static boolean DEBUG = true;

    public static void warnning(String TAG , String message){
        if (DEBUG){
            Log.w(TAG, message);
        }
    }

    public static void normal(String TAG , String message){
        if (DEBUG){
            Log.d(TAG, message);
        }
    }

    public static void error(String TAG , String message){
        if (DEBUG){
            Log.e(TAG, message);
        }
    }

}
