package com.yunbiao.ybsmartcheckin_live_id.business;

import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;


import com.umeng.analytics.MobclickAgent;
import com.yunbiao.faceview.FaceManager;
import com.yunbiao.ybsmartcheckin_live_id.APP;
import com.yunbiao.ybsmartcheckin_live_id.FlavorType;
import com.yunbiao.ybsmartcheckin_live_id.R;
import com.yunbiao.ybsmartcheckin_live_id.activity.Event.UpdateInfoEvent;
import com.yunbiao.ybsmartcheckin_live_id.afinel.Constants;
import com.yunbiao.ybsmartcheckin_live_id.afinel.ResourceUpdate;
import com.yunbiao.ybsmartcheckin_live_id.bean.CompanyResponse;
import com.yunbiao.ybsmartcheckin_live_id.bean.StaffResponse;
import com.yunbiao.ybsmartcheckin_live_id.bean.WhiteListBean;
import com.yunbiao.ybsmartcheckin_live_id.db2.Company;
import com.yunbiao.ybsmartcheckin_live_id.db2.DaoManager;
import com.yunbiao.ybsmartcheckin_live_id.db2.Depart;
import com.yunbiao.ybsmartcheckin_live_id.db2.User;
import com.yunbiao.ybsmartcheckin_live_id.db2.UserInfo;
import com.yunbiao.ybsmartcheckin_live_id.db2.White;
import com.yunbiao.ybsmartcheckin_live_id.exception.CrashHandler2;
import com.yunbiao.ybsmartcheckin_live_id.system.HeartBeatClient;
import com.yunbiao.ybsmartcheckin_live_id.utils.SpUtils;
import com.yunbiao.ybsmartcheckin_live_id.utils.UIUtils;
import com.yunbiao.ybsmartcheckin_live_id.utils.xutil.MyXutils;
import com.yunbiao.ybsmartcheckin_live_id.views.SyncDialog;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.StringCallback;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Request;
import timber.log.Timber;

/**
 * Created by Administrator on 2019/5/14.
 */

public class SyncManager {
    private static final String TAG = "SyncManager";

    private static SyncManager instance;
    private boolean isFirst = true;
    private boolean isFirstNotBind = true;

    //    private long SYNC_OFFSET = 4 * 60 * 60 * 1000;//更新间隔时间4天
    private long SYNC_OFFSET = 20 * 60 * 1000;//更新间隔时间4天
    private final ExecutorService executorService;

    public static SyncManager instance() {
        if (instance == null) {
            synchronized (SyncManager.class) {
                if (instance == null) {
                    instance = new SyncManager();
                }
            }
        }
        return instance;
    }

    private SyncManager() {
        executorService = Executors.newSingleThreadExecutor();
        File file = new File(Constants.HEAD_PATH);
        if (!file.exists()) {
            file.mkdirs();
        }
        SyncDialog.instance().init(APP.getMainActivity());
    }

    /*================================================================*/

    public void retryOnluCompany(int tag) {
        switch (tag) {
            case 1:
                SyncDialog.setStep(APP.getContext().getString(R.string.sync_not_depart));
                break;
            case 2:
                SyncDialog.setStep(APP.getContext().getString(R.string.sync_not_bind));
                break;
            case -1:
                SyncDialog.setStep(APP.getContext().getString(R.string.sync_params_error));
                break;
            case 4:
                SyncDialog.setStep(APP.getContext().getString(R.string.sync_not_bind));
                break;
            default:
                SyncDialog.setStep(APP.getContext().getString(R.string.sync_get_failed));
                break;
        }
        d("重新获取公司信息");
        new Handler().postDelayed(() -> requestOnlyCompany(), 30 * 1000);
    }

