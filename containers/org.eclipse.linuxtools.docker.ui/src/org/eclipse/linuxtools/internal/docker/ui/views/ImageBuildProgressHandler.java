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
package org.eclipse.linuxtools.internal.docker.ui.views;

import java.io.IOException;
import java.io.OutputStream;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.linuxtools.docker.core.Activator;
import org.eclipse.linuxtools.docker.core.DockerException;
import org.eclipse.linuxtools.docker.core.DockerImageBuildFailedException;
import org.eclipse.linuxtools.docker.core.IDockerConnection;
import org.eclipse.linuxtools.docker.core.IDockerProgressHandler;
import org.eclipse.linuxtools.docker.core.IDockerProgressMessage;
import org.eclipse.linuxtools.internal.docker.core.DockerConnection;
import org.eclipse.linuxtools.internal.docker.ui.BuildConsole;
import org.eclipse.linuxtools.internal.docker.ui.ProgressJob;
import org.eclipse.swt.widgets.Display;

public class ImageBuildProgressHandler implements IDockerProgressHandler {

	private final static String IMAGE_BUILD_COMPLETE = "ImageBuildComplete.msg"; //$NON-NLS-1$
	private final static String IMAGE_BUILDING_JOBNAME = "ImageBuildingJobName.msg"; //$NON-NLS-1$
	private final static String IMAGE_BUILDING = "ImageBuilding.msg"; //$NON-NLS-1$
	private final static String IMAGE_BUILD_STEP = "ImageBuildStep.msg"; //$NON-NLS-1$

	private final BuildConsole console;
	private final OutputStream consoleOutputStream;
	private final String imageName;
	private final DockerConnection connection;
	private final int lines;

	ProgressJob progressJob;

	/**
	 * Create a progress handler to watch the progress of building an imageName
	 * 
	 * @param connection
	 *            - docker connection
	 * @param imageName
	 *            - imageName being built
	 * @param lines
	 *            - number of lines in the Dockerfile
	 */
	public ImageBuildProgressHandler(final IDockerConnection connection,
			final String imageName, final int lines) {
		this.imageName = imageName;
		this.connection = (DockerConnection) connection;
		this.lines = lines;
		this.console = BuildConsole.findConsole(imageName);
		this.consoleOutputStream = console.newOutputStream();
		this.console.clearConsole();
	}

	@Override
	public void processMessage(final IDockerProgressMessage message)
			throws DockerException {
		if (message.error() != null) {
			stopAllJobs();
			throw new DockerImageBuildFailedException(imageName,
					message.error());
		}
		// For imageName build, all the data is in the stream.
		final String status = message.stream();
		if (progressJob == null) {
			if (status != null
					&& status.startsWith(DVMessages
							.getString(IMAGE_BUILD_COMPLETE))) {
				// refresh images
				connection.getImages(true);
			} else {
				ProgressJob newJob = new ProgressJob(
						DVMessages.getFormattedString(IMAGE_BUILDING_JOBNAME,
								imageName),
						DVMessages.getString(IMAGE_BUILDING));
				newJob.setUser(false);
				newJob.setPriority(Job.LONG);
				newJob.schedule();
				progressJob = newJob;
			}

		} else {
			if (status != null
					&& status.startsWith(DVMessages
							.getString(IMAGE_BUILD_COMPLETE))) {
				progressJob.setPercentageDone(100);
				// refresh images
				connection.getImages(true);
			} else if (status != null
					&& status
					.contains(DVMessages.getString(IMAGE_BUILD_STEP))) {
				// Take last step number in the status message (because for
				// quick operations, multiple steps output
				// could be returned at once)
				final int lastStepLocation = status
						.lastIndexOf(DVMessages.getString(IMAGE_BUILD_STEP));
				final String stepNumber = status.substring(lastStepLocation
						+ DVMessages.getString(
						IMAGE_BUILD_STEP).length());
				// Need to separate step # from actual message.
				String[] tokens = stepNumber.split(" ");
				if (lines > 0) {
					long percentage = 100 * (Long.valueOf(tokens[0]) + 1)
							/ lines;
					progressJob.setPercentageDone((int) percentage);
				}
			}
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
						consoleOutputStream.write(buildMessage.getBytes());
					} catch (IOException e) {
						Activator.log(e);
					}
			}
			});
		}
	}

	private void stopAllJobs() {
		progressJob.cancel();
	}

}
