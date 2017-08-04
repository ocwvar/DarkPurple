package com.ocwvar.reimu.Helper

import com.ocwvar.darkpurple.Units.Logger
import java.util.*
import java.util.concurrent.*

/**
 * Project Reimu
 * Created by 区成伟
 * On 2016/12/21 下午8:29
 * File Location com.ocwvar.reimu.Helper
 * Reimu    线程池对象
 */
class RMThreadPool(poolName: String, corePoolSize: Int = 1, maximumPoolSize: Int = 1, keepAliveTime: Long = 0L, unit: TimeUnit = TimeUnit.SECONDS, workQueue: BlockingQueue<Runnable> = LinkedBlockingQueue<Runnable>()) : ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue) {

    private val POOL_TAG: String = poolName
    private val taskHashMap: HashMap<String, FutureTask<Any>> = HashMap()

    init {
        Logger.warning(POOL_TAG, "线程池：$POOL_TAG  已创建 ！")
    }

    fun debug() {
        val count = taskHashMap.size
        println("\n\n\n\n=========$POOL_TAG , Items count: $count =========")
        taskHashMap.forEach(::println)
        println("==============================================")
    }

    /**
     * 执行给定线程载体， 唯一模式：以唯一的TAG来执行
     * @param task      线程载体
     * @param taskTag   线程唯一标识TAG
     */
    fun run_single(task: FutureTask<Any>, taskTag: String): Boolean {
        if (taskHashMap.containsKey(taskTag)) {
            Logger.error(POOL_TAG, "相同线程TAG：$taskTag ，拒绝执行")
            return false
        } else {
            taskHashMap.put(taskTag, task)
            submit(task)
            return true
        }
    }

    /**
     * 当线程执行完成后调用此方法来移除在线程池内的TAG，以允许下一个相同的TAG线程得到执行
     * @param taskTAG       线程唯一标识TAG
     */
    fun onTaskEnd(taskTAG: String) {
        if (taskHashMap.remove(taskTAG) != null) {
            Logger.warning(POOL_TAG, "线程TAG：$taskTAG，已移除成功")
        } else {
            Logger.error(POOL_TAG, "线程TAG：$taskTAG，移除失败，没有相同TAG的条目")
        }
    }

    /**
     * 销毁线程任务
     * @param   taskTAG     线程唯一标识TAG
     */
    fun destroyTask(taskTAG: String) {
        if (taskHashMap.containsKey(taskTAG)) {
            if (taskHashMap[taskTAG]?.cancel(true) ?: false) {
                Logger.warning(POOL_TAG, "线程TAG：$taskTAG，已销毁")
            } else {
                Logger.error(POOL_TAG, "线程TAG：$taskTAG，销毁失败，线程终止失败")
            }
        } else {
            Logger.error(POOL_TAG, "线程TAG：$taskTAG，销毁失败，不存在TAG的线程")
        }

    }

    /**
     * 在线程池中查找任务
     *
     * @param   taskTAG  线程TAG
     * @return  是否存在
     */
    fun index(taskTAG: String): Boolean {
        return taskHashMap.containsKey(taskTAG)
    }

}