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

//import com.dropbox.client2.DropboxAPI;
//import com.dropbox.client2.RESTUtility;
import com.kloudless.Kloudless;

//Note: This class's code is duplicated between Core SDK and Sync SDK.  For now,
//it has to be manually copied, but the code is set up so that it can be used in both
//places, with only a few import changes above.  If you're making changes here, you
//should consider if the other side needs them.  Don't break compatibility if you
//don't have to.  This is a hack we should get away from eventually.

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
     * Check's the current app's manifest setup for authentication.
     * If the manifest is incorrect, an exception will be thrown.
     * If another app on the device is conflicting with this one,
     * the user will (optionally) be alerted and false will be returned.
     *
     * @param context the app context
     * @param appKey the consumer key for the app
     * @param alertUser whether to alert the user for the case where
     *  multiple apps are conflicting.
     *
     * @return {@code true} if this app is properly set up for authentication.
     */
    /*
    public static boolean checkAppBeforeAuth(Context context, String appKey, boolean alertUser) {
        // Check if the app has set up its manifest properly.
        Intent testIntent = new Intent(Intent.ACTION_VIEW);
        String scheme = "db-" +appKey;
        String uri = scheme + "://" + AUTH_VERSION + AUTH_PATH_CONNECT;
        testIntent.setData(Uri.parse(uri));
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> activities = pm.queryIntentActivities(testIntent, 0);

        if (null == activities || 0 == activities.size()) {
            throw new IllegalStateException("URI scheme in your app's " +
                    "manifest is not set up correctly. You should have a " +
                    AuthActivity.class.getName() + " with the " +
                    "scheme: " + scheme);
        } else if (activities.size() > 1) {
            if (alertUser) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Security alert");
                builder.setMessage("Another app on your phone may be trying to " +
                        "pose as the app you are currently using. The malicious " +
                        "app can't access your account, but linking to Dropbox " +
                        "has been disabled as a precaution. Please contact " +
                        "support@dropbox.com.");
                builder.setPositiveButton("OK", new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                builder.show();
            } else {
                Log.w(TAG, "There are multiple apps registered for the AuthActivity " +
                        "URI scheme (" + scheme + ").  Another app may be trying to " +
                        " impersonate this app, so authentication will be disabled.");
            }
            return false;
        } else {
            // Just one activity registered for the URI scheme. Now make sure
            // it's within the same package so when we return from web auth
            // we're going back to this app and not some other app.
            ResolveInfo resolveInfo = activities.get(0);
            if (null == resolveInfo || null == resolveInfo.activityInfo
                    || !context.getPackageName().equals(resolveInfo.activityInfo.packageName)) {
                throw new IllegalStateException("There must be a " +
                        AuthActivity.class.getName() + " within your app's package " +
                        "registered for your URI scheme (" + scheme + "). However, " +
                        "it appears that an activity in a different package is " +
                        "registered for that scheme instead. If you have " +
                        "multiple apps that all want to use the same access" +
                        "token pair, designate one of them to do " +
                        "authentication and have the other apps launch it " +
                        "and then retrieve the token pair from it.");
            }
        }

        return true;
    }
    */

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

        /*
        // Reject attempt to finish authentication if we never started (nonce=null)
        if (null == authStateNonce) {
            authFinished(null);
            return;
        }

        String token = null, secret = null, uid = null, state = null;

        if (intent.hasExtra(EXTRA_ACCESS_TOKEN)) {
            // Dropbox app auth.
            token = intent.getStringExtra(EXTRA_ACCESS_TOKEN);
            secret = intent.getStringExtra(EXTRA_ACCESS_SECRET);
            uid = intent.getStringExtra(EXTRA_UID);
            state = intent.getStringExtra(EXTRA_AUTH_STATE);
        } else {
            // Web auth.
            Uri uri = intent.getData();
            if (uri != null) {
                String path = uri.getPath();
                if (AUTH_PATH_CONNECT.equals(path)) {
                    try {
                        token = uri.getQueryParameter("oauth_token");
                        secret = uri.getQueryParameter("oauth_token_secret");
                        uid = uri.getQueryParameter("uid");
                        state = uri.getQueryParameter("state");
                    } catch (UnsupportedOperationException e) {}
                }
            }
        }

        Intent newResult;
        if (token != null && !token.equals("") &&
                (secret == null || !secret.equals("")) &&
                uid != null && !uid.equals("") &&
                state != null && !state.equals("")) {
            // Reject attempt to link if the nonce in the auth state doesn't match,
            // or if we never asked for auth at all.
            if (!authStateNonce.equals(state)) {
                authFinished(null);
                return;
            }

            // Successful auth.
            newResult = new Intent();
            newResult.putExtra(EXTRA_ACCESS_TOKEN, token);
            newResult.putExtra(EXTRA_ACCESS_SECRET, secret);
            newResult.putExtra(EXTRA_UID, uid);
        } else {
            // Unsuccessful auth, or missing required parameters.
            newResult = null;
        }

        authFinished(newResult);

        */
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

//        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
//        startActivity(intent);
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
