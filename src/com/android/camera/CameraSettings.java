/*
 * Copyright (C) 2009 The Android Open Source Project
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

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.media.CamcorderProfile;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.util.FloatMath;
import android.util.Log;

import com.android.gallery3d.common.ApiHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 *  Provides utilities and keys for Camera settings.
 */
public class CameraSettings {
    private static final int NOT_FOUND = -1;

    public static final String KEY_VERSION = "pref_version_key";
    public static final String KEY_LOCAL_VERSION = "pref_local_version_key";
    public static final String KEY_RECORD_LOCATION = RecordLocationPreference.KEY;
    public static final String KEY_VIDEO_QUALITY = "pref_video_quality_key";
    public static final String KEY_VIDEO_TIME_LAPSE_FRAME_INTERVAL = "pref_video_time_lapse_frame_interval_key";
    public static final String KEY_PICTURE_SIZE = "pref_camera_picturesize_key";
    public static final String KEY_JPEG_QUALITY = "pref_camera_jpegquality_key";
    public static final String KEY_FOCUS_MODE = "pref_camera_focusmode_key";
    public static final String KEY_FOCUS_TIME = "pref_camera_focustime_key";
    public static final String KEY_FLASH_MODE = "pref_camera_flashmode_key";
    public static final String KEY_VIDEOCAMERA_FLASH_MODE = "pref_camera_video_flashmode_key";
    public static final String KEY_WHITE_BALANCE = "pref_camera_whitebalance_key";
    public static final String KEY_VIDEOCAMERA_WHITE_BALANCE = "pref_camera_video_whitebalance_key";
    public static final String KEY_SCENE_MODE = "pref_camera_scenemode_key";
    public static final String KEY_EXPOSURE = "pref_camera_exposure_key";
    public static final String KEY_VIDEOCAMERA_EXPOSURE = "pref_camera_video_exposure_key";
    public static final String KEY_VIDEO_EFFECT = "pref_video_effect_key";
    public static final String KEY_CAMERA_ID = "pref_camera_id_key";
    public static final String KEY_CAMERA_HDR = "pref_camera_hdr_key";
    public static final String KEY_CAMERA_FIRST_USE_HINT_SHOWN = "pref_camera_first_use_hint_shown_key";
    public static final String KEY_VIDEO_FIRST_USE_HINT_SHOWN = "pref_video_first_use_hint_shown_key";
    public static final String KEY_POWER_SHUTTER = "pref_power_shutter";
    public static final String KEY_ISO_MODE = "pref_camera_iso_key";
    public static final String KEY_JPEG = "pref_camera_jpeg_key";
    public static final String KEY_VIDEOCAMERA_JPEG = "pref_camera_video_jpeg_key";
    public static final String KEY_COLOR_EFFECT = "pref_camera_coloreffect_key";
    public static final String KEY_VIDEOCAMERA_COLOR_EFFECT = "pref_camera_video_coloreffect_key";
    public static final String KEY_BURST_MODE = "pref_camera_burst_key";
    public static final String KEY_TRUE_PREVIEW = "pref_true_preview";
    public static final String KEY_STORAGE = "pref_camera_storage_key";
    public static final String KEY_SATURATION = "pref_camera_saturation_key";
    public static final String KEY_VIDEOCAMERA_SATURATION = "pref_camera_video_saturation_key";
    public static final String KEY_CONTRAST = "pref_camera_contrast_key";
    public static final String KEY_VIDEOCAMERA_CONTRAST = "pref_camera_video_contrast_key";
    public static final String KEY_SHARPNESS = "pref_camera_sharpness_key";
    public static final String KEY_VIDEOCAMERA_SHARPNESS = "pref_camera_video_sharpness_key";
    public static final String KEY_VOICE_SHUTTER = "pref_voice_shutter_key";

    public static final String EXPOSURE_DEFAULT_VALUE = "0";
    public static final String SATURATION_DEFAULT_VALUE = "5";
    public static final String CONTRAST_DEFAULT_VALUE = "0";
    public static final String SHARPNESS_DEFAULT_VALUE = "0";
        
    public static final String VALUE_ON = "on";
    public static final String VALUE_OFF = "off";

    public static final int CURRENT_VERSION = 5;
    public static final int CURRENT_LOCAL_VERSION = 2;

