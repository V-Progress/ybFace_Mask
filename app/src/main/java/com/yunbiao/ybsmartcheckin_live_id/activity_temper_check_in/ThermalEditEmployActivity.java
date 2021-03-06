package com.yunbiao.ybsmartcheckin_live_id.activity_temper_check_in;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import com.alibaba.fastjson.JSONObject;
import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.yunbiao.faceview.CompareResult;
import com.yunbiao.faceview.FaceManager;
import com.yunbiao.faceview.FacePreviewInfo;
import com.yunbiao.faceview.FaceView;
import com.yunbiao.ybsmartcheckin_live_id.APP;
import com.yunbiao.ybsmartcheckin_live_id.FlavorType;
import com.yunbiao.ybsmartcheckin_live_id.R;
import com.yunbiao.ybsmartcheckin_live_id.activity.base.BaseActivity;
import com.yunbiao.ybsmartcheckin_live_id.adapter.ThermalSpinnerDepartAdapter;
import com.yunbiao.ybsmartcheckin_live_id.afinel.Constants;
import com.yunbiao.ybsmartcheckin_live_id.afinel.ResourceUpdate;
import com.yunbiao.ybsmartcheckin_live_id.bean.AddStaffResponse;
import com.yunbiao.ybsmartcheckin_live_id.db2.Company;
import com.yunbiao.ybsmartcheckin_live_id.db2.DaoManager;
import com.yunbiao.ybsmartcheckin_live_id.db2.Depart;
import com.yunbiao.ybsmartcheckin_live_id.db2.User;
import com.yunbiao.ybsmartcheckin_live_id.utils.SpUtils;
import com.yunbiao.ybsmartcheckin_live_id.utils.UIUtils;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.builder.PostFormBuilder;
import com.zhy.http.okhttp.callback.StringCallback;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import okhttp3.Call;
import okhttp3.Request;
import timber.log.Timber;


/**
 * Created by Administrator on 2018/8/7.
 */

public class ThermalEditEmployActivity extends BaseActivity implements View.OnClickListener {

    private static final String TAG = "EditEmployActivity";
    public static final String KEY_ID = "entryId";
    public static final String KEY_TYPE = "type";
    public static final int TYPE_ADD = 0;
    public static final int TYPE_EDIT = 1;

    private Button btn_submit;
    private Button btn_TakePhoto;
    private Button btn_ReTakePhoto;
    private Button btn_cancle;

    private ImageView iv_capture;
    private EditText et_name;
    private EditText et_num;
    private Spinner sp_depart;
    private EditText et_sign;
    private EditText et_job;
    private TextView tv_birth;

    private View pbTakePhoto;
    private FaceView faceView;
    private RadioGroup rgSex;

    @BindView(R.id.btn_add_depart)
    Button btnAddDepart;

    private List<Depart> departList = new ArrayList<>();
    private int type;
    private TextView tvTitle;
    private ThermalSpinnerDepartAdapter departAdapter;

    @Override
    protected int getPortraitLayout() {
        return R.layout.activity_thermal_editemploy;
    }

    @Override
    protected int getLandscapeLayout() {
        return R.layout.activity_thermal_editemploy_h;
    }

