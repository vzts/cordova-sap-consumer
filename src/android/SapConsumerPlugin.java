package com.github.vzts.cordova;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONException;

public class SapConsumerPlugin extends CordovaPlugin {

    private final String TAG = SapConsumerPlugin.class.getSimpleName();
    private boolean mIsBound = false;
    private SapConsumerService mConsumerService = null;

    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            mConsumerService = ((SapConsumerService.LocalBinder) service).getService();
            Log.d(TAG, "onServiceConnected");
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            mConsumerService = null;
            mIsBound = false;
            Log.d(TAG, "onServiceDisconnected");
        }
    };

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        
        Log.d(TAG, "initialize called");

        Activity context = cordova.getActivity();
        mIsBound = context.bindService(new Intent(context, SapConsumerService.class), mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public boolean execute(String action, CordovaArgs args,
                           CallbackContext callbackContext) throws JSONException {

        final String ACTION_CONNECT = "connect";
        final String ACTION_SENDDATA = "sendData";

        if (ACTION_CONNECT.equals(action)) {
            mConsumerService.findPeers();
        } else if (ACTION_SENDDATA.equals(action)) {
            if (mIsBound && mConsumerService != null) {
                mConsumerService.sendData(args.getString(0));
            } else {
                Log.e(TAG, "not connected yet!");
            }
        }

        PluginResult r = new PluginResult(PluginResult.Status.OK, "good");
        r.setKeepCallback(true);
        callbackContext.sendPluginResult(r);

        return true;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        Activity context = cordova.getActivity();

        // Clean up connections
        if (mIsBound && mConsumerService != null) {
            if (!mConsumerService.closeConnection()) {
                Log.d(TAG, "Disconnected");
            }
        }
        // Un-bind service
        if (mIsBound) {
            context.unbindService(mConnection);
            mIsBound = false;
        }
        super.onDestroy();
    }
}
