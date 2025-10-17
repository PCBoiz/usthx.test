package vn.edu.usth.usthx.xapi;

import com.twitter.clientlib.TwitterCredentialsOAuth2;
import com.twitter.clientlib.api.TwitterApi;
import com.twitter.clientlib.model.User;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.os.Handler;
import android.os.Looper;

public class TwitterAccountHandler {

    public interface ResponseCallback<T> {
        void onSuccess(T result);
        void onFailure(Exception e);
    }
    private static TwitterAccountHandler instance;
    private TwitterApi client;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());


    private TwitterAccountHandler() {
    }

    public static TwitterAccountHandler getInstance() {
        if (instance == null) {
            instance = new TwitterAccountHandler();
        }
        return instance;
    }


    public void setCredentials(TwitterCredentialsOAuth2 credentials) {
        client = new TwitterApi(credentials);
    }

    public void getMe(ResponseCallback<User> callback) {
        executor.execute(() -> {
            try {
                if (client == null) throw new IllegalStateException("No Client");
                User me = client.users().findMyUser().execute().getData();

                if (me == null) throw new IllegalStateException("Twitter API call failed");

                mainThreadHandler.post(() -> callback.onSuccess(me));
            } catch (Exception e) {
                mainThreadHandler.post(() -> callback.onFailure(e));
            }
        });


    }


}
