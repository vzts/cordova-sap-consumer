package com.github.vzts.cordova;

import java.io.IOException;

import android.content.Intent;
import android.os.Handler;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.samsung.android.sdk.SsdkUnsupportedException;
import com.samsung.android.sdk.accessory.*;

public class SapConsumerService extends SAAgent {
    private static final String TAG = "HelloAccessory(C)";
    private static final Class<ServiceConnection> SASOCKET_CLASS = ServiceConnection.class;
    private final IBinder mBinder = new LocalBinder();
    private ServiceConnection mConnectionHandler = null;

    public SapConsumerService() {
        super(TAG, SASOCKET_CLASS);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        
        Log.d(TAG, "service being created");
        
        SA mAccessory = new SA();
        try {
            mAccessory.initialize(this);
        } catch (SsdkUnsupportedException e) {
            // try to handle SsdkUnsupportedException
            if (processUnsupportedException(e) == true) {
                return;
            }
        } catch (Exception e1) {
            e1.printStackTrace();
            /*
             * Your application can not use Samsung Accessory SDK. Your application should work smoothly
             * without using this SDK, or you may want to notify user and close your application gracefully
             * (release resources, stop Service threads, close UI thread, etc.)
             */
            stopSelf();
        }
        
        Log.d(TAG, "service created");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    protected void onFindPeerAgentsResponse(SAPeerAgent[] peerAgents, int result) {
        if ((result == SAAgent.PEER_AGENT_FOUND) && (peerAgents != null)) {
            for (SAPeerAgent peerAgent : peerAgents)
                requestServiceConnection(peerAgent);
        } else if (result == SAAgent.FINDPEER_DEVICE_NOT_CONNECTED) {
            Log.d(TAG, "FINDPEER_DEVICE_NOT_CONNECTED");
            Log.d(TAG, "Disconnected");
        } else if (result == SAAgent.FINDPEER_SERVICE_NOT_FOUND) {
            Log.d(TAG, "FINDPEER_SERVICE_NOT_FOUND");
            Log.d(TAG, "Disconnected");
        } else {
            Log.d(TAG, "No peers have been found!");
        }
    }

    @Override
    protected void onServiceConnectionRequested(SAPeerAgent peerAgent) {
        if (peerAgent != null) {
            acceptServiceConnectionRequest(peerAgent);
        }
    }

    @Override
    protected void onServiceConnectionResponse(SAPeerAgent peerAgent, SASocket socket, int result) {
        if (result == SAAgent.CONNECTION_SUCCESS) {
            this.mConnectionHandler = (ServiceConnection) socket;
            Log.d(TAG, "Connected");
        } else if (result == SAAgent.CONNECTION_ALREADY_EXIST) {
            Log.d(TAG, "Connected");
            Log.d(TAG, "CONNECTION_ALREADY_EXIST");
        } else if (result == SAAgent.CONNECTION_DUPLICATE_REQUEST) {
            Log.d(TAG, "CONNECTION_DUPLICATE_REQUEST");
        } else {
            Log.d(TAG, "Service Connection Failure");
        }
    }

    @Override
    protected void onError(SAPeerAgent peerAgent, String errorMessage, int errorCode) {
        super.onError(peerAgent, errorMessage, errorCode);
    }

    @Override
    protected void onPeerAgentsUpdated(SAPeerAgent[] peerAgents, int result) {
        final SAPeerAgent[] peers = peerAgents;
        final int status = result;
        if (peers != null) {
            if (status == SAAgent.PEER_AGENT_AVAILABLE) {
                Log.d(TAG, "PEER_AGENT_AVAILABLE");
            } else {
                Log.d(TAG, "PEER_AGENT_UNAVAILABLE");
            }
        }
    }

    public class ServiceConnection extends SASocket {
        public ServiceConnection() {
            super(ServiceConnection.class.getName());
        }

        @Override
        public void onError(int channelId, String errorMessage, int errorCode) {
        }

        @Override
        public void onReceive(int channelId, byte[] data) {
            final String message = new String(data);
            Log.d(TAG, "Received: " + message);
        }

        @Override
        protected void onServiceConnectionLost(int reason) {
            Log.d(TAG, "Disconnected");
            closeConnection();
        }
    }

    public class LocalBinder extends Binder {
        public SapConsumerService getService() {
            return SapConsumerService.this;
        }
    }

    public void findPeers() {
        findPeerAgents();
    }

    public boolean sendData(final String data) {
        boolean retvalue = false;
        if (mConnectionHandler != null) {
            try {
                mConnectionHandler.send(getServiceChannelId(0), data.getBytes());
                retvalue = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
            Log.d(TAG, "Sent: " + data);
        }
        return retvalue;
    }

    public boolean closeConnection() {
        if (mConnectionHandler != null) {
            mConnectionHandler.close();
            mConnectionHandler = null;
            return true;
        } else {
            return false;
        }
    }

    private boolean processUnsupportedException(SsdkUnsupportedException e) {
        e.printStackTrace();
        int errType = e.getType();
        if (errType == SsdkUnsupportedException.VENDOR_NOT_SUPPORTED
                || errType == SsdkUnsupportedException.DEVICE_NOT_SUPPORTED) {
            /*
             * Your application can not use Samsung Accessory SDK. You application should work smoothly
             * without using this SDK, or you may want to notify user and close your app gracefully (release
             * resources, stop Service threads, close UI thread, etc.)
             */
            stopSelf();
        } else if (errType == SsdkUnsupportedException.LIBRARY_NOT_INSTALLED) {
            Log.e(TAG, "You need to install Samsung Accessory SDK to use this application.");
        } else if (errType == SsdkUnsupportedException.LIBRARY_UPDATE_IS_REQUIRED) {
            Log.e(TAG, "You need to update Samsung Accessory SDK to use this application.");
        } else if (errType == SsdkUnsupportedException.LIBRARY_UPDATE_IS_RECOMMENDED) {
            Log.e(TAG, "We recommend that you update your Samsung Accessory SDK before using this application.");
            return false;
        }
        return true;
    }
}
