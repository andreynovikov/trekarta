/*
 * Copyright 2018 Andrey Novikov
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */

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
