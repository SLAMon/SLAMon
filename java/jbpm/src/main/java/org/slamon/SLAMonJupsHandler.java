package org.slamon;

import org.drools.core.process.instance.impl.WorkItemImpl;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.api.runtime.process.WorkItemManager;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;


/**
 * A custom WorkItemHandler implementation to send Push Notifications via JBoss Unified Push Server
 */
public class SLAMonJupsHandler implements WorkItemHandler {

    static final Logger log = Logger.getLogger(Jups.class.getCanonicalName());

    private String mUrl = null;
    private String mAppId = null;
    private String mSecret = null;
    private String mMasterVariantId = null;

    /**
     * Construct SLAMonJupsHandler with the following:
     *   JBoss Unified Push Server URL.
     *   Application Id
     *   Master Secret
     *   Variant Id that will receive all notifications regardless of the target
     */  
    public SLAMonJupsHandler(String url, String appId, String secret, String masterVariantId) {
        mUrl = url;
        mAppId = appId;
        mSecret = secret;
        mMasterVariantId = masterVariantId;
    }

    public void executeWorkItem(final WorkItem workItem, final WorkItemManager manager) {

        String url = mUrl;
        String appId = mAppId;
        String secret = mSecret;
        String masterVariantId = mMasterVariantId;

        String variantId = String.valueOf(workItem.getParameter("variant_id"));
        String alert = String.valueOf(workItem.getParameter("alert"));
        String sound = String.valueOf(workItem.getParameter("sound"));

        String testId = null;

        final String deploymentId = ((WorkItemImpl) workItem).getDeploymentId();

        EngineHolder engine = null;
        try {
            engine = new EngineHolder(deploymentId);
            testId = Util.processId(engine.getEngine(), workItem);
        } finally {
            if (engine != null) {
                engine.close();
            }
        }

        PushNotification notification;
        notification = new PushNotification(masterVariantId, variantId, alert, sound);        
        
        Jups jups;
        jups = new Jups(url, appId, secret);

        String result = jups.send(notification, testId);

        Map<String, Object> results = new HashMap<String, Object>();
        results.put("result", result);
        manager.completeWorkItem(workItem.getId(), results);
    }

    public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
        manager.abortWorkItem(workItem.getId());
    }
}
