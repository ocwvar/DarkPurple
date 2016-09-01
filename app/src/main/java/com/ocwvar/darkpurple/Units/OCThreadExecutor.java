package com.ocwvar.darkpurple.Units;

import android.support.annotation.NonNull;
import android.util.Log;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by 区成伟
 * Date: 2016/3/9  21:42
 * Version 5   16.4.17
 * Project: 可操作线程池
 */

public class OCThreadExecutor extends ThreadPoolExecutor {

    private Map<String, FutureTask> runnableMap;

    public OCThreadExecutor(int maxRunningThread, String poolName) {
        super(maxRunningThread, maxRunningThread, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), new OCThreadFactory(poolName));
        runnableMap = new HashMap<>();
    }

    /**
     * 执行任务
     *
     * @param task 任务对象
     * @param tag  任务唯一TAG
     */
    public void submit(FutureTask task, String tag) {

        synchronized (this) {
            //执行线程
            if (!runnableMap.containsKey(tag)) {
                //如果池内没有相同的任务则可以执行
                Log.d("OCThreadExecutor", "Task submitting TAG: " + tag);
                runnableMap.put(tag, task);
                submit(task);
            } else {
                Log.d("OCThreadExecutor", "Same task TAG. Skipped. ");
            }
        }

    }

    public boolean cancelTask(String tag) {
        return cancelTask(tag, false);
    }

    /**
     * 终止任务
     *
     * @param tag           任务唯一TAG
     * @param isContainText TAG相似即终止
     * @return 处理结果
     */
    public boolean cancelTask(String tag, boolean isContainText) {

        //中断线程

        synchronized (this) {
            if (isContainText) {
                Log.d("OCThread cancelTask", "Request to cancel TAG: " + tag);
                Iterator<String> keysSet = runnableMap.keySet().iterator();
                while (keysSet.hasNext()) {
                    String key = keysSet.next();
                    Log.d("OCThread cancelTask", "KEY:" + key);
                    if (key.contains(tag)) {
                        remove(runnableMap.get(key));
                        FutureTask task = runnableMap.remove(key);
                        if (task != null) {
                            task.cancel(true);
                        }
                        Log.d("OCThread cancelTask", "Task Canceled TAG: " + tag);
                        return true;
                    }
                }
                Log.d("OCThread cancelTask", "TAG dose not exist. Skipped. ");
                return false;
            } else {
                if (runnableMap.containsKey(tag)) {
                    Log.d("OCThread cancelTask", "Task Canceled TAG: " + tag);
                    remove(runnableMap.get(tag));
                    FutureTask task = runnableMap.remove(tag);
                    if (task != null) {
                        task.cancel(true);
                    }
                    return true;
                } else {
                    Log.d("OCThread cancelTask", "TAG dose not exist. Skipped. ");
                    return false;
                }
            }
        }

    }

    /**
     * 终止所有任务
     *
     * @return 处理结果
     */
    public boolean cancelAllTask() {
        Iterator<FutureTask> taskList = runnableMap.values().iterator();
        int count = 0;
        while (taskList.hasNext()) {
            count++;
            FutureTask task = taskList.next();
            task.cancel(true);
            remove(task);
        }
        runnableMap.clear();
        Log.d("OCThreadExecutor", count + " Tasks canceled.");
        return count > 0;
    }

    /**
     * 移除任务TAG
     *
     * @param tag 任务唯一TAG
     * @return 处理结果
     */
    public boolean removeTag(String tag) {

        //移除TAG
        if (runnableMap.remove(tag) != null) {
            Log.d("OCThreadExecutor", "TAG removed. TAG Count:" + runnableMap.size() + "  Pool of " + ((OCThreadFactory) getThreadFactory()).getPoolName());
            return true;
        } else {
            Log.d("OCThreadExecutor", "TAG dose not exist. Skipped. ");
            return false;
        }

    }

    static class OCThreadFactory implements ThreadFactory {

        private final String name;

        public OCThreadFactory(String name) {
            this.name = name;
        }

        public String getPoolName() {
            return name;
        }

        @Override
        public Thread newThread(@NonNull Runnable r) {
            return new OCThread(r, name);
        }

    }

    static class OCThread extends Thread {

        public OCThread(Runnable runnable, String name) {
            super(runnable, name);
            setName(name);
        }

        @Override
        public void run() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
            super.run();
        }
    }

}
