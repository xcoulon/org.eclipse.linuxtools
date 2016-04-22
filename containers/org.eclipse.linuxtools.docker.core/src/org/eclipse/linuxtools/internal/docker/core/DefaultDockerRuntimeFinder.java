/*******************************************************************************
 * Copyright (c) 2016 Red Hat.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat - Initial Contribution
 *******************************************************************************/

package org.eclipse.linuxtools.internal.docker.core;

import java.util.List;

import org.eclipse.linuxtools.docker.core.IDockerRuntime;
import org.eclipse.linuxtools.docker.core.IDockerRuntimesFinder;

/**
 * Default implementation of the {@link IDockerRuntimesFinder}
 */
public class DefaultDockerRuntimeFinder implements IDockerRuntimesFinder {

	@Override
	public List<IDockerRuntime> findExistingDockerRuntimes(
			final String pathToDockerMachine, final String pathToVMDriver) {
		// FIXME: also search for native Docker on Linux and Docker for Mac and
		// Windows.
		return DockerMachineCommandRunner.getAllMachines(pathToDockerMachine,
				pathToVMDriver);
	}

}
