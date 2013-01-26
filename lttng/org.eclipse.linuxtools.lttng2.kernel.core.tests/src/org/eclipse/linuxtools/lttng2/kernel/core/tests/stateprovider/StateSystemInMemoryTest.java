/*******************************************************************************
 * Copyright (c) 2013 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Alexandre Montplaisir - Initial API and implementation
 ******************************************************************************/

package org.eclipse.linuxtools.lttng2.kernel.core.tests.stateprovider;

import org.eclipse.linuxtools.internal.lttng2.kernel.core.stateprovider.CtfKernelStateInput;
import org.eclipse.linuxtools.tmf.core.exceptions.TmfTraceException;
import org.eclipse.linuxtools.tmf.core.statesystem.StateSystemManager;
import org.junit.BeforeClass;

/**
 * State system tests using the in-memory back-end.
 *
 * @author Alexandre Montplaisir
 */
public class StateSystemInMemoryTest extends StateSystemTest {

    /**
     * Initialization
     */
    @BeforeClass
    public static void initialize() {
        try {
            input = new CtfKernelStateInput(CtfTestFiles.getTestTrace());
            ssq = StateSystemManager.newInMemHistory(input, true);
        } catch (TmfTraceException e) {
            e.printStackTrace();
        }
    }
}