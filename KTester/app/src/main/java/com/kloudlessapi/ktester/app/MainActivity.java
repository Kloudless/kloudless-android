package com.kloudlessapi.ktester.app;

import android.content.Context;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Scanner;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import android.os.AsyncTask;

public class MainActivity extends ActionBarActivity {

    static Gson GSON = new GsonBuilder().create();

    private boolean mLoggedIn;

    // Android widgets
    private Button mSubmit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String appId = "BNXDcY40U_THkU18dAxcNyutPOyTqneNB23WdAaw8k53ovpt";
        KAuth auth = new KAuth(appId);
        KAuth.setSharedAuth(auth);

        mSubmit = (Button)findViewById(R.id.auth_button);

        mSubmit.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // This logs you out if you're logged in, or vice versa
                if (mLoggedIn) {
                    logOut();
                } else {
                    KAuth.getSharedAuth().startAuthentication(MainActivity.this);
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        KAuth auth = KAuth.getSharedAuth();

        // The next part must be inserted in the onResume() method of the
        // activity from which session.startAuthentication() was called, so
        // that Kloudless authentication completes properly.
        if (auth.authenticationSuccessful()) {
            try {
                // Mandatory call to complete the auth
                String accountId = auth.finishAuthentication();
                showToast("Added account: " + accountId);

                // initialize Kloudless API, can switch accountId + accountKey later
                String accountKey = (String) KAuth.keysStore.get(accountId);
                Kloudless.accountId = accountId;
                Kloudless.accountKey = accountKey;
            } catch (IllegalStateException e) {
                Log.i("Kloudless", "Couldn't authenticate with Kloudless:" + e.getLocalizedMessage());
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void logOut() {
        // Change UI state to display logged out version
        setLoggedIn(false);
    }

    /**
     * Convenience function to change UI state based on being logged in
     */
    private void setLoggedIn(boolean loggedIn) {
        mLoggedIn = loggedIn;
        if (loggedIn) {
            mSubmit.setText("Unlink from Kloudless");
        } else {
            mSubmit.setText("Link with Kloudless");
        }
    }

    private void showToast(String msg) {
        Toast error = Toast.makeText(this, msg, Toast.LENGTH_LONG);
        error.show();
    }

    /**
     *  Android Button Functions
     */
    // Begin Account Tests
    public void listAccounts(View view) {

        class AccountTask extends AsyncTask {
            private Context context;
            private AccountCollection accounts;

            public AccountTask(Context ctx) {
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
                Toast.makeText(context, "You have " + accounts.count.toString() + " accounts connected.",
                        Toast.LENGTH_LONG).show();
            }
        }

        new AccountTask(this).execute();
    }

    public void getAccountInfo(View view) throws KloudlessException {
        class AccountTask extends AsyncTask {
            private Context context;
            private Account account;

            public AccountTask(Context ctx) {
                this.context = ctx;
            }

            @Override
            protected Object doInBackground(Object... arg0) {

                try {
                    account = Account.retrieve("4", null);
                    Log.i("getAccountInfo", account.toString());
                } catch (KloudlessException e) {
                    Log.e("error", e.getMessage());
                }
                return null;
            }

            @Override
            protected void onPostExecute(Object o) {
                Toast.makeText(context, "Your account info: " + account.toString(),
                        Toast.LENGTH_LONG).show();
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

            public FolderTask(Context ctx) {
                this.context = ctx;
            }

            @Override
            protected Object doInBackground(Object... arg0) {

                try {
                    contents = Folder.contents("root", "4", null);
                    Log.i("getFolderContents", contents.toString());
                } catch (KloudlessException e) {
                    Log.e("error", e.getMessage());
                }
                return null;
            }

            @Override
            protected void onPostExecute(Object o) {
                Toast.makeText(context, "Folder has " + contents.count.toString() + " objects.",
                        Toast.LENGTH_LONG).show();
            }
        }

        new FolderTask(this).execute();
    }

    public void getFolderInfo(View view) throws KloudlessException {
        class FolderTask extends AsyncTask {
            private Context context;
            private Folder folder;

            public FolderTask(Context ctx) {
                this.context = ctx;
            }

            @Override
            protected Object doInBackground(Object... arg0) {

                try {
                    folder = Folder.retrieve("fL2E=", "4", null);
                    Log.i("getFolderInfo", folder.toString());
                } catch (KloudlessException e) {
                    Log.e("error", e.getMessage());
                }
                return null;
            }

            @Override
            protected void onPostExecute(Object o) {
                Toast.makeText(context, "Your folder Info " + folder.toString(),
                        Toast.LENGTH_LONG).show();
            }
        }

        new FolderTask(this).execute();
    }

    public void updateFolder(View view) throws KloudlessException {
        class FolderTask extends AsyncTask {
            private Context context;
            private Folder folder;

            public FolderTask(Context ctx) {
                this.context = ctx;
            }

            @Override
            protected Object doInBackground(Object... arg0) {

                try {
                    HashMap<String, Object> params = new HashMap<String, Object>();
                    params.put("name", "a");
                    folder = Folder.save("fL2E=", "4", params);
                    Log.i("updateFolder", folder.toString());
                } catch (KloudlessException e) {
                    Log.e("error", e.getMessage());
                }
                return null;
            }

            @Override
            protected void onPostExecute(Object o) {
                Toast.makeText(context, "Your folder Info " + folder.toString(),
                        Toast.LENGTH_LONG).show();
            }
        }

        new FolderTask(this).execute();
    }

    public void createFolder(View view) throws KloudlessException {
        class FolderTask extends AsyncTask {
            private Context context;
            private Folder folder;

            public FolderTask(Context ctx) {
                this.context = ctx;
            }

            @Override
            protected Object doInBackground(Object... arg0) {

                try {
                    HashMap<String, Object> params = new HashMap<String, Object>();
                    params.put("name", "new new folder");
                    params.put("parent_id", "root");
                    Folder folder = Folder.create("4", params);
                    Log.i("createFolder", folder.toString());
                } catch (KloudlessException e) {
                    Log.e("error", e.getMessage());
                }
                return null;
            }

            @Override
            protected void onPostExecute(Object o) {
                Toast.makeText(context, "Your folder Info " + folder.toString(),
                        Toast.LENGTH_LONG).show();
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

            public FileTask(Context ctx) {
                this.context = ctx;
            }

            @Override
            protected Object doInBackground(Object... arg0) {

                try {
                    KloudlessResponse response = File.contents("fL3N1cHBvcnQtc2FsZXNmb3JjZS5wbmc\u003d", "4", null);
                    contents = response.getResponseBody();
                    Log.i("downloadFile", contents);
                } catch (KloudlessException e) {
                    Log.e("error", e.getMessage());
                }
                return null;
            }

            @Override
            protected void onPostExecute(Object o) {
                Toast.makeText(context, "Your file contents: " + contents,
                        Toast.LENGTH_LONG).show();
            }
        }

        new FileTask(this).execute();
    }

    public void getFileInfo(View view) throws KloudlessException {
        class FileTask extends AsyncTask {
            private Context context;
            private File file;

            public FileTask(Context ctx) {
                this.context = ctx;
            }

            @Override
            protected Object doInBackground(Object... arg0) {

                try {
                    file = File.retrieve("fL3Rlc3QgKDE2KS50eHQ\u003d", "4", null);
                    Log.i("getFileInfo", file.toString());
                } catch (KloudlessException e) {
                    Log.e("error", e.getMessage());
                }
                return null;
            }

            @Override
            protected void onPostExecute(Object o) {
                Toast.makeText(context, "Your file Info " + file.toString(),
                        Toast.LENGTH_LONG).show();
            }
        }

        new FileTask(this).execute();
    }

    public void updateFile(View view) throws KloudlessException {
        class FileTask extends AsyncTask {
            private Context context;
            private File file;

            public FileTask(Context ctx) {
                this.context = ctx;
            }

            @Override
            protected Object doInBackground(Object... arg0) {

                try {
                    HashMap<String, Object> params = new HashMap<String, Object>();
                    params.put("name", "test (16).txt");
                    File file = File.save("fL3Rlc3QgKDE2KS50eHQ\u003d", "4", params);
                    Log.i("getFileInfo", file.toString());
                } catch (KloudlessException e) {
                    Log.e("error", e.getMessage());
                }
                return null;
            }

            @Override
            protected void onPostExecute(Object o) {
                Toast.makeText(context, "Your file Info " + file.toString(),
                        Toast.LENGTH_LONG).show();
            }
        }

        new FileTask(this).execute();
    }

    public void uploadFile(View view) throws KloudlessException, IOException {
        class FileTask extends AsyncTask {
            private Context context;
            private File file;

            public FileTask(Context ctx) {
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

                    File file = File.create("4", params);
                    Log.i("getFileInfo", file.toString());
                } catch (KloudlessException e) {
                    Log.e("error", e.getMessage());
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Object o) {
                Toast.makeText(context, "Your file Info " + file.toString(),
                        Toast.LENGTH_LONG).show();
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

            public LinkTask(Context ctx) {
                this.context = ctx;
            }

            @Override
            protected Object doInBackground(Object... arg0) {

                try {
                    links = Link.all("2", null);
                    Log.i("listLinks", links.toString());
                } catch (KloudlessException e) {
                    Log.e("error", e.getMessage());
                }
                return null;
            }

            @Override
            protected void onPostExecute(Object o) {
                Toast.makeText(context, "You have " + links.count.toString() + " links.",
                        Toast.LENGTH_LONG).show();
            }
        }

        new LinkTask(this).execute();
    }

    public void getLinkInfo(View view) throws KloudlessException {
        class LinkTask extends AsyncTask {
            private Context context;
            private Link link;

            public LinkTask(Context ctx) {
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
                Toast.makeText(context, "Your link info " + link.toString(),
                        Toast.LENGTH_LONG).show();
            }
        }

        new LinkTask(this).execute();
    }

    public void updateLink(View view) throws KloudlessException {
        class LinkTask extends AsyncTask {
            private Context context;
            private Link link;

            public LinkTask(Context ctx) {
                this.context = ctx;
            }

            @Override
            protected Object doInBackground(Object... arg0) {

                try {
                    HashMap<String, Object> params = new HashMap<String, Object>();
                    params.put("active", false);
                    Link link = Link.save("iywSjUZMos2_M_HTHpJU", "2", params);
                    Log.i("getLinkInfo", link.toString());
                } catch (KloudlessException e) {
                    Log.e("error", e.getMessage());
                }
                return null;
            }

            @Override
            protected void onPostExecute(Object o) {
                Toast.makeText(context, "Your link info " + link.toString(),
                        Toast.LENGTH_LONG).show();
            }
        }

        new LinkTask(this).execute();
    }

    public void createLink(View view) throws KloudlessException {
        class LinkTask extends AsyncTask {
            private Context context;
            private Link link;

            public LinkTask(Context ctx) {
                this.context = ctx;
            }

            @Override
            protected Object doInBackground(Object... arg0) {

                try {
                    HashMap<String, Object> params = new HashMap<String, Object>();
                    params.put("file_id", "fL3Rlc3QtZHJvcGJveC5wbmc\u003d");
                    Link link = Link.create("4", params);
                    Log.i("getLinkInfo", link.toString());
                } catch (KloudlessException e) {
                    Log.e("error", e.getMessage());
                }
                return null;
            }

            @Override
            protected void onPostExecute(Object o) {
                Toast.makeText(context, "Your link info " + link.toString(),
                        Toast.LENGTH_LONG).show();
            }
        }

        new LinkTask(this).execute();
    }

    public void deleteLink(View view) throws KloudlessException {
        // TODO: very simple, follow other file methods, but use delete
    }

}
