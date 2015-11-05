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

import java.util.UUID;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.linuxtools.docker.ui.Activator;
import org.eclipse.linuxtools.internal.docker.ui.databinding.BaseDatabindingModel;
import org.eclipse.linuxtools.internal.docker.ui.wizards.ImageRunResourceVolumesVariablesModel.MountType;

/**
 * @author xcoulon
 *
 */
public class DataVolumeModel extends BaseDatabindingModel
		implements Comparable<DataVolumeModel> {

	public static final String CONTAINER_PATH = "containerPath"; //$NON-NLS-1$

	public static final String MOUNT_TYPE = "mountType"; //$NON-NLS-1$

	public static final String MOUNT = "mount"; //$NON-NLS-1$

	public static final String HOST_PATH_MOUNT = "hostPathMount"; //$NON-NLS-1$

	public static final String READ_ONLY_VOLUME = "readOnly"; //$NON-NLS-1$

	public static final String CONTAINER_MOUNT = "containerMount"; //$NON-NLS-1$

	private final String id = UUID.randomUUID().toString();

	private String containerPath;

	private MountType mountType;

	private String mount;

	private String hostPathMount;

	private String containerMount;

	private boolean readOnly = false;

	/**
	 * Parses the given value and returns an instance of {@link DataVolumeModel}
	 * .
	 * 
	 * @param hostVolume
	 *            the value to parse
	 * @return the {@link DataVolumeModel} or <code>null</code> if parsing
	 *         failed.
	 */
	public static DataVolumeModel hostVolumeFromString(
			final String hostVolume) {
		final String[] items = hostVolume.split(":");
		if (items.length == 3) {
			final String hostPath = items[0];
			final String containerPath = items[1];
			final boolean readOnly = items[2].indexOf("ro") != -1;
			return new DataVolumeModel(containerPath, hostPath, readOnly);
		}
		Activator.log(new Status(IStatus.WARNING, Activator.PLUGIN_ID,
				WizardMessages.getFormattedString(
						"DataVolumeModel.unableToParseHostVolume", //$NON-NLS-1$
						hostVolume)));
		return null;
	}

	public DataVolumeModel() {
	}

	public DataVolumeModel(final String containerPath) {
		this.containerPath = containerPath;
		this.mountType = MountType.NONE;
	}

	public DataVolumeModel(final String containerPath, final String hostPath,
			final boolean readOnly) {
		this.containerPath = containerPath;
		this.mountType = MountType.HOST_FILE_SYSTEM;
		this.hostPathMount = hostPath;
		this.mount = this.hostPathMount;
		this.readOnly = readOnly;
	}

	public DataVolumeModel(final DataVolumeModel selectedDataVolume) {
		this.containerPath = selectedDataVolume.getContainerPath();
		this.mountType = selectedDataVolume.getMountType();
		if (this.mountType != null) {
			switch (this.mountType) {
			case CONTAINER:
				this.containerMount = selectedDataVolume.getMount();
				break;
			case HOST_FILE_SYSTEM:
				this.hostPathMount = selectedDataVolume.getMount();
				this.readOnly = selectedDataVolume.isReadOnly();
				break;
			case NONE:
				break;
			}
		} else {
			this.mountType = MountType.NONE;
		}
	}

	public String getContainerPath() {
		return this.containerPath;
	}

	public void setContainerPath(final String containerPath) {
		firePropertyChange(CONTAINER_PATH, this.containerPath,
				this.containerPath = containerPath);
	}

	public String getMount() {
		return mount;
	}

	public void setMount(final String mount) {
		firePropertyChange(MOUNT, this.mount, this.mount = mount);
	}

	public MountType getMountType() {
		return mountType;
	}

	public void setMountType(final MountType mountType) {
		// ignore 'null' assignments that may come from the UpdateStrategy
		// in
		// the EditDataVolumePage when a radion button is unselected.
		if (mountType == null) {
			return;
		}
		firePropertyChange(MOUNT_TYPE, this.mountType,
				this.mountType = mountType);
		if (this.mountType == MountType.NONE) {
			setMount("");
		}

	}

	public String getHostPathMount() {
		return hostPathMount;
	}

	public void setHostPathMount(final String hostPathMount) {
		firePropertyChange(HOST_PATH_MOUNT, this.hostPathMount,
				this.hostPathMount = hostPathMount);
		if (this.mountType == MountType.HOST_FILE_SYSTEM) {
			setMount(this.hostPathMount);
		}
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	public void setReadOnly(final boolean readOnly) {
		firePropertyChange(READ_ONLY_VOLUME, this.readOnly,
				this.readOnly = readOnly);
	}

	public String getContainerMount() {
		return this.containerMount;
	}

	public void setContainerMount(final String containerMount) {
		firePropertyChange(CONTAINER_MOUNT, this.containerMount,
				this.containerMount = containerMount);
		if (this.mountType == MountType.CONTAINER) {
			setMount(this.containerMount);
		}
	}

	@Override
	public int compareTo(final DataVolumeModel other) {
		return this.getContainerPath().compareTo(other.getContainerPath());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
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
		DataVolumeModel other = (DataVolumeModel) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

}
