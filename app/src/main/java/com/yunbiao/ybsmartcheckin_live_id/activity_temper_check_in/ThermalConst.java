package com.yunbiao.ybsmartcheckin_live_id.activity_temper_check_in;

import com.yunbiao.ybsmartcheckin_live_id.APP;
import com.yunbiao.ybsmartcheckin_live_id.R;

public class ThermalConst {

    public interface Key {
        String FACE_ENABLED = "thermalFaceEnabled"; //启用人脸识别
        String TEMPER_ENABLED = "thermalTemperEnabled";//启用测温
        String TEMPER_MODULE = "thermalTemperatureModule";//测温模块
        String THERMAL_MIRROR = "thermalMirror";
        String LOW_TEMP_MODE = "lowTempMode";
        String AMBIENT_CORRECT = "ambientCorrect";
        String THERMAL_CORRECT = "thermalCorrect";
        String PERSON_FRAME = "personFrame";
        String SPEECH_DELAY = "speechDelay";
        String TEMP_MIN_THRESHOLD = "tempMinThreshold";
        String TEMP_WARNING_THRESHOLD = "tempWarningThreshold";
        String THERMAL_IMAGE_MIRROR = "thermalImageMirror";
        String THERMAL_F_ENABLED = "fEnabled";
        String SHOW_DIALOG = "thermalShowDialog";
        String VOICE_SPEED = "thermalVoiceSpeed";
        String SHOW_MAIN_LOGO = "thermalShowMainLogo";
        String SHOW_MAIN_INFO = "thermalShowMainInfo";
        String SHOW_MAIN_THERMAL = "thermalShowMainThermal";
        String MAIN_LOGO_TEXT = "thermalMainLogoText";
        String MAIN_LOGO_IMG = "thermalMainLogoImg";
        String AUTO_TEMPER = "thermalAutoTemper";
        String LOCAL_PRIORITY = "thermalLocalPriority";

        String MASK_DETECT_ENABLED = "thermalMaskDetectEnabled";
        //欢迎语
        String WELCOME_TIP_CONTENT = "welcomeTips";
        String WELCOME_TIP_ENABLED = "welcomeTipEnabled";
        //距离提示
        String DISTANCE_TIP_CONTENT = "thermalCloseTips";
        String DISTANCE_TIP_ENABLED = "distanceTip";
        //对框提示
        String FRAME_TIP_CONTENT = "frameTipContent";
        String FRAME_TIP_ENABLED = "frameTipEnabled";
        //正常播报
        String NORMAL_BROADCAST = "normalBroadcast";
        String NORMAL_TEMPER_SHOW = "normalTemperShow";
        String NORMAL_TEMPER_LOCATION = "normalTemperLocation";
        String NORMAL_BROADCAST_ENABLED = "normalBroadcastEnabled";
        //异常播报
        String WARNING_BROADCAST = "warningBroadcast";
        String WARNING_TEMPER_SHOW = "warningTemperShow";
        String WARNING_TEMPER_LOCATION = "warningTemperLocation";
        String WARNING_BROAD_ENABLED = "warningBroadcastEnabled";
        //单位
        String CENTIGRADE = "centigradeUnit";
        String FAHRENHEIT = "fahrenheitUnit";
        String MASK_TIP = "thermalMaskTip";
        String MASK_TIP_ENABLED = "thermalMaskTipEnabled";
        String TITLE_ENABLED = "thermalTitleEnabled";
        String TIP_DELAY = "thermalTipDelay";
        String VERIFY_MASK = "thermalVerifyMask";
    }

    public static class Default {
        public static final long TIP_DELAY = 500;
        public static final boolean VERIFY_MASK = true;
        public static boolean FACE_ENABLED = false;
        public static boolean TEMPER_ENABLED = true;
        public static int TEMPER_MODULE = TemperModuleType.MLX_16_4;
        public static boolean TITLE_ENABLED = true;
        public static int DEFAULT_LOGO_ID = R.mipmap.yb_logo;

        public static final float VOICE_SPEED = 1.8f;
        public static boolean SHOW_MAIN_LOGO = true;//LOGO
        public static boolean SHOW_MAIN_INFO = false;//主页信息
        public static final boolean SHOW_MAIN_THERMAL = true;//热成像
        public static final boolean SHOW_DIALOG = false;
        public static boolean THERMAL_MIRROR = true;
        public static boolean LOW_TEMP = true;
        public static float AMBIENT_CORRECT = 25.0f;
        public static float THERMAL_CORRECT = 0.0f;
        public static boolean PERSON_FRAME = true;
        public static long SPEECH_DELAY = 5000;
        public static float TEMP_MIN_THRESHOLD = 35.5f;
        public static float TEMP_WARNING_THRESHOLD = 37.3f;
        public static boolean THERMAL_IMAGE_MIRROR = true;
        public static boolean THERMAL_F_ENABLED = false;
        public static String MAIN_LOGO_TEXT = "YBFACE";
        public static String MAIN_LOGO_IMG = "";
        public static boolean AUTO_TEMPER = false;

        public static boolean MASK_TIP_ENABLED = true;

        public static boolean MASK_DETECT_ENABLED = true;
        //欢迎语
        public static final boolean WELCOME_TIP_ENABLED = true;
        //距离提示
        public static final boolean DISTANCE_TIP_ENABLED = true;
        //对框提示
        public static final boolean FRAME_TIP_ENABLED = true;
        //正常播报
        public static final boolean NORMAL_TEMPER_SHOW = true;
        public static final int NORMAL_TEMPER_LOCATION = 2;
        public static final boolean NORMAL_BROADCAST_ENABLED = true;
        //异常播报
        public static final boolean WARNING_TEMPER_SHOW = true;
        public static final int WARNING_TEMPER_LOCATION = 2;
        public static final boolean WARNING_BROAD_ENABLED = true;
        public static boolean LOCAL_PRIORITY = true;

        public static String WELCOME_TIP_CONTENT = APP.getContext().getResources().getString(R.string.setting_default_welcome_tip);
    }

}
