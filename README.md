kloudless-android
==============
# KloudlessAPI for android

You can sign up for a Kloudless Developer account at https://developers.kloudless.com.

Requirements
============
AndroidStudio or Eclipse
KloudlessSDK.jar

Installation
============

You'll need to manually install the following JARs:

* The Kloudless JAR from [S3](https://s3-us-west-2.amazonaws.com/kloudless-static-assets/p/platform/sdk/kloudless-java-0.1.1.jar)
* [Google Gson](http://code.google.com/p/google-gson/) from <http://google-gson.googlecode.com/files/google-gson-2.2.4-release.zip>.

Usage
=====
Add the following files to your project:

* KAuth.java
* AuthActivity.java

Look at the example project KTester

Testing
=======
See the KTester Sample Project


Documentation
=======
See the [Kloudless API Docs](https://developers.kloudless.com/docs) for the offi
cial reference.

You can obtain an App ID at the [Developer Portal](https://developers.kloudless
.com).

Here is a basic example of the most important methods in the Kloudless Android SDK.

Step 1. Modifying MainActivity with your App ID.

```java
// Insert your App ID from your Kloudless App Details.
String appId = "YOUR APP ID HERE";

// The KAuth object keeps track of all accounts and account keys per application.
KAuth auth = new KAuth(appId);

// Use a class instance for referencing the KAuth object.
KAuth.setSharedAuth(auth);
```

Step 2. Authenticate users and set the Kloudless Client with auth credentials.

```java
// Start the authentication from an Activity.
KAuth.getSharedAuth().startAuthentication(MainActivity.this);

...
// This will be called when authentication finishes
protected void onResume() {
    super.onResume();
    // grab the shared auth
    KAuth auth = KAuth.getSharedAuth();
    // if successful
    if (auth.authenticationSuccessful()) {

        // Mandatory call to complete the auth
        String accountId = auth.finishAuthentication();
        showToast("Added account: " + accountId);

        // initialize Kloudless API, can switch accountId + accountKey later
        String accountKey = (String) KAuth.keysStore.get(accountId);
        Kloudless.accountId = accountId;
        Kloudless.accountKey = accountKey;

    }
}
```

Step 3. Make a few API requests from the client.

```java
// Retrieve all the files/folders in an account at the root level.
private Context context;
private MetadataCollection contents;

String folderId = "root";
String accountId = "42";

contents = Folder.contents(folderId, accountId, null);
Log.i("getFolderContents", contents.toString());
```