    /***
     * 仅获取公司信息
     */
    public void requestOnlyCompany() {
        show();
        SyncDialog.setStep(APP.getContext().getString(R.string.sync_company_1));
        d("获取公司信息");
        d("地址：" + ResourceUpdate.COMPANYINFO);
        d("参数：" + HeartBeatClient.getDeviceNo());
        CrashHandler2.getInstance().addTestText("address：" + ResourceUpdate.COMPANYINFO + "\n");
        CrashHandler2.getInstance().addTestText("params：" + HeartBeatClient.getDeviceNo() + "\n");
        OkHttpUtils.post()
                .url(ResourceUpdate.COMPANYINFO)
                .addParams("deviceNo", HeartBeatClient.getDeviceNo())
                .build().execute(new StringCallback() {
            @Override
            public void onError(Call call, Exception e, int id) {
                CrashHandler2.getInstance().addTestText("error：" + (e == null ? "NULL" : e.getMessage()) + "\n");
                d(e);
                if (isFirst) {//如果是第一次则加载缓存
                    EventBus.getDefault().post(new UpdateInfoEvent());
                    isFirst = false;
                }
                retryOnluCompany(0);
            }

            @Override
            public void onResponse(String response, int id) {
                CrashHandler2.getInstance().addTestText(response);
                d(response);
                if (TextUtils.isEmpty(response)) {
                    retryOnluCompany(0);
                    return;
                }
                CompanyResponse companyResponse = null;
                try {
                    companyResponse = new Gson().fromJson(response, CompanyResponse.class);
                } catch (Exception e) {
                    e.printStackTrace();
                    retryOnluCompany(2);
                    return;
                }
                if (companyResponse == null) {
                    retryOnluCompany(0);
                    return;
                }

                if (companyResponse.getStatus() != 1 && companyResponse.getStatus() != 5) {
                    if (companyResponse.getStatus() == 4) {
                        if (isFirstNotBind) {
                            EventBus.getDefault().post(new UpdateInfoEvent());
                            isFirst = false;

                            deleteNotBindUser();
                        }
                        saveCompanyInfo(null);
                        dissmissDialog();
                    }
                    retryOnluCompany(companyResponse.getStatus());
                    return;
                }

                show();

                //同步部门数据
                Company company = companyResponse.getCompany();

                long currTime = System.currentTimeMillis();
                long lastTime = SpUtils.getLong(SpUtils.LAST_INIT_TIME);
                int comid = SpUtils.getCompany().getComid();

                Log.e("canSync", "currTime-----------> " + currTime);
                Log.e("canSync", "lastTime-----------> " + lastTime);
                Log.e("canSync", "SYNC_OFFSET-----------> " + SYNC_OFFSET);
                Log.e("canSync", "comid-----------> " + comid);
                Log.e("canSync", "company.getComid()-----------> " + company.getComid());

                //保存部门数据
                saveCompanyInfo(company);
                d("保存部门数据");

                //初始化存储路径
                Constants.initStorage();
                d("初始化存储路径");

                //发送系统信息更新事件
                EventBus.getDefault().post(new UpdateInfoEvent());
                d("发送系统信息更新事件");

                dissmissDialog();
            }
        });
    }

    private void deleteNotBindUser() {
        List<User> users = DaoManager.get().queryAll(User.class);
        if (users == null || users.size() <= 0) {
            return;
        }
        for (int i = 0; i < users.size(); i++) {
            User user = users.get(i);
            if (user.getCompanyId() != Constants.NOT_BIND_COMPANY_ID) {
                DaoManager.get().delete(user);
            }
        }
    }

    /***
     * 1.获取公司数据
     */
    public void requestCompany() {
        show();

        SyncDialog.setStep(APP.getContext().getString(R.string.sync_company_1));
        d("获取公司信息");
        d("地址：" + ResourceUpdate.COMPANYINFO);
        d("参数：" + HeartBeatClient.getDeviceNo());
        CrashHandler2.getInstance().addTestText("获取公司信息\n");
        CrashHandler2.getInstance().addTestText("地址：" + ResourceUpdate.COMPANYINFO + "\n");
        CrashHandler2.getInstance().addTestText("参数：" + HeartBeatClient.getDeviceNo() + "\n");

        OkHttpUtils.post()
                .url(ResourceUpdate.COMPANYINFO)
                .addParams("deviceNo", HeartBeatClient.getDeviceNo())
                .build().execute(new StringCallback() {
            @Override
            public void onError(Call call, Exception e, int id) {
                CrashHandler2.getInstance().addTestText("异常：" + (e == null ? "NULL" : e.getMessage()) + "\n");
                d(e);
                if (isFirst) {//如果是第一次则加载缓存
                    EventBus.getDefault().post(new UpdateInfoEvent());
                    isFirst = false;
                }
                retryGetCompany(0);
            }

            @Override
            public void onResponse(String response, int id) {
                CrashHandler2.getInstance().addTestText(response);
                d(response);
                if (TextUtils.isEmpty(response)) {
                    retryGetCompany(0);
                    return;
                }
                CompanyResponse companyResponse = null;
                try {
                    companyResponse = new Gson().fromJson(response, CompanyResponse.class);
                } catch (Exception e) {
                    e.printStackTrace();
                    StringBuffer stringBuffer = new StringBuffer();
                    stringBuffer.append(HeartBeatClient.getDeviceNo())
                            .append("\n").append("" + response);
                    MobclickAgent.reportError(APP.getContext(), stringBuffer.toString());
                    retryOnluCompany(2);
                    return;
                }
                if (companyResponse.getStatus() != 1 && companyResponse.getStatus() != 5) {
                    if (companyResponse.getStatus() == 4) {
                        if (isFirstNotBind) {
                            EventBus.getDefault().post(new UpdateInfoEvent());
                            isFirst = false;
                        }
                        saveCompanyInfo(null);
                        dissmissDialog();
                    }
                    retryGetCompany(companyResponse.getStatus());
                    return;
                }

                //同步部门数据
                Company company = companyResponse.getCompany();

                long currTime = System.currentTimeMillis();
                long lastTime = SpUtils.getLong(SpUtils.LAST_INIT_TIME);
                int comid = SpUtils.getCompany().getComid();
                boolean canSync = currTime - lastTime > SYNC_OFFSET || comid != company.getComid();

                Log.e("canSync", "currTime-----------> " + currTime);
                Log.e("canSync", "lastTime-----------> " + lastTime);
                Log.e("canSync", "SYNC_OFFSET-----------> " + SYNC_OFFSET);
                Log.e("canSync", "comid-----------> " + comid);
                Log.e("canSync", "company.getComid()-----------> " + company.getComid());

                //设置闹钟
//                AlarmManagerUtil.setAlarm(companyResponse.getCompany());

                //保存部门数据
                saveCompanyInfo(company);
                d("保存部门数据");

                //初始化存储路径
                Constants.initStorage();
                d("初始化存储路径");

                //初始化人脸库
                FaceManager.getInstance().init(APP.getContext());

                //发送系统信息更新事件
                EventBus.getDefault().post(new UpdateInfoEvent());
                d("发送系统信息更新事件");

                //请求同步员工
                requestUser();
            }
        });
    }

