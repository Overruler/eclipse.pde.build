package org.eclipse.pde.internal.build.ant;

public class TarFileSet extends ZipFileSet {

	/**
	 * @param dir
	 * @param file
	 * @param defaultexcludes
	 * @param includes
	 * @param includesfile
	 * @param excludes
	 * @param excludesfile
	 * @param prefix
	 * @param casesensitive
	 */
	public TarFileSet(String dir, boolean file, String defaultexcludes, String includes, String includesfile, String excludes, String excludesfile, String prefix, String casesensitive) {
		super(dir, file, defaultexcludes, includes, includesfile, excludes, excludesfile, prefix, casesensitive);
	}

	protected void print(AntScript script) {
		script.printTab();
		script.print("<tarfileset"); //$NON-NLS-1$
		if (file)
			script.printAttribute("file", dir, false); //$NON-NLS-1$
		else
			script.printAttribute("dir", dir, false); //$NON-NLS-1$
		script.printAttribute("defaultexcludes", defaultexcludes, false); //$NON-NLS-1$
		script.printAttribute("includes", includes, false); //$NON-NLS-1$
		script.printAttribute("includesfile", includesfile, false); //$NON-NLS-1$
		script.printAttribute("excludes", excludes, false); //$NON-NLS-1$
		script.printAttribute("excludesfile", excludesfile, false); //$NON-NLS-1$
		script.printAttribute("casesensitive", casesensitive, false); //$NON-NLS-1$
		if (file)
			script.printAttribute("fullpath", prefix, false); //$NON-NLS-1$
		else
			script.printAttribute("prefix", prefix, false); //$NON-NLS-1$
		script.println("/>"); //$NON-NLS-1$
	}
}