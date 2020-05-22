package com.yunbiao.ybsmartcheckin_live_id.activity_temper_check_in;

import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.intelligence.hardware.temperature.TemperatureModule;
import com.intelligence.hardware.temperature.callback.HotImageK1604CallBack;
import com.intelligence.hardware.temperature.callback.HotImageK3232CallBack;
import com.intelligence.hardware.temperature.callback.InfraredTempCallBack;
import com.intelligence.hardware.temperature.callback.MLX90621YsTempCallBack;
import com.intelligence.hardware.temperature.callback.Smt3232TempCallBack;
import com.yunbiao.faceview.CompareResult;
import com.yunbiao.faceview.FacePreviewInfo;
import com.yunbiao.faceview.FaceView;
import com.yunbiao.ybsmartcheckin_live_id.R;
import com.yunbiao.ybsmartcheckin_live_id.activity.base.BaseGpioActivity;
import com.yunbiao.ybsmartcheckin_live_id.afinel.Constants;
import com.yunbiao.ybsmartcheckin_live_id.business.KDXFSpeechManager;
import com.yunbiao.ybsmartcheckin_live_id.business.SignManager;
import com.yunbiao.ybsmartcheckin_live_id.db2.Sign;
import com.yunbiao.ybsmartcheckin_live_id.utils.SpUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import io.reactivex.functions.Consumer;

public abstract class BaseThermal2Activity extends BaseGpioActivity implements FaceView.FaceCallback {
    private static final String TAG = "BaseThermalActivity";
    private long mSpeechDelay;//播报延迟
    protected int mCurrMode = -99;//当前模式
    private final float HIGHEST_TEMPER = 45.0f;//最高提示温度值
    private Float mTempMinThreshold;//温度播报阈值
    private Float mTempWarningThreshold;//温度报警阈值
    private float mTempCorrect;//温度补正
    private boolean mThermalImgMirror;//热成像镜像
    private boolean lowTempModel;//低温模式
    private boolean mFEnabled;//华氏度开关
    private boolean mPrivacyMode;//隐私模式
    private boolean mAutoTemper;//自动模式

    private Random random = new Random();
    private TypedArray noFaceArray;
    private TypedArray hasFaceArray;

    private Bitmap mLastHotImage;
    private List<Float> mCacheTemperList = Collections.synchronizedList(new ArrayList<Float>());//最终温度结果集
    private float mCacheBeforeTemper = 0.0f;//缓存之前的温度
    private long mCacheTime = 0;//缓存时间
    private float mCacheDiffValue = 2.0f;//缓存温差值
    private boolean mHasFace = false;//是否有人脸
    private boolean isFaceToFar = true;//人脸距离
    private boolean isFaceInsideRange = false;//人脸是否在范围内（仅红外）
    private View mDistanceView;//范围限制View
    private boolean isResultShown = false;//测温结果是否正在显示
    private Rect mAreaRect = new Rect();//范围限制View的Rect
    private boolean isActivityPaused = false;//当前Activity是否已pause
    private boolean isMLXRunning = false;//MLX芯片是否正在运行
    private int distanceTipNumber = 0;//距离提示的次数
    private List<Float> autoCheckList = new ArrayList<>();//自动模式缓存集合

    private ThermalViewInterface viewInterface;
    private SpeechBean speechBean = new SpeechBean();
    private boolean isMaskDetectEnabled;

    @Override
    protected void initData() {
        super.initData();
        noFaceArray = getResources().obtainTypedArray(R.array.noFaceArray);
        hasFaceArray = getResources().obtainTypedArray(R.array.hasFaceArray);
        viewInterface = setViewInterface();
        if (viewInterface == null) {
            viewInterface = new ThermalViewInterAbsImp();
        }
    }

    protected abstract ThermalViewInterface setViewInterface();

