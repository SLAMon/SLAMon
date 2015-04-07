package org.slamon;

import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.internal.runtime.manager.RuntimeManagerRegistry;
import org.kie.internal.runtime.manager.context.EmptyContext;

/**
 * Simple RAII -style helper to obtain and release RuntimeEngine.
 * With Java 1.7 this could be used as follows:
 * <pre>
 * try (EngineHolder engine = new EngineHolder(deploymentId)) {
 *     do stuff with engine
 * }
 * </pre>
 */
public class EngineHolder implements java.io.Closeable {

    private RuntimeManager mManager;
    private RuntimeEngine mEngine;

    public EngineHolder(String deploymentId) {
        mManager = RuntimeManagerRegistry.get().getManager(deploymentId);
        mEngine = mManager.getRuntimeEngine(EmptyContext.get());
    }

    @Override
    public void close() {
        mManager.disposeRuntimeEngine(mEngine);
    }

    RuntimeEngine getEngine() {
        return mEngine;
    }
}
