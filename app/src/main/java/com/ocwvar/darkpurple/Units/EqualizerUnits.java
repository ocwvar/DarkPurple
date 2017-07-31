package com.ocwvar.darkpurple.Units;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Project DarkPurple
 * Created by 区成伟
 * On 2016/10/14 11:15
 * File Location com.ocwvar.darkpurple.Units
 * 均衡器Units
 */

public class EqualizerUnits {

    private static EqualizerUnits equalizerUnits;
    private final String SP_NAME = "Equalizers";
    private final String LAST_SAVE_NAME = "_lsn";
    private HashMap<String, int[]> savedEqualizers;
    private Context appContext;

    private EqualizerUnits() {
        savedEqualizers = new HashMap<>();
    }

    public synchronized static EqualizerUnits getInstance() {
        if (equalizerUnits == null) {
            equalizerUnits = new EqualizerUnits();
        }
        return equalizerUnits;
    }

    /**
     * 初始化
     *
     * @param appContext 全局Context
     */
    public void init(Context appContext) {
        this.appContext = appContext;
        loadEqualizer();
    }

    /**
     * 储存均衡器配置
     *
     * @param name      配置名
     * @param equalizer 配置参数
     * @param callback  执行回调
     */
    public void saveEqualizer(@NonNull String name, @NonNull int[] equalizer, @NonNull OnEqualizerListChangedCallback callback) {

        if (!TextUtils.isEmpty(name) && !isExisted(name) && save2XML(name, equalizer)) {
            this.savedEqualizers.put(name, equalizer);
            callback.onSaveCompleted(name);
        } else {
            callback.onSaveFailed(name);
        }

    }

    /**
     * 储存最后一次的频谱变化,用于下次启动的时候应用
     *
     * @param values 频谱值
     */
    public void saveLastTimeEqualizer(int[] values) {
        if (appContext != null) {
            final SharedPreferences.Editor editor = appContext.getSharedPreferences(SP_NAME, 0).edit();
            for (int i = 0; i < 10; i++) {
                editor.putInt(LAST_SAVE_NAME + i, values[i]);
            }
            editor.apply();
        }
    }

    /**
     * 读取最后一次的频谱变化,没有数据则返回 {0,0,0,0,0,0,0,0,0,0}
     */
    public int[] loadLastTimeEqualizer() {
        if (appContext != null) {
            final SharedPreferences sp = appContext.getSharedPreferences(SP_NAME, 0);
            final int[] values = new int[10];
            for (int i = 0; i < 10; i++) {
                values[i] = sp.getInt(LAST_SAVE_NAME + i, 0);
            }
            return values;
        } else {
            return new int[10];
        }
    }

    /**
     * 读取保存的配置
     */
    private void loadEqualizer() {

        final Set<String> names = getNames();
        final Set<int[]> values = getValues(names);

        updateHashMap(names, values);

    }

    /**
     * 移除一个均衡器配置
     *
     * @param name     配置名
     * @param callback 执行回调
     */
    public void removeEqualizerValue(@NonNull String name, @NonNull OnEqualizerListChangedCallback callback) {

        final Set<String> names = getNames();

        if (appContext != null && !TextUtils.isEmpty(name) && savedEqualizers.containsKey(name) && names != null) {

            final SharedPreferences.Editor editor = appContext.getSharedPreferences(SP_NAME, 0).edit();

            //清除名字
            names.remove(name);
            editor.putStringSet("names", names);

            //清除值
            for (int i = 0; i < 10; i++) {
                editor.remove(name + i);
            }

            //根据执行结果出发回调
            if (editor.commit() && savedEqualizers.remove(name) != null) {
                callback.onRemoveCompleted(name, savedEqualizers.size());
            } else {
                callback.onRemoveFailed(name, savedEqualizers.size());
            }

        }
    }

    /**
     * 获取整个HashMap
     *
     * @return 储存数据的HashMap
     */
    public HashMap<String, int[]> getHashMap() {
        return this.savedEqualizers;
    }