    private static final String TAG = "CameraSettings";

    private final Context mContext;
    private final Parameters mParameters;
    private final CameraInfo[] mCameraInfo;
    private final int mCameraId;

    public CameraSettings(Activity activity, Parameters parameters,
                          int cameraId, CameraInfo[] cameraInfo) {
        mContext = activity;
        mParameters = parameters;
        mCameraId = cameraId;
        mCameraInfo = cameraInfo;
    }

    public PreferenceGroup getPreferenceGroup(int preferenceRes) {
        PreferenceInflater inflater = new PreferenceInflater(mContext);
        PreferenceGroup group =
                (PreferenceGroup) inflater.inflate(preferenceRes);
        if (mParameters != null) initPreference(group);
        return group;
    }

    @TargetApi(ApiHelper.VERSION_CODES.HONEYCOMB)
    public static String getDefaultVideoQuality(int cameraId,
            String defaultQuality) {
        if (ApiHelper.HAS_FINE_RESOLUTION_QUALITY_LEVELS) {
            if (CamcorderProfile.hasProfile(
                    cameraId, Integer.valueOf(defaultQuality))) {
                return defaultQuality;
            }
        }
        return Integer.toString(CamcorderProfile.QUALITY_HIGH);
    }

    public static void initialCameraPictureSize(
            Context context, Parameters parameters) {
        // When launching the camera app first time, we will set the picture
        // size to the first one in the list defined in "arrays.xml" and is also
        // supported by the driver.
        List<Size> supported = parameters.getSupportedPictureSizes();
        if (supported == null) return;
        for (String candidate : context.getResources().getStringArray(
                R.array.pref_camera_picturesize_entryvalues)) {
            if (setCameraPictureSize(candidate, supported, parameters)) {
                SharedPreferences.Editor editor = ComboPreferences
                        .get(context).edit();
                editor.putString(KEY_PICTURE_SIZE, candidate);
                editor.apply();
                return;
            }
        }
        Log.e(TAG, "No supported picture size found");
    }

    public static void removePreferenceFromScreen(
            PreferenceGroup group, String key) {
        removePreference(group, key);
    }

    public static boolean setCameraPictureSize(
            String candidate, List<Size> supported, Parameters parameters) {
        int index = candidate.indexOf('x');
        if (index == NOT_FOUND) return false;
        int width = Integer.parseInt(candidate.substring(0, index));
        int height = Integer.parseInt(candidate.substring(index + 1));
        for (Size size : supported) {
            if (size.width == width && size.height == height) {
                parameters.setPictureSize(width, height);
                return true;
            }
        }
        return false;
    }

    public static int getMaxVideoDuration(Context context) {
        int duration = 0;  // in milliseconds, 0 means unlimited.
        try {
            duration = context.getResources().getInteger(R.integer.max_video_recording_length);
        } catch (Resources.NotFoundException ex) {
        }
        return duration;
    }

