package com.yunbiao.ybsmartcheckin_live_id;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.yunbiao.ybsmartcheckin_live_id.activity_temper_check_in.TemperModuleType;
import com.yunbiao.ybsmartcheckin_live_id.activity_temper_check_in.ThermalConst;
import com.yunbiao.ybsmartcheckin_live_id.activity_temper_check_in.ThermalImage2Activity;
import com.yunbiao.ybsmartcheckin_live_id.afinel.ResourceUpdate;
import com.yunbiao.ybsmartcheckin_live_id.common.power.PowerOffTool;
import com.yunbiao.ybsmartcheckin_live_id.db2.DaoManager;
import com.yunbiao.ybsmartcheckin_live_id.db2.Exception;
import com.yunbiao.ybsmartcheckin_live_id.system.HeartBeatClient;
import com.yunbiao.ybsmartcheckin_live_id.activity.base.BaseActivity;
import com.yunbiao.ybsmartcheckin_live_id.afinel.Constants;
import com.yunbiao.ybsmartcheckin_live_id.utils.CommonUtils;
import com.yunbiao.ybsmartcheckin_live_id.utils.SdCardUtils;
import com.yunbiao.ybsmartcheckin_live_id.utils.SpUtils;
import com.yunbiao.ybsmartcheckin_live_id.utils.ThreadUitls;
import com.yunbiao.ybsmartcheckin_live_id.utils.UIUtils;
import com.yunbiao.ybsmartcheckin_live_id.utils.ZXingUtils;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.StringCallback;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import pl.droidsonroids.gif.GifDrawable;
import pl.droidsonroids.gif.GifImageView;

public class SplashActivity extends BaseActivity {
    private static final String TAG = "SplashActivity";

    public static String[] PERMISSONS = {android.Manifest.permission.READ_EXTERNAL_STORAGE
            , android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            , android.Manifest.permission.ACCESS_FINE_LOCATION
            , android.Manifest.permission.ACCESS_COARSE_LOCATION
            , android.Manifest.permission.READ_PHONE_STATE
            , android.Manifest.permission.CAMERA
            /*,Manifest.permission.SYSTEM_ALERT_WINDOW*/};
    private YBPermission ybPermission;
    private TextView tvLocalMac;
    private TextView tvWifiMac;
    private View llCodeArea;
    private TextView tvError;
    private ImageView ivCode;
    private TextView tvJump;

    @Override
    protected int getPortraitLayout() {
        return R.layout.activity_splash;
    }

    @Override
    protected int getLandscapeLayout() {
        return R.layout.activity_splash;
    }

    @Override
    protected void initView() {
        super.initView();
        tvError = findViewById(R.id.tv_error_mac);
        ivCode = findViewById(R.id.iv_code);
        tvWifiMac = findViewById(R.id.tv_wifi_mac);
        tvLocalMac = findViewById(R.id.tv_local_mac);
        llCodeArea = findViewById(R.id.ll_code_area);
        tvJump = findViewById(R.id.tv_jump);
    }

    @Override
    protected void initData() {
        SdCardUtils.Capacity capacity = SdCardUtils.getUsedCapacity();
        double remainingSpace = capacity.getAll_mb() - capacity.getUsed_mb();
        String availSpace = String.format(getResString(R.string.alvailable_space),capacity.formatD(remainingSpace),"mb");
        if(remainingSpace <= 30){
            Toast.makeText(this, getResString(R.string.clean_storage_must), Toast.LENGTH_SHORT).show();
            finish();
        } else if (remainingSpace <= 160) {
            showAlert(getResString(R.string.clean_storage_must) + "\n"+ availSpace,false,null, APP::exit,0);
        } else if (capacity.getAll_mb() - capacity.getUsed_mb() < (capacity.getAll_mb() / 10)) {
            showAlert(getResString(R.string.clean_storage_please) + "\n"+ availSpace,true, APP::exit, this::checkPermission,45000);
        } else {
            checkPermission();
        }
    }

    private void checkPermission(){
        GifImageView gifImageView = findViewById(R.id.giv);
        setOpenGif(gifImageView);

        ybPermission = new YBPermission(permissionListener);
        ybPermission.checkPermission(this, PERMISSONS);
    }

