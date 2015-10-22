/*******************************************************************************
 * Copyright (c) 2014, 2015 Red Hat.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat - Initial Contribution
 *******************************************************************************/
package org.eclipse.linuxtools.internal.docker.core;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.equinox.security.storage.EncodingUtils;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.linuxtools.docker.core.Activator;
import org.eclipse.linuxtools.docker.core.DockerConnectionManager;
import org.eclipse.linuxtools.docker.core.DockerContainerNotFoundException;
import org.eclipse.linuxtools.docker.core.DockerException;
import org.eclipse.linuxtools.docker.core.EnumDockerLoggingStatus;
import org.eclipse.linuxtools.docker.core.IDockerConfParameter;
import org.eclipse.linuxtools.docker.core.IDockerConnection;
import org.eclipse.linuxtools.docker.core.IDockerConnectionInfo;
import org.eclipse.linuxtools.docker.core.IDockerContainer;
import org.eclipse.linuxtools.docker.core.IDockerContainerConfig;
import org.eclipse.linuxtools.docker.core.IDockerContainerExit;
import org.eclipse.linuxtools.docker.core.IDockerContainerInfo;
import org.eclipse.linuxtools.docker.core.IDockerContainerListener;
import org.eclipse.linuxtools.docker.core.IDockerHostConfig;
import org.eclipse.linuxtools.docker.core.IDockerImage;
import org.eclipse.linuxtools.docker.core.IDockerImageInfo;
import org.eclipse.linuxtools.docker.core.IDockerImageListener;
import org.eclipse.linuxtools.docker.core.IDockerImageSearchResult;
import org.eclipse.linuxtools.docker.core.IDockerPortBinding;
import org.eclipse.linuxtools.docker.core.IDockerProgressHandler;
import org.eclipse.linuxtools.docker.core.ILogger;
import org.eclipse.linuxtools.docker.core.Messages;
import org.eclipse.osgi.util.NLS;

import com.spotify.docker.client.ContainerNotFoundException;
import com.spotify.docker.client.DockerCertificateException;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.DockerClient.AttachParameter;
import com.spotify.docker.client.DockerClient.BuildParameter;
import com.spotify.docker.client.DockerClient.LogsParameter;
import com.spotify.docker.client.LogStream;
import com.spotify.docker.client.messages.Container;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.ContainerExit;
import com.spotify.docker.client.messages.ContainerInfo;
import com.spotify.docker.client.messages.HostConfig;
import com.spotify.docker.client.messages.HostConfig.LxcConfParameter;
import com.spotify.docker.client.messages.Image;
import com.spotify.docker.client.messages.ImageInfo;
import com.spotify.docker.client.messages.ImageSearchResult;
import com.spotify.docker.client.messages.Info;
import com.spotify.docker.client.messages.PortBinding;
import com.spotify.docker.client.messages.Version;

/**
 * A connection to a Docker daemon. The connection may rely on Unix Socket or TCP connection (using the REST API). 
 * All low-level communication is delegated to a wrapped {@link DockerClient}.
 * 
 *
 */
public class DockerConnection implements IDockerConnection, Closeable {

	// Builder allowing different binding modes (unix socket vs TCP connection)
	public static class Builder {

		private String unixSocketPath;
		private String name;
		private String tcpHost;
		private String tcpCertPath;

		public Builder name(final String name) {
			this.name = name;
			return this;

		}

		public Builder unixSocket(String unixSocketPath) {
			if (unixSocketPath != null && !unixSocketPath.matches("\\w+://.*")) { //$NON-NLS-1$
				unixSocketPath = "unix://" + unixSocketPath; //$NON-NLS-1$
			}
			this.unixSocketPath = unixSocketPath;
			return this;
		}

