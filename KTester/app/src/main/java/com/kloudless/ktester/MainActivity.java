package com.kloudless.ktester;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.customtabs.CustomTabsIntent;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kloudless.Kloudless;
import com.kloudless.exception.KloudlessException;
import com.kloudless.model.Account;
import com.kloudless.model.AccountCollection;
import com.kloudless.model.File;
import com.kloudless.model.Folder;
import com.kloudless.model.Link;
import com.kloudless.model.LinkCollection;
import com.kloudless.model.MetadataCollection;
import com.kloudless.net.KloudlessResponse;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.HashMap;
import java.util.Scanner;


public class MainActivity extends AppCompatActivity {

    static Gson GSON = new GsonBuilder().create();

    // If you want to test, please use your own folder and file ids.
    public String packageName = BuildConfig.APPLICATION_ID;
    public String packageSuffix = packageName.substring(packageName.lastIndexOf(".") + 1);
    public String redirect_uri = String.format("%s://kloudless/callback", packageSuffix);
    public String clientId = "Am3oC0zHwvFc5zPNZk7lku98jGlhRWbjSiAnsc7pUYApaaU3";
    public String folderId = "root";
    public String fileId = "fL3N1cHBvcnQtc2FsZXNmb3JjZS5wbmc=";
    public String linkId = "iywSjUZMos2_M_HTHpJU";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create the UI
        final Button button = (Button) findViewById(R.id.button);
        button.setOnClickListener(v -> {
            CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
            CustomTabsIntent customTabsIntent = builder.build();

            // Code here executes on main thread after user presses button
            String urlString = String.format("%s/v%s/oauth/?client_id=%s&redirect_uri=%s&response_type=token&state=12345",
                Kloudless.BASE_URL, Kloudless.apiVersion, clientId, redirect_uri);
            Uri url = Uri.parse(urlString);
            customTabsIntent.launchUrl(MainActivity.this, url);
        });

        // Capture Intent
        Intent intent = getIntent();
        String action = intent.getAction();
        String link = intent.getDataString();