    @Override
    protected void onResume() {
        super.onResume();
        isActivityPaused = false;
        //口罩
        isMaskDetectEnabled = SpUtils.getBoolean(ThermalConst.Key.MASK_DETECT_ENABLED, ThermalConst.Default.MASK_DETECT_ENABLED);
        setMaskDetectEnable(isMaskDetectEnabled);

        //隐私模式
        mPrivacyMode = SpUtils.getBoolean(Constants.Key.PRIVACY_MODE, Constants.Default.PRIVACY_MODE);
        //播报延时
        mSpeechDelay = SpUtils.getLong(ThermalConst.Key.SPEECH_DELAY, ThermalConst.Default.SPEECH_DELAY);
        //测温最小阈值
        mTempMinThreshold = SpUtils.getFloat(ThermalConst.Key.TEMP_MIN_THRESHOLD, ThermalConst.Default.TEMP_MIN_THRESHOLD);
        //测温报警阈值
        mTempWarningThreshold = SpUtils.getFloat(ThermalConst.Key.TEMP_WARNING_THRESHOLD, ThermalConst.Default.TEMP_WARNING_THRESHOLD);
        //热成像图像镜像
        mThermalImgMirror = SpUtils.getBoolean(ThermalConst.Key.THERMAL_IMAGE_MIRROR, ThermalConst.Default.THERMAL_IMAGE_MIRROR);
        //低温模式
        lowTempModel = SpUtils.getBoolean(ThermalConst.Key.LOW_TEMP_MODE, ThermalConst.Default.LOW_TEMP);
        //温度补偿
        mTempCorrect = SpUtils.getFloat(ThermalConst.Key.THERMAL_CORRECT, ThermalConst.Default.THERMAL_CORRECT);
        TemperatureModule.getIns().setmCorrectionValue(mTempCorrect);
        //华氏度
        mFEnabled = SpUtils.getBoolean(ThermalConst.Key.THERMAL_F_ENABLED, ThermalConst.Default.THERMAL_F_ENABLED);
        //自动模式
        mAutoTemper = SpUtils.getBoolean(ThermalConst.Key.AUTO_TEMPER, ThermalConst.Default.AUTO_TEMPER);
        //初始化播报
        speechBean.initContent();
        KDXFSpeechManager.instance().setSpeed(speechBean.getSpeechSpeed());
        //模式切换
        mCurrMode = SpUtils.getIntOrDef(ThermalConst.Key.MODE, ThermalConst.Default.MODE);//当前模式
        viewInterface.onModeChanged(mCurrMode);

        String portPath = mCurrentOrientation == Configuration.ORIENTATION_PORTRAIT ? "/dev/ttyS1" : "/dev/ttyS4";
        switch (mCurrMode) {
            //NO_Temper
            case ThermalConst.ONLY_FACE:
                isMLXRunning = false;
                TemperatureModule.getIns().initSerialPort(this, portPath, 115200);
                break;
            //红外
            case ThermalConst.ONLY_INFRARED:
            case ThermalConst.FACE_INFRARED:
                isMLXRunning = false;
                TemperatureModule.getIns().initSerialPort(this, portPath, 9600);
                TemperatureModule.getIns().setInfraredTempCallBack(infraredTempCallBack);
                break;
            //32*32
            case ThermalConst.ONLY_THERMAL_HM_32_32:
            case ThermalConst.FACE_THERMAL_HM_32_32:
                isMLXRunning = false;
                TemperatureModule.getIns().initSerialPort(this, portPath, 115200);
                updateUIHandler.postDelayed(() -> TemperatureModule.getIns().startHotImageK3232(mThermalImgMirror, lowTempModel, imageK3232CallBack), 2000);
                break;
            //HM_16*4
            case ThermalConst.ONLY_THERMAL_HM_16_4:
            case ThermalConst.FACE_THERMAL_HM_16_4:
                isMLXRunning = false;
                portPath = mCurrentOrientation == Configuration.ORIENTATION_PORTRAIT ? "/dev/ttyS3" : "/dev/ttyS4";
                TemperatureModule.getIns().initSerialPort(this, portPath, 19200);
                updateUIHandler.postDelayed(() -> TemperatureModule.getIns().startHotImageK1604(mThermalImgMirror, lowTempModel, hotImageK1604CallBack), 2000);
                break;
            //MLX_16*4
            case ThermalConst.ONLY_THERMAL_MLX_16_4:
            case ThermalConst.FACE_THERMAL_MLX_16_4:
                if (!isMLXRunning) {
                    updateUIHandler.postDelayed(() -> {
                        TemperatureModule.getIns().startMLX90621YsI2C(lowTempModel, 16 * 30, 4 * 40, mlx90621YsTempCallBack);
                        isMLXRunning = true;
                    }, 1 * 1000);
                } else {
                    //如果自动模式开启，则默认全关
                    if (mAutoTemper) {
                        TemperatureModule.getIns().setHotImageColdMode(false);
                        TemperatureModule.getIns().setHotImageHotMode(false, 45.0f);
                    } else {
                        //如果自动模式关闭，则关闭高温模式并且以设置为准调整低温模式
                        TemperatureModule.getIns().setHotImageColdMode(lowTempModel);
                        TemperatureModule.getIns().setHotImageHotMode(false, 45.0f);
                    }
                }
                break;
            //SMT
            case ThermalConst.ONLY_THERMAL_SMT:
            case ThermalConst.FACE_THERMAL_SMT:
                isMLXRunning = false;
                Log.e(TAG, "onResume: 开启测温模块");
                TemperatureModule.getIns().initSerialPort(this, "/dev/ttyS3", 115200);
                updateUIHandler.postDelayed(() -> TemperatureModule.getIns().startSmt3232Temp(lowTempModel, smt3232TempCallBack), 2000);
                break;

        }
    }

    protected abstract void setMaskDetectEnable(boolean enable);

    @Override
    protected void onPause() {
        super.onPause();
        isActivityPaused = true;
    }

    //==测温相关============================================================================================================
    //==测温相关============================================================================================================
    //==测温相关============================================================================================================
    //==测温相关============================================================================================================
    //==测温相关============================================================================================================
    //==测温相关============================================================================================================
    //==测温相关============================================================================================================
    //==测温相关============================================================================================================
    //==测温相关============================================================================================================
    //==测温相关============================================================================================================
    private Bitmap selectInfaredImage(boolean hasFace) {
        int i = random.nextInt(20);
        int id;
        if (hasFace) {
            id = hasFaceArray.getResourceId(i, R.mipmap.h_1);
        } else {
            id = noFaceArray.getResourceId(i, R.mipmap.n_1);
        }
        return BitmapFactory.decodeStream(getResources().openRawResource(id));
    }

    private InfraredTempCallBack infraredTempCallBack = new InfraredTempCallBack() {
        @Override
        public void newestInfraredTemp(float measureF, float afterF, float ambientF) {
            handleTemperature(null, measureF, afterF);
        }
    };
    private HotImageK3232CallBack imageK3232CallBack = new HotImageK3232CallBack() {
        @Override
        public void newestHotImageData(final Bitmap imageBmp, final float sensorT, final float maxT, final float minT, final float bodyMaxT, final boolean isBody, final int bodyPercentage) {
            handleTemperature(imageBmp, sensorT, maxT);
        }

        @Override
        public void dataRecoveryFailed() {
            d("获取数据失败");
        }
    };
    private HotImageK1604CallBack hotImageK1604CallBack = new HotImageK1604CallBack() {
        @Override
        public void newestHotImageData(final Bitmap imageBmp, final float originalMaxT, final float maxT, final float minT) {
            handleTemperature(imageBmp, originalMaxT, maxT);
        }

        @Override
        public void dataRecoveryFailed() {
            d("获取数据失败");
        }
    };