		public Builder tcpHost(String tcpHost) {
			if (tcpHost != null) {
				if (!tcpHost.matches("\\w+://.*")) { //$NON-NLS-1$
					tcpHost = "tcp://" + tcpHost; //$NON-NLS-1$
				}
				this.tcpHost = tcpHost.replace("tcp://", "http://"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			return this;
		}

		public Builder tcpCertPath(final String tcpCertPath) {
			this.tcpCertPath = tcpCertPath;
			if (this.tcpHost != null && this.tcpCertPath != null) {
				this.tcpHost = tcpHost.replace("http://", "https://");
			}
			return this;
		}

		public DockerConnection build() {
			if (unixSocketPath != null) {
				return new DockerConnection(name, unixSocketPath, null, null);
			} else {
				return new DockerConnection(name, tcpHost, tcpCertPath, null,
						null);

			}
		}
	}

	private final String name;
	private final String socketPath;
	private final String tcpHost;
	private final String tcpCertPath;
	private final String username;
	private final Object imageLock = new Object();
	private final Object containerLock = new Object();
	private final Object actionLock = new Object();
	private final Object clientLock = new Object();
	private DockerClientFactory dockerClientFactory = new DockerClientFactory();
	private DockerClient client;

	private Map<String, Job> actionJobs;

	private Map<String, LogThread> loggingThreads = new HashMap<>();

	// private Set<String> printIds = new HashSet<String>();

	private List<IDockerContainer> containers;
	private boolean containersLoaded = false;
	private List<IDockerImage> images;
	private boolean imagesLoaded = false;

	ListenerList containerListeners;
	ListenerList imageListeners;

	/**
	 * Constructor for a unix socket based connection
	 */
	private DockerConnection(final String name, final String socketPath,
			final String username, final String password) {
		this.name = name;
		this.socketPath = socketPath;
		this.username = username;
		this.tcpHost = null;
		this.tcpCertPath = null;
		storePassword(socketPath, username, password);

	}

	/**
	 * Constructor for a REST-based connection
	 */
	private DockerConnection(final String name, final String tcpHost,
			final String tcpCertPath, final String username,
			final String password) {
		this.name = name;
		this.socketPath = null;
		this.username = username;
		this.tcpHost = tcpHost;
		this.tcpCertPath = tcpCertPath;
		storePassword(socketPath, username, password);
		// Add the container refresh manager to watch the containers list
		DockerContainerRefreshManager dcrm = DockerContainerRefreshManager
				.getInstance();
		addContainerListener(dcrm);
	}

	private void storePassword(String uri, String username, String passwd) {
		ISecurePreferences root = SecurePreferencesFactory.getDefault();
		String key = DockerConnection.getPreferencesKey(uri, username);
		ISecurePreferences node = root.node(key);
		try {
			if (passwd != null && !passwd.equals("")) //$NON-NLS-1$
				node.put("password", passwd, true /* encrypt */);
		} catch (StorageException e) {
			Activator.log(e);
		}
	}

	public static String getPreferencesKey(String uri, String username) {
		String key = "/org/eclipse/linuxtools/docker/core/"; //$NON-NLS-1$
		key += uri + "/" + username; //$NON-NLS-1$
		return EncodingUtils.encodeSlashes(key);
	}

	@Override
	public boolean isOpen() {
		return this.client != null;
	}

	@Override
	public void open(boolean registerContainerRefreshManager)
			throws DockerException {
		// synchronized block to avoid concurrent attempts to open a connection
		// to the same Docker daemon
		synchronized (this) {
			if (this.client == null) {
				try {
					setClient(dockerClientFactory.getClient(this.socketPath,
							this.tcpHost, this.tcpCertPath));
					if (registerContainerRefreshManager) {
						// Add the container refresh manager to watch the
						// containers
						// list
						DockerContainerRefreshManager dcrm = DockerContainerRefreshManager
								.getInstance();
						addContainerListener(dcrm);
					}
				} catch (DockerCertificateException e) {
					throw new DockerException(NLS
							.bind(Messages.Open_Connection_Failure, this.name));
				}

			}
		}
	}

	public DockerClient getClient() {
		return client;
	}

	public void setClient(final DockerClient client) {
		this.client = client;
	}
	/**
	 * Change the default {@link DockerClientFactory}
	 * 
	 * @param dockerClientFactory
	 *            the new {@link DockerClientFactory} to use when opening a
	 *            connection.
	 */
	public void setDockerClientFactory(
			final DockerClientFactory dockerClientFactory) {
		this.dockerClientFactory = dockerClientFactory;
	}

	@Override
	public void ping() throws DockerException {
		try {
			if (client != null) {
				client.ping();
			} else {
				throw new DockerException(Messages.Docker_Daemon_Ping_Failure);
			}
		} catch (com.spotify.docker.client.DockerException
				| InterruptedException e) {
			throw new DockerException(Messages.Docker_Daemon_Ping_Failure, e);
		}
	}

	@Override
	public void close() {
		synchronized (clientLock) {
			if (client != null) {
				this.client.close();
				this.client = null;
			}
		}
	}

	@Override
	public IDockerConnectionInfo getInfo() throws DockerException {
		try {
			final Info info = client.info();
			final Version version = client.version();
			return new DockerConnectionInfo(info, version);
		} catch (com.spotify.docker.client.DockerRequestException e) {
			throw new DockerException(e.message());
		} catch (com.spotify.docker.client.DockerException | InterruptedException e) {
			throw new DockerException(Messages.Docker_General_Info_Failure, e);
		}
	}
	
	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getUri() {
		return this.socketPath != null ? this.socketPath : this.tcpHost;
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public void addContainerListener(IDockerContainerListener listener) {
		if (containerListeners == null)
			containerListeners = new ListenerList(ListenerList.IDENTITY);
		containerListeners.add(listener);
	}

	@Override
	public void removeContainerListener(IDockerContainerListener listener) {
		if (containerListeners != null)
			containerListeners.remove(listener);
	}

	/**
	 * Get a copy of the client to use in parallel threads for long-standing
	 * operations such as logging or waiting until finished. The user of the
	 * copy should close it when the operation is complete.
	 * 
	 * @return copy of client
	 * @throws DockerException
	 * @throws DockerCertificateException
	 * @see DockerConnection#open(boolean)
	 */
	private DockerClient getClientCopy() throws DockerException {
		try {
			return dockerClientFactory.getClient(this.socketPath, this.tcpHost,
					this.tcpCertPath);
		} catch (DockerCertificateException e) {
			throw new DockerException(
					NLS.bind(Messages.Open_Connection_Failure, this.name));
		}
	}

	public void notifyContainerListeners(List<IDockerContainer> list) {
		if (containerListeners != null) {
			Object[] listeners = containerListeners.getListeners();
			for (int i = 0; i < listeners.length; ++i) {
				((IDockerContainerListener) listeners[i])
						.listChanged(this,
						list);
			}
		}
	}

	public Job getActionJob(String id) {
		synchronized (actionLock) {
			Job j = null;
			if (actionJobs != null) {
				return actionJobs.get(id);
			}
			return j;
		}
	}

	public void registerActionJob(String id, Job j) {
		synchronized (actionLock) {
			if (actionJobs == null)
				actionJobs = new HashMap<>();
			actionJobs.put(id, j);
		}
	}

	public void removeActionJob(String id, Job j) {
		synchronized (actionLock) {
			if (actionJobs != null && actionJobs.get(id) == j)
				actionJobs.remove(id);
		}
	}

	@Override
	public List<IDockerContainer> getContainers() {
		return getContainers(false);
	}

	@Override
	public List<IDockerContainer> getContainers(final boolean force) {
		List<IDockerContainer> latestContainers;
		synchronized (containerLock) {
			latestContainers = this.containers;
		}
		if (!isContainersLoaded() || force) {
			try {
				latestContainers = listContainers();
			} catch (DockerException e) {
				synchronized (containerLock) {
					this.containers = Collections.emptyList();
				}
				Activator.log(e);
			} finally {
				this.containersLoaded = true;
			}
		}
		return latestContainers;
	}

	@Override
	public boolean isContainersLoaded() {
		return containersLoaded;
	}

	/**
	 * Class to perform logging of a container run to a given output stream
	 * (usually a console stream).
	 *
	 */
	private class LogThread extends AbstractKillableThread implements ILogger {
		private String id;
		private DockerClient copyClient;
		private OutputStream outputStream;
		private boolean follow;

		public LogThread(String id, DockerClient copyClient, boolean follow) {
			this.id = id;
			this.copyClient = copyClient;
			this.follow = follow;
		}

		@Override
		public LogThread clone() {
			return new LogThread(id, copyClient, follow);
		}

		@Override
		public void setOutputStream(OutputStream stream) {
			outputStream = stream;
		}

		@Override
		public void execute() throws InterruptedException, IOException {
			try {
				// Add timestamps to log based on user preference
				IEclipsePreferences preferences = InstanceScope.INSTANCE
						.getNode("org.eclipse.linuxtools.docker.ui"); //$NON-NLS-1$

				boolean timestamps = preferences.getBoolean(
						"logTimestamp", true); //$NON-NLS-1$

				LogStream stream = null;

				if (timestamps)
					stream = copyClient.logs(id, LogsParameter.FOLLOW,
							LogsParameter.STDOUT, LogsParameter.STDERR,
							LogsParameter.TIMESTAMPS);
				else
					stream = copyClient.logs(id, LogsParameter.FOLLOW,
							LogsParameter.STDOUT, LogsParameter.STDERR);

				// First time through, don't sleep before showing log data
				int delayTime = 100;

				do {
					Thread.sleep(delayTime);
					// Second time in loop and following, pause a second to
					// allow other threads to do meaningful work
					delayTime = 1000;
					while (stream.hasNext()) {
						ByteBuffer b = stream.next().content();
						byte[] bytes = new byte[b.remaining()];
						b.get(bytes);
						if (outputStream != null)
							outputStream.write(bytes);
					}
				} while (follow && !stop);
				listContainers();
			} catch (com.spotify.docker.client.DockerRequestException e) {
				Activator.logErrorMessage(e.message());
				throw new InterruptedException();
			} catch (com.spotify.docker.client.DockerException | IOException e) {
				Activator.logErrorMessage(e.getMessage());
				throw new InterruptedException();
			} catch (Exception e) {
				/*
				 * Temporary workaround for BZ #477485
				 * Remove when docker-client logs() uses noTimeoutClient.
				 */
				if (e.getCause() instanceof SocketTimeoutException) {
					execute();
				} else {
					Activator.logErrorMessage(e.getMessage());
				}
			} finally {
				follow = false;
				copyClient.close(); // we are done with copyClient..dispose
				if (outputStream != null)
					outputStream.close();
			}
		}
	}

	private List<IDockerContainer> listContainers() throws DockerException {
		final List<IDockerContainer> dclist = new ArrayList<>();
		synchronized (containerLock) {
			List<Container> list = null;
			try {
				synchronized (clientLock) {
					// Check that client is not null as this connection may have
					// been closed but there is an async request to update the
					// containers list left in the queue
					if (client == null)
						return dclist;
					list = client.listContainers(
							DockerClient.ListContainersParam.allContainers());
				}
			} catch (com.spotify.docker.client.DockerException
					| InterruptedException e) {
				throw new DockerException(
						NLS.bind(
						Messages.List_Docker_Containers_Failure,
						this.getName()), e);
			}

			// We have a list of containers. Now, we translate them to our own
			// core format in case we decide to change the underlying engine
			// in the future.
			for (Container c : list) {
				// For containers that have exited, make sure we aren't tracking
				// them with a logging thread.
				if (c.status().startsWith(Messages.Exited_specifier)) {
					if (loggingThreads.containsKey(c.id())) {
						loggingThreads.get(c.id()).requestStop();
						loggingThreads.remove(c.id());
					}
				}
				if (!c.status().equals(Messages.Removal_In_Progress_specifier)) {
					dclist.add(new DockerContainer(this, c));
				}
			}
			containers = dclist;
		}
		// perform notification outside of containerLock so we don't have a View
		// causing a deadlock
		notifyContainerListeners(dclist);
		return dclist;
	}

	@Override
	public IDockerContainer getContainer(String id) {
		List<IDockerContainer> containers = getContainers();
		for (IDockerContainer container : containers) {
			if (container.id().equals(id)) {
				return container;
			}
		}
		return null;
	}

	@Override
	public IDockerContainerInfo getContainerInfo(final String id) {
		try {
			final ContainerInfo info = client.inspectContainer(id);
			return new DockerContainerInfo(info);
		} catch (com.spotify.docker.client.DockerRequestException e) {
			Activator.logErrorMessage(e.message());
			return null;
		} catch (com.spotify.docker.client.DockerException
				| InterruptedException e) {
			Activator.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Failed to inspect container '" + id + "'", e));
			return null;
		}
	}

	@Override
	public IDockerImageInfo getImageInfo(String id) {
		try {
			final ImageInfo info = client.inspectImage(id);
			return new DockerImageInfo(info);
		} catch (com.spotify.docker.client.DockerRequestException e) {
			Activator.logErrorMessage(e.message());
			return null;
		} catch (com.spotify.docker.client.DockerException
				| InterruptedException e) {
			Activator.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
					"Failed to inspect container '" + id + "'", e));
			return null;
		}
	}

