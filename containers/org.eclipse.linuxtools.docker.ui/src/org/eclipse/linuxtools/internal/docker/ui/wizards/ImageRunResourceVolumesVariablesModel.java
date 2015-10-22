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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.linuxtools.docker.core.DockerException;
import org.eclipse.linuxtools.docker.core.IDockerConnection;
import org.eclipse.linuxtools.docker.core.IDockerConnectionInfo;
import org.eclipse.linuxtools.docker.core.IDockerImage;
import org.eclipse.linuxtools.docker.core.IDockerImageInfo;
import org.eclipse.linuxtools.internal.docker.ui.databinding.BaseDatabindingModel;

/**
 * Databinding model for the {@link ImageRunResourceVolumesVariablesPage}
 *
 * @author xcoulon
 *
 */
public class ImageRunResourceVolumesVariablesModel
		extends BaseDatabindingModel {

	public enum MountType {
		NONE, HOST_FILE_SYSTEM, CONTAINER;
	}

	/** the 'low' CPU share weight variableValue. */
	public static final int LOW = 512;

	/** the default 'medium' CPU share weight variableValue. */
	public static final int MEDIUM = 1024;

	/** the 'high' CPU share weight variableValue. */
	public static final int HIGH = 2048;

	public static final String ENABLE_RESOURCE_LIMITATIONS = "enableResourceLimitations"; //$NON-NLS-1$

	public static final String CPU_SHARE_WEIGHT = "cpuShareWeight"; //$NON-NLS-1$

	public static final String MEMORY_LIMIT = "memoryLimit"; //$NON-NLS-1$

	public static final String DATA_VOLUMES = "dataVolumes"; //$NON-NLS-1$

	public static final String SELECTED_DATA_VOLUMES = "selectedDataVolumes"; //$NON-NLS-1$

	public static final String ENVIRONMENT_VARIABLES = "environmentVariables"; //$NON-NLS-1$

	private boolean enableResourceLimitations = false;

	private final IDockerConnection connection;

	private final IDockerConnectionInfo info;

	private IDockerImageInfo imageInfo = null;

	private int memoryLimit = 512;

	private int cpuShareWeighting = 1024;

	private Set<DataVolumeModel> selectedDataVolumes = new HashSet<>();

	private WritableList dataVolumes = new WritableList();

	private WritableList environmentVariables = new WritableList();

	private IDockerImage selectedImage;

	public ImageRunResourceVolumesVariablesModel(
			final IDockerConnection connection) throws DockerException {
		this.connection = connection;
		this.info = connection.getInfo();
	}

	public ImageRunResourceVolumesVariablesModel(
			final IDockerImage selectedImage) throws DockerException {
		this(selectedImage.getConnection());
		this.selectedImage = selectedImage;
	}

	public IDockerConnection getConnection() {
		return connection;
	}

	/**
	 * Refreshes the list of Volumes to display in the for the given
	 * 
	 * @param selectedImage
	 */
	public void setSelectedImage(final IDockerImage selectedImage) {
		if (this.selectedImage == null
				|| !this.selectedImage.equals(selectedImage)) {
			this.selectedImage = selectedImage;
				this.imageInfo = selectedImage.getConnection()
						.getImageInfo(selectedImage.id());
			final List<DataVolumeModel> volumes = new ArrayList<>();
			for (String volume : this.imageInfo.config().volumes()) {
				volumes.add(new DataVolumeModel(volume));
			}
			setDataVolumes(volumes);
		}

	}

	public IDockerImage getSelectedImage() {
		return selectedImage;
	}

	public IDockerImageInfo getSelectedImageInfo() {
		return imageInfo;
	}

	public WritableList getDataVolumes() {
		return dataVolumes;
	}

	public void setDataVolumes(final Collection<DataVolumeModel> volumes) {
		this.dataVolumes.clear();
		if (volumes != null) {
			this.dataVolumes.addAll(volumes);
		}
	}

	public void removeDataVolume(final DataVolumeModel dataVolume) {
		this.dataVolumes.remove(dataVolume);
	}

	public Set<DataVolumeModel> getSelectedDataVolumes() {
		return selectedDataVolumes;
	}

	public void setSelectedDataVolumes(
			final Set<DataVolumeModel> selectedDataVolumes) {
		firePropertyChange(SELECTED_DATA_VOLUMES, this.selectedDataVolumes,
				this.selectedDataVolumes = selectedDataVolumes);
	}

	public WritableList getEnvironmentVariables() {
		return environmentVariables;
	}

	public void setEnvironmentVariables(
			final List<String> environmentVariables) {
		this.environmentVariables.clear();
		if (environmentVariables != null) {
			for (String envVariable : environmentVariables) {
				// pattern is "<name>=<value>"
				final String[] items = envVariable.split("=");
				if (items.length == 2) {
					this.environmentVariables.add(
							new EnvironmentVariableModel(items[0], items[1]));
				}
			}
		}
	}

	public void removeEnvironmentVariable(
			final EnvironmentVariableModel variable) {
		this.environmentVariables.remove(variable);
	}

	/**
	 * @return the total memory of the Docker daemon, in MB
	 * @throws DockerException
	 */
	public int getTotalMemory() {
		return (int) (this.info.getTotalMemory() / 1048576);
	}

	public boolean isEnableResourceLimitations() {
		return enableResourceLimitations;
	}

	public void setEnableResourceLimitations(
			boolean enableResourceLimitations) {
		firePropertyChange(ENABLE_RESOURCE_LIMITATIONS,
				this.enableResourceLimitations,
				this.enableResourceLimitations = enableResourceLimitations);
	}

	/**
	 * The memory allocated for the container, in MB.
	 * 
	 * @return
	 */
	public int getMemoryLimit() {
		return memoryLimit;
	}

	/**
	 * The memory allocated for the container, in Bytes.
	 * 
	 * @return
	 */
	public long getMemory() {
		return memoryLimit * 1048576;
	}

	public void setMemoryLimit(final int memoryLimit) {
		firePropertyChange(MEMORY_LIMIT, this.memoryLimit,
				this.memoryLimit = memoryLimit);
	}

	public int getCpuShareWeight() {
		return cpuShareWeighting;
	}

	public void setCpuShareWeight(final int cpuShareWeighting) {
		firePropertyChange(CPU_SHARE_WEIGHT, this.cpuShareWeighting,
				this.cpuShareWeighting = cpuShareWeighting);
	}

}