    private void initPreference(PreferenceGroup group) {
        ListPreference videoQuality = group.findPreference(KEY_VIDEO_QUALITY);
        ListPreference timeLapseInterval = group.findPreference(KEY_VIDEO_TIME_LAPSE_FRAME_INTERVAL);
        ListPreference pictureSize = group.findPreference(KEY_PICTURE_SIZE);
        ListPreference whiteBalance =  group.findPreference(KEY_WHITE_BALANCE);
        ListPreference sceneMode = group.findPreference(KEY_SCENE_MODE);
        ListPreference flashMode = group.findPreference(KEY_FLASH_MODE);
        ListPreference focusMode = group.findPreference(KEY_FOCUS_MODE);
        IconListPreference exposure =
                (IconListPreference) group.findPreference(KEY_EXPOSURE);
        IconListPreference cameraIdPref =
                (IconListPreference) group.findPreference(KEY_CAMERA_ID);
        ListPreference videoFlashMode =
                group.findPreference(KEY_VIDEOCAMERA_FLASH_MODE);
        ListPreference videoEffect = group.findPreference(KEY_VIDEO_EFFECT);
        ListPreference cameraHdr = group.findPreference(KEY_CAMERA_HDR);
        ListPreference isoMode = group.findPreference(KEY_ISO_MODE);
        ListPreference colorEffect = group.findPreference(KEY_COLOR_EFFECT);
        ListPreference videoColorEffect = group.findPreference(KEY_VIDEOCAMERA_COLOR_EFFECT);
        ListPreference storage = group.findPreference(KEY_STORAGE);
        IconListPreference videoExposure =
                (IconListPreference) group.findPreference(KEY_VIDEOCAMERA_EXPOSURE);
        ListPreference saturation = group.findPreference(KEY_SATURATION);
        ListPreference videoSaturation = group.findPreference(KEY_VIDEOCAMERA_SATURATION);  
        ListPreference sharpness = group.findPreference(KEY_SHARPNESS);
        ListPreference videoSharpness = group.findPreference(KEY_VIDEOCAMERA_SHARPNESS);  
        ListPreference contrast = group.findPreference(KEY_CONTRAST);
        ListPreference videoContrast = group.findPreference(KEY_VIDEOCAMERA_CONTRAST);  
        ListPreference voiceShutter = group.findPreference(KEY_VOICE_SHUTTER);

        // Since the screen could be loaded from different resources, we need
        // to check if the preference is available here
        if (videoQuality != null) {
            filterUnsupportedOptions(group, videoQuality, getSupportedVideoQuality());
        }

        if (pictureSize != null) {
            filterUnsupportedOptions(group, pictureSize, sizeListToStringList(
                    mParameters.getSupportedPictureSizes()));
            filterSimilarPictureSize(group, pictureSize);
        }
        if (whiteBalance != null) {
            filterUnsupportedOptions(group,
                    whiteBalance, mParameters.getSupportedWhiteBalance());
        }
        if (sceneMode != null) {
            filterUnsupportedOptions(group,
                    sceneMode, mParameters.getSupportedSceneModes());
        }
        if (flashMode != null) {
            filterUnsupportedOptions(group,
                    flashMode, mParameters.getSupportedFlashModes());
        }
        if (focusMode != null) {
            filterUnsupportedOptions(group,
                    focusMode, mParameters.getSupportedFocusModes());
            if (!mContext.getResources().getBoolean(R.bool.wantsFocusModes)) {
                // Remove the focus mode if we can use tap-to-focus.
                removePreference(group, focusMode.getKey());
            }
        }
        if (videoFlashMode != null) {
            filterUnsupportedOptions(group,
                    videoFlashMode, mParameters.getSupportedFlashModes());
        }
        if (exposure != null) buildExposureCompensation(group, exposure);
        if (videoExposure != null) buildExposureCompensation(group, videoExposure);
        if (cameraIdPref != null) buildCameraId(group, cameraIdPref);

        if (timeLapseInterval != null) {
            if (ApiHelper.HAS_TIME_LAPSE_RECORDING) {
                resetIfInvalid(timeLapseInterval);
            } else {
                removePreference(group, timeLapseInterval.getKey());
            }
        }
        if (videoEffect != null) {
            if (ApiHelper.HAS_EFFECTS_RECORDING) {
                initVideoEffect(group, videoEffect);
                resetIfInvalid(videoEffect);
            } else {
                filterUnsupportedOptions(group, videoEffect, null);
            }
        }
        if (cameraHdr != null && (!ApiHelper.HAS_CAMERA_HDR
                    || !Util.isCameraHdrSupported(mParameters))) {
            removePreference(group, cameraHdr.getKey());
        }
        if (isoMode != null) {
            filterUnsupportedOptions(group,
                    isoMode, mParameters.getSupportedIsoValues());
        }
        if (colorEffect != null) {
            filterUnsupportedOptions(group,
                    colorEffect, mParameters.getSupportedColorEffects());
        }
        if (videoColorEffect != null) {
            filterUnsupportedOptions(group,
                    videoColorEffect, mParameters.getSupportedColorEffects());
        }
        if (storage != null) {
            buildStorage(group, storage);
        }
        
        if (saturation!=null){
            if (!Util.hasHTCPictureOptions()){
                removePreference(group, saturation.getKey());
            } else {
                buildSaturation(group, saturation);
            }
        }
        if (videoSaturation!=null){
            if (!Util.hasHTCPictureOptions()){
                removePreference(group, videoSaturation.getKey());
            } else {
                buildSaturation(group, videoSaturation);
            }
        }

        if (sharpness!=null){
            if (!Util.hasHTCPictureOptions()){
                removePreference(group, sharpness.getKey());
            } else {
                buildSharpness(group, sharpness);
            }
        }
        if (videoSharpness!=null){
            if (!Util.hasHTCPictureOptions()){
                removePreference(group, videoSharpness.getKey());
            } else {
                buildSharpness(group, videoSharpness);
            }
        }

        if (contrast!=null){
            if (!Util.hasHTCPictureOptions()){
                removePreference(group, contrast.getKey());
            } else {
                buildContrast(group, contrast);
            }
        }
        if (videoContrast!=null){
            if (!Util.hasHTCPictureOptions()){
                removePreference(group, videoContrast.getKey());
            } else {
                buildContrast(group, videoContrast);
            }
        }
    }

