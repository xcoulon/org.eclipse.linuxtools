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

package org.eclipse.linuxtools.internal.docker.ui;

import java.io.IOException;

import org.eclipse.linuxtools.docker.core.IDockerImage;
import org.eclipse.linuxtools.internal.docker.ui.views.DVMessages;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.IOConsoleOutputStream;

/**
 * @author xcoulon
 *
 */
public class BuildConsole extends IOConsole {

	/** Id of this console. */
	public static final String ID = "imageBuildLog"; //$NON-NLS-1$
	public static final String IMAGE_BUILD_LOG_TITLE = "ImageBuildLog.title"; //$NON-NLS-1$
	public static final String DEFAULT_ID = "__DEFAULT_ID__"; //$NON-NLS-1$

	private final String imageName;
	private final IOConsoleOutputStream outputStream;

	/**
	 * Returns a reference to the console that is for the given container id. If
	 * such a console does not yet exist, it will be created.
	 *
	 * @param imageName
	 *            The name of the {@link IDockerImage} being built
	 * @return A console instance.
	 */
	public static BuildConsole findConsole(final String imageName) {
		for (IConsole console : ConsolePlugin.getDefault().getConsoleManager()
				.getConsoles()) {
			if (console instanceof BuildConsole
					&& ((BuildConsole) console).imageName.equals(imageName)) {
				return (BuildConsole) console;
			}
		}
		// no existing console, create new one
		final BuildConsole console = new BuildConsole(imageName);
		ConsolePlugin.getDefault().getConsoleManager()
				.addConsoles(new IConsole[] { console });
		return console;
	}

	private BuildConsole(final String imageName) {
		super(DVMessages.getFormattedString(IMAGE_BUILD_LOG_TITLE, imageName),
				ID, null, true);
		this.imageName = imageName;
		this.outputStream = super.newOutputStream();
	}

	public void write(final byte[] bytes) throws IOException {
		this.outputStream.write(bytes);
	}

	public void close() throws IOException {
		this.outputStream.close();
	}

	/**
	 * Show this console in the Console View.
	 */
	public void showConsole() {
		// Show this console
		ConsolePlugin.getDefault().getConsoleManager().showConsoleView(this);
	}

}
