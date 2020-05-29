package com.yunbiao.ybsmartcheckin_live_id.activity_temper_check_in;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.alibaba.fastjson.JSONObject;
import com.bumptech.glide.Glide;
import com.lcw.library.imagepicker.ImagePicker;
import com.lcw.library.imagepicker.utils.ImageLoader;
import com.yunbiao.ybsmartcheckin_live_id.APP;
import com.yunbiao.ybsmartcheckin_live_id.FlavorType;
import com.yunbiao.ybsmartcheckin_live_id.R;
import com.yunbiao.ybsmartcheckin_live_id.activity.Event.DisplayOrientationEvent;
import com.yunbiao.ybsmartcheckin_live_id.activity.base.BaseActivity;
import com.yunbiao.ybsmartcheckin_live_id.afinel.Constants;
import com.yunbiao.ybsmartcheckin_live_id.afinel.ResourceUpdate;
import com.yunbiao.ybsmartcheckin_live_id.system.HeartBeatClient;
import com.yunbiao.ybsmartcheckin_live_id.activity.PowerOnOffActivity;
import com.yunbiao.ybsmartcheckin_live_id.utils.SpUtils;
import com.yunbiao.ybsmartcheckin_live_id.utils.UIUtils;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.StringCallback;

import org.greenrobot.eventbus.EventBus;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Request;

public class ThermalSettingActivity extends BaseActivity {
    private static final String TAG = "SettingActivity";


    @Override
    protected int getPortraitLayout() {
        return R.layout.activity_thermal_setting;
    }

    @Override
    protected int getLandscapeLayout() {
        return R.layout.activity_thermal_setting;
    }

    @Override
    protected void initView() {
        RadioGroup rgTab = findViewById(R.id.rg_tab_setting);
        rgTab.check(R.id.rb_face_setting);
        replaceFragment(R.id.fl_setting_content, new FaceSettingFragment());
        rgTab.setOnCheckedChangeListener((group, checkedId) -> {
            Fragment fragment = null;
            switch (checkedId) {
                case R.id.rb_face_setting:
                    fragment = new FaceSettingFragment();
                    break;
                case R.id.rb_page_setting:
                    fragment = new PageSettingFragment();
                    break;
                case R.id.rb_temper_setting:
                    fragment = new TemperSettingFragment();
                    break;
                case R.id.rb_device_setting:
                    fragment = new DeviceSettingFragment();
                    break;
            }
            replaceFragment(R.id.fl_setting_content, fragment);
        });
    }

    @Override
    protected void initData() {

    }


    public void jumpTag(View view) {
        final boolean jumpTag = SpUtils.getBoolean(Constants.JUMP_TAG, Constants.DEFAULT_JUMP_TAG);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(APP.getContext().getResources().getString(R.string.setting_switch_function));
        builder.setMessage(APP.getContext().getResources().getString(R.string.setting_switch_tip1));
        builder.setNegativeButton(APP.getContext().getResources().getString(R.string.setting_switch_cancel), (dialog, which) -> dialog.dismiss());
        builder.setPositiveButton(APP.getContext().getResources().getString(R.string.setting_switch_confirm), (dialog, which) -> {
            dialog.dismiss();
            SpUtils.saveBoolean(Constants.JUMP_TAG, !jumpTag);

            APP.exit2();
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private static final int REQEST_SELECT_IMAGES_CODE = 12345;
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQEST_SELECT_IMAGES_CODE && resultCode == RESULT_OK) {
            List<String> imagePaths = data.getStringArrayListExtra(ImagePicker.EXTRA_SELECT_IMAGES);
            if (1 > imagePaths.size()) {
                return;
            }
            String imgPath = imagePaths.get(0);
            ImageView ivMainLogo = findViewById(R.id.iv_main_logo);
            if (TextUtils.isEmpty(imgPath)) {
                UIUtils.showShort(this, getResString(R.string.select_img_failed));
                ivMainLogo.setImageResource(R.mipmap.yb_logo);
            } else {
                File file = new File(imgPath);
                if (!file.exists()) {
                    UIUtils.showShort(this, getResString(R.string.select_img_failed_not_exists));
                    ivMainLogo.setImageResource(R.mipmap.yb_logo);
                } else {
                    SpUtils.saveStr(ThermalConst.Key.MAIN_LOGO_IMG, imgPath);
                    ivMainLogo.setImageBitmap(BitmapFactory.decodeFile(file.getPath()));
                }
            }
        }
    }

    public static class FaceSettingFragment extends Fragment {
        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_face_setting, container, false);
            return rootView;
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            initView(getView());
        }