    public interface SyncCallback{
        void onFailed(String message);
        void onFinish();
    }
    public void requestUser(SyncCallback callback){
        int comid = SpUtils.getCompany().getComid();
        Map<String, String> params = new HashMap<>();
        params.put("companyId", comid + "");
        d("地址：" + ResourceUpdate.GETSTAFF);
        d("参数：" + params.toString());
        OkHttpUtils.post()
                .url(ResourceUpdate.GETSTAFF)
                .params(params)
                .build()
                .execute(new StringCallback() {
                    @Override
                    public void onError(Call call, Exception e, int id) {
                        d(e);
                        callback.onFailed(e.getMessage());
                    }

                    @Override
                    public void onResponse(String response, int id) {
                        d(response);
                        if (TextUtils.isEmpty(response)) {
                            callback.onFailed("Response is empty");
                            return;
                        }

                        StaffResponse staffResponse = null;
                        try {
                            staffResponse = new Gson().fromJson(response, StaffResponse.class);
                        } catch (Exception e) {
                            callback.onFailed("Data format error");
                            return;
                        }

                        if (staffResponse == null) {
                            callback.onFailed("Response is empty");
                            return;
                        }

                        if (staffResponse.getStatus() != 1) {
                            callback.onFailed(String.valueOf(staffResponse.getStatus()));
                            return;
                        }

                        List<Depart> dep = staffResponse.getDep();
                        sync(comid, dep,callback);
                    }
                });
    }
    /***
     * 3.同步数据库
     * @param comId 公司ID
     * @param departList 部门列表
     */
    private void sync(final int comId, final List<Depart> departList,SyncCallback callback) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                SyncDialog.setStep(APP.getContext().getString(R.string.sync_database_3));
                Map<String, File> allFaceMap = FaceManager.getInstance().getAllFaceMap();

                //生成统一的部门列表和员工列表（此为远程数据）
                Map<Long, Depart> departMap = new HashMap<>();
                Map<String, User> userMap = new HashMap<>();
                if (departList != null) {
                    for (Depart depart : departList) {
                        depart.setCompId(comId);
                        depart.setId(depart.getDepId());
                        departMap.put(depart.getDepId(), depart);

                        List<User> entry = depart.getEntry();
                        for (User user : entry) {
                            user.setCompanyId(comId);

                            user.setDepartId(depart.getDepId());
                            user.setDepartName(depart.getDepName());
                            String head = user.getHead();
                            if (!TextUtils.isEmpty(head)) {
                                String filepath = Constants.HEAD_PATH + head.substring(head.lastIndexOf("/") + 1);
                                user.setHeadPath(filepath);
                            }
                            userMap.put(user.getFaceId(), user);
                        }
                    }
                }
                d("部门数据：" + departMap.size());
                d("用户数据：" + userMap.size());

                //查询当前库中的数据
                List<Depart> departs = DaoManager.get().queryDepartByCompId(comId);
                List<User> users = DaoManager.get().queryUserByCompId(comId);
                d("更新前部门库数据：" + (departs == null ? 0 : departs.size()));
                d("更新前员工库数据：" + (users == null ? 0 : users.size()));

                //检查不存在的部门和用户并删除
                if(departs != null && departs.size() > 0){
                    for (Depart depart : departs) {
                        long depId = depart.getDepId();
                        if (!departMap.containsKey(depId)) {
                            DaoManager.get().delete(depart);
                        }
                    }
                }

