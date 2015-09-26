/*
 *   Copyright (c) 2009 - 2015 Carmen Alvarez
 *
 *   This file is part of Nounours for Android.
 *
 *   Nounours for Android is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Nounours for Android is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with Nounours for Android.  If not, see <http://www.gnu.org/licenses/>.
 */

package ca.rmen.nounours.nounours;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Handler;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import ca.rmen.nounours.Constants;
import ca.rmen.nounours.Nounours;
import ca.rmen.nounours.NounoursAnimationHandler;
import ca.rmen.nounours.NounoursSoundHandler;
import ca.rmen.nounours.NounoursVibrateHandler;
import ca.rmen.nounours.R;
import ca.rmen.nounours.compat.DisplayCompat;
import ca.rmen.nounours.compat.EnvironmentCompat;
import ca.rmen.nounours.data.Image;
import ca.rmen.nounours.data.Theme;
import ca.rmen.nounours.settings.NounoursSettings;
import ca.rmen.nounours.util.FileUtil;
import ca.rmen.nounours.util.ThemeUtil;

/**
 * Implementation of the abstract Nounours class, containing logic specific to
 * Android.
 *
 * @author Carmen Alvarez
 */
public class AndroidNounours extends Nounours {

    private static final String TAG = Constants.TAG + AndroidNounours.class.getSimpleName();

    private final Context mContext;
    private final Handler mHandler;
    private final AnimationHandler mAnimationHandler;
    private final ImageView mImageView;
    private final ImageCache mImageCache;
    private final AnimationCache mAnimationCache;

    private ProgressDialog mProgressDialog;
    private AlertDialog mAlertDialog;


    /**
     * Open the CSV data files and call the superclass
     * {@link Nounours#init(NounoursAnimationHandler, NounoursSoundHandler, NounoursVibrateHandler, InputStream, InputStream, InputStream, InputStream, InputStream, InputStream, InputStream, InputStream, InputStream, InputStream, String)}
     * method.
     *
     * @param context The android mContext.
     */
    public AndroidNounours(final Context context, Handler handler, ImageView imageView) {

        mContext = context;
        mHandler = handler;
        mImageView = imageView;
        mImageCache = new ImageCache(context, mImageCacheListener);
        mAnimationCache = new AnimationCache(context, this, mImageCache);

        String themeId = NounoursSettings.getThemeId(context);
        if (!FileUtil.isSdPresent())
            themeId = Nounours.DEFAULT_THEME_ID;
        mAnimationHandler = new AnimationHandler(this, imageView, mAnimationCache);
        SoundHandler soundHandler = new SoundHandler(this, context);
        VibrateHandler vibrateHandler = new VibrateHandler(context);
        Resources resources = context.getResources();
        final InputStream propertiesFile = resources.openRawResource(R.raw.nounours);
        final InputStream themePropertiesFile = resources.openRawResource(R.raw.nounoursdeftheme);
        final InputStream imageFile = resources.openRawResource(R.raw.image);
        final InputStream imageSetFile = resources.openRawResource(R.raw.imageset);
        final InputStream featureFile = resources.openRawResource(R.raw.feature);
        final InputStream imageFeatureFile = resources.openRawResource(R.raw.imagefeatureassoc);
        final InputStream adjacentImageFile = resources.openRawResource(R.raw.adjacentimage);
        final InputStream animationFile = resources.openRawResource(R.raw.animation);
        final InputStream flingAnimationFile = resources.openRawResource(R.raw.flinganimation);
        final InputStream soundFile = resources.openRawResource(R.raw.sound);

        try {
            init(mAnimationHandler, soundHandler, vibrateHandler, propertiesFile, themePropertiesFile, imageFile,
                    imageSetFile, featureFile, imageFeatureFile, adjacentImageFile, animationFile, flingAnimationFile,
                    soundFile, themeId);
            setEnableVibrate(NounoursSettings.isSoundEnabled(context));
            setEnableSound(NounoursSettings.isSoundEnabled(context));
            setEnableRandomAnimations(NounoursSettings.isRandomAnimationEnabled(context));
            setIdleTimeout(NounoursSettings.getIdleTimeout(context));
        } catch (final IOException e) {
            Log.d(getClass().getName(), "Error initializing nounours", e); //$NON-NLS-1$
        }
    }

