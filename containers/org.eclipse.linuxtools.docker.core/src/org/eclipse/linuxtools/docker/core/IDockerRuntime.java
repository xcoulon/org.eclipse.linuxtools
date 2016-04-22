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

package org.eclipse.linuxtools.docker.core;

/**
 * An Existing instance of Docker running natively (Docker for Linux) or in a VM
 * (Docker for OSX and Windows or Docker Machine).
 */
public interface IDockerRuntime {

	/**
	 * @return the name of the Docker Machine.
	 */
	public String getName();

	/**
	 * @return the state of the Docker Machine.
	 */
	public String getState();

	/**
	 * @return the URL of the Docker Machine, or <code>null</code> if none was
	 *         found (e.g., machine was not running).
	 */
	public String getURL();

	/**
	 * @return the Docker version of the Docker Machine, or <code>null</code> if
	 *         none was found (e.g., machine was not running).
	 */
	public String getDockerVersion();

	/**
	 * @return the optional errors of the Docker Machine, or <code>null</code>
	 *         of none was found.
	 */
	public String getErrors();

}
