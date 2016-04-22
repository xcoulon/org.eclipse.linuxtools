/*******************************************************************************
 * Copyright (c) 2015 Red Hat.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat - Initial Contribution
 *******************************************************************************/

package org.eclipse.linuxtools.internal.docker.ui.wizards;

import java.util.List;

import org.eclipse.linuxtools.docker.core.IDockerRuntime;
import org.eclipse.linuxtools.internal.docker.ui.databinding.BaseDatabindingModel;

/**
 * 
 */
public class NewDockerConnectionPageModel extends BaseDatabindingModel {

	/** name of the field handling the list of available Docker instances. */
	public static final String AVAILABLE_INSTANCES = "availableInstances"; //$NON-NLS-1$
	/** name of the field handling the connection name. */
	public static final String CONNECTION_NAME = "connectionName"; //$NON-NLS-1$
	/** name of the field handling the custom settings flag. */
	public static final String USE_CUSTOM_SETTINGS = "useCustomSettings"; //$NON-NLS-1$
	/** name of the field handling the custom url/port. */
	public static final String CUSTOM_HOST = "customHost"; //$NON-NLS-1$
	/** name of the field handling the custom client auth flag. */
	public static final String CUSTOM_TLS_VERIFY = "customTLSVerify"; //$NON-NLS-1$
	/** name of the field handling the custom path to client certificates. */
	public static final String CUSTOM_CERT_PATH = "customCertPath"; //$NON-NLS-1$

	/** the list of available Docker instances. */
	private List<IDockerRuntime> availableInstances;
	/** the name of the connection. */
	private String connectionName;
	/** flag to indicate if custom settings are used. */
	private boolean useCustomSettings = false;
	/** the url/port. */
	private String customHost = null;
	/** flag to indicate if auth with certificates is enabled. */
	private boolean customTLSVerify = false;
	/** path to auth certificates (if enabled). */
	private String customCertPath = null;

	/**
	 * @return the name of the connection
	 */
	public String getConnectionName() {
		return connectionName;
	}

	/**
	 * @param connectionName
	 *            the name of the connection to set
	 */
	public void setConnectionName(final String connectionName) {
		firePropertyChange(CONNECTION_NAME, this.connectionName,
				this.connectionName = connectionName);
	}

	/**
	 * @return flag to indicate if the connection is based on custom settings
	 */
	public boolean isUseCustomSettings() {
		return useCustomSettings;
	}

	/**
	 * Indicates if the connection is based on custom settings
	 * 
	 * @param useCustomSettings
	 *            the custom settings flag
	 */
	public void setUseCustomSettings(final boolean useCustomSettings) {
		firePropertyChange(USE_CUSTOM_SETTINGS, this.useCustomSettings,
				this.useCustomSettings = useCustomSettings);
	}

	/**
	 * @return the url/port (if REST API connection is used)
	 */
	public String getCustomHost() {
		return customHost;
	}

	/**
	 * @param customHost
	 *            the url/port (if REST API connection is used) to set
	 */
	public void setCustomHost(final String customHost) {
		firePropertyChange(CUSTOM_HOST, this.customHost,
				this.customHost = customHost);
	}

	/**
	 * @return flag to indicate if auth with certificates is enabled
	 */
	public boolean isCustomTLSVerify() {
		return customTLSVerify;
	}

	/**
	 * @param customTLSVerify
	 *            flag to indicate if auth with certificates is enabled
	 */
	public void setCustomTLSVerify(final boolean customTLSVerify) {
		firePropertyChange(CUSTOM_TLS_VERIFY, this.customTLSVerify,
				this.customTLSVerify = customTLSVerify);
	}

	/**
	 * @return path to auth certificates (if enabled)
	 */
	public String getCustomCertPath() {
		return customCertPath;
	}

	/**
	 * @param customCertPath
	 *            path to auth certificates (if enabled) to set
	 */
	public void setCustomCertPath(final String customCertPath) {
		firePropertyChange(CUSTOM_CERT_PATH, this.customCertPath,
				this.customCertPath = customCertPath);
	}

	/**
	 * @return the list of available Docker instances.
	 */
	public List<IDockerRuntime> getAvailableInstances() {
		return availableInstances;
	}

	/**
	 * @param availableInstances
	 *            the list of available Docker instances.
	 */
	public void setAvailableInstances(
			final List<IDockerRuntime> availableInstances) {
		firePropertyChange(AVAILABLE_INSTANCES, this.availableInstances,
				this.availableInstances = availableInstances);
	}

	/**
	 * An existing Docker instance. May be available or not, depending on its
	 * state.
	 */
	@Deprecated
	public static class DockerInstance {

		/** The type of the instance (native, Docker Machine). */
		private final String type;
		/** The name of the instance. */
		private final String name;
		/** The URL to connect to (can be a Unix socket, too). */
		private final String url;
		/** The flag to enable auth. */
		private final boolean useAuth;
		/** The path to certificates (or null if auth is disabled). */
		private final String pathToCertificates;
		/** The instance state (or <code>null</code> if unknown). */
		private final String state;
		/** The version of Docker (or <code>null</code> if unknown). */
		private final String version;
		/**
		 * The errors if any known or <code>null</code> if none was reported.
		 */
		private final String errors;

		/**
		 * Constructor.
		 * 
		 * @param type
		 *            the type of the instance.
		 * @param name
		 *            the name of the instance.
		 * @param url
		 *            the url to connect to (can be a Unix socket, too).
		 * @param useAuth
		 *            the flag to enable auth.
		 * @param pathToCertificates
		 *            the path to certificates (or null if auth is disabled).
		 * @param state
		 *            the instance state.
		 * @param version
		 *            the version of Docker (or <code>null</code> if unknown).
		 * @param errors
		 *            the errors if any or <code>null</code> if none was
		 *            reported.
		 */
		public DockerInstance(final String type, final String name,
				final String url, final boolean useAuth,
				final String pathToCertificates, final String state,
				final String version, final String errors) {
			this.type = type;
			this.name = name;
			this.url = url;
			this.useAuth = useAuth;
			this.pathToCertificates = pathToCertificates;
			this.state = state;
			this.version = version;
			this.errors = errors;
		}

		/**
		 * @return the type of the instance.
		 */
		public String getType() {
			return type;
		}

		/**
		 * @return the name of the instance.
		 */
		public String getName() {
			return name;
		}

		/**
		 * @return the url to connect to (can be a Unix socket, too).
		 */
		public String getUrl() {
			return url;
		}

		/**
		 * @return the flag to enable auth.
		 */
		public boolean isUseAuth() {
			return useAuth;
		}

		/**
		 * @return the path to certificates (or null if auth is disabled).
		 */
		public String getPathToCertificates() {
			return pathToCertificates;
		}

		/**
		 * @return the instance state.
		 */
		public String getState() {
			return state;
		}

		/**
		 * @return the version of Docker (or <code>null</code> if unknown).
		 */
		public String getVersion() {
			return version;
		}

		/**
		 * @return the errors if any or <code>null</code> if none was reported.
		 */
		public String getErrors() {
			return errors;
		}

	}
}
