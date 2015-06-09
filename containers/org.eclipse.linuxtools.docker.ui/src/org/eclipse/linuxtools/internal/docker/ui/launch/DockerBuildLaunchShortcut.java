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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.ui.ILaunchShortcut2;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.linuxtools.docker.core.DockerException;
import org.eclipse.linuxtools.docker.ui.Activator;
import org.eclipse.linuxtools.internal.docker.ui.jobs.ImageBuildJob;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.part.FileEditorInput;

/**
 * Shortcut launcher to trigger a build image operation from a selected
 * Dockerfile.
 * 
 * @author xcoulon
 *
 */
public class DockerBuildLaunchShortcut implements ILaunchShortcut2 {

	@Override
	public void launch(final ISelection selection, final String mode) {
		final IFile dockerfile = getDockerFile(selection);
		buildImage(dockerfile);
	}

	@Override
	public void launch(final IEditorPart editor, final String mode) {
		final IFile dockerfile = getDockerFile(editor);
		buildImage(dockerfile);
	}

	private void buildImage(final IFile dockerfile) {
		try {
		if (dockerfile != null && dockerfile.exists()) {
			final ImageBuild wizard = new ImageBuild(dockerfile);
			final boolean createImage = openWizard(wizard, "Docker build", //$NON-NLS-1$
					Display.getDefault().getActiveShell());
			if (createImage) {
				final Job buildImageJob = new ImageBuildJob(
						wizard.getConnection(), wizard.getImageName(),
						dockerfile, wizard.getBuildOptions());
				buildImageJob.setPriority(Job.LONG);
				buildImageJob.schedule();

			}
		}
		} catch (DockerException e) {
			Activator.log(e);
		}
	}

	private IFile getDockerFile(final IEditorPart editor) {
		final FileEditorInput editorInput = (FileEditorInput) editor
				.getEditorInput();
		return editorInput.getFile();
	}

	private IFile getDockerFile(final ISelection selection) {
		if (selection instanceof IStructuredSelection
				&& ((IStructuredSelection) selection)
						.getFirstElement() instanceof IFile) {
			return (IFile) ((IStructuredSelection) selection).getFirstElement();
		}
		return null;
	}

	private boolean openWizard(final Wizard wizard, final String title,
			final Shell activeShell) {
		WizardDialog wizardDialog = new WizardDialog(activeShell, wizard);
		wizardDialog.setTitle(title);
		wizardDialog.create();
		wizardDialog.getShell().setSize(450, 350);
		return wizardDialog.open() == Window.OK;
	}

	@Override
	public ILaunchConfiguration[] getLaunchConfigurations(
			final ISelection selection) {
		return null;
	}

	@Override
	public ILaunchConfiguration[] getLaunchConfigurations(
			final IEditorPart editorpart) {
		return null;
	}

	@Override
	public IResource getLaunchableResource(final ISelection selection) {
		return null;
	}

	@Override
	public IResource getLaunchableResource(final IEditorPart editorpart) {
		return null;
	}

}
