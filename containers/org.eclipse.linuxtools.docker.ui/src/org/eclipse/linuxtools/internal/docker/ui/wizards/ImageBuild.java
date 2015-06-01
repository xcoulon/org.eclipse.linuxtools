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

import java.io.IOException;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.linuxtools.internal.docker.ui.utils.ImageBuildUtils;

public class ImageBuild extends Wizard {

	private ImageBuildPage mainPage;
	private String imageName;
	private IPath directory;
	private int lines;

	public ImageBuild() {
		super();
	}

	public String getImageName() {
		return imageName;
	}

	public IPath getDirectory() {
		return directory;
	}

	public int getNumberOfLines() {
		return lines;
	}

	@Override
	public void addPages() {
		mainPage = new ImageBuildPage();
		addPage(mainPage);
	}

	@Override
	public boolean canFinish() {
		return mainPage.isPageComplete();
	}

	@Override
	public boolean performFinish() {
		imageName = mainPage.getImageName();
		directory = new Path(mainPage.getDirectory());

		try {
			lines = ImageBuildUtils
					.numberOfLines(directory.append("Dockerfile").toString());
		} catch (IOException e) {
			// do nothing
		}

		return true;
	}

}