	@Override
	public void addImageListener(IDockerImageListener listener) {
		if (imageListeners == null)
			imageListeners = new ListenerList(ListenerList.IDENTITY);
		imageListeners.add(listener);
	}

	@Override
	public void removeImageListener(IDockerImageListener listener) {
		if (imageListeners != null)
			imageListeners.remove(listener);
	}

	public void notifyImageListeners(List<IDockerImage> list) {
		if (imageListeners != null) {
			Object[] listeners = imageListeners.getListeners();
			for (int i = 0; i < listeners.length; ++i) {
				((IDockerImageListener) listeners[i]).listChanged(this, list);
			}
		}
	}

	@Override
	public List<IDockerImage> getImages() {
		return getImages(false);
	}

	@Override
	public List<IDockerImage> getImages(final boolean force) {
		List<IDockerImage> latestImages;
		synchronized (imageLock) {
			latestImages = this.images;
		}
		if (!isImagesLoaded() || force) {
			try {
				latestImages = listImages();
			} catch (DockerException e) {
				synchronized (imageLock) {
					this.images = Collections.emptyList();
				}
				Activator.log(e);
			} finally {
				this.imagesLoaded = true;
			}
		}
		return latestImages;
	}

	@Override
	public boolean isImagesLoaded() {
		return imagesLoaded;
	}

