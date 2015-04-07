package org.slamon.android;

import android.content.Context;
import android.os.Bundle;
import org.jboss.aerogear.android.unifiedpush.MessageHandler;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Tests for registering for, receiving and handling push notifications
 */

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class PushNotificationTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void testNoGcmId() throws Exception {
        SLAMonPushNotificationManager.setup(null, "http://server.url/", "", new TestNotificationHandler());
        expectedException.expect(RegistrationException.class);
        expectedException.expectMessage("Google Cloud Messaging ID");
        SLAMonPushNotificationManager.registerToServer(Robolectric.application.getApplicationContext());
    }

    @Test
    public void testNoServerURL() throws Exception {
        SLAMonPushNotificationManager.setup("", null, "", new TestNotificationHandler());
        expectedException.expect(RegistrationException.class);
        expectedException.expectMessage("Push Notification Server URL");
        SLAMonPushNotificationManager.registerToServer(Robolectric.application.getApplicationContext());
    }

    @Test
    public void testNoMessageHandler() throws Exception {
        SLAMonPushNotificationManager.setup("1234567890", "http://server.url/", "", null);
        expectedException.expect(RegistrationException.class);
        expectedException.expectMessage("message handler was not set");
        SLAMonPushNotificationManager.registerToServer(Robolectric.application.getApplicationContext());
    }

    @Test
    public void testProperConfiguration() throws Exception {
        SLAMonPushNotificationManager.setup("1234567890", "http://server.url/", "", new TestNotificationHandler());
        SLAMonPushNotificationManager.registerToServer(Robolectric.application.getApplicationContext());
    }

    class TestNotificationHandler implements MessageHandler {
        @Override
        public void onMessage(Context context, Bundle bundle) {

        }

        @Override
        public void onDeleteMessage(Context context, Bundle bundle) {

        }

        @Override
        public void onError() {

        }
    }
}
