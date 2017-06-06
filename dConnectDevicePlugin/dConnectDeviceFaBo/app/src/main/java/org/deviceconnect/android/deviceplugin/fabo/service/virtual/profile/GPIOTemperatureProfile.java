package org.deviceconnect.android.deviceplugin.fabo.service.virtual.profile;

import android.content.Intent;

import org.deviceconnect.android.deviceplugin.fabo.param.ArduinoUno;
import org.deviceconnect.android.profile.api.GetApi;
import org.deviceconnect.message.DConnectMessage;

import java.util.List;

/**
 * GPIO用のTemperatureプロファイル.
 */
public class GPIOTemperatureProfile extends BaseFaBoProfile {
    private List<ArduinoUno.Pin> mPinList;

    public GPIOTemperatureProfile(final List<ArduinoUno.Pin> pinList) {
        mPinList = pinList;

        // GET /gotpai/temperature
        addApi(new GetApi() {
            @Override
            public boolean onRequest(final Intent request, final Intent response) {
                ArduinoUno.Pin pin = mPinList.get(0);

                int value = getFaBoDeviceService().getAnalogValue(pin);
                value = value * 5000 / 1023;
                value = (value - 300) * (100 - (-30)) / (1600 - 300) + (-30);
                value = Math.round(value * 10) / 10;

                response.putExtra("temperature", value);

                setResult(response, DConnectMessage.RESULT_OK);
                return true;
            }
        });
    }

    @Override
    public String getProfileName() {
        return "temperature";
    }
}
