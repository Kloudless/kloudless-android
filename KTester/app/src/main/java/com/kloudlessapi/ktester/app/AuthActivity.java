package com.kloudlessapi.ktester.app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.kloudless.Kloudless;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * This activity is used internally for authentication, but must be exposed both
 * so that Android can launch it and for backwards compatibility.
 */
public class AuthActivity extends Activity {
    private static final String TAG = AuthActivity.class.getName();

    /**
     * The path for a successful callback with token (not the initial auth request).
     */
    public static final String AUTH_PATH_CONNECT = "/services";

    // For communication between KAuth and this activity.
    /*package*/ static final String URL = "URL";

    public static final String ACCOUNT = "ACCOUNT";

    public static final String TOKEN = "TOKEN";

    private static final String DEFAULT_WEB_HOST = "https://api.kloudless.com";

    /**
     * Provider of the local security needs of an AuthActivity.
     *
     * <p>
     * You shouldn't need to use this class directly in your app.  Instead,
     * simply configure {@code java.security}'s providers to match your preferences.
     * </p>
     */
    public interface SecurityProvider {
        /**
         * Gets a SecureRandom implementation for use during authentication.
         */
        SecureRandom getSecureRandom();
    }

    // Class-level state used to replace the default SecureRandom implementation
    // if desired.
    private static SecurityProvider sSecurityProvider = new SecurityProvider() {
        @Override
        public SecureRandom getSecureRandom() {
            return new SecureRandom();
        }
    };
    private static final Object sSecurityProviderLock = new Object();

    /** Used internally. */
    public static Intent result = null;

    private String url;

    // Stored in savedInstanceState to track an ongoing auth attempt, which
    // must include a locally-generated nonce in the response.
    private String authStateNonce = null;

    /**
     * Create an intent which can be sent to this activity to start authentication.
     *
     * @param context the source context
     * @param url the authentication endpoint for the app
     *
     * @return a newly created intent.
     *
     */
    public static Intent makeIntent(Context context, String url) {
        Log.i("makeIntent", String.format("url: %s", url));
        if (url == null) throw new IllegalArgumentException("'url' can't be null");
        Intent intent = new Intent(context, AuthActivity.class);
        intent.putExtra(AuthActivity.URL, url);
        return intent;
    }

    /**
     * Sets the SecurityProvider interface to use for all AuthActivity instances.
     * If set to null (or never set at all), default {@code java.security} providers
     * will be used instead.
     *
     * <p>
     * You shouldn't need to use this method directly in your app.  Instead,
     * simply configure {@code java.security}'s providers to match your preferences.
     * </p>
     *
     * @param prov the new {@code SecurityProvider} interface.
     */
    public static void setSecurityProvider(SecurityProvider prov) {
        synchronized (sSecurityProviderLock) {
            sSecurityProvider = prov;
        }
    }

    private static SecurityProvider getSecurityProvider() {
        synchronized (sSecurityProviderLock) {
            return sSecurityProvider;
        }
    }

    private static SecureRandom getSecureRandom() {
        SecurityProvider prov = getSecurityProvider();
        if (null != prov) {
            return prov.getSecureRandom();
        }
        return new SecureRandom();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_auth);

        Intent intent = getIntent();
        url = intent.getStringExtra(URL);

        Log.i("onCreate", String.format("url: %s", url));

        setTheme(android.R.style.Theme_Translucent_NoTitleBar);

        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("authStateNonce", authStateNonce);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (isFinishing()) {
            return;
        }

        if (authStateNonce != null || url == null) {
            // We somehow returned to this activity without being forwarded
            // here by the official app. Most likely caused by improper setup,
            // but could have other reasons if Android is acting up and killing
            // activities used in our process.
            authFinished(null);
            return;
        }

        result = null;

        startWebAuth(null);

        // Save state that indicates we started a request, only after
        // we started one successfully.
        authStateNonce = null;
    }

    @Override
    protected void onNewIntent(Intent intent) {

        Log.i("onNewIntent", "new intent");

        Uri uri = intent.getData();
        if (uri != null) {
            Log.i("onNewIntent", String.format("uri: %s", uri.getPath()));
        }
    }

    private void authFinished(Intent authResult) {
        result = authResult;
        authStateNonce = null;
        finish();
    }

    private String getConsumerSig() {
        return "";
    }

    private void startWebAuth(String state) {

        Log.i("startWebAuth", String.format("url: %s", url));

        final WebView myWebView = (WebView) findViewById(R.id.webview);
        myWebView.getSettings().setJavaScriptEnabled(true);
        myWebView.getSettings().setDomStorageEnabled(true);
        myWebView.addJavascriptInterface(new WebAppInterface(this), "AUTH");
        myWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.proceed(); // Ignore SSL certificate errors
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                myWebView.loadUrl("javascript:window.AUTH.getToken(document.getElementById('access_token').getAttribute('data-value'));");
            };
        });
        myWebView.loadUrl(url);
    }

    public class WebAppInterface {
        Context mContext;
        private String account;
        private String token;

        /** Instantiate the interface and set the context */
        WebAppInterface(Context c) {
            mContext = c;
        }

        @JavascriptInterface
        public void getToken(String bearerToken) throws IOException, JSONException {
            Log.i("getToken", bearerToken);
            this.token = bearerToken;

            KAuth tmp = new KAuth("");

            // Verify the token and retrieve the account id
            String url = String.format("%s://%s/v%s/oauth/token",
                    tmp.kProtocolHTTPS, tmp.kAPIHost, tmp.kAPIVersion);
            java.net.URL obj = new URL(url);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) obj.openConnection();
            String authorization = String.format("Bearer %s", token);
            conn.setRequestProperty("Authorization", authorization);

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            String responseString = response.toString();
            System.out.println(responseString);
            JSONObject responseJson = new JSONObject(responseString);
            this.account = responseJson.getString("account_id");

            if (this.account != null && this.token != null) {
                result = new Intent();
                result.putExtra(ACCOUNT, this.account);
                result.putExtra(TOKEN, this.token);
                finish();
            }
        }
    }
}
