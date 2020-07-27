Android SDK for the Kloudless API
==============

Android SDK for the [Kloudless API](https://developers.kloudless.com)

## Getting Started Using the Kloudless Java SDK for Android

Requirements:

1. AndroidStudio 4.0.1 or equivalent
2. You need to have registered as a Kloudless app at
   https://developers.kloudless.com. You should have an App key and API Key.

Note: The SDK is designed to work with Java 1.8 or above.

## Building and using the example app

1. Open the project KTester in Android Studio
2. Fill in the values for fileId and linkId
3. Build and Run the MainActivity.java
4. Once running, you can test the functionalities of the API without errors.

## Authorizing users with your own Android app

Copy this code from the example app from lines 46 through 131 into your
application to mirror how this example app authorizes users:
https://github.com/Kloudless/kloudless-android/blob/master/KTester/app/src/main/java/com/kloudless/ktester/MainActivity.java#L46

You can add the code to your own activity rather than the `onCreate` of
a separate activity. 

Then, make the following changes:

1. The example app uses android.support.customtabs.CustomTabsIntent to handle
the OAuth 2.0 flow to Kloudless and stores the access token and account ID at
Kloudless.bearerToken and Kloudless.accountId.
2. To authorize users from your own app, first register a Redirect URI
scheme in the Kloudless developer portal `App Details` page (e.g. 
ktester://kloudless/callback), replacing `ktester` with your app's package suffix
as shown [here](https://github.com/Kloudless/kloudless-android/blob/master/KTester/app/src/main/java/com/kloudless/ktester/MainActivity.java#L49),
or any other scheme.
3. Add an Intent Filter to the AndroidManifest.xml that matches the Redirect
URI scheme, similar to the one the example app sets up
[here](https://github.com/Kloudless/kloudless-android/blob/master/KTester/app/src/main/AndroidManifest.xml#L20).
4. Initialize your own CustomTabsIntent Builder and launch the authorization
URL with your own button action, as shown in the example code in
[MainActivity](https://github.com/Kloudless/kloudless-android/blob/master/KTester/app/src/main/java/com/kloudless/ktester/MainActivity.java#L70).
5. Once your app retrieves the token, make an API call to verify it
and retrieve the account ID as shown in the
[example app](https://github.com/Kloudless/kloudless-android/blob/master/KTester/app/src/main/java/com/kloudless/ktester/MainActivity.java#L92).
6. You can now successfully make requests to the Kloudless API with the bearer
token and account ID.

Note: You will need to add `androidx.browser:browser:1.2.0` to your
module's build.gradle file and add the `android.permission.INTERNET` to your
`AndroidManifest.xml` file.

## Adding the Kloudless Java SDK to an Android Application (using maven).

1. Verify that Maven is a valid repository in your Project's build.gradle
2. Add `com.kloudless:kloudless-java:1.0.4` to your module's build.gradle file.
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

To authenticate with your own Application ID or Client ID, please follow
the steps above.