                if(users != null && users.size() > 0){
                    for (User user : users) {
                        String faceId = user.getFaceId();
                        if (!userMap.containsKey(faceId)) {
                            DaoManager.get().delete(user);
                        } else {
                            userMap.get(faceId).setAddTag(user.getAddTag());
                        }
                    }
                }

                for (Map.Entry<String, File> entry : allFaceMap.entrySet()) {
                    if (!userMap.containsKey(entry.getKey())) {
                        entry.getValue().delete();
                    }
                }

                //添加、更新部门和用户
                for (Map.Entry<Long, Depart> entry : departMap.entrySet()) {
                    Depart value = entry.getValue();
                    DaoManager.get().addOrUpdate(value);
                }
                for (Map.Entry<String, User> entry : userMap.entrySet()) {
                    User value = entry.getValue();
                    DaoManager.get().addOrUpdate(value);
                }

                //再次检查库中数据是否正确
                departs = DaoManager.get().queryDepartByCompId(comId);
                users = DaoManager.get().queryUserByCompId(comId);
                d("更新后部门库数据：" + (departs == null ? 0 : departs.size()));
                d("更新后员工库数据：" + (users == null ? 0 : users.size()));

                //生成下载队列
                final Queue<User> userQueue = new LinkedList<>();
                if(users != null && users.size() > 0){
                    for (User user : users) {
                        String headPath = user.getHeadPath();
                        File file = new File(headPath);
                        if (file == null || !file.exists()) {
                            userQueue.offer(user);
                        }
                    }
                }
                d("没有头像的用户数量：" + userQueue.size());

                final int totalSize = userQueue.size();
                if (totalSize != 0) {
                    SyncDialog.setStep(APP.getContext().getString(R.string.sync_head_4));
                    SyncDialog.setProgress(1, totalSize);
                }

