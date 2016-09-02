/*
 ChromeCastSettingFragmentPage3.java
 Copyright (c) 2014 NTT DOCOMO,INC.
 Released under the MIT license
 http://opensource.org/licenses/mit-license.php
 */
package org.deviceconnect.android.deviceplugin.chromecast.setting;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.MediaRouteButton;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout.LayoutParams;

import org.deviceconnect.android.deviceplugin.chromecast.BuildConfig;
import org.deviceconnect.android.deviceplugin.chromecast.ChromeCastApplication;
import org.deviceconnect.android.deviceplugin.chromecast.R;
import org.deviceconnect.android.deviceplugin.chromecast.core.ChromeCastDiscovery;

/**
 * チュートリアル画面.
 * <p>
 * 画面を作成する
 * </p>
 * Chromecastの設定
 * 
 * @author NTT DOCOMO, INC.
 */
public class ChromeCastSettingFragmentPage3 extends Fragment {
    /** Chromecastアプリのパッケージ名. */
    private static final String PACKAGE_NAME = "com.google.android.apps.chromecast.app";
    /** バッジの横サイズ. */
    private int mBadgeWidth = 0;
    /** バッジの縦サイズ. */
    private int mBadgeHeight = 0;
    /** ChromeCast接続用Button. */
    private MediaRouteButton mMediaRouteButton;
    /** ChromeCastを管理するApplication. */
    private ChromeCastApplication mApp;

    private int mRouteCount = 0;

    /**
     * Chromecast App (Google) のインストール状態を調べる.
     * 
     * @param   context         コンテキスト
     * @return  インストール状態    （true: インストールされている, false: インストールされていない）
     */
    private boolean isApplicationInstalled(final Context context) {
        boolean installed = false;
        try {
            context.getPackageManager().getPackageInfo(PACKAGE_NAME, PackageManager.GET_META_DATA);
            installed = true;
        } catch (NameNotFoundException e) {
            if (BuildConfig.DEBUG) { 
                e.printStackTrace();
            }
        }
        return installed;
    }

    /**
     * Chromecast App (Google) のインストール状態に応じて、Buttonの背景を変更する.
     * 
     * @param   button  ボタン
     */
    private void changeButtonBackground(final Button button) {
        if (isApplicationInstalled(button.getContext())) {
            button.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            button.setBackgroundResource(R.drawable.button_blue);
            button.setText(getResources().getString(R.string.chromecast_settings_step_3_button));
        } else {
            button.setLayoutParams(new LayoutParams(mBadgeWidth, mBadgeHeight));
            button.setBackgroundResource(R.drawable.button_google_play);
            button.setText("");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        changeButtonBackground((Button) getActivity().findViewById(R.id.buttonChromecastSettingApp));
    }

    @Override
    public View onCreateView(final LayoutInflater inflater,
            final ViewGroup container, final Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.chromecast_settings_step_3, container, false);
        Toolbar toolbar = (Toolbar) rootView.findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.activity_setting_page_title));

        toolbar.setNavigationIcon(R.drawable.ic_close_light);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getActivity().finish();
            }
        });

        Button button = (Button) rootView.findViewById(R.id.buttonChromecastSettingApp);
        button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                Intent intent;
                if (isApplicationInstalled(v.getContext())) {
                    intent = v.getContext().getPackageManager().getLaunchIntentForPackage(PACKAGE_NAME);
                } else {
                    intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + PACKAGE_NAME));
                }
                startActivity(intent);
            }
        });

        Bitmap image = BitmapFactory.decodeResource(getResources(), R.drawable.button_google_play);
        mBadgeWidth = image.getWidth();
        mBadgeHeight = image.getHeight();
        image.recycle();
        mMediaRouteButton = (MediaRouteButton) rootView.findViewById(R.id.media_route_button);
        mApp = (ChromeCastApplication) getActivity().getApplication();
        if (mApp != null) {
            mApp.initialize();
            MediaRouteSelector selector = mApp.getDiscovery().getMediaRouteSelector();
            mMediaRouteButton.setRouteSelector(selector);
        }

        return rootView;
    }
    @Override
    public void onResume() {
        super.onResume();
        if (mApp != null) {
            ChromeCastDiscovery disocovery = mApp.getDiscovery();
            if (disocovery != null) {
                disocovery.registerEvent();
            }
        }
    }

    @Override
    public void onPause() {
        if (mApp != null) {
            ChromeCastDiscovery disocovery = mApp.getDiscovery();
            if (disocovery != null) {
                disocovery.unregisterEvent();
            }
        }
        super.onPause();
    }

}
