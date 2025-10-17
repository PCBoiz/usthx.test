package vn.edu.usth.usthx.xapi;

import android.app.Service;

import android.os.Binder;

import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;


import java.io.IOException;

import fi.iki.elonen.NanoHTTPD;

class TwitterCallbackServer extends NanoHTTPD {
    static final int PORT = 1234;
    private final TwitterCallbackListener initiator;


    public TwitterCallbackServer(TwitterCallbackListener initiator) {
        super(PORT);
        this.initiator = initiator;
    }


    @Override
    public Response serve(IHTTPSession session) {
        Log.i("TWITTER_AUTH_SERVER", "New request");
        try {
            String state = session.getParameters().get("state").get(0);
            String code = session.getParameters().get("code").get(0);
            this.initiator.serverResponse(state,code);
        } catch(Exception e) {
            return newFixedLengthResponse(Response.Status.EXPECTATION_FAILED, MIME_PLAINTEXT, "An Error has occured, X Api response is unexpected");
        }

        return newFixedLengthResponse(Response.Status.OK,MIME_PLAINTEXT, "Authentification done, you can comeback to the app");

    }

}

public class TwitterCallbackListener extends Service {
    private final TwitterCallbackServer server;
    private final IBinder binder = new TwitterServerBinder();

    public String code = null;
    public String state = null;
    public TwitterCallbackListener() {
        server = new TwitterCallbackServer(this);
    }

    public class TwitterServerBinder extends Binder {
        public TwitterCallbackListener getService() {
            return TwitterCallbackListener.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (!server.isAlive()) {
            try {
                server.start();
                Log.i("TWITTER_SERVICE", "Serveur créer par bind");
            } catch (IOException e) {
                    throw new RuntimeException(e);
            }
        }

        return binder;

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) throws RuntimeException{
        return super.onStartCommand(intent, flags, startId);
    }

    void serverResponse(String state, String code) {
        this.state =state;
        this.code = code;
    }

    public Bundle getResponse() {
        Bundle resp = new Bundle();
        resp.putString("code", this.code);
        resp.putString("state", this.state);
        this.state = null;
        this.code = null;
        return resp;
    }


    @Override
    public void onDestroy() {
        Log.i("TWITTER_SERVICE", "Service détruit");
        if (server != null && server.isAlive()) {
            server.stop();
            Log.i("TWITTER_SERVICE", "Serveur détruit");

        }
        super.onDestroy();
    }

}