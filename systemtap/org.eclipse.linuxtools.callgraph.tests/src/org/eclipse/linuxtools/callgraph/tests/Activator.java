package org.eclipse.linuxtools.callgraph.tests;

import java.io.IOException;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.eclipse.linuxtools.callgraph.tests";
	
	public static String getPluginLocation() {
		Bundle bundle = Platform.getBundle(PLUGIN_ID);

		URL locationUrl = FileLocator.find(bundle,new Path("/"), null); //$NON-NLS-1$
		URL fileUrl = null;
		try {
			fileUrl = FileLocator.toFileURL(locationUrl);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return fileUrl.getFile();
	}
}