        if (Intent.ACTION_VIEW.equals(action)) {
            Uri url = Uri.parse(link);
            String fragment = url.getFragment();
            HashMap<String, String> data = new HashMap<>();
            for (String frag : fragment.split("&")) {
                String[] map = frag.split("=");
                data.put(map[0], map[1]);
            }

            Kloudless.bearerToken = data.get("access_token");
            verifyToken(Kloudless.bearerToken);
        }
    }

    public void verifyToken(String bearerToken) {
        class TokenTask extends AsyncTask {
            private Context context;

            private TokenTask(Context ctx) {
                this.context = ctx;
            }

            @Override
            protected Object doInBackground(Object... arg0) {

                try {
                    String tokenUrl = String.format("%s/v%s/oauth/token", Kloudless.BASE_URL, Kloudless.apiVersion);

                    java.net.URL obj = new URL(tokenUrl);
                    java.net.HttpURLConnection conn = (java.net.HttpURLConnection) obj.openConnection();
                    String authorization = String.format("Bearer %s", bearerToken);
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
                    JSONObject responseJson = new JSONObject(responseString);
                    Kloudless.accountId = responseJson.getString("account_id");

                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }
                return Kloudless.accountId;
            }
        }
        new TokenTask(this).execute();
    }

    /**
     *  Android Button Functions
     */
    // Begin Account Tests
    public void listAccounts(View view) {

        class AccountTask extends AsyncTask {
            private Context context;
            private AccountCollection accounts;

            private AccountTask(Context ctx) {
                this.context = ctx;
            }

            @Override
            protected Object doInBackground(Object... arg0) {

                try {
                    accounts = Account.all(null);
                    Log.i("listAccounts", accounts.toString());
                } catch (KloudlessException e) {
                    Log.e("error", e.getMessage());
                }
                return null;
            }

            @Override
            protected void onPostExecute(Object o) {
                if (accounts != null) {
                    Toast.makeText(context, "You have " + accounts.count.toString() + " accounts connected.",
                            Toast.LENGTH_LONG).show();
                }
            }
        }

        new AccountTask(this).execute();
    }

    public void getAccountInfo(View view) throws KloudlessException {
        class AccountTask extends AsyncTask {
            private Context context;
            private Account account;

            private AccountTask(Context ctx) {
                this.context = ctx;
            }

            @Override
            protected Object doInBackground(Object... arg0) {

                try {
                    account = Account.retrieve(Kloudless.accountId, null);
                    Log.i("getAccountInfo", account.toString());
                } catch (KloudlessException e) {
                    Log.e("error", e.getMessage());
                }
                return null;
            }

            @Override
            protected void onPostExecute(Object o) {
                if (account != null) {
                    Toast.makeText(context, "Your account info: " + account.toString(),
                            Toast.LENGTH_LONG).show();
                }
            }
        }

        new AccountTask(this).execute();
    }

    public void deleteAccount(View view) throws KloudlessException {
        // TODO: very simple, follow other account methods, but use delete
    }

    // Begin Folder Tests
    public void getFolderContents(View view) throws KloudlessException {
        class FolderTask extends AsyncTask {
            private Context context;
            private MetadataCollection contents;

            private FolderTask(Context ctx) {
                this.context = ctx;
            }

            @Override
            protected Object doInBackground(Object... arg0) {

                try {
                    System.out.println(String.format("Bearer Token: %s", Kloudless.bearerToken));
                    contents = Folder.contents("root", Kloudless.accountId, null);
                    Log.i("getFolderContents", contents.toString());
                } catch (KloudlessException e) {
                    Log.e("error", e.getMessage());
                }
                return null;
            }

            @Override
            protected void onPostExecute(Object o) {
                if (contents != null) {
                    Toast.makeText(context, "Folder has " + contents.count.toString() + " objects.",
                            Toast.LENGTH_LONG).show();
                }
            }
        }

        new FolderTask(this).execute();
    }

    public void getFolderInfo(View view) throws KloudlessException {
        class FolderTask extends AsyncTask {
            private Context context;
            private Folder folder;

            private FolderTask(Context ctx) {
                this.context = ctx;
            }

            @Override
            protected Object doInBackground(Object... arg0) {

                try {
                    folder = Folder.retrieve(folderId, Kloudless.accountId, null);
                    Log.i("getFolderInfo", folder.toString());
                } catch (KloudlessException e) {
                    Log.e("error", e.getMessage());
                }
                return null;
            }

            @Override
            protected void onPostExecute(Object o) {
                if (folder != null) {
                    Toast.makeText(context, "Your folder Info " + folder.toString(),
                            Toast.LENGTH_LONG).show();
                }
            }
        }

        new FolderTask(this).execute();
    }

    public void updateFolder(View view) throws KloudlessException {
        class FolderTask extends AsyncTask {
            private Context context;
            private Folder folder;

            private FolderTask(Context ctx) {
                this.context = ctx;
            }

            @Override
            protected Object doInBackground(Object... arg0) {

                try {
                    HashMap<String, Object> params = new HashMap<String, Object>();
                    params.put("name", "a");
                    folder = Folder.save(folderId, Kloudless.accountId, params);
                    Log.i("updateFolder", folder.toString());
                } catch (KloudlessException e) {
                    Log.e("error", e.getMessage());
                }
                return null;
            }

            @Override
            protected void onPostExecute(Object o) {
                if (folder != null) {
                    Toast.makeText(context, "Your folder Info " + folder.toString(),
                            Toast.LENGTH_LONG).show();
                }
            }
        }

        new FolderTask(this).execute();
    }

    public void createFolder(View view) throws KloudlessException {
        class FolderTask extends AsyncTask {
            private Context context;
            private Folder folder;

            private FolderTask(Context ctx) {
                this.context = ctx;
            }

            @Override
            protected Object doInBackground(Object... arg0) {

                try {
                    HashMap<String, Object> params = new HashMap<String, Object>();
                    params.put("name", "new new folder");
                    params.put("parent_id", "root");
                    folder = Folder.create(Kloudless.accountId, params);
                    Log.i("createFolder", folder.toString());
                } catch (KloudlessException e) {
                    Log.e("error", e.getMessage());
                }
                return null;
            }

            @Override
            protected void onPostExecute(Object o) {
                if (folder != null) {
                    Toast.makeText(context, "Your folder Info " + folder.toString(),
                            Toast.LENGTH_LONG).show();
                }
            }
        }

        new FolderTask(this).execute();
    }

    public void deleteFolder(View view) throws KloudlessException {
        // TODO: very simple, follow other folder methods, but use delete
    }


    // Begin File Tests
    public void downloadFile(View view) throws KloudlessException, IOException {
        class FileTask extends AsyncTask {
            private Context context;
            private String contents;

            private FileTask(Context ctx) {
                this.context = ctx;
            }

            @Override
            protected Object doInBackground(Object... arg0) {

                try {
                    KloudlessResponse response = File.contents(fileId, Kloudless.accountId, null);
                    contents = response.getResponseBody();
                    Log.i("downloadFile", contents);
                } catch (KloudlessException e) {
                    Log.e("error", e.getMessage());
                }
                return null;
            }

            @Override
            protected void onPostExecute(Object o) {
                if (contents != null) {
                    Toast.makeText(context, "Your file contents: " + contents,
                            Toast.LENGTH_LONG).show();
                }
            }
        }

        new FileTask(this).execute();
    }

    public void getFileInfo(View view) throws KloudlessException {
        class FileTask extends AsyncTask {
            private Context context;
            private File file;

            private FileTask(Context ctx) {
                this.context = ctx;
            }

            @Override
            protected Object doInBackground(Object... arg0) {

                try {
                    file = File.retrieve(fileId, Kloudless.accountId, null);
                    Log.i("getFileInfo", file.toString());
                } catch (KloudlessException e) {
                    Log.e("error", e.getMessage());
                }
                return null;
            }

            @Override
            protected void onPostExecute(Object o) {
                if (file != null) {
                    Toast.makeText(context, "Your file Info " + file.toString(),
                            Toast.LENGTH_LONG).show();
                }
            }
        }

        new FileTask(this).execute();
    }

    public void updateFile(View view) throws KloudlessException {
        class FileTask extends AsyncTask {
            private Context context;
            private File file;

            private FileTask(Context ctx) {
                this.context = ctx;
            }

            @Override
            protected Object doInBackground(Object... arg0) {

                try {
                    HashMap<String, Object> params = new HashMap<>();
                    params.put("name", "test (16).txt");
                    file = File.save(fileId, Kloudless.accountId, params);
                    Log.i("getFileInfo", file.toString());
                } catch (KloudlessException e) {
                    Log.e("error", e.getMessage());
                }
                return null;
            }

            @Override
            protected void onPostExecute(Object o) {
                if (file != null) {
                    Toast.makeText(context, "Your file Info " + file.toString(),
                            Toast.LENGTH_LONG).show();
                }
            }
        }

        new FileTask(this).execute();
    }

    public void uploadFile(View view) throws KloudlessException, IOException {
        class FileTask extends AsyncTask {
            private Context context;
            private File file;

            private FileTask(Context ctx) {
                this.context = ctx;
            }

            @Override
            protected Object doInBackground(Object... arg0) {

                try {
                    String text = "Hello, World!";
                    String path = "/tmp/new.txt";
                    PrintWriter writer = new PrintWriter(path, "UTF-8");
                    writer.println(text);
                    writer.close();
                    java.io.File f = new java.io.File(path);
                    Scanner scanner = new Scanner(f);
                    String contents = scanner.next();
                    scanner.close();

                    HashMap<String, Object> params = new HashMap<String, Object>();
                    HashMap<String, Object> metadata = new HashMap<String, Object>();
                    metadata.put("name",  "testtesttest.txt");
                    metadata.put("parent_id", "root");
                    params.put("metadata", GSON.toJson(metadata));
                    params.put("file", contents.getBytes());

                    System.out.println(params);

                    file = File.create(Kloudless.accountId, params);
                    Log.i("getFileInfo", file.toString());
                } catch (KloudlessException e) {
                    Log.e("error", e.getMessage());
                } catch (FileNotFoundException | UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Object o) {
                if (file != null) {
                    Toast.makeText(context, "Your file Info " + file.toString(),
                            Toast.LENGTH_LONG).show();
                }
            }
        }

        new FileTask(this).execute();
    }

    public void deleteFile(View view) throws KloudlessException {
        // TODO: very simple, follow other file methods, but use delete
    }

    // Begin Link Tests
    public void listLinks(View view) throws KloudlessException {
        class LinkTask extends AsyncTask {
            private Context context;
            private LinkCollection links;

            private LinkTask(Context ctx) {
                this.context = ctx;
            }

            @Override
            protected Object doInBackground(Object... arg0) {

                try {
                    links = Link.all(Kloudless.accountId, null);
                    Log.i("listLinks", links.toString());
                } catch (KloudlessException e) {
                    Log.e("error", e.getMessage());
                }
                return null;
            }

            @Override
            protected void onPostExecute(Object o) {
                if (links != null) {
                    Toast.makeText(context, "You have " + links.count.toString() + " links.",
                            Toast.LENGTH_LONG).show();
                }
            }
        }

        new LinkTask(this).execute();
    }

    public void getLinkInfo(View view) throws KloudlessException {
        class LinkTask extends AsyncTask {
            private Context context;
            private Link link;

            private LinkTask(Context ctx) {
                this.context = ctx;
            }

            @Override
            protected Object doInBackground(Object... arg0) {

                try {
                    link = Link.retrieve("iywSjUZMos2_M_HTHpJU", "2", null);
                    Log.i("getLinkInfo", link.toString());
                } catch (KloudlessException e) {
                    Log.e("error", e.getMessage());
                }
                return null;
            }

            @Override
            protected void onPostExecute(Object o) {
                if (link != null) {
                    Toast.makeText(context, "Your link info " + link.toString(),
                            Toast.LENGTH_LONG).show();
                }
            }
        }

        new LinkTask(this).execute();
    }

    public void updateLink(View view) throws KloudlessException {
        class LinkTask extends AsyncTask {
            private Context context;
            private Link link;

            private LinkTask(Context ctx) {
                this.context = ctx;
            }

            @Override
            protected Object doInBackground(Object... arg0) {

                try {
                    HashMap<String, Object> params = new HashMap<>();
                    params.put("active", false);
                    link = Link.save(linkId, Kloudless.accountId, params);
                    Log.i("getLinkInfo", link.toString());
                } catch (KloudlessException e) {
                    Log.e("error", e.getMessage());
                }
                return null;
            }

            @Override
            protected void onPostExecute(Object o) {
                if (link != null) {
                    Toast.makeText(context, "Your link info " + link.toString(),
                            Toast.LENGTH_LONG).show();
                }
            }
        }

        new LinkTask(this).execute();
    }

    public void createLink(View view) throws KloudlessException {
        class LinkTask extends AsyncTask {
            private Context context;
            private Link link;

            private LinkTask(Context ctx) {
                this.context = ctx;
            }

            @Override
            protected Object doInBackground(Object... arg0) {

                try {
                    HashMap<String, Object> params = new HashMap<>();
                    params.put("file_id", fileId);
                    link = Link.create(Kloudless.accountId, params);
                    Log.i("getLinkInfo", link.toString());
                } catch (KloudlessException e) {
                    Log.e("error", e.getMessage());
                }
                return null;
            }

            @Override
            protected void onPostExecute(Object o) {
                if (link != null) {
                    Toast.makeText(context, "Your link info " + link.toString(),
                            Toast.LENGTH_LONG).show();
                }
            }
        }

        new LinkTask(this).execute();
    }

    public void deleteLink(View view) throws KloudlessException {
        // TODO: very simple, follow other file methods, but use delete
    }
}