    private MLX90621YsTempCallBack mlx90621YsTempCallBack = new MLX90621YsTempCallBack() {
        @Override
        public void newestHotImageData(Bitmap bitmap, final float originalMaxT, final float maxT, final float minT) {
            handleTemperature(bitmap, originalMaxT, maxT);
        }

        @Override
        public void dataRecoveryFailed() {
            d("获取数据失败");
        }
    };

    private int nobodyTemperTag = 0;
    private int nobodyFaceTag = 0;

    private void startAutoCheck(float originT) {
        Log.e(TAG, "handleTemperature: 原始温度：" + originT);
        if (autoCheckList.size() < 10) {
            autoCheckList.add(originT);
        } else {
            int highTotal = 0;
            int lowTotal = 0;
            for (Float aFloat : autoCheckList) {
                if (aFloat <= 16f) {
                    lowTotal++;
                } else if (aFloat >= 29f) {
                    highTotal++;
                }
            }
            Log.e(TAG, "handleTemperature: 低温数量：" + lowTotal + " ----- " + autoCheckList.size());
            Log.e(TAG, "handleTemperature: 高温数量：" + highTotal + " ----- " + autoCheckList.size());
            int total = (autoCheckList.size() / 2) + 1;
            if (lowTotal >= total) {
                currTemperMode = 0;
                Log.e(TAG, "handleTemperature: 开启低温模式");
                TemperatureModule.getIns().setHotImageColdMode(true);
                TemperatureModule.getIns().setHotImageHotMode(false, 45f);
            } else if (highTotal >= total) {
                currTemperMode = 2;
                Log.e(TAG, "handleTemperature: 开启高温模式");
                TemperatureModule.getIns().setHotImageColdMode(false);
                TemperatureModule.getIns().setHotImageHotMode(true, 45f);
            } else {
                currTemperMode = 1;
                Log.e(TAG, "handleTemperature: 常温模式");
                TemperatureModule.getIns().setHotImageColdMode(false);
                TemperatureModule.getIns().setHotImageHotMode(false, 45f);
            }
            autoCheckList.clear();
        }
    }

    private List<Float> mNoBodyTemperList = new ArrayList<>();
    private int currTemperMode = -1;
    private int maskTag = -1;
    private int maskTipNumber = -1;
    //温度处理的主要逻辑
    private void handleTemperature(Bitmap imageBmp, float originT, float afterT) {
        if (isActivityPaused) {
            return;
        }
        if (mCurrMode == ThermalConst.ONLY_FACE) {
            return;
        }

        if (imageBmp == null) {
            imageBmp = selectInfaredImage(mHasFace);
        }

        mLastHotImage = imageBmp;
        sendUpdateHotInfoMessage(imageBmp, afterT);

        if (!mHasFace) {
            if (distanceTipNumber != 0) {
                distanceTipNumber = 0;
            }
            if(maskTag != -1){
                maskTag = -1;
            }
            if (maskTipNumber != 0) {
                maskTipNumber = 0;
            }
            if (mCacheBeforeTemper == 0.0f || originT < mCacheBeforeTemper) {
                mCacheBeforeTemper = originT;
            }
            mCacheTime = 0;
            if (mCacheTemperList.size() > 0) {
                mCacheTemperList.clear();
            }
            //自动调整模式
            if (mAutoTemper && !lowTempModel) {
                startAutoCheck(originT);
            }
            sendClearAllUIMessage();
            return;
        }

        if (autoCheckList.size() > 0) {
            autoCheckList.clear();
        }

        if (isFaceToFar && !isResultShown) {
            if (mCacheTime != 0) mCacheTime = 0;
            if (mCacheTemperList.size() > 0) mCacheTemperList.clear();
            sendTipsMessage(speechBean.getDistanceContent());
            if (speechBean.isDistanceEnabled() && distanceTipNumber < 5)
                KDXFSpeechManager.instance().playNormalAdd(speechBean.getDistanceContent(), () -> distanceTipNumber++);
            return;
        }

        if (mCurrMode == ThermalConst.ONLY_INFRARED || mCurrMode == ThermalConst.FACE_INFRARED) {
            if (speechBean.isFrameEnabled() && !isFaceInsideRange && !isResultShown) {
                mCacheTime = 0;
                if (mCacheTemperList.size() > 0) {
                    mCacheTemperList.clear();
                }
                sendTipsMessage(speechBean.getFrameContent());
                return;
            }
        }
        //全部通过后清除提示框
        sendResetTipsMessage(0);

        //判断口罩标签
        if (isMaskDetectEnabled && !isResultShown) {
            if(maskTag == -1){
                return;
            } else if (maskTag == 0 || maskTag == 1) {
                sendMaskTipMessage(speechBean.getMaskTip());
                if (speechBean.isMaskTipEnabled() && maskTipNumber < 5) {
                    KDXFSpeechManager.instance().playNormalAdd(speechBean.getMaskTip(), () -> maskTipNumber++);
                }
            } else {
                //通过后清除口罩提示框
                sendClearMaskTipMessage(0);
            }
        }

        if (originT - mCacheBeforeTemper < mCacheDiffValue) {
            return;
        }

        if (afterT < mTempMinThreshold) {
            mCacheTime = 0;
            if (mCacheTemperList.size() > 0) {
                mCacheTemperList.clear();
            }
            return;
        }

        final int temperListSize = mCurrMode == ThermalConst.ONLY_INFRARED || mCurrMode == ThermalConst.FACE_INFRARED || mCurrMode == ThermalConst.FACE_THERMAL_HM_16_4 || mCurrMode == ThermalConst.FACE_THERMAL_HM_16_4 ? 2 : 4;
        if (mCurrMode == ThermalConst.ONLY_THERMAL_HM_32_32 || mCurrMode == ThermalConst.ONLY_INFRARED || mCurrMode == ThermalConst.ONLY_THERMAL_HM_16_4 || mCurrMode == ThermalConst.ONLY_THERMAL_MLX_16_4) {
            long currentTimeMillis = System.currentTimeMillis();
            if (mCacheTime != 0 && currentTimeMillis - mCacheTime < mSpeechDelay) {
                return;
            }
            mCacheTemperList.add(afterT);
            if (mCacheTemperList.size() < temperListSize) {
                return;
            }
            mCacheTime = currentTimeMillis;

            Collections.sort(mCacheTemperList);
            Float max = mCacheTemperList.get(mCacheTemperList.size() / 2 + 1);
            mCacheTemperList.clear();

            float resultTemper = max;
            if (resultTemper < mTempWarningThreshold) {
                if (mCurrMode != ThermalConst.ONLY_THERMAL_HM_16_4
                        && mCurrMode != ThermalConst.FACE_THERMAL_HM_16_4
                        && mCurrMode != ThermalConst.ONLY_THERMAL_MLX_16_4
                        && mCurrMode != ThermalConst.FACE_THERMAL_MLX_16_4) {
                    resultTemper += mTempCorrect;
                }

                if (mCurrMode != ThermalConst.ONLY_INFRARED && mCurrMode != ThermalConst.FACE_INFRARED && mCacheBeforeTemper != 0.0f) {
                    float currDiffValue = originT - mCacheBeforeTemper - 3.0f;
                    mCacheDiffValue = mCacheDiffValue == 2.0f
                            //判断当前差值是否大于2.0f，如果是则存值
                            ? Math.max(currDiffValue, mCacheDiffValue)
                            //判断当前差值是否大于2并且小于缓存差值，如果是则存值
                            : (currDiffValue > 2.0f && currDiffValue < mCacheDiffValue ? currDiffValue : mCacheDiffValue);
                }
            }
            resultTemper = formatF(resultTemper);
            sendResultMessage(resultTemper, "");//发送结果
            if (resultTemper >= mTempMinThreshold && resultTemper < mTempWarningThreshold) {
                openDoor();
            }

            if (resultTemper < HIGHEST_TEMPER) {
                Sign temperatureSign = SignManager.instance().getTemperatureSign(resultTemper);
                SignManager.instance().uploadTemperatureSign(viewInterface.getFacePicture(), mLastHotImage.copy(mLastHotImage.getConfig(), false), temperatureSign, mPrivacyMode, sign -> {
                    Log.e(TAG, "accept: 保存完成");
                    sendUpdateSignMessage(sign);//发送列表更新事件
                });
            }
        } else if (mCurrMode == ThermalConst.FACE_THERMAL_HM_32_32 || mCurrMode == ThermalConst.FACE_INFRARED || mCurrMode == ThermalConst.FACE_THERMAL_HM_16_4 || mCurrMode == ThermalConst.FACE_THERMAL_MLX_16_4) {
            mCacheTemperList.add(afterT);
        }
    }

