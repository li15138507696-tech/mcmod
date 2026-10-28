package net.guzhenren.tianfu.client;

/** In-memory wizard cursor for the current client player session; it is not gameplay data. */
public final class TianfuWizardSession {
    private static volatile TianfuWizardStep current;

    private TianfuWizardSession() {
    }

    /** Return the last step visited in this client session, or null before the first opening. */
    public static TianfuWizardStep current() {
        return current;
    }

    /** Remember a step; passing null clears the cursor. */
    public static void remember(TianfuWizardStep step) {
        current = step;
    }

    /** Clear the cursor when the client player lifecycle changes. */
    public static void clear() {
        current = null;
    }
}