    @Override
    protected void initView() {
        tvTitle = findViewById(R.id.tv_title_edt);
        faceView = findViewById(R.id.face_view);
        et_name = findViewById(R.id.et_name);
        sp_depart = findViewById(R.id.sp_depart);
        et_sign = findViewById(R.id.et_sign);
        et_job = findViewById(R.id.et_job);
        et_num = findViewById(R.id.et_num);
        tv_birth = findViewById(R.id.tv_birth);
        btn_submit = findViewById(R.id.btn_submit);
        iv_capture = findViewById(R.id.iv_capture);
        btn_TakePhoto = findViewById(R.id.btn_TakePhoto);
        btn_cancle = findViewById(R.id.btn_cancle);
        btn_ReTakePhoto = findViewById(R.id.btn_ReTakePhoto);
        pbTakePhoto = findViewById(R.id.alv_take_photo);
        rgSex = findViewById(R.id.rg_sex);

        btn_TakePhoto.setOnClickListener(this);
        btn_ReTakePhoto.setOnClickListener(this);
        tv_birth.setOnClickListener(this);
        btn_submit.setOnClickListener(this);
        btn_cancle.setOnClickListener(this);

        faceView.setCallback(faceCallback);

        btnAddDepart.setOnClickListener(view -> addDepartDialog());

        if(Constants.FLAVOR_TYPE == FlavorType.ITALY){
            btnAddDepart.setVisibility(View.GONE);
        }
    }