	@Override
	public List<IDockerImage> listImages() throws DockerException {
		final List<IDockerImage> dilist = new ArrayList<>();
		synchronized (imageLock) {
			List<Image> rawImages = null;
			try {
				synchronized (clientLock) {
					// Check that client is not null as this connection may have
					// been closed but there is an async request to update the
					// images list left in the queue
					if (client == null)
						return dilist;
					rawImages = client.listImages(
							DockerClient.ListImagesParam.allImages());
				}
			} catch (com.spotify.docker.client.DockerRequestException e) {
				throw new DockerException(e.message());
			} catch (com.spotify.docker.client.DockerException
					| InterruptedException e) {
				DockerException f = new DockerException(e);
				throw f;
			}
			// We have a list of images. Now, we translate them to our own
			// core format in case we decide to change the underlying engine
			// in the future. We also look for intermediate and dangling images.
			final Set<String> imageParentIds = new HashSet<>();
			for (Image rawImage : rawImages) {
				imageParentIds.add(rawImage.parentId());
			}
			for (Image rawImage : rawImages) {
				final boolean taggedImage = !(rawImage.repoTags() != null
						&& rawImage.repoTags().size() == 1
						&& rawImage
						.repoTags().contains("<none>:<none>")); //$NON-NLS-1$
				final boolean intermediateImage = !taggedImage
						&& imageParentIds.contains(rawImage.id());
				final boolean danglingImage = !taggedImage
						&& !intermediateImage;
				// FIXME: if an image with a unique ID belongs to multiple repos, we should
				// probably have multiple instances of IDockerImage
				final Map<String, List<String>> repoTags = DockerImage.extractTagsByRepo(rawImage.repoTags());
				for(Entry<String, List<String>> entry : repoTags.entrySet()) {
					final String repo = entry.getKey();
					final List<String> tags = entry.getValue();
					dilist.add(new DockerImage(this, rawImage
							.repoTags(), repo, tags, rawImage.id(), rawImage.parentId(),
							rawImage.created(), rawImage.size(), rawImage
									.virtualSize(), intermediateImage,
							danglingImage));
				}
			}
			images = dilist;
		}
		// Perform notification outside of lock so that listener doesn't cause a
		// deadlock to occur
		notifyImageListeners(dilist);
		return dilist;
	}

