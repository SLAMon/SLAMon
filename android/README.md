# SLAMon Android Agent Service

## In general

Android Agent Service is a background service that ask for tasks from AFM and executes them.

## Add SLAMon as dependency

Currently the SLAMon project needs to be cloned and the project explicitly included. Using gradle the settings.gradle
would look like this

```gradle
rootProject.name = 'My SLAMon Project'

include ':android:slamon-android'
project(':android:slamon-android').projectDir = new File(settingsDir, 'path/to/SLAMon/android/slamon-android')

include ':java:slamon-lib'
project(':java:slamon-lib').projectDir = new File(settingsDir, 'path/to/SLAMon/java/slamon-lib')
```

## Required manifest tags

### For the Agent Service

The required use permissions from the application are the access to Internet for AFM connection,
and phone state reading for UUID generation.

```xml
<manifest>
    <!-- Agent Service permissions -->
    <uses-permission android:name="android.permission.INTERNET"/>
    <uses-permission android:name="android.permission.READ_PHONE_STATE"/>
</manifest>
```

### For the push notification receiving

Add necessary permissions to enable push notification receiving on the application. [YOUR PACKAGE] states the package
name such as "org.slamon.android".

```xml
<manifest package="[YOUR PACKAGE]">
    <!-- Push Notification permissions -->
    <uses-permission android:name="android.permission.INTERNET"/>
    <uses-permission android:name="android.permission.GET_ACCOUNTS"/>
    <uses-permission android:name="android.permission.WAKE_LOCK"/>
    <uses-permission android:name="com.google.android.c2dm.permission.RECEIVE"/>
    <permission android:name="[YOUR PACKAGE].permission.C2D_MESSAGE"
                android:protectionLevel="signature"/>
    <uses-permission android:name="[YOUR PACKAGE]"/>

    <application>
        <!-- The Google Cloud Messaging service -->
        <receiver android:name="org.jboss.aerogear.android.unifiedpush.gcm.AeroGearGCMMessageReceiver"
                  android:permission="com.google.android.c2dm.permission.SEND">
            <intent-filter>
                <action android:name="com.google.android.c2dm.intent.RECEIVE"/>
                <action android:name="com.google.android.c2dm.intent.REGISTRATION"/>
                <category android:name="[YOUR PACKAGE]"/>
            </intent-filter>
        </receiver>
    </application>
</manifest>
```

## Creating SLAMon application

The SlamonActivity visualizes the connection state and provides a part of the debug log from the AgentService that runs
in the background. To create the application, create a class extends the SlamonActivity and add appropriate parts.


### Starting and stopping the AgentService

You can create a basic application that visualises what happens in the AgentService by extending the SlamonActivity
base and starting your AgentService. If you are only looking for the visualization, remember to stop the service when
application is closed.

```java
public class MySlamonActivity extends SlamonActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); // Required to start Activity properly

        // Start your SLAMon Agent Service
        Intent intent = new Intent(this, MySLAMonService.class); // Service that extends SLAMonAndroidAgentService
        startService(intent);
    }

    @Override
    protected void onDestroy() {
        // Stop the SLAMonService
        Intent intent = new Intent(this, MySLAMonService.class);
        stopService(intent);
        super.onDestroy();
    }
}
```

### Receiving push notifications

You can allow the visualization application to also receive push notifications, such as test flow failed or succeeded,
from the AFM. The implementation uses JBoss Unified Push servers and libraries to handle push notifications.
Currently only one push server is used, but the server can send push notifications to multiple variant IDs to be
received.

To receive push notifications, the Android device needs to have Google Play Store installed, because the push
notifications are forwarded using its services. Google Play Services redirect the push notification to any registered
handlers. To use the example NotificationHandler that sends an intent broadcast as well as launches a notification,
add your activity class as its parameter to ensure proper functionality.

