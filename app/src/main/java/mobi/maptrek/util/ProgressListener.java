package mobi.maptrek.util;

/**
 * Callback interface for progress monitoring.
 */
public interface ProgressListener {
    /**
     * Called when operation is about to start and maximum progress is known.
     *
     * @param length Maximum progress
     */
    void onProgressStarted(int length);

    /**
     * Called on operation progress.
     *
     * @param progress Current progress
     */
    void onProgressChanged(int progress);

    /**
     * Called when operation has ended, is not called if error (exception) has occurred.
     */
    void onProgressFinished();

    /**
     * Called when progress step is annotated.
     *
     * @param annotation Annotation of a step.
     */
    void onProgressAnnotated(String annotation);
}