    private void buildStorage(PreferenceGroup group, ListPreference storage) {
        StorageManager sm = (StorageManager) mContext.getSystemService(Context.STORAGE_SERVICE);
        StorageVolume[] volumes = sm.getVolumeList();
        String[] entries = new String[volumes.length];
        String[] entryValues = new String[volumes.length];
        int primary = 0;

        if (volumes.length < 2) {
            // No need for storage setting  264
            removePreference(group, storage.getKey());
            return;
        }

        for (int i = 0; i < volumes.length; i++) {
            StorageVolume v = volumes[i];
            entries[i] = v.getDescription(mContext);
            entryValues[i] = v.getPath();
            if (v.isPrimary()) {
                primary = i;
            }
        }
        storage.setEntries(entries);
        storage.setEntryValues(entryValues);

        // Filter saved invalid value
        if (storage.findIndexOfValue(storage.getValue()) < 0) {
            // Default to the primary storage
            storage.setValueIndex(primary);
        }
    }

    private void buildExposureCompensation(
            PreferenceGroup group, IconListPreference exposure) {
        
        //HOX+ max exposure = 12, min exposure = -12, step = 0.1
        String entries[] = new String[7];
        String entryValues[] = new String[7];
        int[] icons = new int[7];

        entries[6] = "-3";
        entries[5] = "-2";
        entries[4] = "-1";
        entries[3] = "0";
        entries[2] = "+1";
        entries[1] = "+2";
        entries[0] = "+3";

		entryValues[6] = "-12";
		entryValues[5] = "-8";
		entryValues[4] = "-4";
		entryValues[3] = "0";
		entryValues[2] = "4";
		entryValues[1] = "8";
		entryValues[0] = "12";

		TypedArray iconIds = mContext.getResources().obtainTypedArray(R.array.pref_camera_exposure_icons);
		icons[6] = iconIds.getResourceId(0, 0);
		icons[5] = iconIds.getResourceId(1, 0);
       	icons[4] = iconIds.getResourceId(2, 0);
		icons[3] = iconIds.getResourceId(3, 0);
		icons[2] = iconIds.getResourceId(4, 0);
		icons[1] = iconIds.getResourceId(5, 0);
		icons[0] = iconIds.getResourceId(6, 0);

        exposure.setUseSingleIcon(true);
        exposure.setEntries(entries);
        exposure.setEntryValues(entryValues);
        exposure.setLargeIconIds(icons);
    }

    private void buildSaturation(PreferenceGroup group, ListPreference saturation){
        String entries[] = new String[11];
        String entryValues[] = new String[11];

        for(int i=0; i<entries.length; i++){
            entries[i] = Integer.toString(i);
            entryValues[i] = Integer.toString(i);
        }
 
        saturation.setEntries(entries);
        saturation.setEntryValues(entryValues);
    }

    private void buildSharpness(PreferenceGroup group, ListPreference sharpness){
        String entries[] = new String[5];
        String entryValues[] = new String[5];

        int j=-2;
        for(int i=0; i<entries.length; i++){
            entries[i] = Integer.toString(j);
            entryValues[i] = Integer.toString(j);
            j+=1;
        }
 
        sharpness.setEntries(entries);
        sharpness.setEntryValues(entryValues);
    }

    private void buildContrast(PreferenceGroup group, ListPreference contrast){
        String entries[] = new String[11];
        String entryValues[] = new String[11];

        int j=-100;
        for(int i=0; i<entries.length; i++){
            entries[i] = Integer.toString(j);
            entryValues[i] = Integer.toString(j);
            j+=20;
        }
 
        contrast.setEntries(entries);
        contrast.setEntryValues(entryValues);
    }    
    