    /**
     * 获取单个均衡器配置的参数
     *
     * @param name 配置名
     * @return 配置的参数
     */
    public int[] getEqualizerValues(String name) {
        return this.savedEqualizers.get(name);
    }

    /**
     * 是否存在相同名字的配置
     *
     * @param name 配置名
     * @return 执行结果
     */
    private boolean isExisted(String name) {
        return savedEqualizers.containsKey(name);
    }

    /**
     * 储存到SP文件中
     *
     * @param name      配置名
     * @param equalizer 配置参数
     * @return 执行结果
     */
    private boolean save2XML(String name, int[] equalizer) {

        if (appContext == null) {
            return false;
        } else {

            //先获取已有的名字集合
            final SharedPreferences sp = appContext.getSharedPreferences(SP_NAME, 0);

            //如果是第一次获取,为空则重新创建一个新的对象
            Set<String> namesSet = sp.getStringSet("names", null);
            if (namesSet == null) {
                namesSet = new LinkedHashSet<>();
            }

            //将新的配置名字加进去
            namesSet.add(name);

            final SharedPreferences.Editor editor = appContext.getSharedPreferences(SP_NAME, 0).edit();

            //将配置参数写进文件 例如:  save0 save1 ... save9
            for (int i = 0; i < 10; i++) {
                editor.putInt(name + i, equalizer[i]);
            }

            //将操作写入文件 , 并传回执行结果
            return editor.putStringSet("names", namesSet).commit();
        }

    }

    /**
     * 读取所有保存的配置名字
     *
     * @return 名字集合
     */
    private
    @Nullable
    Set<String> getNames() {
        if (appContext == null) {
            return null;
        } else {

            final SharedPreferences sp = appContext.getSharedPreferences(SP_NAME, 0);

            final Set<String> namesSet = sp.getStringSet("names", null);

            if (namesSet != null) {
                return namesSet;
            } else {
                return null;
            }
        }
    }

    /**
     * 读取所有保存的配置参数
     *
     * @param names 名字集合
     * @return 参数集合
     */
    private
    @Nullable
    Set<int[]> getValues(@Nullable Set<String> names) {
        if (names == null || appContext == null) {
            return null;
        } else {

            final SharedPreferences sp = appContext.getSharedPreferences(SP_NAME, 0);
            final Set<int[]> values = new LinkedHashSet<>();

            for (String name : names) {

                //从迭代器中逐个获取名字
                //每个参数的存储数组
                final int[] singleValue = new int[10];
                //遍历获取
                for (int i = 0; i < 10; i++) {
                    singleValue[i] = sp.getInt(name + i, 0);
                }

                values.add(singleValue);
            }

            return values;
        }
    }

    /**
     * 组合获取到的名字和配置参数
     *
     * @param names  名字集合
     * @param values 参数集合
     */
    private void updateHashMap(@Nullable Set<String> names, @Nullable Set<int[]> values) {
        if (names != null && values != null) {

            final Iterator<String> namesIterator = names.iterator();
            final Iterator<int[]> valuesIterator = values.iterator();

            //重置或初始化HashMap
            if (savedEqualizers == null) {
                savedEqualizers = new HashMap<>();
            } else {
                savedEqualizers.clear();
            }

            while (namesIterator.hasNext()) {
                try {
                    savedEqualizers.put(namesIterator.next(), valuesIterator.next());
                } catch (Exception e) {
                    break;
                }
            }

        }
    }

    /**
     * 监听接口
     */
    public interface OnEqualizerListChangedCallback {

        /**
         * 储存一个配置成功回调
         *
         * @param name 配置名
         */
        void onSaveCompleted(String name);

        /**
         * 储存一个配置失败回调
         *
         * @param name 配置名
         */
        void onSaveFailed(String name);

        /**
         * 移除一个配置成功回调
         *
         * @param name      配置名
         * @param restCount 剩余数量
         */
        void onRemoveCompleted(String name, int restCount);

        /**
         * 移除一个配置失败回调
         *
         * @param name      配置名
         * @param restCount 剩余数量
         */
        void onRemoveFailed(String name, int restCount);

    }

}
