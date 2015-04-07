package org.slamon.android;

import android.content.Context;
import android.util.Log;
import org.jboss.aerogear.android.core.Callback;
import org.jboss.aerogear.android.unifiedpush.MessageHandler;
import org.jboss.aerogear.android.unifiedpush.PushRegistrar;
import org.jboss.aerogear.android.unifiedpush.RegistrarManager;
import org.jboss.aerogear.android.unifiedpush.gcm.AeroGearGCMPushConfiguration;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

/**
 * Class that handles registration to JBoss Unified Push Server
 */
public class SLAMonPushNotificationManager {
    private static final String TAG = SLAMonPushNotificationManager.class.getCanonicalName();
    private static Map<String, String> variants = new HashMap<>(); // Variant ID -> Secret
    private static String GCM_SENDER_ID;
    private static String UNIFIED_PUSH_URL;
    private static String ALIAS;
    private static MessageHandler messageHandler;

    /**
     * Required setup to connect to correct Push Server
     *
     * @param gcmSenderId       Google Cloud Messaging account ID
     * @param unifiedPushUrl    JBoss Unified Push Server URL
     * @param alias             Device alias
     * @param handler           Push notification handler
     */
    public static void setup(String gcmSenderId, String unifiedPushUrl, String alias, MessageHandler handler) {
        GCM_SENDER_ID = gcmSenderId;
        UNIFIED_PUSH_URL = unifiedPushUrl;
        ALIAS = alias;
        messageHandler = handler;
    }

    public static void setMessageHandler(MessageHandler messageHandler) {
        SLAMonPushNotificationManager.messageHandler = messageHandler;
    }

    /**
     * Add variant ID and its secret of the requested service from which to receive push notifications
     *
     * @param variantId         Desired application variant's id
     * @param secret            Variant id's secret
     */
    public static void addVariantId(String variantId, String secret) {
        variants.put(variantId, secret);
    }

    /**
     * Add map of variant IDs and associated secrets of the requested services from which to receive push notifications
     *
     * @param variantList       Map containing variant id and secret pairs
     */
    public static void addVariantList(Map<String, String> variantList) {
        variants.putAll(variantList);
    }

    /**
     * Register to all variant IDs
     *
     * @param context
     * @throws RegistrationException
     */
    public static void registerToServer(Context context) throws RegistrationException {
        if (GCM_SENDER_ID == null) {
            throw new RegistrationException("Configuration parameter Google Cloud Messaging ID was missing");
        } else if (UNIFIED_PUSH_URL == null) {
            throw new RegistrationException("Configuration parameter Push Notification Server URL was missing");
        } else if (messageHandler == null) {
            throw new RegistrationException("Push notification message handler was not set");
        } else {
            Log.i(TAG, "Starting registering to JUPS for " + variants.size() + " variant IDs");
            for (Map.Entry<String, String> entry : variants.entrySet()) {
                try {
                    String configName = "SLAMonRegistrar_" + entry.getKey();
                    RegistrarManager.config(configName, AeroGearGCMPushConfiguration.class)
                            .setPushServerURI(new URI(UNIFIED_PUSH_URL))
                            .setSenderIds(GCM_SENDER_ID)
                            .setAlias(ALIAS)
                            .setVariantID(entry.getKey())
                            .setSecret(entry.getValue())
                            .asRegistrar();
                    PushRegistrar registrar = RegistrarManager.getRegistrar(configName);
                    Log.d(TAG, "JUPS config ready with name " + configName);

                    registerToJUPS(context, registrar);
                } catch (URISyntaxException e) {
                    Log.w(TAG, "JUPS URI was in incorrect format");
                    throw new RegistrationException("JUPS URI was in incorrect format: " + e.getMessage());
                }
            }

        }
    }

    /**
     * Unregister from the JBoss Unified Push Server to stop receiving push notifications
     *
     * @param context
     */
    public static void unregisterFromServer(Context context) {
        unregisterMessageHandler();
        for (Map.Entry<String, String> entry : variants.entrySet()) {
            String configName = "SLAMonRegistrar_" + entry.getKey();
            unregisterFromJUPS(context, configName);
        }
    }

    /**
     * Register message handler to handle received push notifications
     */
    public static void registerMessageHandler() {
        RegistrarManager.registerBackgroundThreadHandler(messageHandler);
    }

    /**
     * Unregister message handler to stop handling of received push notifications
     */
    public static void unregisterMessageHandler() {
        RegistrarManager.unregisterBackgroundThreadHandler(messageHandler);
    }

    /**
     * Register PushRegistrar to server to receive push notifications from the specified service
     *
     * @param context   Application context
     * @param registrar PushRegistrar configured for specific application variant
     * @throws RegistrationException
     */
    private static void registerToJUPS(final Context context, final PushRegistrar registrar)
            throws RegistrationException {
        registrar.register(context, new Callback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.i(TAG, "JUPS registration successful");
                RegistrarManager.registerBackgroundThreadHandler(messageHandler);
            }

            @Override
            public void onFailure(Exception e) {
                Log.w(TAG, "Registration to JUPS failed: " + e);
            }
        });
    }

    /**
     * Unregister from receiving push notifications from JBoss Unified Push Server for specified application variant
     *
     * @param context    Application context
     * @param configName Variant config name to unregister
     */
    private static void unregisterFromJUPS(final Context context, final String configName) {
        PushRegistrar registrar = RegistrarManager.getRegistrar(configName);
        registrar.unregister(context, new Callback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.i(TAG, "Successfully unregistered from JUPS");
                RegistrarManager.unregisterBackgroundThreadHandler(messageHandler);
            }

            @Override
            public void onFailure(Exception e) {
                Log.i(TAG, "Failed to unregister from JUPS: " + e);
            }
        });
    }
}