                //开始下载
                download(userQueue, new DownloadCallback() {
                    @Override
                    public void onSingleComplete(User user, File result) {
                        int progress = totalSize - userQueue.size();
                        SyncDialog.setProgress(progress, totalSize);
                        user.setAddTag(UserInfo.HEAD_HAS_UPDATE);
                        user.setHeadPath(result.getPath());
                        DaoManager.get().addOrUpdate(user);
                    }

                    @Override
                    public void onSingleFailed(User user, Throwable ex) {
                        int progress = totalSize - userQueue.size();
                        SyncDialog.setProgress(progress, totalSize);
                        user.setAddTag(UserInfo.HEAD_DOWNLOAD_FAILED);
                        DaoManager.get().addOrUpdate(user);
                    }

                    @Override
                    public void onFinished() {
                        updateFace(callback);
                    }
                });
            }
        });
    }

    /***
     * 5.更新人脸库
     */
    private void updateFace(SyncCallback callback) {
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                SyncDialog.setStep(APP.getContext().getString(R.string.sync_face_5));
                Log.e(TAG, "run: 开始同步人脸库");
                Company company = SpUtils.getCompany();
                List<User> users = DaoManager.get().queryUserByCompId(company.getComid());
                Log.e(TAG, "updateFace: 用户总数：" + (users == null ? 0 : users.size()));

                if (users == null) {
                    callback.onFinish();
                    return;
                }

                int totalSize = users.size();
                for (int i = 0; i < totalSize; i++) {
                    SyncDialog.setProgress(i, totalSize);

                    User user = users.get(i);
                    String faceId = user.getFaceId();
                    String headPath = user.getHeadPath();
                    int addTag = user.getAddTag();

                    //判断照片是否更新
                    boolean isExists = FaceManager.getInstance().checkFace(faceId);

                    //人脸文件存在，并且状态是无需更新时则不继续
                    if (addTag != UserInfo.HEAD_HAS_UPDATE && isExists) {
                        Log.e(TAG, "照片未更新且文件存在：" + i + " --- " + faceId);
                        continue;
                    }

                    //添加进库
                    boolean addUser = FaceManager.getInstance().addUser(faceId, headPath);

                    //添加人脸库失败，并且addTag为下载图片失败或添加人脸库失败的时候，无需更新数据库
                    if ((!addUser) && (addTag == UserInfo.HEAD_DOWNLOAD_FAILED || addTag == UserInfo.ADD_FACE_DB_FAILED)) {
                        Log.e(TAG, "添加失败且无需更新：" + i + " --- " + faceId + " --- " + addTag);
                        continue;
                    }

                    addTag = addUser ? UserInfo.HANDLE_SUCCESS : UserInfo.ADD_FACE_DB_FAILED;
                    user.setAddTag(addTag);
                    long l = DaoManager.get().update(user);
                    Log.e(TAG, "添加人脸库结果：" + addUser + " --- " + i + " --- " + faceId + " --- 更新数据库结果：" + l);
                }

                FaceManager.getInstance().reloadRegisterList();
                int size = FaceManager.getInstance().getTotalSize();
                Log.e(TAG, "onFinished: 完毕后库中数据：" + size);
                dissmissDialog();

                callback.onFinish();
            }
        });
    }

    /***
     * 2.获取员工数据
     */
    public void requestUser() {
        show();
        d("请求员工信息");
        SyncDialog.setStep(APP.getContext().getString(R.string.sync_staff_2));
        final int comid = SpUtils.getInt(SpUtils.COMPANYID);
        Map<String, String> params = new HashMap<>();
        params.put("companyId", comid + "");
        d("地址：" + ResourceUpdate.GETSTAFF);
        d("参数：" + params.toString());
        OkHttpUtils.post()
                .url(ResourceUpdate.GETSTAFF)
                .params(params)
                .build()
                .execute(new StringCallback() {
                    @Override
                    public void onError(Call call, Exception e, int id) {
                        d(e);
                        retryRequestUser(0);
                    }

                    @Override
                    public void onResponse(String response, int id) {
                        d(response);
                        if (TextUtils.isEmpty(response)) {
                            retryRequestUser(0);
                            return;
                        }

                        StaffResponse staffResponse = null;
                        try {
                            staffResponse = new Gson().fromJson(response, StaffResponse.class);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        if (staffResponse == null) {
                            retryRequestUser(0);
                            return;
                        }

                        if (staffResponse.getStatus() != 1) {
                            retryRequestUser(staffResponse.getStatus());
                            return;
                        }

                        List<Depart> dep = staffResponse.getDep();
                        sync(comid, dep);
                    }
                });
    }

    /***
     * 3.同步数据库
     * @param comId 公司ID
     * @param departList 部门列表
     */
    private void sync(final int comId, final List<Depart> departList) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                SyncDialog.setStep(APP.getContext().getString(R.string.sync_database_3));
                Map<String, File> allFaceMap = FaceManager.getInstance().getAllFaceMap();

                //生成统一的部门列表和员工列表（此为远程数据）
                Map<Long, Depart> departMap = new HashMap<>();
                Map<String, User> userMap = new HashMap<>();
                if (departList != null) {
                    for (Depart depart : departList) {
                        depart.setCompId(comId);
                        depart.setId(depart.getDepId());
                        departMap.put(depart.getDepId(), depart);

                        List<User> entry = depart.getEntry();
                        for (User user : entry) {
                            user.setCompanyId(comId);

                            user.setDepartId(depart.getDepId());
                            user.setDepartName(depart.getDepName());
                            String head = user.getHead();
                            if (!TextUtils.isEmpty(head)) {
                                String filepath = Constants.HEAD_PATH + head.substring(head.lastIndexOf("/") + 1);
                                user.setHeadPath(filepath);
                            }
                            userMap.put(user.getFaceId(), user);
                        }
                    }
                }
                d("部门数据：" + departMap.size());
                d("用户数据：" + userMap.size());

                //查询当前库中的数据
                List<Depart> departs = DaoManager.get().queryDepartByCompId(comId);
                List<User> users = DaoManager.get().queryUserByCompId(comId);
                d("更新前部门库数据：" + (departs == null ? 0 : departs.size()));
                d("更新前员工库数据：" + (users == null ? 0 : users.size()));

                //检查不存在的部门和用户并删除
                if(departs != null && departs.size() > 0){
                    for (Depart depart : departs) {
                        long depId = depart.getDepId();
                        if (!departMap.containsKey(depId)) {
                            DaoManager.get().delete(depart);
                        }
                    }
                }

                if(users != null && users.size() > 0){
                    for (User user : users) {
                        String faceId = user.getFaceId();
                        if (!userMap.containsKey(faceId)) {
                            DaoManager.get().delete(user);
                        } else {
                            userMap.get(faceId).setAddTag(user.getAddTag());
                        }
                    }
                }

                for (Map.Entry<String, File> entry : allFaceMap.entrySet()) {
                    if (!userMap.containsKey(entry.getKey())) {
                        entry.getValue().delete();
                    }
                }

                //添加、更新部门和用户
                for (Map.Entry<Long, Depart> entry : departMap.entrySet()) {
                    Depart value = entry.getValue();
                    DaoManager.get().addOrUpdate(value);
                }
                for (Map.Entry<String, User> entry : userMap.entrySet()) {
                    User value = entry.getValue();
                    DaoManager.get().addOrUpdate(value);
                }

                //再次检查库中数据是否正确
                departs = DaoManager.get().queryDepartByCompId(comId);
                users = DaoManager.get().queryUserByCompId(comId);
                d("更新后部门库数据：" + (departs == null ? 0 : departs.size()));
                d("更新后员工库数据：" + (users == null ? 0 : users.size()));

                //生成下载队列
                final Queue<User> userQueue = new LinkedList<>();
                for (User user : users) {
                    String headPath = user.getHeadPath();
                    File file = new File(headPath);
                    if (file == null || !file.exists()) {
                        userQueue.offer(user);
                    }
                }
                d("没有头像的用户数量：" + userQueue.size());

                final int totalSize = userQueue.size();
                if (totalSize != 0) {
                    SyncDialog.setStep(APP.getContext().getString(R.string.sync_head_4));
                    SyncDialog.setProgress(1, totalSize);
                }

                //开始下载
                download(userQueue, new DownloadCallback() {
                    @Override
                    public void onSingleComplete(User user, File result) {
                        int progress = totalSize - userQueue.size();
                        SyncDialog.setProgress(progress, totalSize);
                        user.setAddTag(UserInfo.HEAD_HAS_UPDATE);
                        user.setHeadPath(result.getPath());
                        DaoManager.get().addOrUpdate(user);
                    }

                    @Override
                    public void onSingleFailed(User user, Throwable ex) {
                        int progress = totalSize - userQueue.size();
                        SyncDialog.setProgress(progress, totalSize);
                        user.setAddTag(UserInfo.HEAD_DOWNLOAD_FAILED);
                        DaoManager.get().addOrUpdate(user);
                    }

                    @Override
                    public void onFinished() {
                        updateFace();
                    }
                });
            }
        });
    }

    /***
     * 4.下载图片
     * @param queue
     * @param callback
     */
    private void download(final Queue<User> queue, final DownloadCallback callback) {
        if (queue == null || queue.size() <= 0) {
            callback.onFinished();
            return;
        }

        final User user = queue.poll();
        Log.e(TAG, "下载：" + user.getName() + " —— " + user.getHead());
        String head = user.getHead();
        MyXutils.getInstance().downLoadFile(head, user.getHeadPath(), false, new MyXutils.XDownLoadCallBack() {
            @Override
            public void onLoading(long total, long current, boolean isDownloading) {
                Log.e(TAG, "下载进度... " + current + " —— " + total);
            }

            @Override
            public void onSuccess(File result) {
                Log.e(TAG, "下载成功... " + result.getPath());
                callback.onSingleComplete(user, result);
            }

            @Override
            public void onError(Throwable ex) {
                Log.e(TAG, "下载失败... " + (ex != null ? ex.getMessage() : "NULL"));
                callback.onSingleFailed(user, ex);
            }

            @Override
            public void onFinished() {
                download(queue, callback);
            }
        });
    }

    /***
     * 5.更新人脸库
     */
    private void updateFace() {
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                SyncDialog.setStep(APP.getContext().getString(R.string.sync_face_5));
                Log.e(TAG, "run: 开始同步人脸库");
                Company company = SpUtils.getCompany();
                List<User> users = DaoManager.get().queryUserByCompId(company.getComid());
                Log.e(TAG, "updateFace: 用户总数：" + (users == null ? 0 : users.size()));

                if (users == null) {
                    dissmissDialog();
                    return;
                }

                int totalSize = users.size();
                for (int i = 0; i < totalSize; i++) {
                    SyncDialog.setProgress(i, totalSize);

                    User user = users.get(i);
                    String faceId = user.getFaceId();
                    String headPath = user.getHeadPath();
                    int addTag = user.getAddTag();

                    //判断照片是否更新
                    boolean isExists = FaceManager.getInstance().checkFace(faceId);

                    //人脸文件存在，并且状态是无需更新时则不继续
                    if (addTag != UserInfo.HEAD_HAS_UPDATE && isExists) {
                        Log.e(TAG, "照片未更新且文件存在：" + i + " --- " + faceId);
                        continue;
                    }

                    //添加进库
                    boolean addUser = FaceManager.getInstance().addUser(faceId, headPath);

                    //添加人脸库失败，并且addTag为下载图片失败或添加人脸库失败的时候，无需更新数据库
                    if ((!addUser) && (addTag == UserInfo.HEAD_DOWNLOAD_FAILED || addTag == UserInfo.ADD_FACE_DB_FAILED)) {
                        Log.e(TAG, "添加失败且无需更新：" + i + " --- " + faceId + " --- " + addTag);
                        continue;
                    }

                    addTag = addUser ? UserInfo.HANDLE_SUCCESS : UserInfo.ADD_FACE_DB_FAILED;
                    user.setAddTag(addTag);
                    long l = DaoManager.get().update(user);
                    Log.e(TAG, "添加人脸库结果：" + addUser + " --- " + i + " --- " + faceId + " --- 更新数据库结果：" + l);
                }

                FaceManager.getInstance().reloadRegisterList();
                int size = FaceManager.getInstance().getTotalSize();
                Log.e(TAG, "onFinished: 完毕后库中数据：" + size);
                dissmissDialog();
            }
        });
    }

    /***
     * 检查并更新人脸库
     * @param userQueue
     * @param callback
     */
    private void addOrUpdateFace(final Queue<User> userQueue, final AddFaceCallback callback) {
        if (userQueue == null || userQueue.size() <= 0) {
            FaceManager.getInstance().reloadRegisterList();
            callback.onFinished();
            return;
        }

        final User user = userQueue.poll();
        String faceId = user.getFaceId();
        String headPath = user.getHeadPath();

        //不管存不存在，直接更新
        boolean b = FaceManager.getInstance().addUser(faceId, headPath);
        Log.e(TAG, "addOrUpdateFace: 添加：" + faceId + " --- " + b);
        user.setAddTag(b ? 1 : -1);
        DaoManager.get().addOrUpdate(user);

        callback.onSingleAddResult(user, 0, b, 1);
        addOrUpdateFace(userQueue, callback);
    }

    public void syncUsers(JSONArray jsonArray) {
        final List<User> users = new ArrayList<>();
        try {
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                User user = new User();
                int companyid = SpUtils.getInt(SpUtils.COMPANYID);
                user.setCompanyId(companyid);
                user.setName(jsonObject.getString("name"));
                user.setSex(jsonObject.getInt("sex"));
                user.setHead(jsonObject.getString("head"));
                user.setFaceId(jsonObject.getString("faceId"));
                user.setDepartId(jsonObject.getInt("depId"));
                user.setCardId(jsonObject.getString("cardId"));
                user.setId(jsonObject.getInt("id"));
                user.setNumber(jsonObject.getString("number"));
                user.setPosition(jsonObject.getString("position"));
                String head = jsonObject.getString("head");
                if (!TextUtils.isEmpty(head)) {
                    String filepath = Constants.HEAD_PATH + head.substring(head.lastIndexOf("/") + 1);
                    user.setHeadPath(filepath);
                }
                users.add(user);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        //生成下载队列
        final Queue<User> userQueue = new LinkedList<>();
        for (User user : users) {
            String headPath = user.getHeadPath();
            if (headPath != null) {
                File file = new File(headPath);
                if (file == null || !file.exists()) {
                    userQueue.offer(user);
                }
            }
        }
        d("没有头像的用户数量：" + userQueue.size());

        final int totalSize = userQueue.size();
        if (totalSize != 0) {
            SyncDialog.setStep(APP.getContext().getString(R.string.sync_head_4));
            SyncDialog.setProgress(1, totalSize);
        }

        //开始下载
        download(userQueue, new DownloadCallback() {
            @Override
            public void onSingleComplete(User user, File result) {
                int progress = totalSize - userQueue.size();
                SyncDialog.setProgress(progress, totalSize);
                user.setAddTag(UserInfo.HEAD_HAS_UPDATE);
                user.setHeadPath(result.getPath());
                DaoManager.get().addOrUpdate(user);
            }

            @Override
            public void onSingleFailed(User user, Throwable ex) {
                int progress = totalSize - userQueue.size();
                SyncDialog.setProgress(progress, totalSize);
                user.setAddTag(UserInfo.HEAD_DOWNLOAD_FAILED);
                DaoManager.get().addOrUpdate(user);
            }

            @Override
            public void onFinished() {
                updateUserFace(users);
            }
        });
    }

    private void updateUserFace(final List<User> users) {
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                Log.e(TAG, "updateFace: 用户总数：" + (users == null ? 0 : users.size()));
                final Queue<User> userQueue = new LinkedList<>();
                userQueue.addAll(users);
                final int totalSize = userQueue.size();
                addOrUpdateFace(userQueue, new AddFaceCallback() {
                    @Override
                    public void onSingleAddResult(User user, int type, boolean result, int tag) {
                        user.setAddTag(result ? 0 : type == 0 ? -2 : -3);
                        DaoManager.get().addOrUpdate(user);
                        int progress = totalSize - userQueue.size();
                        SyncDialog.setProgress(progress, totalSize);
                    }

                    @Override
                    public void onFinished() {
                        int size = FaceManager.getInstance().getTotalSize();
                        Log.e(TAG, "onFinished: 完毕后库中数据：" + size);
                        dissmissDialog();
                    }
                });
            }
        });
    }

    private void dissmissDialog() {
        SpUtils.saveLong(SpUtils.LAST_INIT_TIME, System.currentTimeMillis());
        SyncDialog.instance().dismiss();
        VisitorManager.getInstance().autoSyncVisitor();
    }

    private void show() {
        int comid = SpUtils.getCompany().getComid();
        if (comid == Constants.NOT_BIND_COMPANY_ID) {
            return;
        }

        if (!SyncDialog.instance().isShown()) {
            SyncDialog.instance().show();
        }
    }

    public void destory() {
        OkHttpUtils.getInstance().cancelTag(this);
        SyncDialog.instance().dismiss();
    }

    /*===========判断方法=====================================================================================*/
    private void retryGetCompany(int tag) {
        switch (tag) {
            case 1:
                SyncDialog.setStep(APP.getContext().getString(R.string.sync_not_depart));
                break;
            case -1:
                SyncDialog.setStep(APP.getContext().getString(R.string.sync_params_error));
                break;
            case 4:
                SyncDialog.setStep(APP.getContext().getString(R.string.sync_not_bind));
                break;
            default:
                SyncDialog.setStep(APP.getContext().getString(R.string.sync_get_failed));
                break;
        }
        d("重新获取公司信息");
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                requestCompany();
            }
        }, 30 * 1000);
    }

    private void retryRequestUser(int tag) {
        Log.e(TAG, "retryRequestUser: ------------------ " + tag);

        switch (tag) {
            case 4:
                SyncDialog.setStep(APP.getContext().getString(R.string.sync_not_depart));
                break;
            default:
                SyncDialog.setStep(APP.getContext().getString(R.string.sync_get_failed));
                break;
        }
        d("重新获取员工信息");
        SyncDialog.setStep(APP.getContext().getString(R.string.sync_get_failed));
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                requestUser();
            }
        }, 10 * 1000);
    }

    private void saveCompanyInfo(Company company) {
        SpUtils.setCompany(company);
        if (company != null) {
            //存公司ID
            SpUtils.saveInt(SpUtils.COMPANYID, company.getComid());
            d("缓存公司信息");
            SpUtils.saveStr(SpUtils.MENU_PWD, company.getDevicePwd());
            //2019.10.21 添加  获取是否显示职称，默认显示displayPosition，1是显示
            SpUtils.saveInt(SpUtils.DISPLAYPOSITION, company.getDisplayPosition());
        } else {
            SpUtils.saveInt(SpUtils.COMPANYID, 0);
        }
    }

    private void d(String log) {
        Timber.tag(this.getClass().getSimpleName());
        Timber.d(log);
    }

    private void d(Throwable t) {
        Timber.tag(this.getClass().getSimpleName());
        Timber.d(t);
    }

    public interface WhiteListSyncListener{
        void onSuccess();
        void onFailed(String msg);
    }

    public void requestWhiteList(WhiteListSyncListener listSyncListener) {
        String url = ResourceUpdate.CERTIFICATES_WHITE_LIST;
        Log.e(TAG, "requestWhiteList: " + url);
        OkHttpUtils.post().url(url).addParams("deviceNo", HeartBeatClient.getDeviceNo()).build().execute(new StringCallback() {
            @Override
            public void onError(Call call, Exception e, int id) {
                Log.e(TAG, "onError: 白名单获取失败：" + (e == null ? "NULL" : e.getMessage()));
                if(listSyncListener != null){
                    listSyncListener.onFailed((e == null ? "NULL" : e.getMessage()));
                }
            }

            @Override
            public void onResponse(String response, int id) {
                Log.e(TAG, "onResponse: 获取成功：" + response);

                WhiteListBean whiteListBean = new Gson().fromJson(response, WhiteListBean.class);
                if (whiteListBean == null) {
                    if(listSyncListener != null){
                        listSyncListener.onFailed("数据解析异常");
                    }
                    return;
                }

                if (whiteListBean.getStatus() != 1) {
                    if(listSyncListener != null){
                        listSyncListener.onFailed(whiteListBean.getMessage());
                    }
                    return;
                }

                boolean b = DaoManager.get().deleteAll(White.class);
                Log.e(TAG, "onResponse: 删除上一个白名单成功：" + b);
                List<White> whites = DaoManager.get().queryAll(White.class);
                Log.e(TAG, "onResponse: 检视数据：" + (whites == null ? 0 : whites.size()));

                List<WhiteListBean.WhiteBean> data = whiteListBean.getData();
                for (WhiteListBean.WhiteBean datum : data) {
                    String num1 = datum.getProvince();
                    String num2 = datum.getCity();
                    String num3 = datum.getCounty();

                    String num = num1;
                    if (!TextUtils.equals("00", num2)) {
                        num += num2;
                    }
                    if (!TextUtils.equals("00", num3)) {
                        num += num3;
                    }
                    White white = new White();
                    white.setNum(num);
                    long l = DaoManager.get().addOrUpdate(white);
                    Log.e(TAG, "onResponse: 白名单入库：" + l);
                }


                if(listSyncListener != null){
                    listSyncListener.onSuccess();
                }
            }
        });
    }

    public interface AddFaceCallback {
        void onSingleAddResult(User user, int type, boolean result, int tag);

        void onFinished();
    }

    private interface DownloadCallback {
        void onSingleComplete(User user, File result);

        void onSingleFailed(User user, Throwable ex);

        void onFinished();
    }
}
