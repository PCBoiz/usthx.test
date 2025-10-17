package vn.edu.usth.usthx;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.twitter.clientlib.TwitterCredentialsOAuth2;
import com.twitter.clientlib.model.User;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import vn.edu.usth.usthx.xapi.TwitterAccountHandler;
import vn.edu.usth.usthx.xapi.TwitterAuthenticator;
import vn.edu.usth.usthx.xapi.TwitterCallbackListener;

public class LoginActivity extends AppCompatActivity {

    private Button btnContinueWithX;

    private TwitterAuthenticator twitterAuthenticator;

    private TwitterCallbackListener service;
    private boolean bound = false;

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            TwitterCallbackListener.TwitterServerBinder binder = (TwitterCallbackListener.TwitterServerBinder) service;
            LoginActivity.this.service = binder.getService();
            LoginActivity.this.bound = true;

        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            LoginActivity.this.bound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        btnContinueWithX = findViewById(R.id.btnContinueWithX);

        btnContinueWithX.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleXLogin();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        this.twitterAuthenticator = new TwitterAuthenticator();
        twitterAuthenticator.restoreCredentials(this);






        if (twitterAuthenticator.isAuthenticated()) {
            TwitterCredentialsOAuth2 auths = twitterAuthenticator.generateCredentials();
                    //Intent intent = new Intent(this, MainActivity.class);
            TwitterAccountHandler client = TwitterAccountHandler.getInstance();
            client.setCredentials(auths);
            client.getMe(new TwitterAccountHandler.ResponseCallback<>() {
                @Override
                public void onSuccess(User user) {
                    runOnUiThread(() -> {
                        TextView txt = new TextView(LoginActivity.this);
                        txt.setText(user.getUsername());
                        txt.setGravity(Gravity.CENTER);
                        Button logoutBtn = new AppCompatButton(LoginActivity.this);
                        logoutBtn.setText("Logout");
                        logoutBtn.setGravity(Gravity.CENTER);
                        logoutBtn.setOnClickListener(v -> {
                            twitterAuthenticator.clearCredentials(LoginActivity.this);
                            Toast.makeText(LoginActivity.this, "Logged out", Toast.LENGTH_SHORT).show();
                            recreate();
                        });
                        LoginActivity.this.addContentView(txt, new ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                        ));
                        LoginActivity.this.addContentView(logoutBtn, new ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                        ));

                    });
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e("TWITTER_AUTH_ACTIVITY", "user notAuthenticated:"+e.toString());
                    Toast.makeText(LoginActivity.this, "User not authenticated: "+e, Toast.LENGTH_LONG).show();
                }
            });
        }

    }

    public void onLoginButtonClick(View v) {
        try {

            if (!bound) {
                bindService(
                        new Intent(this, TwitterCallbackListener.class),
                        connection,
                        Context.BIND_AUTO_CREATE
                );
            }
            String url = this.twitterAuthenticator.getUrl();
            Uri uri = Uri.parse(url);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);

        } catch (Exception e) {

            Toast.makeText(this, "An error as occured try again later", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!bound) return;
        Bundle resp = this.service.getResponse();
        Log.i("TWITTER_AUTH_ACTIVITY", resp.toString());
        if (resp.getString("code") != null && resp.getString("state") != null) {

            String code = resp.getString("code");
            String state = resp.getString("state");
            this.unbindService(connection);
            this.bound = false;

            try {
                CountDownLatch credentialsLatch = new CountDownLatch(1);
                this.twitterAuthenticator.generateTokens(code, state, new TwitterAuthenticator.GenerateTokensCallback() {
                    @Override
                    public void onTokensGenerated(String accessToken, String refreshToken) {
                        credentialsLatch.countDown();
                        LoginActivity.this.twitterAuthenticator.saveCredentials(LoginActivity.this);
                        runOnUiThread(() ->
                                Toast.makeText(LoginActivity.this, "Authentification done", Toast.LENGTH_SHORT).show()
                        );
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e("TWITTER_AUTH_ACTIVITY", e.toString());
                        runOnUiThread(() -> Toast.makeText(LoginActivity.this, "An error as occured try again: " + e, Toast.LENGTH_SHORT).show());
                        credentialsLatch.countDown();
                    }
                });
                credentialsLatch.await(15, TimeUnit.SECONDS);
                TwitterCredentialsOAuth2 credentials = this.twitterAuthenticator.generateCredentials();


                TwitterAccountHandler client = TwitterAccountHandler.getInstance();
                client.setCredentials(credentials);

                client.getMe(new TwitterAccountHandler.ResponseCallback<>() {
                    @Override
                    public void onSuccess(User user) {
                        credentialsLatch.countDown();
                        runOnUiThread(() -> {
                            TextView txt = new TextView(LoginActivity.this);
                            txt.setText(user.getUsername());
                            ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.WRAP_CONTENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT
                            );
                            LoginActivity.this.addContentView(txt, params);
                        });
                    }

                    @Override
                    public void onFailure(Exception e) {
                        credentialsLatch.countDown();
                        Log.e("TWITTER_AUTH_ACTIVITY", "user notAuthenticated:" + e.toString());

                        runOnUiThread(() -> Toast.makeText(LoginActivity.this, "User not authenticated: " + e, Toast.LENGTH_LONG).show());
                    }
                });

            } catch (Exception e) {
                Log.e("TWITTER_AUTH_ACTIVITY", e.toString());
                Toast.makeText(this, "An error as occured try again", Toast.LENGTH_LONG).show();
            }

        }
    }
}