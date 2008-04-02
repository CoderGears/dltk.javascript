package org.eclipse.dltk.javascript.internal.launching;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.dltk.core.environment.IDeployment;
import org.eclipse.dltk.core.environment.IFileHandle;
import org.eclipse.dltk.internal.launching.AbstractInterpreterInstallType;
import org.eclipse.dltk.javascript.core.JavaScriptNature;
import org.eclipse.dltk.javascript.launching.JavaScriptLaunchingPlugin;
import org.eclipse.dltk.launching.EnvironmentVariable;
import org.eclipse.dltk.launching.IInterpreterInstall;
import org.eclipse.dltk.launching.LibraryLocation;
import org.osgi.framework.Bundle;

public class GenericJavaScriptInstallType extends
		AbstractInterpreterInstallType {

	public static final String DBGP_FOR_RHINO_BUNDLE_ID = "org.eclipse.dltk.javascript.rhino.dbgp";
	public static final String EMBEDDED_RHINO_BUNDLE_ID = "org.eclipse.dltk.javascript.rhino";

	public String getNatureId() {
		return JavaScriptNature.NATURE_ID;
	}

	public String getName() {
		return "Generic Rhino install";
	}

	public LibraryLocation[] getDefaultLibraryLocations(IFileHandle installLocation,
			EnvironmentVariable[] variables, IProgressMonitor monitor) {
		Bundle bundle = Platform.getBundle(EMBEDDED_RHINO_BUNDLE_ID);

		URL resolve;
		try {
			resolve = FileLocator.toFileURL(bundle
					.getResource("/org/mozilla/classfile/ByteCode.class"));
			try {

				File fl = new File(new URI(resolve.toString())).getParentFile()
						.getParentFile().getParentFile().getParentFile();
				return new LibraryLocation[] { new LibraryLocation(new Path(fl
						.getAbsolutePath())) };
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		return new LibraryLocation[0];
	}

	private static String[] possibleExes = { "js" };

	protected String getPluginId() {
		return JavaScriptLaunchingPlugin.PLUGIN_ID;
	}

	protected String[] getPossibleInterpreterNames() {
		return possibleExes;
	}

	protected IInterpreterInstall doCreateInterpreterInstall(String id) {
		return new GenericJavaScriptInstall(this, id);
	}

	protected void filterEnvironment(Map environment) {
		// make sure that $auto_path is clean
		environment.remove("TCLLIBPATH");
		// block wish from showing window under linux
		environment.remove("DISPLAY");
	}

	public IStatus validateInstallLocation(IFileHandle installLocation) {
		return Status.OK_STATUS;
	}

	protected IPath createPathFile(IDeployment deployment) throws IOException {
		// this method should not be used
		throw new RuntimeException("This method should not be used");
	}

	protected String[] parsePaths(String result) {
		ArrayList paths = new ArrayList();
		String subs = null;
		int index = 0;
		while (index < result.length()) {
			// skip whitespaces
			while (index < result.length()
					&& Character.isWhitespace(result.charAt(index)))
				index++;
			if (index == result.length())
				break;

			if (result.charAt(index) == '{') {
				int start = index;
				while (index < result.length() && result.charAt(index) != '}')
					index++;
				if (index == result.length())
					break;
				subs = result.substring(start + 1, index);
			} else {
				int start = index;
				while (index < result.length() && result.charAt(index) != ' ')
					index++;
				subs = result.substring(start, index);
			}

			paths.add(subs);
			index++;
		}

		return (String[]) paths.toArray(new String[paths.size()]);
	}

	protected ILog getLog() {
		return JavaScriptLaunchingPlugin.getDefault().getLog();
	}
}
