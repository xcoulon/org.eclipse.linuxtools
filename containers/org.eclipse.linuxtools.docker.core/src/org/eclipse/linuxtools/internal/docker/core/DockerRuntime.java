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

import org.eclipse.linuxtools.docker.core.IDockerRuntime;

/**
 * An existing instance of Docker running natively or in a VM.
 */
public class DockerRuntime implements IDockerRuntime {

	/** the name of the machine. */
	private final String name;

	/** the state of the machine. */
	private final String state;

	/** the URL of the machine or <code>null</code> if none was found. */
	private final String url;

	/**
	 * the Docker version of the machine or <code>null</code> if none was found.
	 */
	private final String dockerVersion;

	/**
	 * the optional errors of the Docker Machine, or <code>null</code> of none
	 * was found.
	 */
	private final String errors;

	/**
	 * Full constructor.
	 * 
	 * @param name
	 *            the name of the machine.
	 * @param state
	 *            the state of the machine.
	 * @param url
	 *            the URL of the machine or <code>null</code> if none was found
	 * @param dockerVersion
	 *            the Docker version of the machine or <code>null</code> if none
	 *            was found.
	 * @param errors
	 *            the optional errors of the Docker Machine, or
	 *            <code>null</code> of none was found.
	 */
	public DockerRuntime(final String name, final String state,
			final String url, final String dockerVersion, final String errors) {
		this.name = name;
		this.state = state;
		this.url = url;
		this.dockerVersion = dockerVersion;
		this.errors = errors;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public String getState() {
		return this.state;
	}

	@Override
	public String getURL() {
		return this.url;
	}

	@Override
	public String getDockerVersion() {
		return this.dockerVersion;
	}

	@Override
	public String getErrors() {
		return this.errors;
	}

}