    private void buildCameraId(
            PreferenceGroup group, IconListPreference preference) {
        int numOfCameras = mCameraInfo.length;
        if (numOfCameras < 2) {
            removePreference(group, preference.getKey());
            return;
        }

        CharSequence[] entryValues = new CharSequence[numOfCameras];
        for (int i = 0; i < numOfCameras; ++i) {
            entryValues[i] = "" + i;
        }
        preference.setEntryValues(entryValues);
    }

    private static boolean removePreference(PreferenceGroup group, String key) {
        for (int i = 0, n = group.size(); i < n; i++) {
            CameraPreference child = group.get(i);
            if (child instanceof PreferenceGroup) {
                if (removePreference((PreferenceGroup) child, key)) {
                    return true;
                }
            }
            if (child instanceof ListPreference &&
                    ((ListPreference) child).getKey().equals(key)) {
                group.removePreference(i);
                return true;
            }
        }
        return false;
    }

    private void filterUnsupportedOptions(PreferenceGroup group,
            ListPreference pref, List<String> supported) {

        // Remove the preference if the parameter is not supported or there is
        // only one options for the settings.
        if (supported == null || supported.size() <= 1) {
            removePreference(group, pref.getKey());
            return;
        }

        pref.filterUnsupported(supported);
        if (pref.getEntries().length <= 1) {
            removePreference(group, pref.getKey());
            return;
        }

        resetIfInvalid(pref);
    }

    private void filterSimilarPictureSize(PreferenceGroup group,
            ListPreference pref) {
        pref.filterDuplicated();
        if (pref.getEntries().length <= 1) {
            removePreference(group, pref.getKey());
            return;
        }
        resetIfInvalid(pref);
    }

    private void resetIfInvalid(ListPreference pref) {
        // Set the value to the first entry if it is invalid.
        String value = pref.getValue();
        if (pref.findIndexOfValue(value) == NOT_FOUND) {
            pref.setValueIndex(0);
        }
    }

    private static List<String> sizeListToStringList(List<Size> sizes) {
        ArrayList<String> list = new ArrayList<String>();
        for (Size size : sizes) {
            list.add(String.format(Locale.ENGLISH, "%dx%d", size.width, size.height));
        }
        return list;
    }

    public static void upgradeLocalPreferences(SharedPreferences pref) {
        int version;
        try {
            version = pref.getInt(KEY_LOCAL_VERSION, 0);
        } catch (Exception ex) {
            version = 0;
        }
        if (version == CURRENT_LOCAL_VERSION) return;

        SharedPreferences.Editor editor = pref.edit();
        if (version == 1) {
            // We use numbers to represent the quality now. The quality definition is identical to
            // that of CamcorderProfile.java.
            editor.remove("pref_video_quality_key");
        }
        editor.putInt(KEY_LOCAL_VERSION, CURRENT_LOCAL_VERSION);
        editor.apply();
    }

    public static void upgradeGlobalPreferences(SharedPreferences pref) {
        upgradeOldVersion(pref);
        upgradeCameraId(pref);
    }

    private static void upgradeOldVersion(SharedPreferences pref) {
        int version;
        try {
            version = pref.getInt(KEY_VERSION, 0);
        } catch (Exception ex) {
            version = 0;
        }
        if (version == CURRENT_VERSION) return;

        SharedPreferences.Editor editor = pref.edit();
        if (version == 0) {
            // We won't use the preference which change in version 1.
            // So, just upgrade to version 1 directly
            version = 1;
        }
        if (version == 1) {
            // Change jpeg quality {65,75,85} to {normal,fine,superfine}
            String quality = pref.getString(KEY_JPEG_QUALITY, "85");
            if (quality.equals("65")) {
                quality = "normal";
            } else if (quality.equals("75")) {
                quality = "fine";
            } else {
                quality = "superfine";
            }
            editor.putString(KEY_JPEG_QUALITY, quality);
            version = 2;
        }
        if (version == 2) {
            editor.putString(KEY_RECORD_LOCATION,
                    pref.getBoolean(KEY_RECORD_LOCATION, false)
                    ? CameraSettings.VALUE_ON
                    : RecordLocationPreference.VALUE_NONE);
            version = 3;
        }
        if (version == 3) {
            // Just use video quality to replace it and
            // ignore the current settings.
            editor.remove("pref_camera_videoquality_key");
            editor.remove("pref_camera_video_duration_key");
        }

        editor.putInt(KEY_VERSION, CURRENT_VERSION);
        editor.apply();
    }

