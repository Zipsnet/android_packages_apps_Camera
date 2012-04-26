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

import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.os.SystemClock;

import com.android.gallery3d.ui.GLCanvas;
import com.android.gallery3d.ui.Raw2DTexture;

/**
 * Class to handle the capture animation.
 */
public class CaptureAnimManager {

    private static final String TAG = "CaptureAnimManager";
    private static final float ZOOM_DELTA = 0.2f;  // The amount of change for zooming out.
    private static final float ZOOM_IN_BEGIN = 1f - ZOOM_DELTA;  // Pre-calculated value for
                                                                 // convenience.
    private static final float CAPTURE_ANIM_DURATION = 800;  // milliseconds.
    private static final float GAP_RATIO = 0.1f;  // The gap between preview and review based
                                                  // on the view dimension.
    private static final float TOTAL_RATIO = 1f + GAP_RATIO;

    private final Interpolator mZoomOutInterpolator = new DecelerateInterpolator();
    private final Interpolator mZoomInInterpolator = new AccelerateInterpolator();
    private final Interpolator mSlideInterpolator = new AccelerateDecelerateInterpolator();

    private int mAnimOrientation;  // Could be 0, 90, 180 or 270 degrees.
    private long mAnimStartTime;  // milliseconds.
    private float mCenterX;  // The center of the whole view including preview and review.
    private float mCenterY;
    private float mCenterDelta;  // The amount of the center will move after whole animation.
    private float mGap;  // mGap = (width or height) * GAP_RATIO. (depends on orientation)
    private int mDrawWidth;
    private int mDrawHeight;
    private int mDrawX;
    private int mDrawY;
    private float mHalfGap;  // mHalfGap = mGap / 2f.
    private float[] mTextureTransformMatrix;

    /* preview: camera preview view.
     * review: view of picture just taken.
     */
    public CaptureAnimManager() {
    }

    public void setOrientation(int animOrientation) {
        mAnimOrientation = animOrientation;
    }

    // x, y, h and h: the rectangle area where the animation takes place in.
    // transformMatrix: used to show the texture.
    public void startAnimation(int x, int y, int w, int h, float[] transformMatrix) {
        mAnimStartTime = SystemClock.uptimeMillis();
        mTextureTransformMatrix = transformMatrix;
        // Set the views to the initial positions.
        mDrawWidth = w;
        mDrawHeight = h;
        mDrawX = x;
        mDrawY = y;
        switch (mAnimOrientation) {
            case 0:  // Preview is on the left.
                mGap = w * GAP_RATIO;
                mHalfGap = mGap / 2f;
                mCenterX = x - mHalfGap;
                mCenterDelta = w * (TOTAL_RATIO);
                mCenterY = y + h / 2f;
                break;
            case 90:  // Preview is below.
                mGap = h * GAP_RATIO;
                mHalfGap = mGap / 2f;
                mCenterY = y + h + mHalfGap;
                mCenterDelta = -h * (TOTAL_RATIO);
                mCenterX = x + w / 2f;
                break;
            case 180:  // Preview on the right.
                mGap = w * GAP_RATIO;
                mHalfGap = mGap / 2f;
                mCenterX = x + mHalfGap;
                mCenterDelta = -w * (TOTAL_RATIO);
                mCenterY = y + h / 2f;
                break;
            case 270:  // Preview is above.
                mGap = h * GAP_RATIO;
                mHalfGap = mGap / 2f;
                mCenterY = y - mHalfGap;
                mCenterDelta = h * (TOTAL_RATIO);
                mCenterX = x + w / 2f;
                break;
        }
    }

    // Returns true if the animation has been drawn.
    public boolean drawAnimation(GLCanvas canvas, CameraScreenNail preview,
                Raw2DTexture review) {
        long timeDiff = SystemClock.uptimeMillis() - mAnimStartTime;;
        if (timeDiff > CAPTURE_ANIM_DURATION) return false;
        float fraction = timeDiff / CAPTURE_ANIM_DURATION;
        float scale = calculateScale(fraction);
        float centerX = mCenterX;
        float centerY = mCenterY;
        if (mAnimOrientation == 0 || mAnimOrientation == 180) {
            centerX = mCenterX + mCenterDelta * mSlideInterpolator.getInterpolation(fraction);
        } else {
            centerY = mCenterY + mCenterDelta * mSlideInterpolator.getInterpolation(fraction);
        }

        float height = mDrawHeight * scale;
        float width = mDrawWidth * scale;
        int previewX = (int) centerX;
        int previewY = (int) centerY;
        int reviewX = (int) centerX;
        int reviewY = (int) centerY;
        switch (mAnimOrientation) {
            case 0:
                previewX = Math.round(centerX - width - mHalfGap * scale);
                previewY = Math.round(centerY - height / 2f);
                reviewX = Math.round(centerX + mHalfGap * scale);
                reviewY = previewY;
                break;
            case 90:
                previewY = Math.round(centerY + mHalfGap * scale);
                previewX = Math.round(centerX - width / 2f);
                reviewY = Math.round(centerY - height - mHalfGap * scale);
                reviewX = previewX;
                break;
            case 180:
                previewX = Math.round(centerX + width + mHalfGap * scale);
                previewY = Math.round(centerY - height / 2f);
                reviewX = Math.round(centerX - mHalfGap * scale);
                reviewY = previewY;
                break;
            case 270:
                previewY = Math.round(centerY - height - mHalfGap * scale);
                previewX = Math.round(centerX - width / 2f);
                reviewY = Math.round(centerY + mHalfGap * scale);
                reviewX = previewX;
                break;
        }
        float alpha = canvas.getAlpha();
        canvas.setAlpha(fraction);
        preview.directDraw(canvas, previewX, previewY, Math.round(width), Math.round(height));
        canvas.setAlpha(alpha);

        // Flip animation texture vertically. OpenGL uses bottom left point as the origin (0,0).
        canvas.save(GLCanvas.SAVE_FLAG_MATRIX);
        int cx = Math.round(reviewX + width / 2);
        int cy = Math.round(reviewY + height / 2);
        canvas.translate(cx, cy);
        canvas.scale(1, -1, 1);
        canvas.translate(-cx, -cy);
        canvas.drawTexture(review, mTextureTransformMatrix,
                reviewX, reviewY, (int) width, (int) height);
        canvas.restore();

        return true;
    }

    // Calculate the zoom factor based on the given time fraction.
    private float calculateScale(float fraction) {
        float value = 1f;
        if (fraction <= 0.5f) {
            // Zoom in for the beginning.
            value = 1f - ZOOM_DELTA * mZoomOutInterpolator.getInterpolation(
                    fraction * 2);
        } else {
            // Zoom out for the last.
            value = ZOOM_IN_BEGIN + ZOOM_DELTA * mZoomInInterpolator.getInterpolation(
                    (fraction - 0.5f) * 2f);
        }
        return value;
    }
}