	@Override
	public boolean hasImage(final String repository, final String tag) {
		for (IDockerImage image : getImages()) {
			if (image.repo().equals(repository)) {
				for (String imageTag : image.tags()) {
					if (imageTag.startsWith(tag)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	public void pullImage(final String id, final IDockerProgressHandler handler)
			throws DockerException, InterruptedException {
		try {
			DockerProgressHandler d = new DockerProgressHandler(handler);
			client.pull(id, d);
			listImages();
		} catch (com.spotify.docker.client.DockerRequestException e) {
			throw new DockerException(e.message());
		} catch (com.spotify.docker.client.DockerException e) {
			DockerException f = new DockerException(e);
			throw f;
		}
	}
	
	@Override
	public List<IDockerImageSearchResult> searchImages(final String term) throws DockerException {
		try {
			final List<ImageSearchResult> searchResults = client.searchImages(term);
			final List<IDockerImageSearchResult> results = new ArrayList<>();
			for(ImageSearchResult r : searchResults) {
				results.add(new DockerImageSearchResult(r.getDescription(), r.isOfficial(), r.isAutomated(), r.getName(), r.getStarCount()));
			}
			return results;
		} catch (com.spotify.docker.client.DockerException | InterruptedException e) {
			throw new DockerException(e);
		}
	}
	
	@Override
	public void pushImage(final String name, final IDockerProgressHandler handler)
			throws DockerException, InterruptedException {
		try {
			DockerProgressHandler d = new DockerProgressHandler(handler);
			client.push(name, d);
		} catch (com.spotify.docker.client.DockerRequestException e) {
			throw new DockerException(e.message());
		} catch (com.spotify.docker.client.DockerException e) {
			DockerException f = new DockerException(e);
			throw f;
		}
	}

	@Override
	public void removeImage(final String name) throws DockerException,
			InterruptedException {
		try {
			client.removeImage(name, true, false);
		} catch (com.spotify.docker.client.DockerRequestException e) {
			throw new DockerException(e.message());
		} catch (com.spotify.docker.client.DockerException e) {
			DockerException f = new DockerException(e);
			throw f;
		}
	}

	@Override
	public void removeTag(final String tag) throws DockerException,
			InterruptedException {
		try {
			client.removeImage(tag, false, false);
		} catch (com.spotify.docker.client.DockerRequestException e) {
			throw new DockerException(e.message());
		} catch (com.spotify.docker.client.DockerException e) {
			DockerException f = new DockerException(e);
			throw f;
		}
	}

	@Override
	public void tagImage(final String name, final String newTag) throws DockerException,
			InterruptedException {
		try {
			client.tag(name, newTag);
		} catch (com.spotify.docker.client.DockerRequestException e) {
			throw new DockerException(e.message());
		} catch (com.spotify.docker.client.DockerException e) {
			DockerException f = new DockerException(e);
			throw f;
		}
	}

	@Override
	public String buildImage(final IPath path,
			final IDockerProgressHandler handler)
					throws DockerException, InterruptedException {
		try {
			final DockerProgressHandler d = new DockerProgressHandler(handler);
			final java.nio.file.Path p = FileSystems.getDefault()
					.getPath(path.makeAbsolute().toOSString());
			return client.build(p, d, BuildParameter.FORCE_RM);
		} catch (com.spotify.docker.client.DockerRequestException e) {
			throw new DockerException(e.message());
		} catch (com.spotify.docker.client.DockerException | IOException e) {
			DockerException f = new DockerException(e);
			throw f;
		}
	}

	@Override
	public String buildImage(final IPath path, final String name,
			final IDockerProgressHandler handler)
					throws DockerException, InterruptedException {
		try {
			DockerProgressHandler d = new DockerProgressHandler(handler);
			java.nio.file.Path p = FileSystems.getDefault().getPath(
					path.makeAbsolute().toOSString());
			return client.build(p, name, d, BuildParameter.FORCE_RM);
		} catch (com.spotify.docker.client.DockerRequestException e) {
			throw new DockerException(e.message());
		} catch (com.spotify.docker.client.DockerException | IOException e) {
			DockerException f = new DockerException(e);
			throw f;
		}
	}

	public void save() {
		// Currently we have to save all clouds instead of just this one
		DockerConnectionManager.getInstance().saveConnections();
	}

	@Override
	@Deprecated
	public String createContainer(IDockerContainerConfig c)
			throws DockerException, InterruptedException {
		IDockerHostConfig hc = new DockerHostConfig(HostConfig.builder()
				.build());
		return createContainer(c, hc);
	}

	@Override
	@Deprecated
	public String createContainer(final IDockerContainerConfig c,
			final String containerName)
			throws DockerException, InterruptedException {
		IDockerHostConfig hc = new DockerHostConfig(HostConfig.builder()
				.build());
		return createContainer(c, hc, containerName);
	}

	@Override
	public String createContainer(final IDockerContainerConfig c,
			final IDockerHostConfig hc) throws DockerException,
			InterruptedException {
		return createContainer(c, hc, null);
	}

	@Override
	public String createContainer(final IDockerContainerConfig c,
			IDockerHostConfig hc,
			final String containerName)
			throws DockerException, InterruptedException {

		try {
			HostConfig.Builder hbuilder = HostConfig.builder()
					.containerIDFile(hc.containerIDFile())
					.publishAllPorts(hc.publishAllPorts())
					.privileged(hc.privileged()).networkMode(hc.networkMode());
			if (hc.binds() != null)
				hbuilder.binds(hc.binds());
			if (hc.dns() != null)
				hbuilder.dns(hc.dns());
			if (hc.dnsSearch() != null)
				hbuilder.dnsSearch(hc.dnsSearch());
			if (hc.links() != null)
				hbuilder.links(hc.links());
			if (hc.lxcConf() != null) {
				List<IDockerConfParameter> lxcconf = hc.lxcConf();
				ArrayList<LxcConfParameter> lxcreal = new ArrayList<>();
				for (IDockerConfParameter param : lxcconf) {
					lxcreal.add(new LxcConfParameter(param.key(), param.value()));
				}
				hbuilder.lxcConf(lxcreal);
			}
			if (hc.portBindings() != null) {
				Map<String, List<IDockerPortBinding>> bindings = hc
						.portBindings();
				HashMap<String, List<PortBinding>> realBindings = new HashMap<>();

				for (Entry<String, List<IDockerPortBinding>> entry : bindings
						.entrySet()) {
					String key = entry.getKey();
					List<IDockerPortBinding> bindingList = entry.getValue();
					ArrayList<PortBinding> newList = new ArrayList<>();
					for (IDockerPortBinding binding : bindingList) {
						newList.add(PortBinding.of(binding.hostIp(),
								binding.hostPort()));
					}
					realBindings.put(key, newList);
				}
				hbuilder.portBindings(realBindings);
			}
			if (hc.volumesFrom() != null)
				hbuilder.volumesFrom(hc.volumesFrom());

			ContainerConfig.Builder builder = ContainerConfig.builder()
					.hostname(c.hostname()).domainname(c.domainname())
					.user(c.user()).memory(c.memory())
					.memorySwap(c.memorySwap()).cpuShares(c.cpuShares())
					.cpuset(c.cpuset()).attachStdin(c.attachStdin())
					.attachStdout(c.attachStdout())
					.attachStderr(c.attachStderr()).tty(c.tty())
					.openStdin(c.openStdin()).stdinOnce(c.stdinOnce())
					.cmd(c.cmd()).image(c.image())
					.hostConfig(hbuilder.build())
					.workingDir(c.workingDir())
					.networkDisabled(c.networkDisabled());
			// For those fields that are Collections and not set, they will be null.
			// We can't use their values to set the builder's fields as they are
			// expecting non-null Collections to copy over. In those cases, we just
			// don't set those fields in the builder.
			if (c.portSpecs() != null) {
				builder = builder.portSpecs(c.portSpecs());
			}
			if (c.exposedPorts() != null) {
				builder = builder.exposedPorts(c.exposedPorts());
			}
			if (c.env() != null) {
				builder = builder.env(c.env());
			}
			if (c.volumes() != null) {
				builder = builder.volumes(c.volumes());
			}
			if (c.entrypoint() != null) {
				builder = builder.entrypoint(c.entrypoint());
			}
			if (c.onBuild() != null) {
				builder = builder.onBuild(c.onBuild());
			}

			// create container with default random name if an empty/null
			// containerName argument was passed
			final ContainerCreation creation = client
					.createContainer(builder.build(),
					(containerName != null && !containerName.isEmpty())
							? containerName : null);
			final String id = creation.id();
			// force a refresh of the current containers to include the new one
			listContainers();
			return id;
		} catch (ContainerNotFoundException e) {
			throw new DockerContainerNotFoundException(e);
		} catch (com.spotify.docker.client.DockerRequestException e) {
			throw new DockerException(e.message());
		} catch (com.spotify.docker.client.DockerException e) {
			throw new DockerException(e);
		}
	}

	@Override
	public void stopContainer(final String id) throws DockerException,
			InterruptedException {
		try {
			// stop container or kill after 10 seconds
			client.stopContainer(id, 10); // allow up to 10 seconds to stop
			synchronized (loggingThreads) {
				if (loggingThreads.containsKey(id)) {
					loggingThreads.get(id).kill();
					loggingThreads.remove(id);
				}
			}
			// list of containers needs to be updated once the given container is stopped, to reflect it new state.
			listContainers();
		} catch (ContainerNotFoundException e) {
			throw new DockerContainerNotFoundException(e);
		} catch (com.spotify.docker.client.DockerRequestException e) {
			throw new DockerException(e.message());
		} catch (com.spotify.docker.client.DockerException e) {
			throw new DockerException(e.getMessage(), e.getCause());
		}
	}

	@Override
	public void killContainer(final String id) throws DockerException,
			InterruptedException {
		try {
			// kill container
			client.killContainer(id);
			synchronized (loggingThreads) {
				if (loggingThreads.containsKey(id)) {
					loggingThreads.get(id).kill();
					loggingThreads.remove(id);
				}
			}
			listContainers(); // update container list
		} catch (ContainerNotFoundException e) {
			throw new DockerContainerNotFoundException(e);
		} catch (com.spotify.docker.client.DockerRequestException e) {
			throw new DockerException(e.message());
		} catch (com.spotify.docker.client.DockerException e) {
			throw new DockerException(e.getMessage(), e.getCause());
		}
	}

	@Override
	public void pauseContainer(final String id) throws DockerException,
			InterruptedException {
		try {
			// pause container
			client.pauseContainer(id);
			listContainers(); // update container list
		} catch (ContainerNotFoundException e) {
			throw new DockerContainerNotFoundException(e);
		} catch (com.spotify.docker.client.DockerRequestException e) {
			throw new DockerException(e.message());
		} catch (com.spotify.docker.client.DockerException e) {
			throw new DockerException(e.getMessage(), e.getCause());
		}
	}

	@Override
	public void unpauseContainer(final String id, final OutputStream stream)
			throws DockerException, InterruptedException {
		try {
			// unpause container
			client.unpauseContainer(id);
			if (stream != null) {
				synchronized (loggingThreads) {
					LogThread t = loggingThreads.get(id);
					if (t == null || !t.isAlive()) {
						t = new LogThread(id, getClientCopy(), true);
						loggingThreads.put(id, t);
						t.setOutputStream(stream);
						t.start();
					} else {
						// we aren't going to use the stream given...close it
						try {
							stream.close();
						} catch (IOException e) {
							// do nothing...we tried to close the stream
						}
					}
				}
			}
			listContainers(); // update container list
		} catch (ContainerNotFoundException e) {
			throw new DockerContainerNotFoundException(e);
		} catch (com.spotify.docker.client.DockerRequestException e) {
			throw new DockerException(e.message());
		} catch (com.spotify.docker.client.DockerException e) {
			throw new DockerException(e.getMessage(), e.getCause());
		}
	}

	@Override
	public void removeContainer(final String id) throws DockerException,
			InterruptedException {
		try {
			// kill container
			client.removeContainer(id);
			listContainers(); // update container list
		} catch (ContainerNotFoundException e) {
			throw new DockerContainerNotFoundException(e);
		} catch (com.spotify.docker.client.DockerRequestException e) {
			throw new DockerException(e.message());
		} catch (com.spotify.docker.client.DockerException e) {
			throw new DockerException(e.getMessage(), e.getCause());
		}
	}

	@Override
	@Deprecated
	public void startContainer(String id, IDockerHostConfig config,
			OutputStream stream)
			throws DockerException, InterruptedException {
		startContainer(id, stream);
	}

	@Override
	@Deprecated
	public void startContainer(String id, String loggingId,
			IDockerHostConfig config, OutputStream stream)
			throws DockerException, InterruptedException {
		startContainer(id, loggingId, stream);
	}

	@Override
	public void startContainer(final String id, final OutputStream stream)
			throws DockerException, InterruptedException {
		try {
			// start container
			client.startContainer(id);
			// Log the started container if a stream is provided
			final IDockerContainerInfo containerInfo = getContainerInfo(id);
			if (stream != null && containerInfo != null
					&& containerInfo.config() != null
					&& !containerInfo.config().tty()) {
				// display logs for container
				synchronized (loggingThreads) {
					LogThread t = loggingThreads.get(id);
					if (t == null || !t.isAlive()) {
						t = new LogThread(id, getClientCopy(), true);
						loggingThreads.put(id, t);
						t.setOutputStream(stream);
						t.start();
					}
				}
			}
			// list of containers needs to be refreshed once the container started, to reflect it new state.
			listContainers(); 
		} catch (ContainerNotFoundException e) {
			throw new DockerContainerNotFoundException(e);
		} catch (com.spotify.docker.client.DockerRequestException e) {
			throw new DockerException(e.message());
		} catch (com.spotify.docker.client.DockerException e) {
			throw new DockerException(e.getMessage(), e.getCause());
		}
	}

	@Override
	public void startContainer(String id, String loggingId, OutputStream stream)
			throws DockerException, InterruptedException {
		try {
			// start container with host config
			client.startContainer(id);
			// Log the started container based on user preference
			// Log the started container based on user preference
			// Log the started container based on user preference
			IEclipsePreferences preferences = InstanceScope.INSTANCE
					.getNode("org.eclipse.linuxtools.docker.ui"); //$NON-NLS-1$

			boolean autoLog = preferences.getBoolean("autoLogOnStart", true); //$NON-NLS-1$

			if (autoLog && !getContainerInfo(id).config().tty()) {
				synchronized (loggingThreads) {
					LogThread t = loggingThreads.get(loggingId);
					if (t == null || !t.isAlive()) {
						t = new LogThread(id, getClientCopy(), true);
						loggingThreads.put(loggingId, t);
						t.setOutputStream(stream);
						t.start();
					}
				}
			}
			// update container list
			listContainers();
		} catch (ContainerNotFoundException e) {
			throw new DockerContainerNotFoundException(e);
		} catch (com.spotify.docker.client.DockerRequestException e) {
			throw new DockerException(e.message());
		} catch (com.spotify.docker.client.DockerException e) {
			throw new DockerException(e);
		}
	}

	@Override
	public void commitContainer(final String id, final String repo, final String tag,
			final String comment, final String author) throws DockerException {
		ContainerInfo info;
		try {
			info = client.inspectContainer(id);
			client.commitContainer(id, repo, tag, info.config(), comment,
					author);
			// update images list
			listImages();
			getImages(true);
		} catch (com.spotify.docker.client.DockerRequestException e) {
			throw new DockerException(e.message());
		} catch (com.spotify.docker.client.DockerException
				| InterruptedException e) {
			throw new DockerException(e.getMessage(), e.getCause());
		}
	}

	public EnumDockerLoggingStatus loggingStatus(final String id) {
		synchronized (loggingThreads) {
			LogThread t = loggingThreads.get(id);
			if (t == null)
				return EnumDockerLoggingStatus.LOGGING_NONE;
			if (t.isAlive())
				return EnumDockerLoggingStatus.LOGGING_ACTIVE;
			return EnumDockerLoggingStatus.LOGGING_COMPLETE;
		}
	}

	@Override
	public void stopLoggingThread(final String id) throws InterruptedException {
		synchronized (loggingThreads) {
			LogThread t = loggingThreads.get(id);
			if (t != null)
				t.requestStop();
		}
		while (loggingStatus(id) == EnumDockerLoggingStatus.LOGGING_ACTIVE) {
			Thread.sleep(1000);
		}

	}

	@Override
	public void logContainer(final String id, final OutputStream stream)
			throws DockerException, InterruptedException {
		try {
			// Figure out if we are logging a running container or not
			// Pass that info to see whether the LogThread should just terminate
			// or keep running
			synchronized (loggingThreads) {
				ContainerInfo info = client.inspectContainer(id);
				LogThread t = loggingThreads.get(id);
				if (t == null || !t.isAlive()) {
					t = new LogThread(id, getClientCopy(), info.state()
							.running());
					loggingThreads.put(id, t);
					t.setOutputStream(stream);
					t.start();
				} else {
					// we aren't going to use the stream given...close it
					try {
						stream.close();
					} catch (IOException e) {
						// do nothing...we tried to close the stream
					}
				}
			}
		} catch (ContainerNotFoundException e) {
			throw new DockerContainerNotFoundException(e);
		} catch (com.spotify.docker.client.DockerRequestException e) {
			throw new DockerException(e.message());
		} catch (com.spotify.docker.client.DockerException e) {
			throw new DockerException(e.getMessage(), e.getCause());
		}
	}

	public IDockerContainerExit waitForContainer(final String id)
			throws DockerException, InterruptedException {
		try {
			// wait for container to exit
			DockerClient copy = getClientCopy();
			ContainerExit x = copy.waitContainer(id);
			DockerContainerExit exit = new DockerContainerExit(x.statusCode());
			listContainers(); // update container list
			copy.close(); // dispose of copy now we are finished
			return exit;
		} catch (ContainerNotFoundException e) {
			throw new DockerContainerNotFoundException(e);
		} catch (com.spotify.docker.client.DockerRequestException e) {
			throw new DockerException(e.message());
		} catch (com.spotify.docker.client.DockerException e) {
			throw new DockerException(e.getMessage(), e.getCause());
		}
	}

	public WritableByteChannel attachCommand(final String id,
			final InputStream in, final OutputStream out)
					throws DockerException {

		final byte[] prevCmd = new byte[1024];
		try {
			final LogStream pty_stream = client.attachContainer(id,
					AttachParameter.STDIN, AttachParameter.STDOUT,
					AttachParameter.STDERR, AttachParameter.STREAM,
					AttachParameter.LOGS);
			final boolean isTtyEnabled = getContainerInfo(id).config().tty();

			// Data from the given input stream
			// Written to container's STDIN
			Thread t_in = new Thread(new Runnable() {
				@Override
				public void run() {
					byte[] buff = new byte[1024];
					int n;
					try {
						WritableByteChannel pty_out = HttpHijackWorkaround
								.getOutputStream(pty_stream, getUri());
						while ((n = in.read(buff)) != -1
								&& getContainerInfo(id).state().running()) {
							synchronized (prevCmd) {
								pty_out.write(ByteBuffer.wrap(buff, 0, n));
								for (int i = 0; i < prevCmd.length; i++) {
									prevCmd[i] = buff[i];
								}
							}
							buff = new byte[1024];
						}
					} catch (Exception e) {
					}
				}
			});

			t_in.start();
			// Incoming data from container's STDOUT
			// Written to the given output stream
			Thread t_out = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						InputStream pty_in = HttpHijackWorkaround
								.getInputStream(pty_stream);
						while (getContainerInfo(id).state().running()) {
							byte[] buff = new byte[1024];
							int n = pty_in.read(buff);
							if (n > 0) {
								/*
								 * The container's STDOUT contains initial input
								 * we sent to its STDIN and the result. eg. >
								 * echo once < echo once \n $ once
								 * 
								 * Try to remove this unwanted data from the
								 * stream.
								 */
								if (isTtyEnabled) {
									int idex = 0;
									synchronized (prevCmd) {
										/*
										 * Check if buff contains a prefix of
										 * prevCmd ignoring differences in
										 * carriage return (10,13). Save the
										 * prefix's ending index.
										 */
										for (int i = 0; i < prevCmd.length; i++) {
											if (prevCmd[i] != buff[i]
													&& (prevCmd[i] != 10 && buff[i] != 13)
													&& (prevCmd[i] != 13 && buff[i] != 10)
													&& prevCmd[i] != 0) {
												idex = 0;
												break;
											} else if (prevCmd[i] != 0) {
												idex++;
											}
										}
									}
									// A prefix exists, remove it
									// Do not include the ending NL/CR
									if (idex != 0) {
										shiftLeft(buff, idex + 1);
									}
									n = removeTerminalCodes(buff);
								} else {
									/*
									 * If not in TTY mode, first 8 bytes are
									 * header data describing payload which we
									 * don't need.
									 */
									shiftLeft(buff, 8);
									n = n - 8;
								}
								out.write(buff, 0, n);
							}
						}
					} catch (Exception e) {
						/*
						 * Temporary workaround for BZ #469717
						 * Remove this when we begin using a release with :
						 * https://github.com/spotify/docker-client/pull/223
						 */
						if (e instanceof SocketTimeoutException) {
							try {
								attachCommand(id, in, out);
							} catch (DockerException e1) {
							}
						}
					}
				}
			});

			/*
			 * Our handling of STDOUT for terminals is mandatory, but the
			 * logging framework can handle catching output very early so use it
			 * for now.
			 */
			if (isTtyEnabled) {
				t_out.start();
			}

			return HttpHijackWorkaround.getOutputStream(pty_stream, getUri());
		} catch (Exception e) {
			throw new DockerException(e.getMessage(), e.getCause());
		}
	}

	/*
	 * Incoming data from container's STDOUT contains terminal codes which the
	 * Eclipse Console does not support. Either we install/use some terminal
	 * plugin that does, or we need to remove these.
	 */
	private static int removeTerminalCodes(final byte[] buff) {
		String tmp = new String(buff);
		byte[] tmp_buff = tmp.replaceAll("\u001B]0;.*\u0007", "")
				.replaceAll("\u001B\\[([0-9]{1,2}(;[0-9]{1,2})?)?[mK]", "")
				.replaceAll("\u001B\\[\\?[0-9]{1,4}h\\[", "").getBytes();
		for (int i = 0; i < buff.length; i++) {
			if (i >= tmp_buff.length) {
				buff[i] = 0;
			} else {
				buff[i] = tmp_buff[i];
			}
		}
		return getByteLength(buff);
	}

	/*
	 * Shift contents of buff[idex] .. buff[buff.length-1] to buff[0] ..
	 * buff[(buff.length-1) - idex]
	 */
	private static void shiftLeft(byte[] buff, int idex) {
		for (int i = 0; i < buff.length; i++) {
			if (idex + i < buff.length) {
				buff[i] = buff[idex + i];
			} else {
				buff[i] = 0;
			}
		}
	}

	/*
	 * Get the number of non-zero bytes from the beginning of the byte array.
	 */
	private static int getByteLength(byte[] buff) {
		int n;
		for (n = 0; n < buff.length; n++) {
			if (buff[n] == 0) {
				break;
			}
		}
		if ((n == buff.length - 1) && buff[buff.length - 1] != 0) {
			return buff.length;
		}
		return n;
	}

	@Override
	public String getTcpCertPath() {
		return tcpCertPath;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DockerConnection other = (DockerConnection) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return name;
	}

}
