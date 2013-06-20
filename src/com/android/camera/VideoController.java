/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.camera;

import android.content.Context;
import android.view.LayoutInflater;
import android.hardware.Camera.Size;

import com.android.camera.ui.AbstractSettingPopup;
import com.android.camera.ui.ListPrefSettingPopup;
import com.android.camera.ui.MoreSettingPopup;
import com.android.camera.ui.PieItem;
import com.android.camera.ui.PieItem.OnClickListener;
import com.android.camera.ui.PieRenderer;
import com.android.camera.ui.TimeIntervalPopup;

public class VideoController extends PieController
        implements MoreSettingPopup.Listener,
        ListPrefSettingPopup.Listener,
        TimeIntervalPopup.Listener {


    private static String TAG = "CAM_videocontrol";
    private static float FLOAT_PI_DIVIDED_BY_TWO = (float) Math.PI / 2;

    private VideoModule mModule;
    private String[] mOtherKeys;
    private String[] mPictureKeys;

    // First level popup
    private MoreSettingPopup mPopup;
    // Second level popup
    private AbstractSettingPopup mSecondPopup;
    // First level popup
    private MoreSettingPopup mPicturePopup;
    private MoreSettingPopup mActivePopup;

    public VideoController(CameraActivity activity, VideoModule module, PieRenderer pie) {
        super(activity, pie);
        mModule = module;
    }

    public void initialize(PreferenceGroup group) {
        super.initialize(group);
        mPopup = null;
        mPicturePopup = null;
        mSecondPopup = null;
        float sweep = FLOAT_PI_DIVIDED_BY_TWO / 2;

        addItem(CameraSettings.KEY_VIDEOCAMERA_FLASH_MODE, FLOAT_PI_DIVIDED_BY_TWO - sweep, sweep);
        addItem(CameraSettings.KEY_VIDEOCAMERA_EXPOSURE, 3 * FLOAT_PI_DIVIDED_BY_TWO - 2 * sweep, sweep);
        addItem(CameraSettings.KEY_VIDEOCAMERA_WHITE_BALANCE, 3 * FLOAT_PI_DIVIDED_BY_TWO + sweep, sweep);
        if (group.findPreference(CameraSettings.KEY_CAMERA_ID) != null) {
            PieItem item = makeItem(R.drawable.ic_switch_video_facing_holo_light);
            item.setFixedSlice(FLOAT_PI_DIVIDED_BY_TWO + sweep,  sweep);
            item.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(PieItem item) {
                    // Find the index of next camera.
                    ListPreference pref = mPreferenceGroup.findPreference(CameraSettings.KEY_CAMERA_ID);
                    if (pref != null) {
                        int index = pref.findIndexOfValue(pref.getValue());
                        CharSequence[] values = pref.getEntryValues();
                        index = (index + 1) % values.length;
                        int newCameraId = Integer.parseInt((String) values[index]);
                        mListener.onCameraPickerClicked(newCameraId);
                    }
                }
            });
            mRenderer.addItem(item);
        }
        mOtherKeys = new String[] {
                CameraSettings.KEY_STORAGE,
                CameraSettings.KEY_RECORD_LOCATION,
                CameraSettings.KEY_POWER_SHUTTER,
                CameraSettings.KEY_TRUE_PREVIEW,
                CameraSettings.KEY_VIDEO_QUALITY,
                CameraSettings.KEY_VIDEO_EFFECT,
                CameraSettings.KEY_VIDEOCAMERA_JPEG,
                CameraSettings.KEY_VIDEO_TIME_LAPSE_FRAME_INTERVAL};

        mPictureKeys= new String[] {
                CameraSettings.KEY_VIDEOCAMERA_FLASH_MODE,
                CameraSettings.KEY_VIDEOCAMERA_WHITE_BALANCE,
                CameraSettings.KEY_VIDEOCAMERA_COLOR_EFFECT,
                CameraSettings.KEY_VIDEOCAMERA_EXPOSURE,
                CameraSettings.KEY_VIDEOCAMERA_SATURATION,
                CameraSettings.KEY_VIDEOCAMERA_CONTRAST,
                CameraSettings.KEY_VIDEOCAMERA_SHARPNESS};

        PieItem settingsItem = makeItem(R.drawable.ic_settings_holo_light);
        settingsItem.setFixedSlice(FLOAT_PI_DIVIDED_BY_TWO *3, sweep);
        settingsItem.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(PieItem item) {
                if (mPopup == null) {
                    initializePopup();
                }
                mActivePopup = mPopup;
                mModule.showPopup(mPopup);
            }
        });
        mRenderer.addItem(settingsItem);

        PieItem pictureItem = makeItem(R.drawable.ic_effects_holo_light);
        pictureItem.setFixedSlice(3 * FLOAT_PI_DIVIDED_BY_TWO - sweep, sweep);
        pictureItem.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(PieItem item) {
                if (mPicturePopup == null) {
                    initializePicturePopup();
                }
                mActivePopup = mPicturePopup;
                mModule.showPopup(mPicturePopup);
            }
        });
        mRenderer.addItem(pictureItem);
    }

    protected void setCameraId(int cameraId) {
        ListPreference pref = mPreferenceGroup.findPreference(CameraSettings.KEY_CAMERA_ID);
        pref.setValue("" + cameraId);
    }

    @Override
    public void reloadPreferences() {
        super.reloadPreferences();
        if (mPopup != null) {
            mPopup.reloadPreference();
        }
        if (mPicturePopup != null) {
            mPicturePopup.reloadPreference();
        }
    }

    @Override
    // Hit when an item in the second-level popup gets selected
    public void onListPrefChanged(ListPreference pref) {
        if (mActivePopup != null && mSecondPopup != null) {
                mModule.dismissPopup(true);
                mActivePopup.reloadPreference();
        }
        onSettingChanged(pref);
    }

    @Override
    public void overrideSettings(final String ... keyvalues) {
        super.overrideSettings(keyvalues);
        if (mPopup == null) initializePopup();
        mPopup.overrideSettings(keyvalues);

        if (mPicturePopup == null) initializePicturePopup();
        mPicturePopup.overrideSettings(keyvalues);
    }

    protected void initializePopup() {
        LayoutInflater inflater = (LayoutInflater) mActivity.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);

        MoreSettingPopup popup = (MoreSettingPopup) inflater.inflate(
                R.layout.more_setting_popup, null, false);
        popup.setSettingChangedListener(this);
        popup.initialize(mPreferenceGroup, mOtherKeys);
        if (mActivity.isSecureCamera()) {
            // Prevent location preference from getting changed in secure camera mode
            popup.setPreferenceEnabled(CameraSettings.KEY_RECORD_LOCATION, false);
        }
        mPopup = popup;
    }

    protected void initializePicturePopup() {
        LayoutInflater inflater = (LayoutInflater) mActivity.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);

        MoreSettingPopup popup = (MoreSettingPopup) inflater.inflate(
                R.layout.more_setting_popup, null, false);
        popup.setSettingChangedListener(this);
        popup.initialize(mPreferenceGroup, mPictureKeys);
        mPicturePopup = popup;
    }
    
    public void popupDismissed(boolean topPopupOnly) {
        if (mActivePopup == mPopup){
            initializePopup();
            mActivePopup = mPopup;
        }
        if (mActivePopup == mPicturePopup){
            initializePicturePopup();
            mActivePopup = mPicturePopup; 
        }

        // if the 2nd level popup gets dismissed
        if (mSecondPopup != null) {
            mSecondPopup = null;
            if (topPopupOnly) mModule.showPopup(mActivePopup);
        }
    }

    @Override
    // Hit when an item in the first-level popup gets selected, then bring up
    // the second-level popup
    public void onPreferenceClicked(ListPreference pref) {
        if (mSecondPopup != null) return;

        LayoutInflater inflater = (LayoutInflater) mActivity.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);

        if (CameraSettings.KEY_VIDEO_TIME_LAPSE_FRAME_INTERVAL.equals(pref.getKey())) {
            TimeIntervalPopup timeInterval = (TimeIntervalPopup) inflater.inflate(
                    R.layout.time_interval_popup, null, false);
            timeInterval.initialize((IconListPreference) pref);
            timeInterval.setSettingChangedListener(this);
            mModule.dismissPopup(true);
            mSecondPopup = timeInterval;
        } else {
            ListPrefSettingPopup basic = (ListPrefSettingPopup) inflater.inflate(
                    R.layout.list_pref_setting_popup, null, false);
            basic.initialize(pref);
            basic.setSettingChangedListener(this);
            mModule.dismissPopup(true);
            mSecondPopup = basic;
        }
        mModule.showPopup(mSecondPopup);
    }
}
