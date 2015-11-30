package org.deviceconnect.android.deviceplugin.theta.core;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.deviceconnect.android.deviceplugin.theta.core.sensor.DefaultHeadTracker;
import org.deviceconnect.android.deviceplugin.theta.core.sensor.HeadTracker;
import org.deviceconnect.android.deviceplugin.theta.core.sensor.HeadTrackingListener;
import org.deviceconnect.android.deviceplugin.theta.utils.BitmapUtils;
import org.deviceconnect.android.deviceplugin.theta.utils.Quaternion;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Spherical View API.
 *
 * <h2>Usage</h2>
 * <code>
 *     // Obtain Spherical View API.
 *     ThetaDeviceApplication app = (ThetaDeviceApplication) getApplication();
 *     SphericalViewApi api = app.getSphericalViewApi();
 *
 *     // Start Image View.
 *     api.startImageView(picture, param, renderer);
 *
 *     // Change Image View Settings.
 *     api.updateImageView(newParam);
 *
 *     // Stop Image View.
 *     api.stopImageView();
 * </code>
 */
public class SphericalViewApi implements HeadTrackingListener {

    private State mState;

    private SphericalViewParam mParam;

    private SphericalViewRenderer mRenderer;

    private final HeadTracker mHeadTracker;

    private Bitmap mTexture;

    private LivePreviewTask mLivePreviewTask;

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    public SphericalViewApi(final Context context) {
        mHeadTracker = new DefaultHeadTracker(context);
    }

    @Override
    public void onHeadRotated(final Quaternion rotation) {
        synchronized (this) {
            if (isRunning()) {
                SphericalViewRenderer.Camera currentCamera = mRenderer.getCamera();
                SphericalViewRenderer.CameraBuilder newCamera = new SphericalViewRenderer.CameraBuilder(currentCamera);
                newCamera.rotate(rotation);
                mRenderer.setCamera(newCamera.create());
            }
        }
    }

    public synchronized void startLiveView(final LiveCamera camera,
                                           final SphericalViewParam param,
                                           final SphericalViewRenderer renderer) {
        if (isRunning()) {
            throw new IllegalStateException("SphericalViewApi is already running.");
        }

        mParam = param;
        if (param.isVRMode()) {
            mHeadTracker.registerTrackingListener(this);
            mHeadTracker.start();
        }

        mRenderer = renderer;
        mRenderer.setStereoMode(param.isStereo());

        mLivePreviewTask = new LivePreviewTask(camera);
        mExecutor.execute(mLivePreviewTask);

        mState = State.RUNNING;
    }

    public synchronized void startImageView(final byte[] picture,
                                            final SphericalViewParam param,
                                            final SphericalViewRenderer renderer) {
        if (isRunning()) {
            throw new IllegalStateException("SphericalViewApi is already running.");
        }

        mParam = param;
        mRenderer = renderer;

        Bitmap texture = BitmapFactory.decodeByteArray(picture, 0, picture.length);
        mTexture = BitmapUtils.resize(texture, 2048, 1024);
        renderer.setTexture(mTexture);

        if (param.isVRMode()) {
            mHeadTracker.registerTrackingListener(this);
            mHeadTracker.start();
        }

        mRenderer.setStereoMode(param.isStereo());

        mState = State.RUNNING;
    }

    public synchronized void updateImageView(final SphericalViewParam param) {
        if (!isRunning()) {
            throw new IllegalStateException("SphericalViewApi is not running.");
        }

        mParam = param;

        if (!mParam.isVRMode() && param.isVRMode()) {
            mHeadTracker.start();
        } else if (mParam.isVRMode() && !param.isVRMode()) {
            mHeadTracker.stop();
            mHeadTracker.unregisterTrackingListener(this);
        }

        SphericalViewRenderer.CameraBuilder camera
            = new SphericalViewRenderer.CameraBuilder(mRenderer.getCamera());
        camera.setFov((float) param.getFOV());
        // TODO Enable to change other parameters.
        mRenderer.setCamera(camera.create());
        mRenderer.setStereoMode(param.isStereo());
    }

    public void resetCameraDirection() {
        mHeadTracker.reset();
    }

    public synchronized void stop() {
        if (isState(State.STOPPED)) {
            throw new IllegalStateException("SphericalViewApi has already stopped.");
        }

        if (mTexture != null) {
            mTexture.recycle();
        }
        if (mLivePreviewTask != null) {
            mLivePreviewTask.stop();
            mLivePreviewTask = null;
        }

        mHeadTracker.stop();
        mHeadTracker.unregisterTrackingListener(this);

        mState = State.STOPPED;
    }

    public synchronized void pause() {
        if (isRunning()) {
            mState = State.PAUSED;
            mHeadTracker.stop();
        }
    }

    public synchronized void resume() {
        if (isPaused()) {
            mState = State.RUNNING;
            mHeadTracker.start();
        }
    }

    public boolean isRunning() {
        return isState(State.RUNNING);
    }

    public boolean isPaused() {
        return isState(State.PAUSED);
    }

    private boolean isState(State state) {
        return mState == state;
    }

    private enum State {

        STOPPED,

        RUNNING,

        PAUSED

    }

    private class LivePreviewTask implements Runnable {

        private boolean mIsStarted;

        private final LiveCamera mLiveCamera;

        public LivePreviewTask(final LiveCamera liveCamera) {
            mLiveCamera = liveCamera;
        }

        public void stop() {
            if (!mIsStarted) {
                return;
            }
            mIsStarted = false;
        }

        @Override
        public void run() {
            mIsStarted = true;
            InputStream is = null;
            MotionJpegInputStream mjpeg = null;
            try {
                is = mLiveCamera.getLiveStream();
                mjpeg = new MotionJpegInputStream(is);
                byte[] frame;

                while (mIsStarted && (frame = mjpeg.readFrame()) != null) {
                    Bitmap texture = BitmapFactory.decodeByteArray(frame, 0, frame.length);
                    // Fix texture size to power of two.
                    texture = BitmapUtils.resize(texture, 512, 256);
                    mRenderer.setTexture(texture);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mIsStarted = false;
                try {
                    if (is != null) {
                        is.close();
                    }
                    if (mjpeg != null) {
                        mjpeg.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

}
