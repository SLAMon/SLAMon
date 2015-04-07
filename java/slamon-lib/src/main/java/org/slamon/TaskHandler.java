package org.slamon;

import java.util.HashMap;
import java.util.Map;

/**
 * Abstract base class for all SLAMon tasks.
 */
public abstract class TaskHandler {

    private static final Map<String, TaskHandler> sHandlers = new HashMap<String, TaskHandler>();

    /**
     * Register a new handler. List of tasks is stored statically.
     *
     * @param handler the new handler to register
     */
    static void registerHandler(TaskHandler handler) {
        sHandlers.put(handler.getName(), handler);
    }

    /**
     * Find matching handler for name and version
     *
     * @param name    handler name
     * @param version handler version
     * @return Matching handler or null if none found
     */
    static TaskHandler getHandler(String name, int version) {
        TaskHandler ret = sHandlers.get(name);
        if (ret != null && ret.getVersion() == version) {
            return ret;
        }
        return null;
    }

    /**
     * Get registered handlers and their versions
     *
     * @return map with registered handler names as keys and versions as values
     */
    static Map<String, Integer> capabilities() {
        Map<String, Integer> ret = new HashMap<String, Integer>();
        for (Map.Entry<String, TaskHandler> handler : sHandlers.entrySet()) {
            ret.put(handler.getKey(), handler.getValue().getVersion());
        }
        return ret;
    }

    /**
     * Execute the task with input parameters. Implementation should
     * return all output parameters in a Map.
     *
     * @param inputParams Map of input parameters
     * @return Map of output parameters
     */
    public abstract Map<String, Object> execute(Map<String, Object> inputParams) throws Exception;

    /**
     * Get task handler name
     *
     * @return name of the handler
     */
    public abstract String getName();

    /**
     * Get task handler version
     *
     * @return version number of the handler
     */
    public abstract int getVersion();

    // Finding all TaskHandlers using Java Reflection. Currently commented out in order not to crash Android library,
    // where this approach does not work
    /*
    static {
        Set<Class<? extends TaskHandler>> handlers = new Reflections(
                ClasspathHelper.forPackage("org.slamon.handlers"), new SubTypesScanner()).getSubTypesOf(TaskHandler.class);
        for (Class<? extends TaskHandler> handler : handlers) {
            try {
                TaskHandler handlerInstance = handler.getDeclaredConstructor().newInstance();
                registerHandler(handlerInstance);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }
    */
}