        private void initView(View view){
            //设置对比度======================================================================
            final EditText edtSimilar = view.findViewById(R.id.edt_similar_threshold);
            int similar = SpUtils.getIntOrDef(SpUtils.SIMILAR_THRESHOLD, 80);
            edtSimilar.setText(similar + "");
            view.findViewById(R.id.btn_set_similar_threshold).setOnClickListener(v -> {
                String similar1 = edtSimilar.getText().toString();
                if (TextUtils.isEmpty(similar1)) {
                    edtSimilar.setText(similar1 + "");
                    return;
                }
                int sml = Integer.parseInt(similar1);
                SpUtils.saveInt(SpUtils.SIMILAR_THRESHOLD, sml);
                Activity activity = APP.getMainActivity();
                if (activity != null) {
                    if (activity instanceof ThermalImage2Activity) {
                        ((ThermalImage2Activity) activity).setFaceViewSimilar();
                    }
                }
            });

            //设置活体=========================================================================
            Switch swLiveness = view.findViewById(R.id.sw_liveness_setting);
            boolean liveness = SpUtils.getBoolean(SpUtils.LIVENESS_ENABLED, false);
            swLiveness.setChecked(liveness);
            swLiveness.setOnCheckedChangeListener((buttonView, isChecked) -> SpUtils.saveBoolean(SpUtils.LIVENESS_ENABLED, isChecked));

            //口罩开关=========================================================================
            Switch swMaskMode = view.findViewById(R.id.sw_mask_mode);
            boolean maskDetectEnabled = SpUtils.getBoolean(ThermalConst.Key.MASK_DETECT_ENABLED,ThermalConst.Default.MASK_DETECT_ENABLED);
            swMaskMode.setChecked(maskDetectEnabled);
            swMaskMode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    SpUtils.saveBoolean(ThermalConst.Key.MASK_DETECT_ENABLED,isChecked);
                }
            });

            //人脸框=========================================================================
            //人脸框横向镜像
            CheckBox cbMirror = view.findViewById(R.id.cb_mirror);
            final boolean mirror = SpUtils.isMirror();
            cbMirror.setChecked(mirror);
            cbMirror.setOnCheckedChangeListener((buttonView, isChecked) -> SpUtils.setMirror(isChecked));
            //人脸框纵向镜像
            CheckBox cbVerticalMirror = view.findViewById(R.id.cb_vertical_mirror);
            boolean isVMirror = SpUtils.getBoolean(SpUtils.IS_V_MIRROR, Constants.DEFAULT_V_MIRROR);
            cbVerticalMirror.setChecked(isVMirror);
            cbVerticalMirror.setOnCheckedChangeListener((buttonView, isChecked) -> SpUtils.saveBoolean(SpUtils.IS_V_MIRROR, isChecked));

            //人脸弹窗=========================================================================
            Switch switchFaceDialog = view.findViewById(R.id.sw_face_dialog);
            boolean faceDialog = SpUtils.getBoolean(ThermalConst.Key.SHOW_DIALOG, ThermalConst.Default.SHOW_DIALOG);
            switchFaceDialog.setChecked(faceDialog);
            switchFaceDialog.setOnCheckedChangeListener((buttonView, isChecked) -> SpUtils.saveBoolean(ThermalConst.Key.SHOW_DIALOG, isChecked));

            //摄像头设置=========================================================================
            //摄像头角度
            Button btnAngle = view.findViewById(R.id.btn_setAngle);
            int angle = SpUtils.getIntOrDef(SpUtils.CAMERA_ANGLE, Constants.DEFAULT_CAMERA_ANGLE);
            btnAngle.setText(getString(R.string.setting_cam_angle) + ":" + angle);
            btnAngle.setOnClickListener(v -> {
                int anInt = SpUtils.getIntOrDef(SpUtils.CAMERA_ANGLE, Constants.DEFAULT_CAMERA_ANGLE);
                if (anInt == 0) {
                    anInt = 90;
                } else if (anInt == 90) {
                    anInt = 180;
                } else if (anInt == 180) {
                    anInt = 270;
                } else {
                    anInt = 0;
                }
                btnAngle.setText(getString(R.string.setting_cam_angle) + ":" + anInt);
                SpUtils.saveInt(SpUtils.CAMERA_ANGLE, anInt);
                EventBus.getDefault().post(new DisplayOrientationEvent());
            });

            Button btnPicRotation = view.findViewById(R.id.btn_picture_rotation);
            int picRotation = SpUtils.getIntOrDef(SpUtils.PICTURE_ROTATION, Constants.DEFAULT_PICTURE_ROTATION);
            btnPicRotation.setText(picRotation == -1 ? (getResources().getString(R.string.setting_picture_rotation)) : (getString(R.string.setting_cam_angle) + ":" + picRotation));
            btnPicRotation.setOnClickListener(v -> {
                int picRotation1 = SpUtils.getIntOrDef(SpUtils.PICTURE_ROTATION, Constants.DEFAULT_PICTURE_ROTATION);
                if (picRotation1 == -1) {
                    picRotation1 = 0;
                } else if (picRotation1 == 0) {
                    picRotation1 = 90;
                } else if (picRotation1 == 90) {
                    picRotation1 = 180;
                } else if (picRotation1 == 180) {
                    picRotation1 = 270;
                } else {
                    picRotation1 = -1;
                }
                btnPicRotation.setText(picRotation1 == -1 ? (getResources().getString(R.string.setting_picture_rotation)) : (getString(R.string.setting_cam_angle) + ":" + picRotation1));
                SpUtils.saveInt(SpUtils.PICTURE_ROTATION, picRotation1);
            });
        }
    }

    public static class PageSettingFragment extends Fragment {
        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_page_setting, container, false);
            return rootView;
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            initView(getView());
        }

        private void initView(View view){
            //屏保===========================================================================================
            boolean isEnabled = SpUtils.getBoolean(SpUtils.POSTER_ENABLED, Constants.DEFAULT_POSTER_ENABLED);
            Switch swPoster = view.findViewById(R.id.sw_poster_setting);
            swPoster.setChecked(isEnabled);
            swPoster.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    SpUtils.saveBoolean(SpUtils.POSTER_ENABLED, isChecked);
                }
            });

            //logo设置===========================================================================================
            boolean showMainLogo = SpUtils.getBoolean(ThermalConst.Key.SHOW_MAIN_LOGO, ThermalConst.Default.SHOW_MAIN_LOGO);
            Switch swMainLogo = view.findViewById(R.id.sw_main_logo_setting);
            swMainLogo.setChecked(showMainLogo);
            swMainLogo.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    SpUtils.saveBoolean(ThermalConst.Key.SHOW_MAIN_LOGO, isChecked);
                }
            });

            //热成像设置===========================================================================================
            boolean showMainThermal = SpUtils.getBoolean(ThermalConst.Key.SHOW_MAIN_THERMAL, ThermalConst.Default.SHOW_MAIN_THERMAL);
            Switch swMainThermal = view.findViewById(R.id.sw_main_thermal_setting);
            swMainThermal.setChecked(showMainThermal);
            swMainThermal.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    SpUtils.saveBoolean(ThermalConst.Key.SHOW_MAIN_THERMAL, isChecked);
                }
            });

            //首页信息==============================================================================================
            boolean showMainInfo = SpUtils.getBoolean(ThermalConst.Key.SHOW_MAIN_INFO, ThermalConst.Default.SHOW_MAIN_INFO);
            Switch swMainInfo = view.findViewById(R.id.sw_main_info_setting);
            swMainInfo.setChecked(showMainInfo);
            swMainInfo.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    SpUtils.saveBoolean(ThermalConst.Key.SHOW_MAIN_INFO, isChecked);
                }
            });

            //二维码设置=============================================================================================
            boolean qrCodeEnabled = SpUtils.getBoolean(SpUtils.QRCODE_ENABLED, Constants.DEFAULT_QRCODE_ENABLED);
            Switch swQrCode = view.findViewById(R.id.sw_qrcode_setting);
            swQrCode.setChecked(qrCodeEnabled);
            swQrCode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    SpUtils.saveBoolean(SpUtils.QRCODE_ENABLED, isChecked);
                }
            });

            //首页logo文字============================================================================================
            String mainLogoText = SpUtils.getStr(ThermalConst.Key.MAIN_LOGO_TEXT, ThermalConst.Default.MAIN_LOGO_TEXT);
            EditText edtMainLogoText = view.findViewById(R.id.edt_main_logo_text);
            edtMainLogoText.setText(mainLogoText);
            edtMainLogoText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    String s1 = s.toString().trim();
                    if (TextUtils.isEmpty(s1)) {
                        SpUtils.remove(ThermalConst.Key.MAIN_LOGO_TEXT);
                    } else {
                        SpUtils.saveStr(ThermalConst.Key.MAIN_LOGO_TEXT, s1);
                    }
                }
            });

            //设置首页LOGO================================================================================
            Button btnRestore = view.findViewById(R.id.btn_restore_main_logo);
            Button btnSaveMainLogo = view.findViewById(R.id.btn_save_main_logo);
            ImageView ivMainLogo = view.findViewById(R.id.iv_main_logo);
            String mainLogoImg = SpUtils.getStr(ThermalConst.Key.MAIN_LOGO_IMG, ThermalConst.Default.MAIN_LOGO_IMG);
            if (TextUtils.isEmpty(mainLogoImg)) {
                ivMainLogo.setImageResource(R.mipmap.yb_logo);
            } else {
                File file = new File(mainLogoImg);
                if (!file.exists()) {
                    ivMainLogo.setImageResource(R.mipmap.yb_logo);
                } else {
                    ivMainLogo.setImageBitmap(BitmapFactory.decodeFile(mainLogoImg));
                }
            }
            btnSaveMainLogo.setOnClickListener(v -> {
                ImagePicker.getInstance()
                        .setTitle(getResources().getString(R.string.select_img_title))//设置标题
                        .showCamera(true)//设置是否显示拍照按钮
                        .showImage(true)//设置是否展示图片
                        .showVideo(false)//设置是否展示视频
//                    .setMaxCount(1)//设置最大选择图片数目(默认为1，单选)
//                    .setImagePaths(mImageList)//保存上一次选择图片的状态，如果不需要可以忽略
                        .setImageLoader(new ImgLoader())//设置自定义图片加载器
                        .start(getActivity(), REQEST_SELECT_IMAGES_CODE);//REQEST_SELECT_IMAGES_CODE为Intent调用的
            });
            btnRestore.setOnClickListener(v -> {
                SpUtils.remove(ThermalConst.Key.MAIN_LOGO_IMG);
                ivMainLogo.setImageResource(R.mipmap.yb_logo);
            });

            //优先级配置=======================================================================================
            Switch swLocalLogo = view.findViewById(R.id.sw_local_logo_setting);
            boolean localPriority = SpUtils.getBoolean(ThermalConst.Key.LOCAL_PRIORITY, ThermalConst.Default.LOCAL_PRIORITY);
            swLocalLogo.setChecked(localPriority);
            swLocalLogo.setOnCheckedChangeListener((buttonView, isChecked) -> SpUtils.saveBoolean(ThermalConst.Key.LOCAL_PRIORITY, isChecked));

            //跳转语音设置
            view.findViewById(R.id.btn_go_speech).setOnClickListener(v -> startActivity(new Intent(getActivity(), SpeechContentActivity.class)));

        }
        class ImgLoader implements ImageLoader {

            @Override
            public void loadImage(ImageView imageView, String imagePath) {
                Glide.with(APP.getContext()).load(imagePath).asBitmap().override(50, 50).into(imageView);
            }

            @Override
            public void loadPreImage(ImageView imageView, String imagePath) {
                Glide.with(APP.getContext()).load(imagePath).asBitmap().override(50, 50).into(imageView);
            }

            @Override
            public void clearMemoryCache() {
                //清理缓存
                Glide.get(APP.getContext()).clearMemory();
            }
        }
    }

    public static class TemperSettingFragment extends Fragment {
        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_temper_setting, container, false);
            return rootView;
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            initView(getView());
        }

        private void initView(View view) {
            //模式==================================================================================
            final TextView tvModelSetting = view.findViewById(R.id.tv_model_setting);
            final String[] items = ThermalConst.models;
            final int model = SpUtils.getIntOrDef(ThermalConst.Key.MODE, ThermalConst.Default.MODE);
            //如果是红外模式或人脸模式则隐藏矫正按钮
            if (model == ThermalConst.FACE_INFRARED || model == ThermalConst.ONLY_INFRARED || model == ThermalConst.ONLY_FACE || model == ThermalConst.ONLY_THERMAL_HM_16_4 || model == ThermalConst.FACE_THERMAL_HM_16_4) {
                view.findViewById(R.id.btn_thermal_corr).setVisibility(View.GONE);
            } else {
                view.findViewById(R.id.btn_thermal_corr).setVisibility(View.VISIBLE);
            }
            Log.e(TAG, "initView: 当前模式=============== " + model);
            switch (model) {
                case 7:
                case 8:
                    view.findViewById(R.id.btn_thermal_corr).setVisibility(View.GONE);
                    break;
                default:
                    view.findViewById(R.id.btn_thermal_corr).setVisibility(View.VISIBLE);
                    break;
            }
            tvModelSetting.setText(items[model]);
            tvModelSetting.setOnClickListener(v -> {
                final int currModel = SpUtils.getIntOrDef(ThermalConst.Key.MODE, ThermalConst.Default.MODE);
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(getResources().getString(R.string.setting_select_model));
                builder.setSingleChoiceItems(items, currModel, (dialog, whichModel) -> {
                    Log.e(TAG, "initView: 选中模式=============== " + whichModel);
                    switch (whichModel) {
                        case 7:
                        case 8:
                            view.findViewById(R.id.btn_thermal_corr).setVisibility(View.GONE);
                            break;
                        default:
                            view.findViewById(R.id.btn_thermal_corr).setVisibility(View.VISIBLE);
                            break;
                    }
                    Log.e(TAG, "onClick: 模式选择：" + whichModel);
                    //如果模式相同则直接隐藏
                    if (whichModel == currModel) {
                        dialog.dismiss();
                        return;
                    }
                    //如果是红外模式则隐藏矫正按钮
                    if (whichModel == ThermalConst.FACE_INFRARED || whichModel == ThermalConst.ONLY_INFRARED || whichModel == ThermalConst.ONLY_FACE) {
                        view.findViewById(R.id.btn_thermal_corr).setVisibility(View.GONE);
                    } else {
                        view.findViewById(R.id.btn_thermal_corr).setVisibility(View.VISIBLE);
                    }
                    SpUtils.saveInt(ThermalConst.Key.MODE, whichModel);
                    tvModelSetting.setText(items[whichModel]);

                    dialog.dismiss();
                });
                AlertDialog alertDialog = builder.create();
                alertDialog.show();
            });

            //隐私模式=====================================================================================================
            boolean isPrivacyModeEnabled = SpUtils.getBoolean(Constants.Key.PRIVACY_MODE, Constants.Default.PRIVACY_MODE);
            Switch swPrivacy = view.findViewById(R.id.sw_privacy_mode);
            swPrivacy.setChecked(isPrivacyModeEnabled);
            swPrivacy.setOnCheckedChangeListener((buttonView, isChecked) -> {
                SpUtils.saveBoolean(Constants.Key.PRIVACY_MODE, isChecked);
            });

            //热成像镜像==========================================================================================
            boolean thermalImgMirror = SpUtils.getBoolean(ThermalConst.Key.THERMAL_IMAGE_MIRROR, ThermalConst.Default.THERMAL_IMAGE_MIRROR);
            Switch swThermalMirror = view.findViewById(R.id.sw_thermal_imag_mirror_setting);
            swThermalMirror.setChecked(thermalImgMirror);
            swThermalMirror.setOnCheckedChangeListener((buttonView, isChecked) -> SpUtils.saveBoolean(ThermalConst.Key.THERMAL_IMAGE_MIRROR, isChecked));

            //====华氏度开关==============================================================
            boolean fEnabled = SpUtils.getBoolean(ThermalConst.Key.THERMAL_F_ENABLED, ThermalConst.Default.THERMAL_F_ENABLED);
            Switch swFEnabled = view.findViewById(R.id.sw_f_enabled_setting);
            swFEnabled.setChecked(fEnabled);
            swFEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> SpUtils.saveBoolean(ThermalConst.Key.THERMAL_F_ENABLED, isChecked));

            //人相框========================
            boolean aBoolean1 = SpUtils.getBoolean(ThermalConst.Key.PERSON_FRAME, ThermalConst.Default.PERSON_FRAME);
            Switch swPersonFrame = view.findViewById(R.id.sw_person_frame_setting);
            swPersonFrame.setChecked(aBoolean1);
            swPersonFrame.setOnCheckedChangeListener((buttonView, isChecked) -> SpUtils.saveBoolean(ThermalConst.Key.PERSON_FRAME, isChecked));

            //===低温模式=========================================================
            Switch swLowTempModel = view.findViewById(R.id.sw_low_temp_model_setting);
            Switch swAutoMode = view.findViewById(R.id.sw_auto_model_setting);
            boolean aBoolean = SpUtils.getBoolean(ThermalConst.Key.LOW_TEMP_MODE, ThermalConst.Default.LOW_TEMP);
            swLowTempModel.setChecked(aBoolean);
            swLowTempModel.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked && swAutoMode.isChecked()) {
                    SpUtils.saveBoolean(ThermalConst.Key.LOW_TEMP_MODE, false);
                    swAutoMode.setChecked(false);
                }
                SpUtils.saveBoolean(ThermalConst.Key.LOW_TEMP_MODE, isChecked);
            });

            //===自动模式=========================================================
            boolean autoTemper = SpUtils.getBoolean(ThermalConst.Key.AUTO_TEMPER, ThermalConst.Default.AUTO_TEMPER);
            swAutoMode.setChecked(autoTemper);
            swAutoMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked && swLowTempModel.isChecked()) {
                    SpUtils.saveBoolean(ThermalConst.Key.LOW_TEMP_MODE, false);
                    swLowTempModel.setChecked(false);
                }
                SpUtils.saveBoolean(ThermalConst.Key.AUTO_TEMPER, isChecked);
            });

            //进入矫正=======================================================================================================
            float thermalCorrect = SpUtils.getFloat(ThermalConst.Key.THERMAL_CORRECT, ThermalConst.Default.THERMAL_CORRECT);
            Button btnCorrectSub = view.findViewById(R.id.btn_correct_sub_setting);
            Button btnCorrectAdd = view.findViewById(R.id.btn_correct_add_setting);
            final EditText edtCorrect = view.findViewById(R.id.edt_correct_setting);
            edtCorrect.setText(thermalCorrect + "");
            View.OnClickListener correctOnclickListener = v -> {
                String s = edtCorrect.getText().toString();
                float corrValue = Float.parseFloat(s);
                if (v.getId() == R.id.btn_correct_sub_setting) {
                    corrValue -= 0.1f;
                } else {
                    corrValue += 0.1f;
                }
                corrValue = ((ThermalSettingActivity)getActivity()).formatF(corrValue);
                SpUtils.saveFloat(ThermalConst.Key.THERMAL_CORRECT, corrValue);
                edtCorrect.setText(corrValue + "");
            };
            btnCorrectSub.setOnClickListener(correctOnclickListener);
            btnCorrectAdd.setOnClickListener(correctOnclickListener);
            view.findViewById(R.id.btn_thermal_corr).setOnClickListener(v -> startActivity(new Intent(getActivity(), TemperatureCorrectActivity.class)));

            //修改测温阈值==========================================================================================
            Button btnMinSub = view.findViewById(R.id.btn_temp_min_threshold_sub_setting);
            Button btnMinAdd = view.findViewById(R.id.btn_temp_min_threshold_add_setting);
            final EditText edtMinThreshold = view.findViewById(R.id.edt_temp_min_threshold_setting);
            //温度最低阈值
            final float minValue = SpUtils.getFloat(ThermalConst.Key.TEMP_MIN_THRESHOLD, ThermalConst.Default.TEMP_MIN_THRESHOLD);
            edtMinThreshold.setText(minValue + "");
            View.OnClickListener minClickListener = v -> {
                String value = edtMinThreshold.getText().toString();
                float v1 = ((ThermalSettingActivity)getActivity()).formatF(Float.parseFloat(value));
                switch (v.getId()) {
                    case R.id.btn_temp_min_threshold_sub_setting:
                        v1 -= 0.1;
                        break;
                    case R.id.btn_temp_min_threshold_add_setting:
                        v1 += 0.1;
                        break;
                }
                v1 = ((ThermalSettingActivity)getActivity()).formatF(v1);
                edtMinThreshold.setText(v1 + "");
                SpUtils.saveFloat(ThermalConst.Key.TEMP_MIN_THRESHOLD, v1);
            };
            btnMinSub.setOnClickListener(minClickListener);
            btnMinAdd.setOnClickListener(minClickListener);

            //修改测温报警值==========================================================================================
            Button btnWarnSub = view.findViewById(R.id.btn_temp_warning_threshold_sub_setting);
            Button btnWarnAdd = view.findViewById(R.id.btn_temp_warning_threshold_add_setting);
            final EditText edtWarnThreshold = view.findViewById(R.id.edt_temp_warning_threshold_setting);
            final float warningValue = SpUtils.getFloat(ThermalConst.Key.TEMP_WARNING_THRESHOLD, ThermalConst.Default.TEMP_WARNING_THRESHOLD);
            edtWarnThreshold.setText(warningValue + "");
            View.OnClickListener warnClickListener = v -> {
                String value = edtWarnThreshold.getText().toString();
                float v1 = ((ThermalSettingActivity)getActivity()).formatF(Float.parseFloat(value));
                if (v.getId() == R.id.btn_temp_warning_threshold_sub_setting) {
                    v1 -= 0.1;
                } else {
                    v1 += 0.1;
                }
                v1 = ((ThermalSettingActivity)getActivity()).formatF(v1);
                edtWarnThreshold.setText(v1 + "");
                SpUtils.saveFloat(ThermalConst.Key.TEMP_WARNING_THRESHOLD, v1);
            };
            btnWarnSub.setOnClickListener(warnClickListener);
            btnWarnAdd.setOnClickListener(warnClickListener);

            //体温播报延时==========================================================================================
            Button btnSpeechDelaySub = view.findViewById(R.id.btn_speech_delay_sub_setting);
            Button btnSpeechDelayAdd = view.findViewById(R.id.btn_speech_delay_add_setting);
            final EditText edtSpeechDelay = view.findViewById(R.id.edt_speech_delay_setting);
            long speechDelayTime = SpUtils.getLong(ThermalConst.Key.SPEECH_DELAY, ThermalConst.Default.SPEECH_DELAY);
            edtSpeechDelay.setText(speechDelayTime + "");
            View.OnClickListener speechDelayOnClickLitsener = v -> {
                String s = edtSpeechDelay.getText().toString();
                long l = Long.parseLong(s);
                if (v.getId() == R.id.btn_speech_delay_add_setting) {
                    l += 100;
                } else {
                    l -= 100;
                    if (l < 1500) {
                        l = 1500;
                    }
                }
                edtSpeechDelay.setText(l + "");
                SpUtils.saveLong(ThermalConst.Key.SPEECH_DELAY, l);
            };
            btnSpeechDelayAdd.setOnClickListener(speechDelayOnClickLitsener);
            btnSpeechDelaySub.setOnClickListener(speechDelayOnClickLitsener);
        }
    }

    public static class DeviceSettingFragment extends Fragment {
        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_device_setting, container, false);
            return rootView;
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            initView(getView());
        }

        private void initView(View view) {
            //CPU状态==================================================================================
            final TextView tvCpuTemper = view.findViewById(R.id.tv_cpu_temper);
            Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
                final String s = CpuUtils.getCpuTemperatureFinder() + "℃";
                getActivity().runOnUiThread(() -> tvCpuTemper.setText(s));
            }, 0, 3, TimeUnit.SECONDS);

            //网络状态=================================================================================
            TextView tvNetState = view.findViewById(R.id.tv_wifi_state);
            String net = "";
            boolean intenetConnected = isInternetConnected(getActivity());
            if (intenetConnected) {
                net = getString(R.string.setting_net_prefix) + getHostIp() + "】";
            } else {
                net = "【WIFI，" + getWifiInfo(0) + getString(R.string.setting_ip_info) + getWifiInfo(1) + "】";
            }
            tvNetState.setText(net);

            //IC读卡器===============================================================================
            boolean readCardEnabled = SpUtils.getBoolean(SpUtils.READ_CARD_ENABLED, Constants.DEFAULT_READ_CARD_ENABLED);
            Switch swReadCard = view.findViewById(R.id.sw_readcard_setting);
            swReadCard.setChecked(readCardEnabled);
            swReadCard.setOnCheckedChangeListener((buttonView, isChecked) -> SpUtils.saveBoolean(SpUtils.READ_CARD_ENABLED, isChecked));

            //开门时间===================================================================================
            final EditText edtDelay = view.findViewById(R.id.edt_delay);
            int cacheDelay = SpUtils.getIntOrDef(SpUtils.GPIO_DELAY, 5);
            edtDelay.setText(cacheDelay + "");
            edtDelay.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    String s1 = edtDelay.getText().toString();
                    if (TextUtils.isEmpty(s1)) {
                        return;
                    }
                    int delay = Integer.parseInt(s1);
                    SpUtils.saveInt(SpUtils.GPIO_DELAY, delay);
                    UIUtils.showShort(getActivity(), getString(R.string.setting_edit_password_success));
                }
            });

            //清除策略================================================================================
            RadioGroup rgClear = view.findViewById(R.id.rg_clear_policy);
            EditText edtPolicy = view.findViewById(R.id.edt_policy_custom);
            int customDate = SpUtils.getIntOrDef(Constants.Key.CLEAR_POLICY_CUSTOM, Constants.Default.CLEAR_POLICY_CUSTOM);
            edtPolicy.setText(String.valueOf(customDate));
            edtPolicy.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }
                @Override
                public void afterTextChanged(Editable s) {
                    String content = s.toString();
                    if(TextUtils.isEmpty(content)){
                        SpUtils.remove(Constants.Key.CLEAR_POLICY_CUSTOM);
                    } else {
                        SpUtils.saveInt(Constants.Key.CLEAR_POLICY_CUSTOM,Integer.parseInt(content));
                    }
                }
            });

            int clearPolicy = SpUtils.getIntOrDef(Constants.Key.CLEAR_POLICY, Constants.Default.CLEAR_POLICY);
            switch (clearPolicy) {
                case 0:
                    rgClear.check(R.id.rb_clear_policy_7);
                    break;
                case 1:
                    rgClear.check(R.id.rb_clear_policy_15);
                    break;
                case 2:
                    rgClear.check(R.id.rb_clear_policy_30);
                    break;
                case 3:
                    rgClear.check(R.id.rb_clear_policy_custom);
                    break;
            }
            rgClear.setOnCheckedChangeListener((group, checkedId) -> {
                switch (checkedId) {
                    case R.id.rb_clear_policy_7:
                        edtPolicy.setEnabled(false);
                        SpUtils.saveInt(Constants.Key.CLEAR_POLICY,0);
                        break;
                    case R.id.rb_clear_policy_15:
                        edtPolicy.setEnabled(false);
                        SpUtils.saveInt(Constants.Key.CLEAR_POLICY,1);
                        break;
                    case R.id.rb_clear_policy_30:
                        edtPolicy.setEnabled(false);
                        SpUtils.saveInt(Constants.Key.CLEAR_POLICY,2);
                        break;
                    case R.id.rb_clear_policy_custom:
                        edtPolicy.setEnabled(true);
                        SpUtils.saveInt(Constants.Key.CLEAR_POLICY,3);
                        break;
                }
            });

            //重启=====================================================================================
            view.findViewById(R.id.tv_reboot).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showAlert(getString(R.string.setting_device_will_reboot), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ProgressDialog progressDialog = UIUtils.coreInfoShow3sDialog(getActivity());
                            progressDialog.setTitle(getString(R.string.setting_device_reboot));
                            progressDialog.setMessage(getString(R.string.setting_3_scond_reboot));
                            progressDialog.setCancelable(false);
                            progressDialog.show();
                            UIUtils.restart.start();
                        }
                    }, null, null);
                }
            });

            //管理密码================================================================================
            view.findViewById(R.id.btn_pwd).setOnClickListener(v -> setPwd());

            //电源设置================================================================================
            view.findViewById(R.id.tv_power).setOnClickListener(v -> startActivity(new Intent(getActivity(), PowerOnOffActivity.class)));

            //设置IP=================================================================================
            initSetIp(view);
        }

        private static class CpuUtils {
            private CpuUtils() {
                //no instance
            }

            private static final List<String> CPU_TEMP_FILE_PATHS = Arrays.asList(
                    "/sys/devices/system/cpu/cpu0/cpufreq/cpu_temp",
                    "/sys/devices/system/cpu/cpu0/cpufreq/FakeShmoo_cpu_temp",
                    "/sys/class/thermal/thermal_zone0/temp",
                    "/sys/class/i2c-adapter/i2c-4/4-004c/temperature",
                    "/sys/devices/platform/tegra-i2c.3/i2c-4/4-004c/temperature",
                    "/sys/devices/platform/omap/omap_temp_sensor.0/temperature",
                    "/sys/devices/platform/tegra_tmon/temp1_input",
                    "/sys/kernel/debug/tegra_thermal/temp_tj",
                    "/sys/devices/platform/s5p-tmu/temperature",
                    "/sys/class/thermal/thermal_zone1/temp",
                    "/sys/class/hwmon/hwmon0/device/temp1_input",
                    "/sys/devices/virtual/thermal/thermal_zone1/temp",
                    "/sys/devices/virtual/thermal/thermal_zone0/temp",
                    "/sys/class/thermal/thermal_zone3/temp",
                    "/sys/class/thermal/thermal_zone4/temp",
                    "/sys/class/hwmon/hwmonX/temp1_input",
                    "/sys/devices/platform/s5p-tmu/curr_temp");

            public static final String getCpuTemperatureFinder() {
                String currTemp = "-1";
                for (String cpuTempFilePath : CPU_TEMP_FILE_PATHS) {
                    Double temp = readOneLine(new File(cpuTempFilePath));
                    String validPath = "";
                    double currentTemp = 0.0D;
                    if (isTemperatureValid(temp)) {
                        validPath = cpuTempFilePath;
                        currentTemp = temp;
                    } else if (isTemperatureValid(temp / (double) 1000)) {
                        validPath = cpuTempFilePath;
                        currentTemp = temp / (double) 1000;
                    }

                    if (!TextUtils.isEmpty(validPath)
                            && (currentTemp != 0)) {
                        currTemp = currentTemp + "";
                    }
                }
                return currTemp;
            }

            private static double readOneLine(File file) {
                FileInputStream fileInputStream = null;
                String s = "";
                try {
                    fileInputStream = new FileInputStream(file);
                    InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                    s = bufferedReader.readLine();
                    fileInputStream.close();
                    inputStreamReader.close();
                    bufferedReader.close();
                } catch (IOException e) {
                }

                double result = 0;
                try {
                    result = Double.parseDouble(s);
                } catch (NumberFormatException ignored) {
                }
                return result;
            }

            private static boolean isTemperatureValid(double temp) {
                return temp >= -30.0D && temp <= 250.0D;
            }
        }

        private EditText edtIp;
        private EditText edtResPort;
        private EditText edtXmppPort;
        private EditText edtProName;

        //初始化IP设置
        private void initSetIp(View view) {
            edtIp = view.findViewById(R.id.edt_ip);
            edtResPort = view.findViewById(R.id.edt_res_port);
            edtXmppPort = view.findViewById(R.id.edt_xmpp_port);
            edtProName = view.findViewById(R.id.edt_pro_name);
            RadioGroup rgServerModel = view.findViewById(R.id.rg_server_model);
            final RadioButton rbYun = view.findViewById(R.id.rb_yun);
            final RadioButton rbJu = view.findViewById(R.id.rb_ju);
            Button btnSave = view.findViewById(R.id.btn_save_address);
            if (SpUtils.getIntOrDef(SpUtils.SERVER_MODEL, Constants.serverModel.YUN) == Constants.serverModel.YUN) {
                rbYun.setChecked(true);
                setServerInfo(Constants.serverModel.YUN);
            } else {
                rbJu.setChecked(true);
                setServerInfo(Constants.serverModel.JU);
            }
            rgServerModel.setOnCheckedChangeListener((group, checkedId) -> {
                if (rbYun.isChecked()) {
                    setServerInfo(Constants.serverModel.YUN);
                }
                if (rbJu.isChecked()) {
                    setServerInfo(Constants.serverModel.JU);
                }
            });

            btnSave.setOnClickListener(v -> {
                String mIp = edtIp.getText().toString();
                String mResPort = edtResPort.getText().toString();
                String mXmppPort = edtXmppPort.getText().toString();
                String mProName = edtProName.getText().toString();
                if (TextUtils.isEmpty(mIp)) {
                    UIUtils.showTitleTip(getActivity(), APP.getContext().getResources().getString(R.string.setting_please_set_ip));
                    return;
                }


                if (TextUtils.isEmpty(mResPort)) {
                    UIUtils.showTitleTip(getActivity(), APP.getContext().getResources().getString(R.string.setting_please_set_res));
                    return;
                }
                int intResPort = Integer.parseInt(mResPort);
                if (intResPort > 65535) {
                    UIUtils.showTitleTip(getActivity(), APP.getContext().getResources().getString(R.string.setting_res_port_error));
                    return;
                }

                if (TextUtils.isEmpty(mXmppPort)) {
                    UIUtils.showTitleTip(getActivity(), APP.getContext().getResources().getString(R.string.setting_please_set_xmpp));
                    return;
                }
                int intXmppPort = Integer.parseInt(mXmppPort);
                if (intXmppPort > 65535) {
                    UIUtils.showTitleTip(getActivity(), APP.getContext().getResources().getString(R.string.setting_xmpp_port_error));
                    return;
                }

                if (TextUtils.isEmpty(mProName)) {
                }

                if (rbYun.isChecked()) {
                    SpUtils.saveInt(SpUtils.SERVER_MODEL, Constants.serverModel.YUN);
                } else if (rbJu.isChecked()) {
                    SpUtils.saveInt(SpUtils.SERVER_MODEL, Constants.serverModel.JU);
                    SpUtils.saveStr(SpUtils.JU_IP_CACHE, mIp);
                    SpUtils.saveStr(SpUtils.JU_RESOURCE_PORT_CACHE, mResPort);
                    SpUtils.saveStr(SpUtils.JU_XMPP_PORT_CACHE, mXmppPort);
                    SpUtils.saveStr(SpUtils.JU_PROJECT_NAME_SUFFIX, mProName);
                }
                UIUtils.showTitleTip(getActivity(), APP.getContext().getResources().getString(R.string.setting_save_succ_please_restart));
            });
        }

        private void setServerInfo(int model) {
            String ip = Constants.NetConfig.PRO_URL;
            String resPort = Constants.NetConfig.PRO_RES_PORT;
            String xmppPort = Constants.NetConfig.PRO_XMPP_PORT;
            String proName = Constants.NetConfig.PRO_SUFFIX;
            if (model == Constants.serverModel.YUN) {
                edtIp.setText(ip);
                edtResPort.setText(resPort);
                edtXmppPort.setText(xmppPort);
                edtProName.setText(proName);
                edtIp.setEnabled(false);
                edtResPort.setEnabled(false);
                edtXmppPort.setEnabled(false);
                edtProName.setEnabled(false);
            } else {
                ip = SpUtils.getStr(SpUtils.JU_IP_CACHE);
                resPort = SpUtils.getStr(SpUtils.JU_RESOURCE_PORT_CACHE);
                xmppPort = SpUtils.getStr(SpUtils.JU_XMPP_PORT_CACHE);
                proName = SpUtils.getStr(SpUtils.JU_PROJECT_NAME_SUFFIX);
                edtIp.setEnabled(true);
                edtResPort.setEnabled(true);
                edtXmppPort.setEnabled(true);
                edtProName.setEnabled(true);

                edtIp.setText(ip);
                edtResPort.setText(resPort);
                edtXmppPort.setText(xmppPort);
                edtProName.setText(proName);
            }
        }

        private String getHostIp() {
            String hostIp = null;
            try {
                Enumeration nis = NetworkInterface.getNetworkInterfaces();
                InetAddress ia = null;
                while (nis.hasMoreElements()) {
                    NetworkInterface ni = (NetworkInterface) nis.nextElement();
                    Enumeration<InetAddress> ias = ni.getInetAddresses();
                    while (ias.hasMoreElements()) {
                        ia = ias.nextElement();
                        if (ia instanceof Inet6Address) {
                            continue;// skip ipv6
                        }
                        String ip = ia.getHostAddress();
                        if (!"127.0.0.1".equals(ip)) {
                            hostIp = ia.getHostAddress();
                            break;
                        }
                    }
                }
            } catch (SocketException e) {
                Log.i("yao", "SocketException");
                e.printStackTrace();
            }
            return hostIp;
        }

        private String getWifiInfo(int type) {
            WifiManager wifiManager = (WifiManager) APP.getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (!wifiManager.isWifiEnabled()) {
                return null;
            }

            WifiInfo wi = wifiManager.getConnectionInfo();
            Log.e(TAG, "getWifiInfo() wi=" + wi);
            if (wi == null) {
                return null;
            }
            if (type == 0) {
                return APP.getContext().getResources().getString(R.string.setting_wifi_name) + wi.getSSID() + APP.getContext().getResources().getString(R.string.setting_wifi_rssi) + wi.getRssi();
            }

            //获取32位整型IP地址
            int ipAdd = wi.getIpAddress();
            Log.e(TAG, "getWifiInfo() ipAdd=" + ipAdd);
            if (ipAdd == 0) {
                return null;
            }
            //把整型地址转换成“*.*.*.*”地址
            String ip = intToIp(ipAdd);
            Log.e(TAG, "getWifiInfo() ip=" + ip);

            if (ip == null || ip.startsWith("0")) {
                return null;
            }
            return ip;
        }

        private static String intToIp(int i) {
            return (i & 0xFF) + "." +
                    ((i >> 8) & 0xFF) + "." +
                    ((i >> 16) & 0xFF) + "." +
                    (i >> 24 & 0xFF);
        }

        private static boolean isNullObject(Object object) {

            if (object == null) {
                return true;
            }

            return false;
        }

        private boolean isInternetConnected(Context context) {
            if (context != null) {
                ConnectivityManager mConnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo mInternetNetWorkInfo = mConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET);
                boolean hasInternet = !isNullObject(mInternetNetWorkInfo) && mInternetNetWorkInfo.isConnected() && mInternetNetWorkInfo.isAvailable();
                return hasInternet;
            }
            return false;
        }

        private void showAlert(String msg, Dialog.OnClickListener onClickListener, Dialog.OnClickListener onCancel, DialogInterface.OnDismissListener onDissmissListener) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(getString(R.string.base_tip));
            builder.setMessage(msg);
            builder.setPositiveButton(getString(R.string.base_ensure), onClickListener);
            builder.setNegativeButton(getString(R.string.base_cancel), onCancel);
            if (onDissmissListener != null) {
                builder.setOnDismissListener(onDissmissListener);
            }

            AlertDialog alertDialog = builder.create();
            Window window = alertDialog.getWindow();
            alertDialog.show();
            window.setWindowAnimations(R.style.mystyle);  //添加动画
        }

        public void setPwd() {
            final Dialog dialog = new Dialog(getActivity());
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.layout_set_pwd);

            final EditText edtPwd = (EditText) dialog.findViewById(R.id.edt_set_pwd);
            final EditText edtPwd2 = (EditText) dialog.findViewById(R.id.edt_set_pwd_again);
            final Button btnCancel = (Button) dialog.findViewById(R.id.btn_pwd_cancel);
            final Button btnConfirm = (Button) dialog.findViewById(R.id.btn_pwd_confirm);

            btnCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });

            btnConfirm.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (TextUtils.isEmpty(edtPwd.getText())) {
                        edtPwd.setError(getString(R.string.setting_password_not_null));
                        return;
                    }
                    if (edtPwd.getText().length() < 6) {
                        edtPwd.setError(getString(R.string.setting_password_min_6));
                        return;
                    }
                    if (TextUtils.isEmpty(edtPwd2.getText())) {
                        edtPwd2.setError(getString(R.string.setting_please_input_password_agian));
                        return;
                    }
                    String pwd = edtPwd.getText().toString();
                    final String pwd2 = edtPwd2.getText().toString();
                    if (!TextUtils.equals(pwd, pwd2)) {
                        edtPwd2.setError(getString(R.string.setting_password_disaccord));
                        return;
                    }

                    btnCancel.setEnabled(false);
                    btnConfirm.setEnabled(false);
                    Map<String, String> params = new HashMap<>();
                    params.put("deviceNo", HeartBeatClient.getDeviceNo());
                    params.put("password", pwd2);
                    OkHttpUtils.post().url(ResourceUpdate.UPDATE_PWD).params(params).build().execute(new StringCallback() {
                        @Override
                        public void onBefore(Request request, int id) {
                            super.onBefore(request, id);
                            UIUtils.showNetLoading(getActivity());
                        }

                        @Override
                        public void onError(Call call, final Exception e, int id) {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    UIUtils.showTitleTip(getActivity(), getString(R.string.setting_edit_password_failed) + ":" + e != null ? e.getMessage() : "NULL");
                                }
                            });
                        }

                        @Override
                        public void onResponse(String response, int id) {
                            JSONObject jsonObject = JSONObject.parseObject(response);
                            final Integer status = jsonObject.getInteger("status");
                            getActivity().runOnUiThread(() -> {
                                if (status == 1) {
                                    UIUtils.showTitleTip(getActivity(), getString(R.string.setting_edit_password_success));
                                    SpUtils.saveStr(SpUtils.MENU_PWD, pwd2);
                                    dialog.dismiss();
                                } else {
                                    UIUtils.showTitleTip(getActivity(), getString(R.string.setting_edit_password_failed));
                                }
                            });
                        }

                        @Override
                        public void onAfter(int id) {
                            UIUtils.dismissNetLoading();
                            btnConfirm.setEnabled(true);
                            btnCancel.setEnabled(true);
                        }
                    });
                }
            });

            dialog.show();
            Window window = dialog.getWindow();
            window.setWindowAnimations(R.style.mystyle);  //添加动画
            window.setBackgroundDrawableResource(android.R.color.transparent);
        }
    }
}