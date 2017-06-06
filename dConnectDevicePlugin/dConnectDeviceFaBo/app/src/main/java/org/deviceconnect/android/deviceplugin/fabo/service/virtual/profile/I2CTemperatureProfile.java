package org.deviceconnect.android.deviceplugin.fabo.service.virtual.profile;

import android.content.Intent;

import org.deviceconnect.android.profile.api.GetApi;
import org.deviceconnect.message.DConnectMessage;

import static org.deviceconnect.android.deviceplugin.fabo.param.FirmataV32.END_SYSEX;
import static org.deviceconnect.android.deviceplugin.fabo.param.FirmataV32.I2C_REQUEST;
import static org.deviceconnect.android.deviceplugin.fabo.param.FirmataV32.START_SYSEX;

public class I2CTemperatureProfile extends BaseFaBoProfile {

    private static final byte ADT7410_ADDRESS = 0x48;

    public I2CTemperatureProfile() {
        // GET /gotpai/temperature
        addApi(new GetApi() {
            @Override
            public boolean onRequest(final Intent request, final Intent response) {
                test();
                setResult(response, DConnectMessage.RESULT_OK);
                return true;
            }
        });
    }

    @Override
    public String getProfileName() {
        return "temperature";
    }


    private void test() {
        byte[] commandDataLeft = {
                START_SYSEX,
                I2C_REQUEST,
                ADT7410_ADDRESS,
                0x03,
                (byte) 0x80,
                END_SYSEX
        };
        getFaBoUsbManager().writeBuffer(commandDataLeft);
    }
}