    private void setOpenGif(GifImageView gifImageView) {
        File splashDir = new File(Constants.SPLASH_DIR_PATH);
        Log.e(TAG, "闪屏动画：" + splashDir.getPath());
        if (!splashDir.exists() || !splashDir.isDirectory()) {
            splashDir.mkdirs();
        }

        GifDrawable gifDrawable;

        File[] files = splashDir.listFiles(pathname -> pathname.isFile());
        if (files != null && files.length > 0) {
            Arrays.sort(files, (o1, o2) -> o1.isDirectory() && o2.isFile() ? -1 : o1.isFile() && o2.isDirectory() ? 1 : o1.getName().compareTo(o2.getName()));
            File file = files[0];
            try {
                gifDrawable = new GifDrawable(file);
                gifImageView.setImageDrawable(gifDrawable);
                gifDrawable.setLoopCount(0);
                gifDrawable.setSpeed(3.0f);
            } catch (IOException e) {
                gifImageView.setImageURI(Uri.fromFile(file));
            }
        } else {
            try {
                gifDrawable = new GifDrawable(getResources(), R.mipmap.splash);
                gifImageView.setImageDrawable(gifDrawable);
                gifDrawable.setLoopCount(0);
                gifDrawable.setSpeed(3.0f);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void showAlert(String msg, boolean showPositive, Runnable negativeRunnable, Runnable positiveRunnable,long delayTime) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(getResString(R.string.alert_title_warning))
                .setMessage(msg)
                .setCancelable(false)
                .setPositiveButton(getResString(R.string.base_ensure), (dialog, which) -> {
                    dialog.dismiss();
                    if (positiveRunnable != null) {
                        positiveRunnable.run();
                    }
                });
        if (showPositive) {
            builder.setNegativeButton(getResString(R.string.base_cancel), (dialog, which) -> {
                dialog.dismiss();
                if (negativeRunnable != null) {
                    negativeRunnable.run();
                }
            });
        }
        AlertDialog alertDialog = builder.create();
        alertDialog.show();

        if(delayTime > 0){
            Button btnPositive = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            CharSequence text = btnPositive.getText();
            CountDownTimer countDownTimer = new CountDownTimer(delayTime,1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    int second = millisUntilFinished < 1000 ? 0 : (int) (millisUntilFinished / 1000);
                    btnPositive.setText(text + "(" + second + ")");
                }

                @Override
                public void onFinish() {
                    btnPositive.performClick();
                }
            };
            countDownTimer.start();
        }
    }

    private YBPermission.PermissionListener permissionListener = new YBPermission.PermissionListener() {
        @Override
        public void onPermissionFailed(String[] objects) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(objects, YBPermission.PERMISSION_REQUEST_CODE);
            }
        }

