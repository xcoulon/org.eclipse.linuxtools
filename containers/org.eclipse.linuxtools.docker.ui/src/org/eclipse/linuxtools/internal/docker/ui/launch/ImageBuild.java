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

package org.eclipse.linuxtools.internal.docker.ui.launch;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.linuxtools.docker.core.EnumImageBuildParameter;
import org.eclipse.linuxtools.docker.core.IDockerConnection;

/**
 * @author xcoulon
 *
 */
public class ImageBuild extends Wizard {

	private final ImageBuildPage buildPage;

	public ImageBuild(final IFile dockerFile) {
		this.buildPage = new ImageBuildPage(dockerFile);
	}

	@Override
	public void addPages() {
		addPage(buildPage);
	}

	@Override
	public boolean performFinish() {
		buildPage.saveState();
		return true;
	}

	public IDockerConnection getConnection() {
		return buildPage.getConnection();
	}

	public String getImageName() {
		return buildPage.getImageRepoTag();
	}

	public List<EnumImageBuildParameter> getBuildOptions() {
		return buildPage.getBuildOptions();
	}

}
