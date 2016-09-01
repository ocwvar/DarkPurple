package com.ocwvar.darkpurple.Services;

/**
 * Created by 区成伟
 * Package: com.ocwvar.darkpurple.Services
 * Data: 2016/7/12 23:16
 * Project: DarkPurple
 * 全局使用音频服务的持有类
 */
public class ServiceHolder {

    private static ServiceHolder holder;
    private AudioService service;

    public static ServiceHolder getInstance() {
        if (holder == null) {
            holder = new ServiceHolder();
        }
        return holder;
    }

    public AudioService getService() {
        return service;
    }

    public void setService(AudioService service) {
        this.service = service;
    }

}
