package com.kloudlessapi.ktester.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.UUID;

/**
 * Created by timothytliu on 4/17/14.
 */
public class KAuth {

    final static String kSDKVersion = "0.0.1"; // TODO: parameterize from build system
    final String kAPIHost = "api.kloudless.com";
    final String kWebHost = "www.kloudless.com";
    final String kAPIVersion = "0";
    final String kProtocolHTTPS = "https";

    static String appId = null;
    static HashMap<String, Object> keysStore = null;
    static KAuth sharedAuth = null;

    /**
     * Creates a new KAuth with the given AppId
     * @return
     */
    public KAuth(String appId) {
        this.appId = appId;
        this.keysStore = new HashMap<String, Object>();
    }

    public static KAuth getSharedAuth() {
        return sharedAuth;
    }

    public static void setSharedAuth(KAuth auth) {
        if (auth.equals(sharedAuth)) return;
        sharedAuth = auth;
    }

    /**
     * Starts the authentication process by launching an activity view.  This is for
     * backwards compatibility for a default url; however, you can pass in a custom URL.
     *
     * @param context
     */
    public void startAuthentication(Context context) {
        String state = UUID.randomUUID().toString();
        String url = String.format("%s://%s/v%s/oauth/?client_id=%s&redirect_uri=urn:ietf:wg:oauth:2.0:oob&response_type=token&state=%s",
                kProtocolHTTPS, kAPIHost, kAPIVersion, appId, state);
        startAuthentication(context, url);
    }

    /**
     * Starts the authentication process by launch an activity view.
     *
     * // default
     * url = "https://api.kloudless.com/services/?app_id=%s&referrer=mobile&retrieve_account_key=true"
     *
     * Authenticate a set of services:
     * url = "https://api.kloudless.com/services/?app_id=%s&referrer=mobile&retrieve_account_key=true&services=box,dropbox"
     *
     * Skip the user selecting and authenticate a specific service:
     * url = "https://api.kloudless.com/services/dropbox?app_id=%@&referrer=mobile&retrieve_account_key=true"
     *
     * Note: Both retrieve_account_key and mobile need to be set to true and mobile respectively to retrieve authentication credentials.
     *
     * @param context
     * @param url - The URL can be customized for various stages of the authentication process
     */
    public void startAuthentication(Context context, String url) {
        // Start Kloudless auth activity.
        Intent intent = new Intent(context, AuthActivity.class);
        intent.putExtra(AuthActivity.URL, url);
        if (!(context instanceof Activity)) {
            // If starting the intent outside of an Activity, must include
            // this. See startActivity(). Otherwise, we prefer to stay in
            // the same task.
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
    }

    /**
     * Returns whether the user successfully authenticated with Kloudless.
     * Reasons for failure include the user canceling authentication, network
     * errors, and improper setup from within your app.
     */
    public boolean authenticationSuccessful() {
        Intent data = AuthActivity.result;

        if (data == null) {
            return false;
        }

        String token = data.getStringExtra(AuthActivity.TOKEN);
        if (token != null && !token.equals("")) {
            return true;
        }

        return false;
    }

    /**
     * Adds a user's account and Account Key when you return
     * to your activity from the Kloudless authentication process. Should be
     * called from your activity's {@code onResume()} method, but only
     * after checking that {@link #authenticationSuccessful()} is {@code true}.
     *
     * @return the authenticated user's Account ID.
     *
     * @throws IllegalStateException if authentication was not successful prior
     *         to this call (check with {@link #authenticationSuccessful()}.
     */
    public String finishAuthentication() throws IllegalStateException, IOException {
        Intent data = AuthActivity.result;

        if (data == null) {
            throw new IllegalStateException();
        }

        String token = data.getStringExtra(AuthActivity.TOKEN);
        String account = data.getStringExtra(AuthActivity.ACCOUNT);
        if (token == null || token.length() == 0) {
            throw new IllegalArgumentException("Invalid result intent passed in. " +
                    "Missing token.");
        }

        if (account == null || account.length() == 0) {
            throw new IllegalArgumentException("Invalid result intent passed in. " +
                    "Missing account.");
        }

        keysStore.put(account, token);

        return account;
    }

}
