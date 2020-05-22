package com.yunbiao.ybsmartcheckin_live_id;

import android.app.Activity;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.arcsoft.face.ActiveFileInfo;
import com.arcsoft.face.ErrorInfo;
import com.arcsoft.face.FaceEngine;
import com.google.gson.Gson;
import com.yunbiao.ybsmartcheckin_live_id.afinel.ResourceUpdate;
import com.yunbiao.ybsmartcheckin_live_id.bean.ActiveCodeResponse;
import com.yunbiao.ybsmartcheckin_live_id.system.HeartBeatClient;
import com.yunbiao.ybsmartcheckin_live_id.utils.CommonUtils;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.StringCallback;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;

public class FaceSDKActive {
    private static final String TAG = "FaceSDKActive";

    private static String wifiMac = "";
    private static String localMac = "";

    public static void active(@NonNull int activeType, @NonNull Activity activity, @NonNull String type, @NonNull ActiveCallback activeCallback) {
        wifiMac = CommonUtils.getWifiMac();
        if (!TextUtils.isEmpty(wifiMac)) {
            wifiMac = wifiMac.replace(":", "-");
        } else {
            wifiMac = "";
        }
        localMac = CommonUtils.getLocalMac();
        if (!TextUtils.isEmpty(localMac)) {
            localMac = localMac.replace(":", "-");
        } else {
            localMac = "";
        }
        if (activeType == 0) {
            offlineActive(activity, activeCallback);
        } else {
            checkActiveState(activity, type, activeCallback);
        }
    }

    private static void offlineActive(Activity context, ActiveCallback callback) {
        File externalStorageDirectory = Environment.getExternalStorageDirectory();
        File[] files = externalStorageDirectory.listFiles();
        if (files == null || files.length <= 0) {
            if (callback != null) {
                callback.getActiveCodeFailed(context.getResources().getString(R.string.splash_active_failed) + "(The activation file was not found)", wifiMac, localMac);
            }
            return;
        }

        File datFile = null;
        for (File file : files) {
            if (file.getName().length() > 10 && file.getName().endsWith(".dat")) {
                datFile = file;
            }
        }
        if (datFile == null) {
            if (callback != null) {
                callback.getActiveCodeFailed(context.getResources().getString(R.string.splash_active_failed) + "(The activation file was not found)", wifiMac, localMac);
            }
            return;
        }

        int code = FaceEngine.activeOffline(APP.getContext(), datFile.getPath());
        Log.e(TAG, "激活结果：: " + code);
        if (code != ErrorInfo.MOK && code != ErrorInfo.MERR_ASF_ALREADY_ACTIVATED) {
            if (callback != null) {
                callback.getActiveCodeFailed(context.getResources().getString(R.string.splash_active_failed) + ":" + code, wifiMac, localMac);
            }
            returnActiveStatus(2, "", -1);
            return;
        }

        if (callback != null) {
            callback.getActiveCodeSuccess();
        }

        if (code == ErrorInfo.MOK) {
            returnActiveStatus(1, "", -1);
        }
    }

    private static void checkActiveState(Activity context, String type, ActiveCallback callback) {
        ActiveFileInfo activeFileInfo = new ActiveFileInfo();
        int code = FaceEngine.getActiveFileInfo(APP.getContext(), activeFileInfo);
        String activeKey = activeFileInfo.getActiveKey();
        String sdkKey = activeFileInfo.getSdkKey();
        String appId = activeFileInfo.getAppId();
        if (code == ErrorInfo.MOK
                && !TextUtils.isEmpty(activeKey)
                && !TextUtils.isEmpty(sdkKey)
                && !TextUtils.isEmpty(appId)) {
            Log.e(TAG, "requestActiveCode: 激活码：" + activeKey + " ----- " + sdkKey + " ----- " + appId);
            int activeCode = FaceEngine.activeOnline(APP.getContext(), activeKey, appId, sdkKey);
            Log.e(TAG, "激活结果：: " + activeCode);
            if (activeCode == ErrorInfo.MOK || activeCode == ErrorInfo.MERR_ASF_ALREADY_ACTIVATED) {
                callback.getActiveCodeSuccess();
            } else {
                requestActiveCode(context, type, callback);
            }
        } else {
            requestActiveCode(context, type, callback);
        }
    }

