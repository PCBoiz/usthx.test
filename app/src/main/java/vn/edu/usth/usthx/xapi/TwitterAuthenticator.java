package vn.edu.usth.usthx.xapi;

import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.pkce.PKCE;
import com.github.scribejava.core.pkce.PKCECodeChallengeMethod;
import com.twitter.clientlib.TwitterCredentialsOAuth2;

import com.twitter.clientlib.auth.TwitterOAuth20Service;
import java.security.SecureRandom;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

public class TwitterAuthenticator {

    private static final String CLIENT_ID = "cVJqUTJwc3RuZWJjYkhOb1N2NkM6MTpjaQ";

    private static final String CLIENT_SECRET = "3246iK-ZksvpJHnU0Sg2dqqe33u88t_3uxCNSUKu0eLprGRA-T";

    private final TwitterOAuth20Service service = new TwitterOAuth20Service(
            TwitterAuthenticator.CLIENT_ID,
            TwitterAuthenticator.CLIENT_SECRET,
            "http://127.0.0.1:1234",
            "offline.access tweet.read tweet.write users.read follows.write follows.read mute.read mute.write like.read like.write list.read list.write block.read block.write bookmark.read bookmark.write media.write");
    private final String state;

    private final PKCE pkce = new PKCE();

    private String latestAccessToken;
    private String latestRefreshToken;

    public interface GenerateTokensCallback {
        void onTokensGenerated(String accessToken, String refreshToken);
        void onError(Exception e);
    }
    public TwitterAuthenticator() {
        this.state = generateUrlSafeRandomString(16);
    }


    private String generateUrlSafeRandomString(int length) {
        byte[] randomBytes = new byte[length];
        new SecureRandom().nextBytes(randomBytes);
        return Base64.encodeToString(randomBytes, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
    }

    private String generateUrl() {

        String challenge = generateUrlSafeRandomString(20);
        pkce.setCodeChallenge(challenge); // Sorry Mr Son this isn't peak security things but i tried
        pkce.setCodeChallengeMethod(PKCECodeChallengeMethod.PLAIN);
        pkce.setCodeVerifier(challenge);

        return service.getAuthorizationUrl(pkce, state);

    }

    public String getUrl() {
        return generateUrl();
    }


    public TwitterCredentialsOAuth2 generateCredentials() {
        return new TwitterCredentialsOAuth2(
                CLIENT_ID,
                CLIENT_SECRET,
                this.latestAccessToken,
                this.latestRefreshToken,
                true
        );
    }

    public void generateTokens(String code, String state, GenerateTokensCallback callback) {
        if (state.equals(this.state)) {

                new Thread(() -> {
                    try {
                        OAuth2AccessToken tokens= TwitterAuthenticator.this.service.getAccessToken(pkce, code);
                        TwitterAuthenticator.this.latestAccessToken = tokens.getAccessToken();
                        TwitterAuthenticator.this.latestRefreshToken = tokens.getRefreshToken();
                        callback.onTokensGenerated(tokens.getAccessToken(), tokens.getRefreshToken());
                    } catch (Exception e) {
                        Log.e("TWITTER_AUTH", e.toString());
                        callback.onError(e);

                    }
                }).start();

        } else {
            throw new IllegalStateException("State mismatch");
        }
    }

    public void restoreCredentials(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences("xusth", Context.MODE_PRIVATE);
        if (!prefs.contains("accessToken")) return;
        this.latestAccessToken  = prefs.getString("accessToken", null);

        this.latestRefreshToken = prefs.getString("refreshToken", null);
    }

    public boolean isAuthenticated() {
        return this.latestAccessToken != null;
    }


    public void saveCredentials(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences("xusth", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString("accessToken",this.latestAccessToken);
        editor.putString("refreshToken", this.latestRefreshToken);
        editor.apply();
        editor.commit();
    }

    public void clearCredentials(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences("xusth", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.remove("accessToken");
        editor.remove("refreshToken");
        editor.apply();
        editor.commit();
        this.latestAccessToken = null;
        this.latestRefreshToken = null;
    }


}
