package org.slamon;

import com.google.api.client.util.Key;

import java.util.HashMap;
import java.util.Map;

/**
 * A data transfer object for sending push notification via JBoss Unified Push Server.
 */
public class PushNotification {

    @Key
    public String[] variants;

    @Key
    public Map<String, Object> message;

    public PushNotification(String masterVariantId, String variantId, String alert, String sound) {

        // Do not add a Variants field when target is "all"
        // JBoss Unified Push will send notification to all variant IDs when the field does not exists
        if (!variantId.equalsIgnoreCase("all")) {

            // masterVariantId is the JBoss Unified Push variant ID
            // where SLAMon will send a copy of the notification to
            // (e.g. variant ID used by the service operation center)
            variants = new String[] {masterVariantId, variantId};
        }

        message = new HashMap<String, Object>();
        message.put("alert", alert);
        message.put("sound", sound);
    }
}
