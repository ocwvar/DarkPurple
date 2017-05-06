package com.ocwvar.darkpurple.Network;

/**
 * Project DarkPurple
 * Created by OCWVAR
 * On 2017/05/06 11:26 PM
 * File Location com.ocwvar.darkpurple.Network
 * This file use to :
 */

public class APIs {

    public static volatile String baseURL = "http://192.168.1.101:1008/";

    public static synchronized String loginURL() {
        return baseURL + "api/User/Login";
    }

    public static synchronized String registerURL() {
        return baseURL + "api/User/Register";
    }

    public static synchronized String uploadFile() {
        return baseURL + "api/Files/Upload";
    }

    public static synchronized String uploadedFiles() {
        return baseURL + "api/Files/MyFiles";
    }

    public static synchronized String removeFiles() {
        return baseURL + "api/Files/RemoveFile";
    }

}