    private static void upgradeCameraId(SharedPreferences pref) {
        // The id stored in the preference may be out of range if we are running
        // inside the emulator and a webcam is removed.
        // Note: This method accesses the global preferences directly, not the
        // combo preferences.
        int cameraId = readPreferredCameraId(pref);
        if (cameraId == 0) return;  // fast path

        int n = CameraHolder.instance().getNumberOfCameras();
        if (cameraId < 0 || cameraId >= n) {
            writePreferredCameraId(pref, 0);
        }
    }

    public static int readPreferredCameraId(SharedPreferences pref) {
        return Integer.parseInt(pref.getString(KEY_CAMERA_ID, "0"));
    }

    public static void writePreferredCameraId(SharedPreferences pref,
            int cameraId) {
        Editor editor = pref.edit();
        editor.putString(KEY_CAMERA_ID, Integer.toString(cameraId));
        editor.apply();
    }

    public static int readExposure(ComboPreferences preferences, String key) {
        String exposure = preferences.getString(
                key,
                EXPOSURE_DEFAULT_VALUE);
        try {
            return Integer.parseInt(exposure);
        } catch (Exception ex) {
            Log.e(TAG, "Invalid exposure: " + exposure);
        }
        return 0;
    }

    public static int readEffectType(SharedPreferences pref) {
        String effectSelection = pref.getString(KEY_VIDEO_EFFECT, "none");
        if (effectSelection.equals("none")) {
            return EffectsRecorder.EFFECT_NONE;
        } else if (effectSelection.startsWith("goofy_face")) {
            return EffectsRecorder.EFFECT_GOOFY_FACE;
        } else if (effectSelection.startsWith("backdropper")) {
            return EffectsRecorder.EFFECT_BACKDROPPER;
        }
        Log.e(TAG, "Invalid effect selection: " + effectSelection);
        return EffectsRecorder.EFFECT_NONE;
    }

    public static Object readEffectParameter(SharedPreferences pref) {
        String effectSelection = pref.getString(KEY_VIDEO_EFFECT, "none");
        if (effectSelection.equals("none")) {
            return null;
        }
        int separatorIndex = effectSelection.indexOf('/');
        String effectParameter =
                effectSelection.substring(separatorIndex + 1);
        if (effectSelection.startsWith("goofy_face")) {
            if (effectParameter.equals("squeeze")) {
                return EffectsRecorder.EFFECT_GF_SQUEEZE;
            } else if (effectParameter.equals("big_eyes")) {
                return EffectsRecorder.EFFECT_GF_BIG_EYES;
            } else if (effectParameter.equals("big_mouth")) {
                return EffectsRecorder.EFFECT_GF_BIG_MOUTH;
            } else if (effectParameter.equals("small_mouth")) {
                return EffectsRecorder.EFFECT_GF_SMALL_MOUTH;
            } else if (effectParameter.equals("big_nose")) {
                return EffectsRecorder.EFFECT_GF_BIG_NOSE;
            } else if (effectParameter.equals("small_eyes")) {
                return EffectsRecorder.EFFECT_GF_SMALL_EYES;
            }
        } else if (effectSelection.startsWith("backdropper")) {
            // Parameter is a string that either encodes the URI to use,
            // or specifies 'gallery'.
            return effectParameter;
        }

        Log.e(TAG, "Invalid effect selection: " + effectSelection);
        return null;
    }

    public static void restorePreferences(Context context,
            ComboPreferences preferences, Parameters parameters) {
        int currentCameraId = readPreferredCameraId(preferences);

        // Clear the preferences of both cameras.
        int backCameraId = CameraHolder.instance().getBackCameraId();
        if (backCameraId != -1) {
            preferences.setLocalId(context, backCameraId);
            Editor editor = preferences.edit();
            editor.clear();
            editor.apply();
        }
        int frontCameraId = CameraHolder.instance().getFrontCameraId();
        if (frontCameraId != -1) {
            preferences.setLocalId(context, frontCameraId);
            Editor editor = preferences.edit();
            editor.clear();
            editor.apply();
        }

        // Switch back to the preferences of the current camera. Otherwise,
        // we may write the preference to wrong camera later.
        preferences.setLocalId(context, currentCameraId);

        upgradeGlobalPreferences(preferences.getGlobal());
        upgradeLocalPreferences(preferences.getLocal());

        // Write back the current camera id because parameters are related to
        // the camera. Otherwise, we may switch to the front camera but the
        // initial picture size is that of the back camera.
        initialCameraPictureSize(context, parameters);
        writePreferredCameraId(preferences, currentCameraId);
    }

