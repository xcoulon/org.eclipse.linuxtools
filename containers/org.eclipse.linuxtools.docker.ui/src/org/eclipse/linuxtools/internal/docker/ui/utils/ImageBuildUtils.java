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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * @author xcoulon
 *
 */
public class ImageBuildUtils {

	/**
	 * Counts the number of lines in the given Docker build file that contain
	 * statements to execute (ignoring comments and empty lines).
	 * 
	 * @param fileName
	 *            the full name of the Docker file to read
	 * @return the number of instructions.
	 * @throws IOException
	 */
	public static int numberOfLines(final String fileName) throws IOException {
		int count = 0;
		try (final InputStream fis = new FileInputStream(fileName);
				final InputStreamReader isr = new InputStreamReader(fis);
				final BufferedReader br = new BufferedReader(isr);) {
			String line;
			while ((line = br.readLine()) != null) {
				// ignore empty lines and comments
				if (line.startsWith("#") || line.trim().isEmpty()) {
					continue;
				}
				count++;
			}
		}
		return count;
	}

}