    private void addDepartDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.act_departList_tip_qsrbmmc));
        View inflate = LayoutInflater.from(this).inflate(R.layout.dialog_depart, null);
        builder.setView(inflate);
        EditText edtDepartName = inflate.findViewById(R.id.et_departName);

        builder.setPositiveButton(getString(R.string.base_ensure), (dialog, which) -> {
            String departName = edtDepartName.getText().toString();
            if(TextUtils.isEmpty(departName)){
                edtDepartName.setError("请输入部门名称");
                return;
            }
            int comid = SpUtils.getCompany().getComid();
            if(comid == Constants.NOT_BIND_COMPANY_ID){
                List<Depart> departs = DaoManager.get().queryDepartByCompId(comid);
                long departId = 0l;
                if(departs != null){
                    for (Depart depart1 : departs) {
                        long depId = depart1.getDepId();
                        if(departId <= depId){
                            departId = depId;
                        }
                    }
                }
                departId += 1l;
                Depart depart = new Depart();
                depart.setId(departId);
                depart.setDepId(departId);
                depart.setCompId(comid);
                depart.setDepName(departName);
                DaoManager.get().add(depart);

                departList.add(depart);
                if(departAdapter != null){
                    departAdapter.notifyDataSetChanged();
                }
            } else {
                Map<String,String> params = new HashMap<>();
                params.put("name", departName);
                params.put("comId", String.valueOf(comid));
                Timber.d("新增部门：" + ResourceUpdate.ADDDEPART);
                Timber.d("参数：" + params.toString());
                OkHttpUtils.post()
                        .url(ResourceUpdate.ADDDEPART)
                        .params(params)
                        .build().execute(new StringCallback() {
                    @Override
                    public void onBefore(Request request, int id) {
                        UIUtils.showNetLoading(ThermalEditEmployActivity.this);
                    }

                    @Override
                    public void onAfter(int id) {
                        UIUtils.dismissNetLoading();
                    }

                    @Override
                    public void onError(Call call, Exception e, int id) {
                        Timber.d("失败：%s", e.getMessage());
                        UIUtils.showShort(ThermalEditEmployActivity.this,getResString(R.string.act_editEmploy_tip_tjsb) + "(" + e == null ?"NULL" : e.getMessage() +")");
                    }

                    @Override
                    public void onResponse(String response, int id) {
                        Timber.d("结果：%s", response);
                        if(TextUtils.isEmpty(response)){
                            UIUtils.showShort(ThermalEditEmployActivity.this,getResString(R.string.act_editEmploy_tip_tjsb));
                            return;
                        }

                        JSONObject jsonObject = JSONObject.parseObject(response);
                        Integer status = jsonObject.getInteger("status");
                        if(status != 1){
                            UIUtils.showShort(ThermalEditEmployActivity.this,getResString(R.string.act_editEmploy_tip_tjsb));
                            return;
                        }

                        Integer depId = jsonObject.getInteger("depId");
                        Depart de = new Depart();
                        de.setId(depId);
                        de.setDepId(depId);
                        de.setCompId(comid);
                        de.setDepName(departName);
                        DaoManager.get().addOrUpdate(de);
                        UIUtils.showShort(ThermalEditEmployActivity.this,getResString(R.string.act_editEmploy_tip_add_success));
                        departList.add(de);
                        if(departAdapter != null){
                            departAdapter.notifyDataSetChanged();
                        }
                    }
                });
            }
        });
        builder.setNegativeButton(getString(R.string.base_cancel), (dialog, which) -> {
            dialog.dismiss();
        });
        builder.show();
    }


    private int mHasFace = -1;
    private FaceView.FaceCallback faceCallback = new FaceView.FaceCallback() {
        @Override
        public void onReady() {
        }


        @Override
        public void onFaceDetection(Boolean hasFace, List<FacePreviewInfo> facePreviewInfoList) {
            if (hasFace) {
                if (facePreviewInfoList != null) {
                    if (facePreviewInfoList.size() == 1) {
                        mHasFace = 1;
                    } else if (facePreviewInfoList.size() > 1) {
                        mHasFace = -2;
                    }
                } else {
                    mHasFace = -1;
                }
            } else {
                mHasFace = -1;
            }
        }

        @Override
        public boolean onFaceDetection(boolean hasFace, FacePreviewInfo facePreviewInfo) {
            if (hasFace) {
                mHasFace = facePreviewInfo != null ? 1 : -1;
            } else {
                mHasFace = -1;
            }
            return false;
        }

        @Override
        public void onFaceVerify(CompareResult faceAuth) {

        }
    };

    @Override
    protected void initData() {
        if (getIntent() == null) {
            finish();
            return;
        }
        type = getIntent().getIntExtra(KEY_TYPE, -1);
        if (type == -1) {
            finish();
            return;
        }

        initDepart();

        //如果是修改，则只初始化部门
        if (type == TYPE_ADD) {

            tvTitle.setText(getResources().getString(R.string.act_editEmploy_zjyg));
            d("类型：新增");
            initAddLogic();
        } else {
            tvTitle.setText(getResources().getString(R.string.act_editEmploy_xgxx));
            d("类型：修改");
            initEditLogic();
        }
    }

    private Depart mCurrAddDepart;

    private boolean isNumberExists(String number){
        return DaoManager.get().queryNumberExists(SpUtils.getCompany().getComid(),number);
    }

    //初始化新增逻辑
    private void initAddLogic() {
        if(departList.size() > 0){
            mCurrAddDepart = departList.get(0);
        }
        rgSex.check(R.id.rb_male);
    }

    private void submitAddUser() {
        if (TextUtils.isEmpty(mCurrPhotoPath)) {
            UIUtils.showShort(this, APP.getContext().getResources().getString(R.string.act_editEmploy_tip_qxpz));
            return;
        }

        String name = et_name.getText().toString();
        if (TextUtils.isEmpty(name)) {
            UIUtils.showShort(this, APP.getContext().getResources().getString(R.string.act_editEmploy_tip_qtxxm));
            return;
        }

        // TODO: 2020/3/18 离线功能
        int comid = SpUtils.getCompany().getComid();
        if(comid != Constants.NOT_BIND_COMPANY_ID){
            if (mCurrAddDepart == null) {
                UIUtils.showShort(this, APP.getContext().getResources().getString(R.string.act_editEmploy_tip_qtxbm));
                return;
            }
        }

        int sex;
        int checkedRadioButtonId = rgSex.getCheckedRadioButtonId();
        if (checkedRadioButtonId == R.id.rb_male) {
            sex = 1;
        } else {
            sex = 0;
        }

        String number = et_num.getText().toString();
        if (TextUtils.isEmpty(number)) {
            UIUtils.showShort(this, APP.getContext().getResources().getString(R.string.act_editEmploy_tip_qtxbh));
            return;
        }

        if(isNumberExists(number)){
            UIUtils.showShort(this, APP.getContext().getResources().getString(R.string.user_number_is_exists));
            return;
        }

        String birthday = tv_birth.getText().toString();
        String position = et_job.getText().toString();
        String signature = et_sign.getText().toString();

        final User addUser = new User();
        addUser.setDepartId(mCurrAddDepart == null ? -1 : mCurrAddDepart.getDepId());
        addUser.setNumber(number);
        addUser.setSex(sex);
        addUser.setDepartName(mCurrAddDepart == null ? "" : mCurrAddDepart.getDepName());
        addUser.setName(name);
        addUser.setHeadPath(mCurrPhotoPath);
        addUser.setCompanyId(comid);
        addUser.setBirthday(birthday);
        addUser.setPosition(position);
        addUser.setAutograph(signature);

        Log.e(TAG, "submitAddUser: 头像路径名称：" + addUser.getHeadPath() );

        // TODO: 2020/3/18 离线功能 
        if (comid == Constants.NOT_BIND_COMPANY_ID) {
            UIUtils.showNetLoading(ThermalEditEmployActivity.this);
            List<User> users = DaoManager.get().queryAll(User.class);
            //第一次进入没有绑定过公司的情况
            if (users == null || users.size() <= 0) {
                addUser.setId(1);
                addUser.setFaceId("1");
            } else {
                long id = 1;
                long faceId = 1;
                for (int i = 0; i < users.size(); i++) {
                    User user = users.get(i);
                    //先取出最大的Id
                    if (user.getId() > id) {
                        id = user.getId();
                    }
                    //再取出最大的FaceId
                    String face = user.getFaceId();
                    int i1 = Integer.parseInt(face);
                    if (i1 > faceId) {
                        faceId = i1;
                    }
                }
                id += 1;
                faceId += 1;
                addUser.setId(id);
                addUser.setFaceId(faceId + "");
            }

            Log.e(TAG, "submitAddUser: " + addUser.getHeadPath());

            boolean b = FaceManager.getInstance().addUser(addUser.getFaceId(), addUser.getHeadPath());
            if (b) {
                DaoManager.get().add(addUser);
                UIUtils.showShort(ThermalEditEmployActivity.this, APP.getContext().getResources().getString(R.string.act_editEmploy_tip_add_success));
                UIUtils.dismissNetLoading();

                finish();
            } else {
                UIUtils.showShort(ThermalEditEmployActivity.this, APP.getContext().getResources().getString(R.string.act_editEmploy_add_face_failed));
                UIUtils.dismissNetLoading();
            }
            return;
        }

        Map<String, String> params = new HashMap<>();
        params.put("depId", addUser.getDepartId() + "");
        params.put("name", addUser.getName());
        params.put("number", addUser.getNumber());
        params.put("sex", addUser.getSex() + "");
        params.put("comId", addUser.getCompanyId() + "");

        if (!TextUtils.isEmpty(addUser.getAutograph())) {
            params.put("autograph", addUser.getAutograph());
        }
        if (!TextUtils.isEmpty(addUser.getPosition())) {
            params.put("position", addUser.getPosition());
        }
        if (!TextUtils.isEmpty(addUser.getBirthday())) {
            params.put("birthday", addUser.getBirthday());
        }

        File file = new File(addUser.getHeadPath());
        params.put("headName", file.getName());

        String addstaff = ResourceUpdate.ADDSTAFF;
        OkHttpUtils.post()
                .url(addstaff)
                .params(params)
                .addFile("head", file.getName(), file)
                .build()
                .execute(new StringCallback() {
                    @Override
                    public void onBefore(Request request, int id) {
                        UIUtils.showNetLoading(ThermalEditEmployActivity.this);
                    }

                    @Override
                    public void onError(Call call, Exception e, int id) {
                        d("提交失败：" + (e == null ? "NULL" : e.getMessage()));
                        UIUtils.showTitleTip(ThermalEditEmployActivity.this, APP.getContext().getResources().getString(R.string.act_editEmploy_submit_failed) + "：" + (e == null ? "NULL" : e.getClass().getSimpleName() + ": " + e.getMessage()));
                        UIUtils.dismissNetLoading();
                    }

                    @Override
                    public void onResponse(String response, int id) {
                        d(response);
                        AddStaffResponse addStaffResponse = new Gson().fromJson(response, AddStaffResponse.class);
                        if (addStaffResponse.getStatus() != 1) {
                            String errMsg;
                            switch (addStaffResponse.getStatus()) {
                                case 2://添加失败
                                    errMsg = getString(R.string.act_editEmploy_tip_tjsb);
                                    break;
                                case 3://员工不存在
                                    errMsg = getString(R.string.act_editEmploy_tip_gygbcz);
                                    break;
                                case 6://部门不存在
                                    errMsg = getString(R.string.act_editEmploy_tip_bczgbm);
                                    break;
                                case 7://不存在公司部门关系
                                    errMsg = getString(R.string.act_editEmploy_tip_gsmyzgbm);
                                    break;
                                case 8://不存在员工的公司部门信息
                                    errMsg = getString(R.string.act_editEmploy_tip_gsmyzgbm);
                                    break;
                                default://参数错误
                                    errMsg = getString(R.string.act_editEmploy_tip_cscw);
                                    break;
                            }
                            UIUtils.showTitleTip(ThermalEditEmployActivity.this, "" + errMsg);
                            UIUtils.dismissNetLoading();
                            return;
                        }

                        addUser.setId(addStaffResponse.getEntryId());
                        addUser.setFaceId(addStaffResponse.getFaceId());

                        boolean b = FaceManager.getInstance().addUser(addUser.getFaceId(), addUser.getHeadPath());
                        if (b) {
                            DaoManager.get().add(addUser);
                            UIUtils.showShort(ThermalEditEmployActivity.this, APP.getContext().getResources().getString(R.string.act_editEmploy_tip_add_success));
                            faceView.postDelayed(() -> {
                                UIUtils.dismissNetLoading();
                                finish();
                            }, 1000);
                        } else {
                            UIUtils.showShort(ThermalEditEmployActivity.this, APP.getContext().getResources().getString(R.string.act_editEmploy_add_face_failed));
                        }
                    }
                });


    }

    private void initDepart() {
        departList.clear();

        Company company = SpUtils.getCompany();
        List<Depart> departs = DaoManager.get().queryDepartByCompId(company.getComid());
        if (departs == null || departs.size() <= 0) {
            UIUtils.showShort(ThermalEditEmployActivity.this, APP.getContext().getResources().getString(R.string.act_editEmploy_please_set_depart));
        } else {
            departList.addAll(departs);
        }

        departAdapter = new ThermalSpinnerDepartAdapter(this, departList);
        sp_depart.setAdapter(departAdapter);
        Drawable drawable = getResources().getDrawable(R.drawable.shape_employ_button);
        sp_depart.setPopupBackgroundDrawable(drawable);
        sp_depart.setOnItemSelectedListener(onItemSelectedListener);
    }

    private AdapterView.OnItemSelectedListener onItemSelectedListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            mCurrAddDepart = departList.get(position);
            mCurrUpdateDepart = departList.get(position);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    };

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_TakePhoto:
                pbTakePhoto.setVisibility(View.VISIBLE);
                btn_TakePhoto.setVisibility(View.GONE);
                handler.sendEmptyMessage(0);
                break;
            case R.id.btn_ReTakePhoto:
                showDialog("是否重置所有信息？", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        resetInfo();
                    }
                });
                break;
            case R.id.tv_birth:
                showCalendar();
                break;
            case R.id.btn_submit:
                if (type == TYPE_ADD) {
                    submitAddUser();
                } else {
                    submitUpdateUser();
                }
                break;
            case R.id.btn_cancle:
            case R.id.iv_back:
                finish();
                break;
        }
    }

    private void resetInfo() {

    }

    private String mUpdatePhotoPath;
    private User user;
    private Depart mCurrUpdateDepart;

    private void initEditLogic() {
        long userId = getIntent().getLongExtra(ThermalEditEmployActivity.KEY_ID, -1);
        if (userId == -1) {
            UIUtils.showShort(this, APP.getContext().getResources().getString(R.string.act_editEmploy_unknown_error));
            finish();
            return;
        }

        user = DaoManager.get().queryUserById(userId);
        if (user == null) {
            UIUtils.showShort(this, APP.getContext().getResources().getString(R.string.act_editEmploy_tip_gygbcz));
            finish();
            return;
        }

        et_name.setText(user.getName());
        et_sign.setText(user.getAutograph());
        rgSex.check(user.getSex() == 1 ? R.id.rb_male : R.id.rb_female);
        et_job.setText(user.getPosition());
        et_num.setText(user.getNumber());
        Glide.with(this).load(user.getHeadPath()).asBitmap().into(iv_capture);
        mUpdatePhotoPath = user.getHeadPath();

        long departId = user.getDepartId();

        int index = 0;
        for (int i = 0; i < departList.size(); i++) {
            Depart depart = departList.get(i);
            if (depart.getDepId() == departId) {
                index = i;
                mCurrUpdateDepart = depart;
            }
        }
        sp_depart.setSelection(index);
    }

    private void submitUpdateUser() {
        if (TextUtils.isEmpty(mUpdatePhotoPath)) {
            UIUtils.showShort(this, APP.getContext().getResources().getString(R.string.act_editEmploy_tip_qxpz));
            return;
        }
        final String name = et_name.getText().toString();
        if (TextUtils.isEmpty(name)) {
            UIUtils.showShort(this, APP.getContext().getResources().getString(R.string.act_editEmploy_tip_qtxxm));
            return;
        }
        int checkedRadioButtonId = rgSex.getCheckedRadioButtonId();
        final int sex = checkedRadioButtonId == R.id.rb_male ? 1 : 0;

        final String number = et_num.getText().toString();
        if (TextUtils.isEmpty(number)) {
            UIUtils.showShort(this, APP.getContext().getResources().getString(R.string.act_editEmploy_tip_qtxbh));
            return;
        }

        if(!TextUtils.equals(number,user.getNumber()) && isNumberExists(number)){
            UIUtils.showShort(this, APP.getContext().getResources().getString(R.string.user_number_is_exists));
            return;
        }

        final String birthday = tv_birth.getText().toString();

        final String position = et_job.getText().toString();

        final String sign = et_sign.getText().toString();

        final File currFile = new File(mUpdatePhotoPath);
        File oldFile = new File(user.getHeadPath());
        final boolean isHeadUpdated = !TextUtils.equals(currFile.getName(), oldFile.getName());

        // TODO: 2020/3/18 离线功能
        if(user.getCompanyId() == Constants.NOT_BIND_COMPANY_ID){
            user.setName(name);
            user.setSex(sex);
            user.setDepartId(mCurrUpdateDepart == null ? -1 : mCurrUpdateDepart.getDepId());
            user.setDepartName(mCurrUpdateDepart == null ? "" :mCurrUpdateDepart.getDepName());
            user.setNumber(number);
            user.setPosition(position);
            user.setBirthday(birthday);
            user.setAutograph(sign);

            if(isHeadUpdated){
                user.setHeadPath(currFile.getPath());
                boolean b1 = FaceManager.getInstance().removeUser(user.getFaceId());
                Log.e(TAG, "submitUpdateUser: 删除旧特征：" + b1);
                boolean b = FaceManager.getInstance().addUser(user.getFaceId(), user.getHeadPath());
                if(b){
                    DaoManager.get().addOrUpdate(user);
                    UIUtils.showShort(this, APP.getContext().getResources().getString(R.string.act_editEmploy_update_complete));
                } else {
                    UIUtils.showShort(this, APP.getContext().getResources().getString(R.string.act_editEmploy_update_failed));
                }
            } else {
                DaoManager.get().addOrUpdate(user);
                UIUtils.showShort(this, APP.getContext().getString(R.string.act_editEmploy_update_info_success));
                UIUtils.dismissNetLoading();

                finish();
            }
            return;
        }

        Map<String, String> params = new HashMap<>();
        params.put("id", user.getId() + "");
        params.put("depId", mCurrUpdateDepart.getDepId() + "");

        if (!TextUtils.equals(name, user.getName())) {
            params.put("name", name);
        }
        if (sex != user.getSex()) {
            params.put("sex", sex + "");
        }
        if (!TextUtils.equals(position, user.getPosition())) {
            params.put("position", position);
        }
        if (!TextUtils.equals(birthday, user.getBirthday())) {
            params.put("birthday", birthday);
        }
        if (!TextUtils.equals(sign, user.getAutograph())) {
            params.put("autograph", sign);
        }
        if (!TextUtils.equals(number, user.getNumber())) {
            params.put("number", number);
        }

        PostFormBuilder builder = OkHttpUtils.post().url(ResourceUpdate.UPDATSTAFF).params(params);
        if (isHeadUpdated) {
            params.put("headName", currFile.getName());
            builder.addFile("head", currFile.getName(), currFile);
        } else {
            File file = new File(Environment.getExternalStorageDirectory(), "1.txt");
            if (!file.exists()) {
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            builder.addFile("head", file.getName(), file);
        }

        builder.build().execute(new StringCallback() {
            @Override
            public void onBefore(Request request, int id) {
                d("开始提交");
                UIUtils.showNetLoading(ThermalEditEmployActivity.this);
            }

            @Override
            public void onError(Call call, Exception e, int id) {
                d("提交失败：" + (e == null ? "NULL" : e.getMessage()));
                UIUtils.showTitleTip(ThermalEditEmployActivity.this, APP.getContext().getResources().getString(R.string.act_editEmploy_submit_failed) + "：" + (e == null ? "NULL" : e.getClass().getSimpleName() + ": " + e.getMessage()));
                UIUtils.dismissNetLoading();
            }

            @Override
            public void onResponse(String response, int id) {
                d(response);
                AddStaffResponse addStaffResponse = new Gson().fromJson(response, AddStaffResponse.class);
                if (addStaffResponse.getStatus() != 1) {
                    String errMsg;
                    switch (addStaffResponse.getStatus()) {
                        case 2://添加失败
                            errMsg = getString(R.string.act_editEmploy_tip_tjsb);
                            break;
                        case 3://员工不存在
                            errMsg = getString(R.string.act_editEmploy_tip_gygbcz);
                            break;
                        case 6://部门不存在
                            errMsg = getString(R.string.act_editEmploy_tip_bczgbm);
                            break;
                        case 7://不存在公司部门关系
                            errMsg = getString(R.string.act_editEmploy_tip_gsmyzgbm);
                            break;
                        case 8://不存在员工的公司部门信息
                            errMsg = getString(R.string.act_editEmploy_tip_gsmyzgbm);
                            break;
                        default://参数错误
                            errMsg = getString(R.string.act_editEmploy_tip_cscw);
                            break;
                    }
                    UIUtils.showTitleTip(ThermalEditEmployActivity.this, "" + errMsg);
                    UIUtils.dismissNetLoading();
                    return;
                }

                user.setHeadPath(currFile.getPath());
                user.setName(name);
                user.setSex(sex);
                user.setDepartId(mCurrUpdateDepart.getDepId());
                user.setDepartName(mCurrUpdateDepart.getDepName());
                user.setNumber(number);
                user.setPosition(position);
                user.setBirthday(birthday);
                user.setAutograph(sign);

                //判断是否需要更新头像
                if (isHeadUpdated) {
                    boolean b = FaceManager.getInstance().addUser(user.getFaceId(), user.getHeadPath());
                    if (!b) {
                        UIUtils.showShort(ThermalEditEmployActivity.this, APP.getContext().getResources().getString(R.string.act_editEmploy_update_failed));
                    } else {

                        long l = DaoManager.get().addOrUpdate(user);
                        Log.e(TAG, "onResponse: 更新用户库：" + l);
                    }
                } else {
                    long l = DaoManager.get().addOrUpdate(user);
                    Log.e(TAG, "onResponse: 更新用户库：" + l);
                }

                faceView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        UIUtils.dismissNetLoading();
                        finish();
                    }
                }, 1 * 1000);
            }
        });
    }

    private String mCurrPhotoPath;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            /*if (mHasFace == -1) {
                UIUtils.showTitleTip(ThermalEditEmployActivity.this, APP.getContext().getResources().getString(R.string.act_editEmploy_no_face));
                pbTakePhoto.setVisibility(View.GONE);
                btn_TakePhoto.setVisibility(View.VISIBLE);
                return;
            } else */if (mHasFace == -2) {
                UIUtils.showTitleTip(ThermalEditEmployActivity.this, APP.getContext().getResources().getString(R.string.act_editEmploy_please_face_only_one));
                pbTakePhoto.setVisibility(View.GONE);
                btn_TakePhoto.setVisibility(View.VISIBLE);
                return;
            }

            Bitmap bitmap;
            if(mHasFace == -1){
                bitmap = faceView.getCurrCameraFrame();
            } else {
                bitmap = faceView.takePicture();
            }
            if (bitmap != null) {
                mCurrPhotoPath = saveBitmap(bitmap);
                mUpdatePhotoPath = mCurrPhotoPath;
                Glide.with(ThermalEditEmployActivity.this).load(mCurrPhotoPath).asBitmap().override(100, 100).into(iv_capture);
            } else {
                UIUtils.showTitleTip(ThermalEditEmployActivity.this, APP.getContext().getResources().getString(R.string.act_editEmploy_photo_failed_try_again));
            }

            pbTakePhoto.setVisibility(View.GONE);
            btn_TakePhoto.setVisibility(View.VISIBLE);
        }
    };

    private void showDialog(String msg, DialogInterface.OnClickListener confirm) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.base_tip) + "!");
        builder.setMessage(msg);
        builder.setPositiveButton(getString(R.string.base_ensure), confirm);
        builder.setNegativeButton(getString(R.string.base_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    /***
     * 保存图片到本地
     * @param mBitmap
     * @return
     */
    public static String saveBitmap(Bitmap mBitmap) {
        File filePic;
        try {
            //格式化时间
            long time = System.currentTimeMillis();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
            String sdfTime = sdf.format(time);
            filePic = new File(Constants.HEAD_PATH + sdfTime + "_m.jpg");
            if (!filePic.exists()) {
                filePic.getParentFile().mkdirs();
                filePic.createNewFile();
            }
            FileOutputStream fos = new FileOutputStream(filePic);
            mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
        return filePic.getAbsolutePath();
    }

    private void showCalendar() {
        Calendar now = Calendar.getInstance();
        DatePickerDialog.OnDateSetListener onDateSetListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                String birthDay = year + "-" + (month + 1) + "-" + dayOfMonth;
                tv_birth.setText(birthDay);
            }
        };
        DatePickerDialog datePickerDialog = new DatePickerDialog(this, onDateSetListener, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        faceView.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        faceView.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        faceView.destory();
        UIUtils.dismissNetLoading();
    }

    @Override
    protected void d(String log) {
        Timber.tag(getClass().getSimpleName());
        Timber.d(log);
    }

    private void d(Throwable t) {
        Timber.tag(getClass().getSimpleName());
        Timber.d(t);
    }
}