    @Override
    protected boolean cacheImages() {
        if(!mImageCache.cacheImages(getImages().values())) return false;
        // Cache animations.
        return mAnimationCache.cacheAnimations();
    }

    /**
     * Something went wrong when trying to load a theme.  Reset to the default one.
     */
    private void resetToDefaultTheme() {
        Log.v(TAG, "resetToDefaultTheme");
        OnClickListener revertToDefaultTheme = new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                NounoursSettings.setThemeId(mContext, DEFAULT_THEME_ID);
                useTheme(Nounours.DEFAULT_THEME_ID);
            }
        };
        CharSequence message = mContext.getText(R.string.themeLoadError);

        showAlertDialog(message, revertToDefaultTheme);
    }

    /**
     * Load the new image set in a separate thread, showing the progress bar
     */
    @Override
    public boolean useTheme(final String id) {
        if (!Nounours.DEFAULT_THEME_ID.equals(id)) {
            File themeDir = new File(getAppDir(), id);
            if (!themeDir.exists()) {
                boolean mkdirsResult = themeDir.mkdirs();
                if(!themeDir.isDirectory()) {
                    Log.v(TAG, "Could not create theme folder " + themeDir + ". mkdirs returned " + mkdirsResult);
                    resetToDefaultTheme();
                    return false;
                }
            }
        }
        int taskSize = 1;
        Theme theme = getThemes().get(id);
        if (!ThemeUtil.isValid(theme)) theme = getCurrentTheme();
        if (!ThemeUtil.isValid(theme)) theme = getDefaultTheme();
        if (!theme.getSounds().isEmpty())
            taskSize = theme.getImages().size() * 2 + theme.getSounds().size();

        // MEMORY
        mImageCache.clearImageCache();
        mAnimationCache.clearAnimationCache();
        Runnable imageCacher = new Runnable() {
            @SuppressWarnings("synthetic-access")
            @Override
            public void run() {

                boolean loadedTheme = AndroidNounours.super.useTheme(id);
                if (!loadedTheme) {
                    if (!Nounours.DEFAULT_THEME_ID.equals(id)) {
                        debug("Could not load theme " + id + ":  load default theme instead");
                        resetToDefaultTheme();
                    }
                }

                runTask(new Runnable() {
                    public void run() {
                        resizeView();
                    }
                });

            }
        };
        runTaskWithProgressBar(imageCacher, mContext.getString(R.string.predownload, ThemeUtil.getThemeLabel(mContext, theme)),
                taskSize);
        return true;

    }

    private void resizeView() {
        Theme theme = getCurrentTheme();
        if (theme == null)
            return;
        ViewGroup.LayoutParams layoutParams = mImageView.getLayoutParams();

        float widthRatio = (float) DisplayCompat.getWidth(mContext) / theme.getWidth();
        float heightRatio = (float) DisplayCompat.getHeight(mContext) / theme.getHeight();
        Log.v(TAG, widthRatio + ": " + heightRatio);
        float ratioToUse = widthRatio > heightRatio ? heightRatio : widthRatio;

        layoutParams.height = (int) (ratioToUse * theme.getHeight());
        layoutParams.width = (int) (ratioToUse * theme.getWidth());
        Log.v(TAG, "Scaling view to " + layoutParams.width + "x" + layoutParams.height);
        mImageView.setLayoutParams(layoutParams);

    }

    /**
     * Update the progress bar with the download status.
     */
    @Override
    protected void updateDownloadProgress(int progress, int max) {
        CharSequence themeLabel = ThemeUtil.getThemeLabel(mContext, getCurrentTheme());
        updateProgressBar(progress, 2 * max, mContext.getString(R.string.downloading, themeLabel));
    }

    @Override
    protected void updatePreloadProgress(int progress, int max) {
        CharSequence themeLabel = ThemeUtil.getThemeLabel(mContext, getCurrentTheme());
        updateProgressBar(progress, 2 * max, mContext.getString(R.string.predownload, themeLabel));
    }

    /**
     * Display a picture on the screen.
     *
     * @see ca.rmen.nounours.Nounours#displayImage(ca.rmen.nounours.data.Image)
     */
    @Override
    protected void displayImage(final Image image) {
        if (image == null) {
            return;
        }
        final Bitmap bitmap = mImageCache.getDrawableImage(image);
        if (bitmap == null)
            return;
        mImageView.setImageBitmap(bitmap);
    }

    /**
     * Trace.
     */
    @Override
    protected void debug(final Object o) {
        if(o instanceof Throwable) {
            Throwable t = (Throwable) o;
            Log.w(TAG, t.getMessage(), t);
        } else {
            Log.v(TAG, "" + o);
        }
    }

    /**
     * UI threads should be run with an Android thread call.
     *
     * @see ca.rmen.nounours.Nounours#runTask(java.lang.Runnable)
     */
    @Override
    protected void runTask(final Runnable task) {
        mHandler.post(task);
    }

    /**
     * Run a task, showing the progress bar while the task runs.
     */
    private void runTaskWithProgressBar(final Runnable task, String message, int max) {
        if (mProgressDialog != null)
            mProgressDialog.dismiss();
        createProgressDialog(max, message);
        Runnable runnable = new Runnable() {

            @Override
            public void run() {
                task.run();
                mProgressDialog.dismiss();
            }
        };
        new Thread(runnable).start();
    }

    /**
     * Update the currently showing progress bar.
     */
    private void updateProgressBar(final int progress, final int max, final String message) {
        Runnable runnable = new Runnable() {

            @Override
            public void run() {
                // show the progress bar if it is not already showing.
                if (mProgressDialog == null || !mProgressDialog.isShowing())
                    createProgressDialog(max, message);
                // Update the progress
                mProgressDialog.setProgress(progress);
                mProgressDialog.setMax(max);
                mProgressDialog.setMessage(message);
                debug("updateProgressBar " + progress + "/" + max + ": " + message);

            }
        };
        runTask(runnable);
    }

    /**
     * Create a determinate progress dialog with the given size and text.
     */
    private void createProgressDialog(int max, String message) {
        mProgressDialog = new ProgressDialog(mContext);
        mProgressDialog.setTitle("");
        mProgressDialog.setMessage(message);
        mProgressDialog.setIndeterminate(max < 0);
        mProgressDialog.setMax(max);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setProgress(0);
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();
        debug("createProgressDialog " + max + ": " + message);
    }

    /**
     * Cleanup.
     */
    public void onDestroy() {
        debug("destroy");
        mImageCache.clearImageCache();
        mAnimationCache.clearAnimationCache();
    }

    @Override
    protected int getDeviceHeight() {
        return mImageView.getHeight();
    }

    @Override
    protected int getDeviceWidth() {
        return mImageView.getWidth();
    }

    @Override
    protected boolean isThemeUpToDate(Theme theme) {
        return true;
    }

    @Override
    protected void setIsThemeUpToDate(Theme theme) {
    }

    private void showAlertDialog(final CharSequence message, final OnClickListener callback) {
        Runnable showAlert = new Runnable() {
            public void run() {
                if (mAlertDialog == null) {
                    AlertDialog.Builder alertBuilder = new AlertDialog.Builder(mContext);

                    alertBuilder.setMessage(mContext.getText(R.string.themeLoadError));
                    alertBuilder.setPositiveButton(mContext.getText(android.R.string.ok), callback);

                    mAlertDialog = alertBuilder.create();

                }
                mAlertDialog.setMessage(message);
                mAlertDialog.show();
            }
        };
        runTask(showAlert);
    }

    @Override
    public File getAppDir() {
        return EnvironmentCompat.getExternalFilesDir(mContext);
    }

    @SuppressWarnings("FieldCanBeLocal")
    private final ImageCache.ImageCacheListener mImageCacheListener = new ImageCache.ImageCacheListener() {
        @Override
        public void onImageLoaded(final Image image, int progress, int total) {
            Runnable runnable = new Runnable() {
                public void run() {
                    setImage(image);
                }
            };
            runTask(runnable);
            CharSequence themeName = ThemeUtil.getThemeLabel(mContext, getCurrentTheme());
            updateProgressBar(total + (progress), 2 * total, mContext.getString(R.string.loading, themeName));

        }
    };
}