    private static void requestActiveCode(Activity context, String type, ActiveCallback callback) {
        String url = ResourceUpdate.GET_ACTIVE_CODE;
        Log.e(TAG, "getActiveCode: 地址:" + url);
        Map<String, String> params = new HashMap<>();
        params.put("deviceNo", HeartBeatClient.getDeviceNo());
        params.put("type", type);
        params.put("wifiMac", wifiMac);
        params.put("localMac", localMac);
        Log.e(TAG, "requestActiveCode: 参数：" + params.toString());
        OkHttpUtils.post().url(url).params(params).build().execute(new StringCallback() {
            @Override
            public void onError(Call call, Exception e, int id) {
                Log.e(TAG, "onError: 请求失败：" + (e == null ? "NULL" : e.getMessage()));
                if (callback != null) {
                    callback.getActiveCodeFailed(e.getMessage(), wifiMac, localMac);
                }
            }

            @Override
            public void onResponse(String response, int id) {
                Log.e(TAG, "onResponse: 响应:" + response);
                if (TextUtils.isEmpty(response)) {
                    if (callback != null) {
                        callback.getActiveCodeFailed("response is null", wifiMac, localMac);
                    }
                    return;
                }
                try {
                    ActiveCodeResponse activeCodeResponse = new Gson().fromJson(response, ActiveCodeResponse.class);
                    if (activeCodeResponse.getStatus() != 1) {
                        if (callback != null) {
                            callback.getActiveCodeFailed(activeCodeResponse.getMessage(), wifiMac, localMac);
                        }
                        return;
                    }

                    activeSDK(context, activeCodeResponse.getActivateCode(), activeCodeResponse.getAppid(), activeCodeResponse.getSdk_key(), callback);
                } catch (Exception e) {
                    e.printStackTrace();
                    if (callback != null) {
                        callback.getActiveCodeFailed(e.getMessage(), wifiMac, localMac);
                    }
                }
            }
        });
    }

    private static void activeSDK(Activity context, String activateCode, String appid, String sdk_key, ActiveCallback callback) {
        int activeCode = FaceEngine.activeOnline(APP.getContext(), activateCode, appid, sdk_key);
        Log.e(TAG, "激活结果：: " + activeCode);
        if (activeCode != ErrorInfo.MOK && activeCode != ErrorInfo.MERR_ASF_ALREADY_ACTIVATED) {
            if (callback != null) {
                callback.getActiveCodeFailed(context.getResources().getString(R.string.splash_active_failed) + ":" + activeCode, wifiMac, localMac);
            }
            returnActiveStatus(2, activateCode, -1);
            return;
        }

        if (callback != null) {
            callback.getActiveCodeSuccess();
        }

        returnActiveStatus(1, activateCode, activeCode);
        if (activeCode == ErrorInfo.MOK) {

        }
    }

    private static void returnActiveStatus(int re_status, String activeCode, int aCode) {
        ActiveFileInfo activeFileInfo = null;
        if (re_status == 1) {
            activeFileInfo = new ActiveFileInfo();
            FaceEngine.getActiveFileInfo(APP.getContext(), activeFileInfo);
        }

        String url = ResourceUpdate.RETURN_ACTIVE_STATUS;
        Map<String, String> params = new HashMap<>();
        params.put("deviceNo", HeartBeatClient.getDeviceNo());
        params.put("re_status", re_status + "");
        params.put("errorCode", String.valueOf(aCode));
        params.put("activateCode", activeCode);
        String wifiMac = CommonUtils.getWifiMac();
        if (!TextUtils.isEmpty(wifiMac)) {
            wifiMac = wifiMac.replace(":", "-");
        } else {
            wifiMac = "";
        }
        params.put("wifiMac", wifiMac);

        String localMac = CommonUtils.getLocalMac();
        if (!TextUtils.isEmpty(localMac)) {
            localMac = localMac.replace(":", "-");
        } else {
            localMac = "";
        }
        params.put("localMac", localMac);
        if (activeFileInfo != null) {
            params.put("startTime", activeFileInfo != null ? activeFileInfo.getStartTime() : "");
            params.put("endTime", activeFileInfo != null ? activeFileInfo.getEndTime() : "");
        }
        Log.e(TAG, "返回激活结果：" + url);
        Log.e(TAG, "参数：" + params.toString());
        OkHttpUtils.post().url(url).params(params).build().execute(new StringCallback() {
            @Override
            public void onError(Call call, Exception e, int id) {
                Log.e(TAG, "onError: 失败：" + (e == null ? "NULL" : e.getMessage()));
            }

            @Override
            public void onResponse(String response, int id) {
                Log.e(TAG, "onResponse: 结果：" + response);
            }
        });
    }

    public interface ActiveCallback {
        void getActiveCodeFailed(String message, String wifiMac, String localMac);

        void getActiveCodeSuccess();
    }

}