    private Smt3232TempCallBack smt3232TempCallBack = new Smt3232TempCallBack() {
        @Override
        public void newestSmt3232Temp(float measureF, float afterF) {
            if (isActivityPaused) {
                return;
            }
            if (mCurrMode == ThermalConst.ONLY_FACE) {
                return;
            }

            if (!mHasFace) {
                if (distanceTipNumber != 0) {
                    distanceTipNumber = 0;
                }
                if(maskTag != -1){
                    maskTag = -1;
                }
                if (maskTipNumber != 0) {
                    maskTipNumber = 0;
                }
                mCacheTime = 0;
                if (mCacheTemperList.size() > 0) {
                    mCacheTemperList.clear();
                }
                sendClearAllUIMessage();
                return;
            }

            if (isFaceToFar && !isResultShown) {
                if (mCacheTime != 0) mCacheTime = 0;
                if (mCacheTemperList.size() > 0) mCacheTemperList.clear();
                sendTipsMessage(speechBean.getDistanceContent());
                if (speechBean.isDistanceEnabled() && distanceTipNumber < 5)
                    KDXFSpeechManager.instance().playNormalAdd(speechBean.getDistanceContent(), () -> distanceTipNumber++);
                return;
            }

            //判断口罩标签
            if (isMaskDetectEnabled) {
                if(maskTag == -1){
                    return;
                } else if (maskTag == 0 || maskTag == 1) {
                    sendMaskTipMessage(speechBean.getMaskTip());
                    if (speechBean.isMaskTipEnabled() && maskTipNumber < 5) {
                        KDXFSpeechManager.instance().playNormalAdd(speechBean.getMaskTip(), () -> maskTipNumber++);
                    }
                    return;
                } else {
                    //通过后清除口罩提示框
                    sendClearMaskTipMessage(0);
                }
            }

            //如果处理后的数值小于播报阈值，清除最终值
            if (afterF < mTempMinThreshold) {
                if (mCacheTime != 0) mCacheTime = 0;
                if (mCacheTemperList.size() > 0) mCacheTemperList.clear();
                return;
            }

            if (mCurrMode == ThermalConst.ONLY_THERMAL_SMT) {
                if (mCacheTime == 0 || System.currentTimeMillis() - mCacheTime > mSpeechDelay) {
                    mCacheTemperList.add(afterF);
                    if (mCacheTemperList.size() < 3) {
                        return;
                    }
                    mCacheTime = System.currentTimeMillis();

                    Float max = Collections.max(mCacheTemperList);
                    mCacheTemperList.clear();

                    float resultTemper = formatF(max);
                    sendResultMessage(resultTemper, "");//发送结果
                    if (resultTemper >= mTempMinThreshold && resultTemper < mTempWarningThreshold) {
                        openDoor();
                    }

                    if (resultTemper < HIGHEST_TEMPER) {
                        Sign temperatureSign = SignManager.instance().getTemperatureSign(resultTemper);
                        SignManager.instance().uploadTemperatureSign(viewInterface.getFacePicture(), null, temperatureSign, false, new Consumer<Sign>() {
                            @Override
                            public void accept(Sign sign) throws Exception {
                                sendUpdateSignMessage(sign);//发送列表更新事件
                            }
                        });
                    }
                }
            } else {
                mCacheTemperList.add(afterF);
            }
        }
    };

