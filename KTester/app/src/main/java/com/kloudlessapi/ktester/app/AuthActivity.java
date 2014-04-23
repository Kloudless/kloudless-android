package com.kloudlessapi.ktester.app;

import java.math.BigInteger;
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

    public static final String ACCOUNT_KEY = "ACCOUNT_KEY";

    private static final String DEFAULT_WEB_HOST = "api.kloudless.com";

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

                myWebView.loadUrl("javascript:window.AUTH.getAccount(document.getElementById('account').title);" +
                        "window.AUTH.getAccountKey(document.getElementById('account_key').title);");
            };
        });
        myWebView.loadUrl(url);
    }

    public class WebAppInterface {
        Context mContext;
        private String account;
        private String accountKey;

        /** Instantiate the interface and set the context */
        WebAppInterface(Context c) {
            mContext = c;
        }

        @JavascriptInterface
        public void getAccount(String account) {
            Log.i("getAccount", account);
            this.account = account;
        }

        @JavascriptInterface
        public void getAccountKey(String accountKey) {
            Log.i("getAccountKey", accountKey);
            this.accountKey = accountKey;

            if (this.account != null && this.accountKey != null) {
                result = new Intent();
                result.putExtra(ACCOUNT, this.account);
                result.putExtra(ACCOUNT_KEY, this.accountKey);
                finish();
            }
        }
    }
}
