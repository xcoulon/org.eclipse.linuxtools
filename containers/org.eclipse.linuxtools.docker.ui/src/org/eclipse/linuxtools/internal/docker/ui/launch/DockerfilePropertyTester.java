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

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IFile;
import org.eclipse.ui.part.FileEditorInput;

/**
 * {@link PropertyTester} that verifies that the given receiver is an
 * {@link IFile} named <code>Dockerfile</code> (case insensitive).
 * 
 * @author xcoulon
 *
 */
public class DockerfilePropertyTester extends PropertyTester {

	@Override
	public boolean test(final Object receiver, final String property,
			final Object[] args, final Object expectedValue) {
		if (property.equals("isDockerfile")) {
		return (receiver instanceof IFile
				&& ((IFile) receiver).getName().equalsIgnoreCase("Dockerfile")); //$NON-NLS-1$
		} else if (property.equals("isDockerfileEditor")) {
			return (receiver instanceof FileEditorInput
					&& ((FileEditorInput) receiver).getFile().getName()
							.equalsIgnoreCase("Dockerfile")); //$NON-NLS-1$
		}
		return false;
	}

}