    private float getResultTemperForFandT() {
        if (mCacheTemperList.size() <= 0) {
            return 0.0f;
        }
        Collections.sort(mCacheTemperList);
        Float max = mCacheTemperList.get(mCacheTemperList.size() / 2 + 1);
        mCacheTemperList.clear();
        return max;
    }

    //===人脸相关=================================================================================
    //===人脸相关=================================================================================
    //===人脸相关=================================================================================
    //===人脸相关=================================================================================
    //===人脸相关=================================================================================
    //===人脸相关=================================================================================
    //===人脸相关=================================================================================
    //===人脸相关=================================================================================
    //===人脸相关=================================================================================
    //===人脸相关=================================================================================
    @Override
    public void onReady() {
        viewInterface.onFaceViewReady();
        mDistanceView = viewInterface.getDistanceView();
    }

    @Override
    public void onFaceDetection(Boolean hasFace, List<FacePreviewInfo> facePreviewInfoList) {

    }

    @Override
    public boolean onFaceDetection(boolean hasFace, FacePreviewInfo facePreviewInfo) {
        if (isActivityPaused) {
            return false;
        }
        mHasFace = hasFace;
        viewInterface.hasFace(hasFace);
        if (!hasFace) {
            if (mCurrMode == ThermalConst.ONLY_FACE) {
                maskTipNumber = 0;
                sendClearMaskTipMessage(0);
            }
            return false;
        }

        //如果是人脸模式
        if (mCurrMode == ThermalConst.ONLY_FACE) {
            //更新口罩标签
            if (isMaskDetectEnabled) {
                maskTag = facePreviewInfo.getMask() == 0 || facePreviewInfo.getMask() == -1
                        ? 0
                        : facePreviewInfo.getFaceShelter() == 0 || facePreviewInfo.getFaceShelter() == -1
                        ? 1
                        : 2;
                if (maskTag != 2) {
                    String resString = getResString(R.string.no_mask_tip);
                    sendMaskTipMessage(resString);
                    if (maskTipNumber < 5) {
                        KDXFSpeechManager.instance().playNormalAdd(resString, () -> maskTipNumber++);
                    }
                    return false;
                }
            }
            //通过后清除口罩提示框
            sendClearMaskTipMessage(0);
            return true;
        }

        //如果限制View为null
        if (mDistanceView == null) {
            return false;
        }

        //距离
        Rect rect = facePreviewInfo.getFaceInfo().getRect();
        int distance = mDistanceView.getMeasuredWidth();
        //如果人脸太远
        isFaceToFar = checkFaceToFar(rect, distance);
        if (isFaceToFar) {
            return false;
        }
        //如果是红外模式则判断人脸区域
        if (mCurrMode == ThermalConst.ONLY_INFRARED || mCurrMode == ThermalConst.FACE_INFRARED) {
            Rect realRect = viewInterface.getRealRect(rect);
            isFaceInsideRange = checkFaceInFrame(realRect, mDistanceView);
            if (!isFaceInsideRange) {
                return false;
            }
        }
        //更新口罩标签
        if (isMaskDetectEnabled) {
            maskTag = facePreviewInfo.getMask() == 0 || facePreviewInfo.getMask() == -1
                    ? 0
                    : facePreviewInfo.getFaceShelter() == 0 || facePreviewInfo.getFaceShelter() == -1
                    ? 1
                    : 2;
            if (maskTag != 2) {
                return false;
            }
        }
        //判断是否仅测温模式
        if (mCurrMode == ThermalConst.ONLY_INFRARED
                || mCurrMode == ThermalConst.ONLY_THERMAL_HM_32_32
                || mCurrMode == ThermalConst.ONLY_THERMAL_HM_16_4
                || mCurrMode == ThermalConst.ONLY_THERMAL_MLX_16_4) {
            return false;
        }
        //判断集合中的温度数据
        if (mCacheTemperList.size() < (mCurrMode == ThermalConst.FACE_INFRARED || mCurrMode == ThermalConst.FACE_THERMAL_HM_16_4 ? 2 : 3)) {
            return false;
        }
        return true;
    }

    @Override
    public void onFaceVerify(CompareResult faceAuth) {
        if (mCurrMode == ThermalConst.ONLY_FACE) {
            if (faceAuth == null || faceAuth.getSimilar() == -1) {
                return;
            }
            Sign sign = SignManager.instance().checkSignData(faceAuth, 0.0f);
            if (sign == null) {
                return;
            }
            KDXFSpeechManager.instance().playNormal(sign.getName());
            viewInterface.updateSignList(sign);

            if (sign.getType() == -2) {
                return;
            }
            openDoor();
        } else {
            float resultTemper = getResultTemperForFandT();
            if (resultTemper <= 0.0f) {
                return;
            }

            if (resultTemper >= HIGHEST_TEMPER) {
                sendResultMessage(resultTemper, "");
                return;
            }

            Sign sign = null;
            if (!TextUtils.equals("-1", faceAuth.getUserName())) {
                sign = SignManager.instance().checkSignData(faceAuth, resultTemper);
            }
            if (sign == null) {
                sign = SignManager.instance().getTemperatureSign(resultTemper);
            }
            Log.e(TAG, "onFaceVerify: 识别的用户名：" + sign.getName());
            sendResultMessage(resultTemper, sign.getName());

            SignManager.instance().uploadTemperatureSign(viewInterface.getFacePicture(), mLastHotImage.copy(mLastHotImage.getConfig(), false), sign, false, new Consumer<Sign>() {
                @Override
                public void accept(Sign sign) throws Exception {
                    viewInterface.updateSignList(sign);
                }
            });

            if (sign.getType() == -2 || sign.getType() == -9) {
                return;
            }
            openDoor();
        }
    }

