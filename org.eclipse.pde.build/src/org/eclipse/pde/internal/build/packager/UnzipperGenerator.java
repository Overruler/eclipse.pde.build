/**********************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors: 
 * IBM - Initial API and implementation
 **********************************************************************/
package org.eclipse.pde.internal.build.packager;

import java.util.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.build.*;

public class UnzipperGenerator extends AbstractScriptGenerator {
	// The path to the file containing the list of zips
	private String directoryLocation = DEFAULT_PACKAGER_DIRECTORY_FILENAME_DESCRIPTOR;
	// The list of zips. The key is the name of the zipfile, and the first property is the place to extract it
	private Properties zipsList;
	// The config info for which the generate the unzip command.
	private Config configInfo; //We only consider one config at a time

	public void generate() throws CoreException {
		openScript(workingDirectory, DEFAULT_UNZIPPER_FILENAME_DESCRIPTOR);
		try {
			generatePrologue();
			generateUnzipCommands();
			generateEpilogue();
		} finally {
			closeScript();
		}
	}

	private void generateEpilogue() {
		script.printTargetEnd();
		script.println();
		script.printProjectEnd();
	}

	private void generatePrologue() {
		script.println();
		configInfo = (Config) getConfigInfos().get(0);
		script.printComment("Unzip script for " + configInfo.toString(".")); //$NON-NLS-1$ //$NON-NLS-2$
		script.println();
		script.printProjectDeclaration("Unzipper", TARGET_MAIN, "."); //$NON-NLS-1$	//$NON-NLS-2$
		script.printTargetDeclaration(TARGET_MAIN, null, null, null, null);
	}

	private void generateUnzipCommands() throws CoreException {
		zipsList = readProperties(workingDirectory, directoryLocation); //$NON-NLS-1$

		String zipEntries = zipsList.getProperty(Config.genericConfig().toString(","), ""); //$NON-NLS-1$	//$NON-NLS-2$
		if (!configInfo.equals(Config.genericConfig()))
			zipEntries += (zipEntries.length() == 0 ? "" : " & ") + zipsList.getProperty(configInfo.toString(","), ""); //$NON-NLS-1$	//$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

		String[] allZipEntries = Utils.getArrayFromString(zipEntries, "&"); //$NON-NLS-1$
		for (int i = 0; i < allZipEntries.length; i++) {
			String[] entryDetail = Utils.getArrayFromString(allZipEntries[i], ","); //$NON-NLS-1$

			if (!entryDetail[1].equals(".")) //$NON-NLS-1$
				script.printMkdirTask("${tempDirectory}/" + entryDetail[1]); //$NON-NLS-1$

			if (entryDetail[0].endsWith(".zip")) { //$NON-NLS-1$
				List parameters = new ArrayList(1);
				parameters.add("-o -X ${unzipArgs} "); //$NON-NLS-1$
				parameters.add(getPropertyFormat("downloadDirectory") + "/" + entryDetail[0]); //$NON-NLS-1$ //$NON-NLS-2$
				script.printExecTask("unzip", "${tempDirectory}/" + entryDetail[1], parameters, null); //$NON-NLS-1$//$NON-NLS-2$
				continue;
			}

			if (entryDetail[0].endsWith(".tar.gz") || entryDetail[0].endsWith(".tar")) { //$NON-NLS-1$ //$NON-NLS-2$
				List parameters = new ArrayList(2);
				parameters.add("-" + (entryDetail[0].endsWith(".gz") ? "z" : "") + "pxvf"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
				parameters.add(getPropertyFormat("downloadDirectory") + "/" + entryDetail[0]); //$NON-NLS-1$ //$NON-NLS-2$
				script.printExecTask("tar", "${tempDirectory}/" + entryDetail[1], parameters, null); //$NON-NLS-1$//$NON-NLS-2$
			}
		}
	}

	public void setDirectoryLocation(String filename) {
		directoryLocation = filename;
	}
}