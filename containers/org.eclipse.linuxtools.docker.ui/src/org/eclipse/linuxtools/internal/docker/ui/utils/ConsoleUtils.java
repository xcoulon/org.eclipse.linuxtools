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

package org.eclipse.linuxtools.internal.docker.ui.utils;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.linuxtools.docker.core.IDockerImage;
import org.eclipse.linuxtools.docker.ui.Activator;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.MessageConsole;

/**
 * @author xcoulon
 *
 */
public class ConsoleUtils {

	/**
	 * Returns a reference to the console that is for the given container id. If
	 * such a console does not yet exist, it will be created.
	 *
	 * @param imageName
	 *            The name of the {@link IDockerImage} being built
	 * @return An {@link IOConsole} instance.
	 */
	public static MessageConsole findConsole(final String imageName) {
		for (IConsole console : ConsolePlugin.getDefault().getConsoleManager()
				.getConsoles()) {
			if (console instanceof MessageConsole
					&& ((MessageConsole) console).getName().equals(imageName)) {
				return (MessageConsole) console;
			}
		}
		// no existing console, create new one
		final MessageConsole console = new MessageConsole(imageName, null);
		ConsolePlugin.getDefault().getConsoleManager()
				.addConsoles(new IConsole[] { console });

		return console;
	}

	/**
	 * Displays the given console in the consoles view which becomes visible if
	 * it was not the case before.
	 * 
	 * @param console
	 *            the console to display
	 */
	public static void displayConsoleView(final IConsole console) {
		IWorkbenchPart part = null;
		try {
			part = bringViewToFront(IConsoleConstants.ID_CONSOLE_VIEW);
			if (part == null) {
				Activator.log(new Status(IStatus.WARNING, Activator.PLUGIN_ID,
						"Could not open console, "
								+ IConsoleConstants.ID_CONSOLE_VIEW
								+ " was not found"));
				return;
			}
			final IConsoleView view = part.getAdapter(IConsoleView.class);
			if (view == null) {
				return;
			}
			view.display(console);
		} catch (PartInitException e) {
			Activator.log(new Status(IStatus.WARNING, Activator.PLUGIN_ID,
					"Could not open console view", e));
		}
	}

	public static final IWorkbenchPart bringViewToFront(String viewId)
			throws PartInitException {
		final IWorkbenchWindow window = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow();
		IWorkbenchPart part = null;
		if (window != null) {
			IWorkbenchPage page = window.getActivePage();
			if (page != null) {
				part = page.findView(viewId);
				if (part == null) {
					part = page.showView(viewId);
				} else /* if( part != null ) */ {
					if (part != null) {
						page.activate(part);
						part.setFocus();
					}
				}
			}
		}
		return part;
	}
}
