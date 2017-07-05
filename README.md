Android SDK for the Kloudless API
==============

Android SDK for the [Kloudless API](https://developers.kloudless.com)

## Getting Started Using the Kloudless Java SDK for Android

Requirements:

1. AndroidStudio 2.3.3 or equivalent
2. You need to have registered as a Kloudless app at
   https://developers.kloudless.com. You should have an App key and API Key.

Note: The SDK is designed to work with Java 1.8 or above.

## Building and using the example app

1. Open the project KTester in Android Studio
2. Fill in the values for fileId and linkId
3. Build and Run the MainActivity.java
4. Once running, you can test the functionalities of the API without errors.

## Authorizing Users without the example app

1. The example app uses android.support.customtabs.CustomTabsIntent to handle
the OAuth 2.0 flow to Kloudless and stores the access token and account id at
Kloudless.bearerToken and Kloudless.accountId.
2. To authorize users without the example app, first register a Redirect URI
scheme in the Kloudless developer portal `App Details` page (e.g. 
ktester://kloudless/callback).
3. Add an Intent Filter to the AndroidManifest.xml that matches the Redirect
URI scheme (e.g. `<data android:scheme="ktester" android:host="kloudless" />`)
4. Modify the `redirect_uri` variable to match. (we use the package suffix)
5. Initialize your own CustomTabsIntent Builder and launch the authorization
url with your own button action.
6. Make an API call to verify the access token returned by the Intent filter
and retrieve the account ID.
7. You can now successfully make requests to the Kloudless API with the bearer
token and account ID.

Note: You will need to add `com.android.support:customtabs:23.2.0` to your
module's build.gradle file and add the `android.permission.INTERNET` to your
`AndroidManifest.xml` file.

## Adding the Kloudless Java SDK to an Android Application (using maven).

1. Verify that Maven is a valid repository in your Project's build.gradle
2. Add `com.kloudless:kloudless-java:1.0.3` to your module's build.gradle file.
3. Under `compileOptions` verify `sourceCompatibility` and `targetCompatibility`
as `JavaVersion.VERSION_1_8`.
4. Under `packagingOptions` exclude `META-INF/LICENSE`
5. Now you can import `com.kloudless.Kloudless` to any file!

Documentation
=======
See the [Kloudless API Docs](https://developers.kloudless.com/docs) for the 
official documentation.

You can obtain an App ID at the 
[Developer Portal](https://developers.kloudless.com).

Step 1. Click the Link Account button to connect an account.

Step 2. Click the additional buttons to test Kloudless API requests.

To make your own requests with the Java SDK you will need to create an
AsyncTask as seen in MainActivity.java.

To authenticate with your own Application ID or Client ID please follow
the steps in Authorizing Users without the example app.

## UPDATES

* 2017/07 - updated SDK to use CustomTabsIntent removed AuthActivity
* 2016/08 - updated SDK to Kloudless v1
* 2014/10 - updated SDK with new methods, modified AuthActivity
* 2014/04 - added initial Example project

## TODO

* more tests
* add multiple account id / access token management
* add additional examples