        @Override
        public void onFinish(boolean isComplete) {
            if (!isComplete) {
                UIUtils.showShort(SplashActivity.this, getString(R.string.splash_request_permission_failed));
                return;
            }

            SpUtils.init();
            DaoManager.get().initDb();

            switch (Constants.FLAVOR_TYPE) {
                case FlavorType.PING_TECH:
                case FlavorType.PING_TECH_SHELTRON:
                    setIp("34.70.221.66","34.70.221.66","5222","80","");
                    break;
                case FlavorType.ITALY:
                    setIp("18.156.95.72","18.156.95.72","5222","80","");
                    break;
            }

            Constants.checkSetIp();
            Constants.initStorage();
            OutputLog.getInstance().initFile(Constants.LOCAL_ROOT_PATH);

            ThreadUitls.runInThread(() -> PowerOffTool.getPowerOffTool().machineStart());

            uploadException(nextRunnable);
        }
    };

    private void setIp(String sIp, String cIp, String xPort, String rPort, String pName) {
        int intOrDef = SpUtils.getIntOrDef(Constants.Key.SERVER_MODEL, -99);
        if (intOrDef == -99)
            SpUtils.saveInt(Constants.Key.SERVER_MODEL, Constants.serverModel.JU);

        String serIp = SpUtils.getStr(Constants.Key.JU_SERVICE_IP_CACHE, "");
        Log.e(TAG, "setIp: 设置的服务地址：" + serIp);
        if (TextUtils.isEmpty(serIp))
            SpUtils.saveStr(Constants.Key.JU_SERVICE_IP_CACHE, sIp);
        String resPort = SpUtils.getStr(Constants.Key.JU_RESOURCE_PORT_CACHE, "");
        if (TextUtils.isEmpty(resPort))
            SpUtils.saveStr(Constants.Key.JU_RESOURCE_PORT_CACHE, rPort);
        String projectName = SpUtils.getStr(Constants.Key.JU_PROJECT_NAME_SUFFIX, "");
        if (TextUtils.isEmpty(projectName))
            SpUtils.saveStr(Constants.Key.JU_PROJECT_NAME_SUFFIX, pName);

        String xmppIp = SpUtils.getStr(Constants.Key.JU_XMPP_IP_CACHE, "");
        if (TextUtils.isEmpty(xmppIp))
            SpUtils.saveStr(Constants.Key.JU_XMPP_IP_CACHE, cIp);
        String xmppPort = SpUtils.getStr(Constants.Key.JU_XMPP_PORT_CACHE, "");
        if (TextUtils.isEmpty(xmppPort))
            SpUtils.saveStr(Constants.Key.JU_XMPP_PORT_CACHE, xPort);

    }

    class QrCode{
        String wifiMac;
        String localMac;
        String deviceNo;
        String error;
        public QrCode(String wifiMac, String localMac,String deviceNo,String error) {
            this.wifiMac = wifiMac;
            this.localMac = localMac;
            this.deviceNo = deviceNo;
            this.error = error;
        }
    }


    private Runnable nextRunnable = () -> {
        UIUtils.dismissNetLoading();

        new Thread(() -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            FaceSDKActive.active(1, this, "1", new FaceSDKActive.ActiveCallback() {
                @Override
                public void getActiveCodeFailed(String message, String wifiMac, String localMac) {
                    String content = new Gson().toJson(new QrCode(wifiMac,localMac,HeartBeatClient.getDeviceNo(),message));
                    Bitmap qrImage = ZXingUtils.generateLogoQrCode(SplashActivity.this,content,400,400);
                    if(qrImage != null){
                        ivCode.setVisibility(View.VISIBLE);
                        ivCode.setImageBitmap(qrImage);
                    } else {
                        ivCode.setVisibility(View.GONE);
                    }

                    llCodeArea.setVisibility(View.VISIBLE);
                    tvError.setText("Error: " + message);
                    tvWifiMac.setText("wifi Mac: " + wifiMac);
                    tvLocalMac.setText("enterMac: " + localMac);
                    tvJump.setOnClickListener(v -> {
                        jump();
                        finish();
                    });
                }

                @Override
                public void getActiveCodeSuccess() {
                    jump();
                    finish();
                }
            });
        }).start();
    };

    private void jump() {
        APP.bindProtectService();

        switch (Constants.FLAVOR_TYPE) {
            case FlavorType.HT:
                ThermalConst.Default.DEFAULT_LOGO_ID = R.mipmap.logo_icon_horizontal;
                ThermalConst.Default.MAIN_LOGO_TEXT = "";
                break;
            case FlavorType.SK:
                ThermalConst.Default.DEFAULT_LOGO_ID = R.mipmap.icon_logo3;
                ThermalConst.Default.MAIN_LOGO_TEXT = "";
                break;
            case FlavorType.OSIMLE:
                ThermalConst.Default.DEFAULT_LOGO_ID = R.mipmap.osimle_logo;
                ThermalConst.Default.MAIN_LOGO_TEXT = "";
                break;
            case FlavorType.SOFT_WORK_Z:
                ThermalConst.Default.DEFAULT_LOGO_ID = R.mipmap.softworkz_logo;
                ThermalConst.Default.MAIN_LOGO_TEXT = "";
                break;
            case FlavorType.SCAN_TEMP:
                ThermalConst.Default.DEFAULT_LOGO_ID = R.mipmap.scan_temp;
                ThermalConst.Default.MAIN_LOGO_TEXT = "";
                break;
            case FlavorType.BIO:
                ThermalConst.Default.SHOW_MAIN_LOGO = false;
                ThermalConst.Default.MAIN_LOGO_TEXT = "";
                ThermalConst.Default.SHOW_MAIN_INFO = false;
                break;
            case FlavorType.PING_TECH:
                ThermalConst.Default.DEFAULT_LOGO_ID = R.mipmap.pingtech_logo;
                ThermalConst.Default.MAIN_LOGO_TEXT = "";
                ThermalConst.Default.TITLE_ENABLED = false;
            case FlavorType.PING_TECH_SHELTRON:
                ThermalConst.Default.DEFAULT_LOGO_ID = R.mipmap.sheltron_logo;
                ThermalConst.Default.MAIN_LOGO_TEXT = "";
                ThermalConst.Default.TITLE_ENABLED = false;
                break;
            case FlavorType.ITALY:
                ThermalConst.Default.DEFAULT_LOGO_ID = R.mipmap.yb_logo;
                Constants.DEFAULT_SCREE_BG = R.mipmap.it_screen_bg;
                ThermalConst.Default.MAIN_LOGO_TEXT = "";
                ThermalConst.Default.TITLE_ENABLED = false;
                ThermalConst.Default.SHOW_MAIN_LOGO = false;
                Constants.Default.POSTER_ENABLED = true;
                Constants.Default.PRIVACY_MODE = true;
                break;
            default:
                ThermalConst.Default.MAIN_LOGO_TEXT = "";
                break;
        }

        String broadTypeStr = CommonUtils.getBroadType2();
        Log.e(TAG, "jump: 板卡信息：" + broadTypeStr);
        switch (broadTypeStr) {
            case "SMT":
                Constants.Default.CAMERA_ANGLE = 270;
                Constants.Default.IS_H_MIRROR = false;
                ThermalConst.Default.TEMPER_MODULE = TemperModuleType.HM_16_4;
                break;
            case "LXR":
                Constants.Default.CAMERA_ANGLE = 0;//横屏
                Constants.Default.IS_H_MIRROR = true;
                ThermalConst.Default.TEMPER_MODULE = TemperModuleType.HM_32_32;
                break;
            case "HARRIS":
            default:
                Constants.Default.CAMERA_ANGLE = 90;
                Constants.Default.IS_H_MIRROR = false;
                ThermalConst.Default.TEMPER_MODULE = TemperModuleType.MLX_16_4;
                break;
        }

        switch (Constants.DEVICE_TYPE) {
            case Constants.DeviceType.TEMPERATURE_CHECK_IN_MASK:
//            case Constants.DeviceType.HT_TEMPERATURE_CHECK_IN_MASK:
                startActivity(new Intent(SplashActivity.this, ThermalImage2Activity.class));
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (ybPermission != null) {
            ybPermission.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    //上传错误日志
    private void uploadException(final Runnable runnable) {
        UIUtils.showNetLoading(this);
        new Thread(() -> {
            final List<Exception> exceptions = DaoManager.get().queryAll(Exception.class);
            if (exceptions == null || exceptions.size() <= 0) {
                Log.e(TAG, "run: 没有异常");
                if (runnable != null) {
                    runOnUiThread(runnable);
                }
                return;
            }
            Log.e(TAG, "run: 异常条数：" + exceptions.size());
            //地址
            String url = ResourceUpdate.DEVICE_EXCEPTION_UPLOAD;
            //版本号
            String versionName = "x.x.x";
            int versionCode = -1;
            try {
                PackageManager mPackageManager = getPackageManager();
                PackageInfo packageInfo = mPackageManager.getPackageInfo(getPackageName(), PackageManager.GET_ACTIVITIES);
                versionName = packageInfo.versionName;
                versionCode = packageInfo.versionCode;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            //设备号
            String str = SpUtils.getStr(SpUtils.DEVICE_NUMBER);
            if (TextUtils.isEmpty(str)) {
                str = "";
            }
            //转jsonString
            String crashArray = new Gson().toJson(exceptions);
            //参数
            Map<String, String> params = new HashMap<>();
            params.put("deviceId", HeartBeatClient.getDeviceNo() + "");
            params.put("deviceType", Constants.DEVICE_TYPE + "");
            params.put("versionName", versionName + "");
            params.put("versionCode", versionCode + "");
            params.put("cpuAbi", "");
            params.put("boardType", CommonUtils.saveBroadInfo() + "");
            params.put("deviceNumber", str + "");
            params.put("crasharray", crashArray + "");

            Log.e(TAG, "异常上传：" + url);
            Log.e(TAG, "参数：" + params.toString());

            OkHttpUtils.post()
                    .url(ResourceUpdate.DEVICE_EXCEPTION_UPLOAD)
                    .params(params)
                    .build()
                    .connTimeOut(5000)
                    .readTimeOut(5000)
                    .writeTimeOut(5000)
                    .execute(new StringCallback() {
                        @Override
                        public void onError(Call call, java.lang.Exception e, int id) {
                            Log.e(TAG, "onError: 上传失败：" + (e == null ? "NULL" : e.getMessage()));
                        }

                        @Override
                        public void onResponse(String response, int id) {
                            Log.e(TAG, "onResponse: 上传结果：" + response);
                            for (Exception exception : exceptions) {
                                DaoManager.get().delete(exception);
                            }
                        }

                        @Override
                        public void onAfter(int id) {
                            if (runnable != null) {
                                runnable.run();
                            }
                        }
                    });
        }).start();
    }

}