    //==UI相关============================================================================================
    //==UI相关============================================================================================
    //==UI相关============================================================================================
    //==UI相关============================================================================================
    //==UI相关============================================================================================
    //==UI相关============================================================================================
    //==UI相关============================================================================================
    //==UI相关============================================================================================
    //==UI相关============================================================================================
    //==UI相关============================================================================================
    //更新热成像图
    private void sendUpdateHotInfoMessage(Bitmap imageBmp, float maxT) {
        Message message = Message.obtain();
        message.what = 0;
        message.obj = imageBmp;
        Bundle bundle = new Bundle();
        bundle.putFloat("temper", maxT);
        message.setData(bundle);
        updateUIHandler.sendMessage(message);
    }

    //测温结果
    private void sendResultMessage(float temperature, String name) {
        Message message = Message.obtain();
        message.what = 1;
        message.obj = temperature;
        Bundle bundle = new Bundle();
        bundle.putString("name", name);
        message.setData(bundle);
        updateUIHandler.sendMessage(message);
    }

    //重置UI
    private void sendResetResultMessage() {
        updateUIHandler.removeMessages(3);
        updateUIHandler.sendEmptyMessageDelayed(3, 3000);
    }

    //提示相关
    private void sendTipsMessage(String string) {
        if (viewInterface.isTipsShown()) {
            return;
        }
        updateUIHandler.removeMessages(2);
        Message message = Message.obtain();
        message.what = 2;
        message.obj = string;
        updateUIHandler.sendMessage(message);
    }

    //重置UI
    private void sendResetTipsMessage(int delay) {
        if (viewInterface.isTipsShown()) {
            updateUIHandler.removeMessages(4);
            updateUIHandler.sendEmptyMessageDelayed(4, delay);
        }
    }

    //发送口罩提示
    private void sendMaskTipMessage(String tip) {
        if (viewInterface.isMaskTipsShown()) {
            return;
        }
        Message message = Message.obtain();
        message.what = 7;
        message.obj = tip;
        updateUIHandler.sendMessageDelayed(message, 100);
    }

    //发送清除口罩提示的消息
    private void sendClearMaskTipMessage(long delay) {
        updateUIHandler.removeMessages(7);
        if (viewInterface.isMaskTipsShown()) {
            updateUIHandler.removeMessages(8);
            updateUIHandler.sendEmptyMessageDelayed(8, delay);
        }
    }

    //清除所有UI提示
    private void sendClearAllUIMessage() {
        if (viewInterface.isTipsShown() || viewInterface.isResultShown() || viewInterface.isMaskTipsShown()) {
            updateUIHandler.removeMessages(5);
            updateUIHandler.sendEmptyMessage(5);
        }
    }

    private void sendUpdateSignMessage(Sign sign) {
        Message message = Message.obtain();
        message.what = 6;
        message.obj = sign;
        updateUIHandler.sendMessage(message);
    }

    private Handler updateUIHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case 0://更新热成像图
                    Bitmap bitmap = (Bitmap) msg.obj;
                    float temper = msg.getData().getFloat("temper", 0.0F);
                    viewInterface.updateHotImage(bitmap, temper, mHasFace);
                    break;
                case 1://测温结果
                    float temperature = (float) msg.obj;
                    String name = msg.getData().getString("name");
                    if (temperature <= 0.0f) {
                        break;
                    }
                    //关闭正在进行的事件
                    updateUIHandler.removeMessages(3);//清除隐藏事件
                    updateUIHandler.sendEmptyMessage(4);
                    KDXFSpeechManager.instance().stopNormal();
                    KDXFSpeechManager.instance().stopWarningRing();
                    isResultShown = true;
                    //显示UI
                    int bgId = getBgId(temperature);
                    String resultText = getResultText(mFEnabled, temperature);
                    viewInterface.showResult(resultText, bgId);


                    Runnable resultRunnable = speechCallback(temperature);
                    String speechText = getSpeechText(mFEnabled, temperature);

