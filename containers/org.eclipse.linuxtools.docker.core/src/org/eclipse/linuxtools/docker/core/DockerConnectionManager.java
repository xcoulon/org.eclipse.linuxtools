/*******************************************************************************
 * Copyright (c) 2014, 2016 Red Hat.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat - Initial Contribution
 *******************************************************************************/
package org.eclipse.linuxtools.docker.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Status;
import org.eclipse.linuxtools.internal.docker.core.DefaultDockerConnectionSettingsFinder;
import org.eclipse.linuxtools.internal.docker.core.DefaultDockerConnectionStorageManager;
import org.eclipse.linuxtools.internal.docker.core.DefaultDockerRuntimeFinder;
import org.eclipse.linuxtools.internal.docker.core.DockerContainerRefreshManager;

public class DockerConnectionManager {

	/**
	 * @deprecated see the {@link IDockerConnectionStorageManager} implementation instead.
	 */
	@Deprecated
	public final static String CONNECTIONS_FILE_NAME = "dockerconnections.xml"; //$NON-NLS-1$

	private static DockerConnectionManager instance;

	private List<IDockerConnection> connections;
	private ListenerList<IDockerConnectionManagerListener> connectionManagerListeners;

	private IDockerConnectionSettingsFinder connectionSettingsFinder = new DefaultDockerConnectionSettingsFinder();
	private IDockerRuntimesFinder dockerRuntimeFinder = new DefaultDockerRuntimeFinder();
	private IDockerConnectionStorageManager connectionStorageManager = new DefaultDockerConnectionStorageManager();

	public static DockerConnectionManager getInstance() {
		if (instance == null) {
			instance = new DockerConnectionManager();
		}
		return instance;
	}

	private DockerConnectionManager() {
		reloadConnections();
	}

	public void reloadConnections() {
		this.connections = connectionStorageManager.loadConnections();
		for (IDockerConnection connection : connections) {
			try {
				connection.open(true);
				notifyListeners(connection,
						IDockerConnectionManagerListener.ADD_EVENT);
			} catch (DockerException e) {
				Activator.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
						e.getMessage()));
			}
		}
	}

	@Deprecated
	public void setConnectionSettingsFinder(
			final IDockerConnectionSettingsFinder connectionSettingsFinder) {
		this.connectionSettingsFinder = connectionSettingsFinder;
	}

	@Deprecated
	public List<IDockerConnectionSettings> findConnectionSettings() {
		// delegate the call to a utility class.
		return connectionSettingsFinder.findConnectionSettings();
	}

	public void setDockerRuntimesFinder(
			final IDockerRuntimesFinder dockerRuntimeFinder) {
		this.dockerRuntimeFinder = dockerRuntimeFinder;
	}

	public List<IDockerRuntime> findAvailableRuntimes(
			final String pathToDockerMachine, final String pathToVMDriver) {
		// delegate the call to a utility class.
		return this.dockerRuntimeFinder
				.findExistingDockerRuntimes(pathToDockerMachine,
						pathToVMDriver);
	}

	public void setConnectionStorageManager(
			final IDockerConnectionStorageManager connectionStorageManager) {
		this.connectionStorageManager = connectionStorageManager;
	}

	public void saveConnections() {
		this.connectionStorageManager.saveConnections(this.connections);
	}

	public IDockerConnection[] getConnections() {
		return connections.toArray(new IDockerConnection[connections.size()]);
	}

	/**
	 * @return an immutable {@link List} of the {@link IDockerConnection} names
	 */
	public List<String> getConnectionNames() {
		final List<String> connectionNames = new ArrayList<>();
		for (IDockerConnection connection : this.connections) {
			connectionNames.add(connection.getName());
		}
		return Collections.unmodifiableList(connectionNames);
	}

	public IDockerConnection findConnection(final String name) {
		if (name != null) {
			for (IDockerConnection connection : connections) {
				if (connection.getName().equals(name))
					return connection;
			}
		}
		return null;
	}

	public void addConnection(final IDockerConnection dockerConnection) throws DockerException {
		if(!dockerConnection.isOpen()) {
			dockerConnection.open(true);
		}
		connections.add(dockerConnection);
		saveConnections();
		notifyListeners(dockerConnection,
				IDockerConnectionManagerListener.ADD_EVENT);
	}

	public void removeConnection(final IDockerConnection connection) {
		connections.remove(connection);
		saveConnections();
		notifyListeners(connection,
				IDockerConnectionManagerListener.REMOVE_EVENT);
		DockerContainerRefreshManager.getInstance()
				.removeContainerRefreshThread(connection);
	}

	/**
	 * Notifies that a connection was renamed.
	 */
	public void notifyConnectionRename() {
		saveConnections();
		notifyListeners(IDockerConnectionManagerListener.RENAME_EVENT);
	}

	public void addConnectionManagerListener(
			IDockerConnectionManagerListener listener) {
		if (connectionManagerListeners == null)
			connectionManagerListeners = new ListenerList<>(
					ListenerList.IDENTITY);
		connectionManagerListeners.add(listener);
	}

	public void removeConnectionManagerListener(
			IDockerConnectionManagerListener listener) {
		if (connectionManagerListeners != null)
			connectionManagerListeners.remove(listener);
	}

	/**
	 * Notifies all listeners that a change occurred on a connection
	 * 
	 * @param type
	 *            the type of change
	 * @deprecated use
	 *             {@link DockerConnectionManager#notifyListeners(IDockerConnection, int)}
	 *             instead
	 */
	@Deprecated
	public void notifyListeners(int type) {
		if (connectionManagerListeners != null) {
			for (IDockerConnectionManagerListener listener : connectionManagerListeners) {
				listener.changeEvent(type);
			}
		}
	}

	/**
	 * Notifies all listeners that a change occurred on the given connection
	 * 
	 * @param connection
	 *            the connection that changed
	 * @param type
	 *            the type of change
	 */
	@SuppressWarnings("deprecation")
	public void notifyListeners(final IDockerConnection connection,
			final int type) {
		if (connectionManagerListeners != null) {
			for (IDockerConnectionManagerListener listener : connectionManagerListeners) {
				if (listener instanceof IDockerConnectionManagerListener2) {
					((IDockerConnectionManagerListener2) listener)
							.changeEvent(connection, type);
				} else {
					// keeping the call to the old method for the listeners that
					// are
					// interested
					listener.changeEvent(type);
				}
			}
		}
	}

}