```java
public class MySlamonActivity extends SlamonActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); // Required to start Activity properly

        // Register to server to receive push notifications
        SLAMonPushNotificationManager.setup("YOUR_GCM_SENDER_ID", "https://your.jboss.unified.push.server",
                "YOUR_DEVICE_ALIAS",
                new MyNotificationHandler(MySlamonActivity.class) /** The push notification handler **/);
        // Add variant IDs and their secrets to receive push notifications from specific variant
        Map<String, String> variants = new HashMap<>();
        variants.put("VARIANT_ID", "SECRET");
        SLAMonPushNotificationManager.addVariantList(variants); // Add a map of variants
        SLAMonPushNotificationManager.addVariantId("VARIANT_ID2", "SECRET2"); // Or a single variant

        try {
            // Register to server
            SLAMonPushNotificationManager.registerToServer(this);
        } catch (RegistrationException e) {
            // Log and handle exception
        }
    }

    @Override
    protected void onDestroy() {
        // Unregister to stop receiving push notification when the application is not running
        SLAMonPushNotificationManager.unregisterFromServer(this);

        super.onDestroy();
    }
}
```

#### Handling push notifications

The notification handlers handle the messages into notifications, broadcasts, filter them out or whatever is needed.
The example implementation NotificationHandler sends a notification and also sends a broadcast to the application to
show in the debug window.

##### Custom push notification handlers
You can create a custom push notification handler to decide what to do with the received push notifications.
The custom handler should extend MessageHandler and implement the required methods.

```java
public class MyNotificationHandler extends MessageHandler {
    @Override
    public void onMessage(Context context, Bundle bundle) {
        // An example handling of a message by creating a notification of every received push notification
        String message = bundle.getString("alert");

        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.slamon_icon)
                .setContentTitle("SLAMon Notifications")
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setContentText(message);

        Notification notification = builder.build();

        // Send a notification with specific ID
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    @Override
    public void onDeleteMessage(Context context, Bundle bundle) {
        // handle GoogleCloudMessaging.MESSAGE_TYPE_DELETED
    }

    @Override
    public void onError() {
        // handle GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR
    }
}
```

## Customising the Agent Service

To customise the Agent Service, extend the SLAMonAndroidAgentService and override onStartCommand method with additional
information such as Agent Name, AFM URL and additional task handlers. Remember to call the superclass's
onStartCommand as that is what starts the Agent and the Service.

```java
public class CustomAgentService extends SLAMonAndroidAgentService {
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        this.addHandlers("my.handler.package.path"); // Add your TaskHandlers in a package
        this.setup("http://afm.url.to.be", "My Agent v. 3"); // Has to be set or exception is thrown
        return super.onStartCommand(intent, flags, startId);
    }
}
```

## AgentService's TaskHandlers

The Agent's core takes care of the Agent-AFM communication, so TaskHandlers need only to focus on executing
the given task given by the AFM.

### Developing custom TaskHandlers

Custom handlers extend TaskHandler class, and implement the required methods. Execute method is the work part that
takes the input parameters Map, works with them, and returns the results Map that is to be sent to the AFM.
GetName and getVersion define the task type and task version the Agent is capable of performing. Android services and
contexts are enabled by adding a public constructor that has a Context parameter.

```java
package org.slamon.handlers;

public class WaitTaskHandler extends TaskHandler {

    private Context context;

    // Add public constructor with only android.Context as parameter to enable Android Context in the handler
    public WaitTaskHandler(Context context) {
        this.context = context;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> inputParams) throws Exception {
        Double waitTime = (Double) inputParams.get("time");
        long time = new Date().getTime();
        Thread.sleep(waitTime.intValue() * 1000);
        Map<String, Object> ret = new HashMap<String, Object>();
        ret.put("waited", new Float(new Date().getTime() - time));
        return ret;
    }

    @Override
    public String getName() {
        return "android-wait";
    }

    @Override
    public int getVersion() {
        return 1;
    }
}
```