                    if(temperature >= HIGHEST_TEMPER){
                        if(speechBean.isWarningEnabled()){
                            KDXFSpeechManager.instance().playNormal(speechText, resultRunnable);
                        } else {
                            ledRed();
                            KDXFSpeechManager.instance().playWaningRing();
                            sendResetResultMessage();
                        }
                    } else if (temperature >= mTempWarningThreshold) {
                        if(speechBean.isWarningEnabled()){
                            if (!TextUtils.isEmpty(name)) {
                                speechText += "，" + name;
                            }
                            KDXFSpeechManager.instance().playNormal(speechText, resultRunnable);
                        } else {
                            ledRed();
                            KDXFSpeechManager.instance().playWaningRing();
                            sendResetResultMessage();
                        }
                    } else {
                        if(speechBean.isNormalEnabled()){
                            if (!TextUtils.isEmpty(name)) {
                                speechText += "，" + name;
                            }
                            KDXFSpeechManager.instance().playNormal(speechText, resultRunnable);
                        } else {
                            ledGreen();
                            KDXFSpeechManager.instance().playPassRing();
                            if (mCurrMode == ThermalConst.ONLY_INFRARED || mCurrMode == ThermalConst.ONLY_THERMAL_HM_16_4 || mCurrMode == ThermalConst.ONLY_THERMAL_HM_32_32) {
                                openDoor();
                            }
                            sendResetResultMessage();
                        }
                    }
                    break;
                case 2://提示相关
                    int id;
                    if (mCurrentOrientation == Configuration.ORIENTATION_PORTRAIT) {
                        id = R.mipmap.bg_verify_nopass;
                    } else {
                        id = R.drawable.shape_main_frame_temperature_warning;
                    }
                    String tips = (String) msg.obj;
                    viewInterface.showTips(tips, id);
                    break;
                case 3://重置UI
                    isResultShown = false;
                    viewInterface.dismissResult();
                    KDXFSpeechManager.instance().stopWarningRing();
                    resetLedDelay(0);//5秒后重置灯光为蓝色
                    break;
                case 4://重置提示
                    viewInterface.dismissTips();
                    break;
                case 5://清除所有UI（除灯光和语音播报）
                    isResultShown = false;
                    viewInterface.clearAllUI();
                    break;
                case 6://仅添加
                    Sign sign = (Sign) msg.obj;
                    viewInterface.updateSignList(sign);
                    break;
                case 7:
                    String tip = (String) msg.obj;
                    viewInterface.showMaskTip(tip);
                    break;
                case 8:
                    viewInterface.clearMaskTip();
                    break;
            }
            return true;
        }
    });

    private Runnable speechCallback(float temperature) {
        if (temperature >= mTempWarningThreshold) {
            ledRed();
            return () -> {
                KDXFSpeechManager.instance().playWaningRing();
                sendResetResultMessage();
            };
        } else {
            ledGreen();
            if (mCurrMode == ThermalConst.ONLY_INFRARED || mCurrMode == ThermalConst.ONLY_THERMAL_HM_16_4 || mCurrMode == ThermalConst.ONLY_THERMAL_HM_32_32) {
                openDoor();
            }
            return () -> sendResetResultMessage();
        }
    }

    private int getBgId(float temperature) {
        if (temperature >= mTempWarningThreshold) {
            if (mCurrentOrientation == Configuration.ORIENTATION_PORTRAIT) {
                return R.mipmap.bg_verify_nopass;
            } else {
                return R.drawable.shape_main_frame_temperature_warning;
            }
        } else {
            if (mCurrentOrientation == Configuration.ORIENTATION_PORTRAIT) {
                return R.mipmap.bg_verify_pass;
            } else {
                return R.drawable.shape_main_frame_temperature_normal;
            }
        }
    }

    private String getSpeechText(boolean fEnabled, float temperature) {
        if (temperature > 45.0f) {
            return getResources().getString(R.string.main_temp_error_tips);
        }

        String temper;
        String unit;
        if (fEnabled) {
            temper = formatF((float) (temperature * 1.8 + 32)) + "℉";
            unit = speechBean.getFahrenheit();
        } else {
            temper = temperature + "℃";
            unit = speechBean.getCentigrade();
        }

        if (temperature >= mTempWarningThreshold) {
            return speechBean.getWarningContentForSpeech(temper, unit);
        } else {
            return speechBean.getNormalContentForSpeech(temper, unit);
        }
    }

    private String getResultText(boolean fEnabled, float temperature) {
        if (temperature > 45.0f) {
            return getResources().getString(R.string.main_temp_error_tips);
        }

        String temper;
        if (fEnabled) {
            temper = formatF((float) (temperature * 1.8 + 32)) + "℉";
        } else {
            temper = temperature + "℃";
        }

        if (temperature >= mTempWarningThreshold) {
            return speechBean.getWarningContentForUI(temper);
        } else {
            return speechBean.getNormalContentForUI(temper);
        }
    }

    public boolean checkFaceToFar(Rect faceRect, int distance) {
        int faceWidth = faceRect.right - faceRect.left;
        return faceWidth < distance;
    }

    public boolean checkFaceInFrame(Rect realRect, View areaView) {
        areaView.getGlobalVisibleRect(mAreaRect);
        //人脸范围补正数值
        int faceRangeCorrectValue = 50;
        mAreaRect.left += faceRangeCorrectValue;
        mAreaRect.right -= faceRangeCorrectValue;
        mAreaRect.top += faceRangeCorrectValue;
        mAreaRect.bottom -= faceRangeCorrectValue;
        return realRect.contains(mAreaRect);
    }

    @Override
    protected void onStop() {
        super.onStop();
        TemperatureModule.getIns().closeHotImageK3232();
        TemperatureModule.getIns().setInfraredTempCallBack(null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        TemperatureModule.getIns().closeSerialPort();
        TemperatureModule.getIns().closeHotImageK3232();
        TemperatureModule.getIns().closeMLX90621YsI2C();
    }

    class SpeechBean {
        private float speechSpeed;

        private String maskTip;
        private boolean maskTipEnabled;

        private String welcomeContent;
        private boolean welcomeEnabled;

        private String distanceContent;
        private boolean distanceEnabled;

        private String frameContent;
        private boolean frameEnabled;

        private String normalContent;
        private boolean normalTemperEnabled;
        private boolean normalEnabled;

        private String warningContent;
        private boolean warningTemperEnabled;
        private boolean warningEnabled;

        private int normalTemperLocation;
        private int warningTemperLocation;

        private String centigrade;
        private String fahrenheit;

        public void initContent() {
            maskTip = SpUtils.getStr(ThermalConst.Key.MASK_TIP,getResString(R.string.no_mask_tip));
            maskTipEnabled = SpUtils.getBoolean(ThermalConst.Key.MASK_TIP_ENABLED,ThermalConst.Default.MASK_TIP_ENABLED);
            speechSpeed = SpUtils.getFloat(ThermalConst.Key.VOICE_SPEED, ThermalConst.Default.VOICE_SPEED);
            welcomeContent = SpUtils.getStr(ThermalConst.Key.WELCOME_TIP_CONTENT, getResources().getString(R.string.setting_default_welcome_tip));
            welcomeEnabled = SpUtils.getBoolean(ThermalConst.Key.WELCOME_TIP_ENABLED, ThermalConst.Default.WELCOME_TIP_ENABLED);
            distanceContent = SpUtils.getStr(ThermalConst.Key.DISTANCE_TIP_CONTENT, getResources().getString(R.string.main_tips_please_close));
            distanceEnabled = SpUtils.getBoolean(ThermalConst.Key.DISTANCE_TIP_ENABLED, ThermalConst.Default.DISTANCE_TIP_ENABLED);
            frameContent = SpUtils.getStr(ThermalConst.Key.FRAME_TIP_CONTENT, getResources().getString(R.string.main_temp_tips_please_in_range));
            frameEnabled = SpUtils.getBoolean(ThermalConst.Key.FRAME_TIP_ENABLED, ThermalConst.Default.FRAME_TIP_ENABLED);
            normalContent = SpUtils.getStr(ThermalConst.Key.NORMAL_BROADCAST, getResources().getString(R.string.main_temp_normal_tips));
            normalTemperEnabled = SpUtils.getBoolean(ThermalConst.Key.NORMAL_TEMPER_SHOW, ThermalConst.Default.NORMAL_TEMPER_SHOW);
            warningContent = SpUtils.getStr(ThermalConst.Key.WARNING_BROADCAST, getResources().getString(R.string.main_temp_warning_tips));
            warningTemperEnabled = SpUtils.getBoolean(ThermalConst.Key.WARNING_TEMPER_SHOW, ThermalConst.Default.WARNING_TEMPER_SHOW);
            normalTemperLocation = SpUtils.getIntOrDef(ThermalConst.Key.NORMAL_TEMPER_LOCATION, ThermalConst.Default.NORMAL_TEMPER_LOCATION);
            warningTemperLocation = SpUtils.getIntOrDef(ThermalConst.Key.WARNING_TEMPER_LOCATION, ThermalConst.Default.WARNING_TEMPER_LOCATION);
            centigrade = SpUtils.getStr(ThermalConst.Key.CENTIGRADE, getResources().getString(R.string.temper_tips_centigrade));
            fahrenheit = SpUtils.getStr(ThermalConst.Key.FAHRENHEIT, getResources().getString(R.string.temper_tips_fahrenheit));
            normalEnabled = SpUtils.getBoolean(ThermalConst.Key.NORMAL_BROADCAST_ENABLED, ThermalConst.Default.NORMAL_BROADCAST_ENABLED);
            warningEnabled = SpUtils.getBoolean(ThermalConst.Key.WARNING_BROAD_ENABLED, ThermalConst.Default.WARNING_BROAD_ENABLED);
        }

        public String getMaskTip() {
            return maskTip;
        }

        public boolean isMaskTipEnabled() {
            return maskTipEnabled;
        }

        public boolean isNormalEnabled() {
            return normalEnabled;
        }

        public boolean isWarningEnabled() {
            return warningEnabled;
        }

        public String getCentigrade() {
            return centigrade;
        }

        public String getFahrenheit() {
            return fahrenheit;
        }

        public float getSpeechSpeed() {
            return speechSpeed;
        }

        public String getWelcomeContent() {
            return welcomeContent;
        }

        public boolean isWelcomeEnabled() {
            return welcomeEnabled;
        }

        public String getDistanceContent() {
            return distanceContent;
        }

        public boolean isDistanceEnabled() {
            return distanceEnabled;
        }

        public String getFrameContent() {
            return frameContent;
        }

        public boolean isFrameEnabled() {
            return frameEnabled;
        }

        public String getNormalContentForUI(String temper) {
            String content = normalContent;
            if (normalTemperEnabled) {
                switch (normalTemperLocation) {
                    case 0:
                        content = temper + content;
                        break;
                    case 1:
                        content = content.replaceFirst("#", temper);
                        break;
                    case 2:
                        content = content + temper;
                        break;
                }
            }
            content = content.replaceAll("#", "");
            return content;
        }

        public String getNormalContentForSpeech(String temper, String unit) {
            String content = normalContent;
            if (normalTemperEnabled) {
                switch (normalTemperLocation) {
                    case 0:
                        content = temper + content;
                        break;
                    case 1:
                        content = content.replaceFirst("#", temper);
                        break;
                    case 2:
                        content = content + temper;
                        break;
                }
            }
            if (content.contains("℃")) {
                content = content.replace("℃", unit);
            } else if (content.contains("℉")) {
                content = content.replace("℉", unit);
            }
            content = content.replaceAll("#", "");
            return content;
        }

        public String getWarningContentForUI(String temper) {
            String content = warningContent;
            if (warningTemperEnabled) {
                switch (warningTemperLocation) {
                    case 0:
                        content = temper + content;
                        break;
                    case 1:
                        content = content.replaceFirst("#", temper);
                        break;
                    case 2:
                        content = content + temper;
                        break;
                }
            }
            content = content.replaceAll("#", "");
            return content;
        }

        public String getWarningContentForSpeech(String temper, String unit) {
            String content = warningContent;
            if (warningTemperEnabled) {
                switch (warningTemperLocation) {
                    case 0:
                        content = temper + content;
                        break;
                    case 1:
                        content = content.replaceFirst("#", temper);
                        break;
                    case 2:
                        content = content + temper;
                        break;
                }
            }
            if (content.contains("℃")) {
                content = content.replace("℃", unit);
            } else if (content.contains("℉")) {
                content = content.replace("℉", unit);
            }
            content = content.replaceAll("#", "");
            return content;
        }
    }
}