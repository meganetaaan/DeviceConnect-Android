
var util = (function(parent, global) {

    var mAccessToken = null;
    var mSessionKey = "test-session-key";

    function init(callback) {
        dConnect.setHost("localhost");
        dConnect.setExtendedOrigin("file://");
        checkDeviceConnect(callback);
    }
    parent.init = init;

    function startManager(onAvailable) {
        var errorCallback = function(errorCode, errorMessage) {
            switch (errorCode) {
            case dConnect.constants.ErrorCode.ACCESS_FAILED:
                alert('Device Connect Managerが起動していません。');
                break;
            case dConnect.constants.ErrorCode.INVALID_SERVER:
                alert('WARNING: Device Connect Manager may be spoofed.');
                break;
            case dConnect.constants.ErrorCode.INVALID_ORIGIN:
                alert('WARNING: Origin of this app is invalid. Maybe the origin is not registered in whiteList.');
                break;
            default:
                alert(errorMessage);
                break;
            }
        };
        dConnect.checkDeviceConnect(onAvailable, errorCallback);
    }

    function checkDeviceConnect(callback) {
        startManager(function(apiVersion) {
            console.log('Device Connect API version: ' + apiVersion);

            if (window.Android) {
                mAccessToken = Android.getCookie('accessToken');
            } else {
                mAccessToken = getCookie('accessToken');
            }

            openWebSocketIfNeeded();

            findHostDevicePlugin(callback);
        });
    }

    function authorization(callback) {
        var scopes = Array(
            'servicediscovery',
            'serviceinformation',
            'system',
            'battery',
            'deviceorientation',
            'mediastreamrecording',
            'vibration');
        dConnect.authorization(scopes, 'ヘルプ画面',
            function(clientId, accessToken) {
                mAccessToken = accessToken;
                openWebSocketIfNeeded();
                if (window.Android) {
                    Android.setCookie("accessToken", mAccessToken);
                } else {
                    setCookie("accessToken", mAccessToken);
                }
                callback();
            },
            function(errorCode, errorMessage) {
                showAlert("認証に失敗しました", errorCode, errorMessage);
            });
    }

    function openWebSocketIfNeeded() {
        dConnect.disconnectWebSocket();

        var accessToken = mAccessToken ? mAccessToken : mSessionKey;
        dConnect.connectWebSocket(accessToken, function(code, message) {
            if (code > 0) {
                alert('WebSocketが切れました。\n code=' + code + " message=" + message);
            }
            console.log('websocket: ' + code + ' - ' + message);
        });
    }

    function findHostDevicePlugin(callback) {
        dConnect.discoverDevices(mAccessToken, function(json) {
            for (var i = 0; i < json.services.length; i++) {
                if (json.services[i].name.toLowerCase().indexOf("host") == 0) {
                    callback(json.services[i]);
                    return;
                }
            }
            alert("HOSTデバイスプラグインの発見に失敗しました。\nデバイスプラグインがインストールされていない可能性があります。");
        }, function(errorCode, errorMessage) {
            if (errorCode == 11 || errorCode == 12 || errorCode == 13 || errorCode == 15) {
                authorization(function() {
                    findHostDevicePlugin(callback);
                });
            } else {
                showAlert("HOSTデバイスプラグインの発見に失敗しました。", errorCode, errorMessage);
            }
        });
    }

    function setCookie(key, value) {
        document.cookie = key + '=' + value;
    }

    function getCookie(name) {
        var result = null;
        var cookieName = name + '=';
        var allCookies = document.cookie;
        var position = allCookies.indexOf(cookieName);
        if (position != -1) {
            var startIndex = position + cookieName.length;
            var endIndex = allCookies.indexOf(';', startIndex);
            if (endIndex == -1) {
                endIndex = allCookies.length;
            }
            result = decodeURIComponent(allCookies.substring(startIndex, endIndex));
        }
        return result;
    }

    function getAccessToken() {
        return mAccessToken;
    }
    parent.getAccessToken = getAccessToken;

    function getSessionKey() {
        return mSessionKey;
    }
    parent.getSessionKey = getSessionKey;

    function showAlert(message, errorCode, errorMessage) {
        alert(message + "\n errorCode: " + errorCode + " \n " + errorMessage);
    }
    parent.showAlert = showAlert;

    return parent;
})(util || {}, this.self || global);