    private ArrayList<String> getSupportedVideoQuality() {
        ArrayList<String> supported = new ArrayList<String>();
        // Check for supported quality
        if (ApiHelper.HAS_FINE_RESOLUTION_QUALITY_LEVELS) {
            getFineResolutionQuality(supported);
        } else {
            supported.add(Integer.toString(CamcorderProfile.QUALITY_HIGH));
            CamcorderProfile high = CamcorderProfile.get(
                    mCameraId, CamcorderProfile.QUALITY_HIGH);
            CamcorderProfile low = CamcorderProfile.get(
                    mCameraId, CamcorderProfile.QUALITY_LOW);
            if (high.videoFrameHeight * high.videoFrameWidth >
                    low.videoFrameHeight * low.videoFrameWidth) {
                supported.add(Integer.toString(CamcorderProfile.QUALITY_LOW));
            }
        }

        return supported;
    }

    @TargetApi(ApiHelper.VERSION_CODES.HONEYCOMB)
    private void getFineResolutionQuality(ArrayList<String> supported) {
        if (CamcorderProfile.hasProfile(mCameraId, CamcorderProfile.QUALITY_1080P)) {
            supported.add(Integer.toString(CamcorderProfile.QUALITY_1080P));
        }
        if (CamcorderProfile.hasProfile(mCameraId, CamcorderProfile.QUALITY_720P)) {
            supported.add(Integer.toString(CamcorderProfile.QUALITY_720P));
        }
        if (CamcorderProfile.hasProfile(mCameraId, CamcorderProfile.QUALITY_480P)) {
            supported.add(Integer.toString(CamcorderProfile.QUALITY_480P));
        }
    }

    /**
     * Enable video mode for certain cameras.
     *
     * @param params
     * @param on
     */
    public static void setVideoMode(Parameters params, boolean on) {
        if (Util.useSamsungCamMode()) {
            params.set("cam_mode", on ? "1" : "0");
        }
        if (Util.useHTCCamMode()) {
            params.set("cam-mode", on ? "1" : "0");
            // capture-mode-supported=normal,contiburst,zsl,hdr,panorama,groupportrait
            params.set("capture-mode", "normal");
        }
    }

    /**
     * Set video size for certain cameras.
     *
     * @param params
     * @param profile
     */
    public static void setEarlyVideoSize(Parameters params, CamcorderProfile profile) {
        if (Util.needsEarlyVideoSize()) {
            params.set("video-size", profile.videoFrameWidth + "x" + profile.videoFrameHeight);
        }
    }

    private void initVideoEffect(PreferenceGroup group, ListPreference videoEffect) {
        CharSequence[] values = videoEffect.getEntryValues();

        boolean goofyFaceSupported =
                EffectsRecorder.isEffectSupported(EffectsRecorder.EFFECT_GOOFY_FACE);
        boolean backdropperSupported =
                EffectsRecorder.isEffectSupported(EffectsRecorder.EFFECT_BACKDROPPER) &&
                Util.isAutoExposureLockSupported(mParameters) &&
                Util.isAutoWhiteBalanceLockSupported(mParameters);

        ArrayList<String> supported = new ArrayList<String>();
        for (CharSequence value : values) {
            String effectSelection = value.toString();
            if (!goofyFaceSupported && effectSelection.startsWith("goofy_face")) continue;
            if (!backdropperSupported && effectSelection.startsWith("backdropper")) continue;
            supported.add(effectSelection);
        }

        filterUnsupportedOptions(group, videoEffect, supported);
    }

    public static void dumpParameters(Parameters params) {
        Set<String> sortedParams = new TreeSet<String>();
        sortedParams.addAll(Arrays.asList(params.flatten().split(";")));
        Log.d(TAG, "Parameters: " + sortedParams.toString());
    }
}
