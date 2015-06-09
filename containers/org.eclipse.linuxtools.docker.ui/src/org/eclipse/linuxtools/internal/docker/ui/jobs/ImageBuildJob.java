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

package org.eclipse.linuxtools.internal.docker.ui.jobs;

import static org.eclipse.linuxtools.internal.docker.ui.commands.BuildImageCommandHandler.ERROR_BUILDING_IMAGE;

import java.io.IOException;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.linuxtools.docker.core.DockerException;
import org.eclipse.linuxtools.docker.core.DockerImageBuildFailedException;
import org.eclipse.linuxtools.docker.core.EnumImageBuildParameter;
import org.eclipse.linuxtools.docker.core.IDockerConnection;
import org.eclipse.linuxtools.docker.core.IDockerImage;
import org.eclipse.linuxtools.docker.core.IDockerProgressHandler;
import org.eclipse.linuxtools.docker.core.IDockerProgressMessage;
import org.eclipse.linuxtools.docker.ui.Activator;
import org.eclipse.linuxtools.internal.docker.ui.BuildConsole;
import org.eclipse.linuxtools.internal.docker.ui.utils.ImageBuildUtils;
import org.eclipse.linuxtools.internal.docker.ui.views.DVMessages;
import org.eclipse.swt.widgets.Display;

/**
 * A {@link Job} to call and progressMonitor the build of an
 * {@link IDockerImage}
 * 
 * @author xcoulon
 *
 */
public class ImageBuildJob extends Job implements IDockerProgressHandler {

	private static final String BUILD_IMAGE_JOB_TITLE = "ImageBuildLog.title";
	private final static String IMAGE_BUILD_COMPLETE = "ImageBuildComplete.msg"; //$NON-NLS-1$
	private final static String IMAGE_BUILD_STEP = "ImageBuildStep.msg"; //$NON-NLS-1$

	/** The {@link IDockerConnection} to use. */
	private final IDockerConnection connection;

	/** The DockerFile used to build the image. */
	private final IResource dockerfile;

	/** Thr number of steps to build the image. */
	private final int numberOfBuildOperations;

	/** The name to give to the image. */
	private final String imageName;

	/** The optional build parameters. */
	private final List<EnumImageBuildParameter> buildParameters;

	/** The console used to display build output messages. */
	private final BuildConsole console;

	/** The progress progressMonitor associated with this {@link Job}. */
	private IProgressMonitor progressMonitor;

	/**
	 * Constructor
	 * 
	 * @param connection
	 * @param imageName
	 * @param dockerfile
	 * @param buildParameters
	 * @throws IOException
	 */
	public ImageBuildJob(final IDockerConnection connection,
			final String imageName, final IResource dockerfile,
			final List<EnumImageBuildParameter> buildParameters)
					throws DockerException {
		super(DVMessages.getFormattedString(BUILD_IMAGE_JOB_TITLE, imageName));
		this.connection = connection;
		this.imageName = imageName;
		this.dockerfile = dockerfile;
		this.buildParameters = buildParameters;
		this.console = BuildConsole.findConsole(imageName);
		try {
			this.numberOfBuildOperations = ImageBuildUtils
					.numberOfLines(dockerfile.getLocation().toOSString());
		} catch (IOException e) {
			throw new DockerException(
					"Failed to count the number of steps in the given Dockerfile",
					e);
		}
	}

	@Override
	protected IStatus run(final IProgressMonitor progressMonitor) {
		try {
			if (numberOfBuildOperations == 0) {
				Activator.log(new Status(IStatus.WARNING, Activator.PLUGIN_ID,
						"Skipping empty Docker build file."));
			} else {
				this.console.clearConsole();
				this.progressMonitor = progressMonitor;
				this.progressMonitor
						.beginTask(
								DVMessages.getFormattedString(
										BUILD_IMAGE_JOB_TITLE, imageName),
						numberOfBuildOperations + 1);
				final IContainer parentFolder = dockerfile.getParent();
				connection.buildImage(parentFolder.getLocation(), imageName,
						this, buildParameters);
			}
		} catch (DockerException | InterruptedException e) {
			Display.getDefault().syncExec(new Runnable() {
				@Override
				public void run() {
					MessageDialog
							.openError(Display.getCurrent().getActiveShell(),
									DVMessages.getFormattedString(
											ERROR_BUILDING_IMAGE, imageName),
							e.getMessage());
				}

			});
		}
		// make sure the progress monitor is 'done' even if the build failed or
		// timed out.
		this.progressMonitor.done();
		return Status.OK_STATUS;
	}

	@Override
	public void processMessage(final IDockerProgressMessage message)
			throws DockerException {
		if (message.error() != null) {
			cancel();
			throw new DockerImageBuildFailedException(imageName,
					message.error());
		}
		// For imageName build, all the data is in the stream.
		final String status = message.stream();
		if (status != null && status
				.startsWith(DVMessages.getString(IMAGE_BUILD_COMPLETE))) {
			// refresh images
			connection.getImages(true);
		} else if (status != null
				&& status.startsWith(DVMessages.getString(IMAGE_BUILD_STEP))) {
			this.progressMonitor.worked(1);
		}
		logMessage(status);
	}

	private void logMessage(final String buildMessage) {
		if (this.console != null) {
			Display.getDefault().asyncExec(new Runnable() {

				@Override
				public void run() {
					console.showConsole();
					try {
						console.write(buildMessage.getBytes("UTF-8"));
					} catch (IOException e) {
						Activator.log(e);
					}
				}
			});
		}
	}
}
