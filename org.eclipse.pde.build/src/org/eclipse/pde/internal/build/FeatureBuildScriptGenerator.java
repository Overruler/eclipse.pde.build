package org.eclipse.pde.internal.build;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import java.io.*;
import java.net.MalformedURLException;
import java.util.*;

import org.eclipse.core.boot.BootLoader;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.model.PluginModel;
import org.eclipse.core.runtime.model.PluginPrerequisiteModel;
import org.eclipse.pde.internal.build.ant.*;
import org.eclipse.update.core.*;
import org.eclipse.update.internal.core.FeatureExecutableFactory;

/**
 * Generates build.xml script for features.
 */
public class FeatureBuildScriptGenerator extends AbstractBuildScriptGenerator {
	
	/**
	 * Indicates whether scripts for this feature's children should be generated.
	 */
	protected boolean generateChildrenScript = true;
	/**
	 * 
	 */
	protected String featureID;
	/**
	 * Where to get the feature description from.
	 */
	protected String featureRootLocation;
	/**
	 * Target feature.
	 */
	protected Feature feature;
/**
 * Returns a list of PluginModel objects representing the elements. The boolean
 * argument indicates whether the list should consist of plug-ins or fragments.
 */
protected List computeElements(boolean fragments) throws CoreException {
	List result = new ArrayList(5);
	IPluginEntry[] pluginList = feature.getPluginEntries();
	for (int i = 0; i < pluginList.length; i++) {
		IPluginEntry entry = pluginList[i];
		if (fragments == entry.isFragment()) { // filter the plugins or fragments
			VersionedIdentifier identifier = entry.getVersionedIdentifier();
			PluginModel model;
			if (fragments)
				model = getRegistry().getFragment(identifier.getIdentifier(), identifier.getVersion().toString());
			else
				model = getRegistry().getPlugin(identifier.getIdentifier(), identifier.getVersion().toString());
			if (model == null)
				throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_PLUGIN_MISSING, Policy.bind("exception.missingPlugin", entry.getVersionedIdentifier().toString()), null));
			else
				result.add(model);
		}
	}
	return result;
}
public void setGenerateChildrenScript(boolean generate) {
	generateChildrenScript = generate;
}
public void generate() throws CoreException {
	if (featureID == null)
		throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_FEATURE_MISSING, Policy.bind("error.missingFeatureId"), null));
	if (installLocation == null)
		throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_INSTALL_LOCATION_MISSING, Policy.bind("error.missingInstallLocation"), null));

	readFeature();
	String custom = (String) getBuildProperties(feature).get(PROPERTY_CUSTOM);
	if (custom != null && custom.equalsIgnoreCase("true"))
		return;

	if (generateChildrenScript)
		generateChildrenScripts();

	try {
		File root = new File(getFeatureRootLocation());
		File target = new File(root, buildScriptName);
		script = new BuildAntScript(new FileOutputStream(target));
		setUpAntBuildScript();
		try {
			generateBuildScript();
		} finally {
			script.close();
		}
	} catch (IOException e) {
		throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_WRITING_SCRIPT, Policy.bind("exception.writeScript"), e));
	}
}
/**
 * Main call for generating the script.
 */
protected void generateBuildScript() throws CoreException {
	generatePrologue();
	generateAllPluginsTarget();
	generateAllFragmentsTarget();
	generateAllChildrenTarget();
	generateChildrenTarget();
	generateBuildJarsTarget(script, feature);
	generateBuildZipsTarget();
	generateBuildUpdateJarTarget();
	generateGatherBinPartsTarget();
	generateZipDistributionWholeTarget();
	generateZipSourcesTarget();
	generateGatherSourcesTarget();
	generateGatherLogTarget();
	generateZipLogsTarget();
	generateCleanTarget();
	generateEpilogue();
}

protected void generateBuildJarsTarget(AntScript script, Feature feature) throws CoreException {
	Properties properties = getBuildProperties(feature);
	JAR[] availableJars = extractJars(properties);
	List jarNames = new ArrayList(availableJars.length);
	List srcNames = new ArrayList(availableJars.length);
	for (int i = 0; i < availableJars.length; i++) {
		JAR jar = availableJars[i];
		String name = jar.getName();
		jarNames.add(name);
		generateJARTarget(script, "", jar);
		generateSRCTarget(script, jar);
		srcNames.add(getSRCName(name));
	}
	int tab = 1;
	script.println();
	script.printTargetDeclaration(tab++, TARGET_BUILD_JARS, Utils.getStringFromCollection(jarNames, ","), null, null, null);
	Map params = new HashMap(2);
	params.put(PROPERTY_TARGET, TARGET_BUILD_JARS);
	script.printAntCallTask(tab, TARGET_ALL_CHILDREN, null, params);
	script.printEndTag(--tab, "target");
	script.println();
	script.printTargetDeclaration(tab++, TARGET_BUILD_SOURCES, Utils.getStringFromCollection(srcNames, ","), null, null, null);
	params.clear();
	params.put(PROPERTY_TARGET, TARGET_BUILD_SOURCES);
	script.printAntCallTask(tab, TARGET_ALL_CHILDREN, null, params);
	script.printEndTag(--tab, "target");
}

/**
 * FIXME: add comments
 */
protected void generateBuildZipsTarget() throws CoreException {
	StringBuffer zips = new StringBuffer();
	Properties props = getBuildProperties(feature);
	for (Iterator iterator = props.entrySet().iterator(); iterator.hasNext();) {
		Map.Entry entry = (Map.Entry) iterator.next();
		String key = (String) entry.getKey();
		if (key.startsWith(PROPERTY_SOURCE_PREFIX) && key.endsWith(PROPERTY_ZIP_SUFFIX)) {
			String zipName = key.substring(PROPERTY_SOURCE_PREFIX.length());
			zips.append(',');
			zips.append(zipName);
			generateZipIndividualTarget(zipName, (String) entry.getValue());
		}
	}
	script.println();
	int tab = 1;
	script.printTargetDeclaration(tab++, TARGET_BUILD_ZIPS, TARGET_INIT + zips.toString(), null, null, null);
	Map params = new HashMap(2);
	params.put(PROPERTY_TARGET, TARGET_BUILD_ZIPS);
	script.printAntCallTask(tab, TARGET_ALL_CHILDREN, null, params);
	script.printString(--tab, "</target>");
}
/**
 * FIXME: add comments
 */
protected void generateZipIndividualTarget(String zipName, String source) throws CoreException {
	int tab = 1;
	script.println();
	script.printTargetDeclaration(tab++, zipName, TARGET_INIT, null, null, null);
	IPath root = new Path(getPropertyFormat(PROPERTY_BASEDIR));
	script.printZipTask(tab, root.append(zipName).toString(), root.append(source).toString());
	script.printString(--tab, "</target>");
}
protected void generateCleanTarget() {
	int tab = 1;
	script.println();
	script.printTargetDeclaration(tab, TARGET_CLEAN, TARGET_INIT, null, null, null);
	tab++;
	Map params = new HashMap(1);
	params.put("target", TARGET_CLEAN);
	script.printAntCallTask(tab, TARGET_ALL_CHILDREN, null, params);
	FileSet[] fileSet = new FileSet[3];
	fileSet [0] = new FileSet(".", null, "*.pdetemp", null, null, null, null);
	fileSet [1] = new FileSet(".", null, "${feature}*.jar", null, null, null, null);
	fileSet [2] = new FileSet(".", null, "${feature}*.zip", null, null, null, null);
	script.printDeleteTask(tab, null, null, fileSet);
	tab--;
	script.printString(tab, "</target>");
}
protected void generateZipLogsTarget() {
	IPath base = new Path(getPropertyFormat(PROPERTY_BASEDIR));
	base = base.append("_temp_");
	int tab = 1;
	script.println();
	script.printTargetDeclaration(tab++, TARGET_ZIP_LOGS, TARGET_INIT, null, null, null);
	script.printProperty(tab, PROPERTY_BASE, base.toString());
	Map params = new HashMap(1);
	params.put(PROPERTY_TARGET, TARGET_GATHER_LOGS);
	params.put(PROPERTY_DESTINATION, getPropertyFormat(PROPERTY_BASE));
	script.printAntCallTask(tab, TARGET_ALL_CHILDREN, "false", params);
	script.printAntCallTask(tab, TARGET_GATHER_LOGS, "false", params);
	IPath destination = new Path(getPropertyFormat(PROPERTY_BASEDIR)).append("${feature}.log.zip");
	script.printZipTask(tab, destination.toString(), getPropertyFormat(PROPERTY_BASE));
	script.printDeleteTask(tab, getPropertyFormat(PROPERTY_BASE), null, null);
	script.printString(--tab, "</target>");
}
protected void generateGatherLogTarget() {
	String source = new Path(getPropertyFormat(PROPERTY_BASEDIR)).toString();
	String destination = new Path(getPropertyFormat(PROPERTY_DESTINATION)).append(getDirectoryName()).toString();
	int tab = 1;
	script.println();
	script.printTargetDeclaration(tab++, TARGET_GATHER_LOGS, TARGET_INIT, null, null, null);
	script.printMkdirTask(tab, destination);
	FileSet fileSet = new FileSet(source, null, "*.log", null, null, null, null);
	script.printCopyTask(tab, null, destination, new FileSet[] {fileSet});
	script.printString(--tab, "</target>");
}
protected void generateGatherSourcesTarget() throws CoreException {
	IPath source = new Path(getPropertyFormat(PROPERTY_BASEDIR));
	IPath destination = new Path(getPropertyFormat(PROPERTY_DESTINATION));
	destination = destination.append(getDirectoryName());
	int tab = 1;
	script.println();
	script.printTargetDeclaration(tab++, TARGET_GATHER_SOURCES, TARGET_INIT, PROPERTY_DESTINATION, null, null);
	script.printMkdirTask(tab, destination.toString());
	Properties props = getBuildProperties(feature);
	for (Iterator iterator = props.entrySet().iterator(); iterator.hasNext();) {
		Map.Entry entry = (Map.Entry) iterator.next();
		String key = (String) entry.getKey();
		if (key.startsWith(PROPERTY_SOURCE_PREFIX) && key.endsWith(PROPERTY_JAR_SUFFIX)) {
			String jarName = key.substring(PROPERTY_SOURCE_PREFIX.length());
			// zip name is jar name without the ".jar" but with "src.zip" appended
			String zip = jarName.substring(0, jarName.length() - 4) + "src.zip";
			script.printCopyTask(tab, source.append(zip).toString(), destination.toString(), null);
		}
	}
	script.printString(--tab, "</target>");
}
/**
 * 
 */
protected String getDirectoryName() {
	return "features/${feature}_" + getPropertyFormat(PROPERTY_VERSION);
}

protected void generateZipSourcesTarget() {
	String featurebase = getPropertyFormat(PROPERTY_FEATURE_BASE);
	int tab = 1;
	script.println();
	script.printTargetDeclaration(tab, TARGET_ZIP_SOURCES, TARGET_INIT, null, null, null);
	tab++;
	IPath destination = new Path(getPropertyFormat(PROPERTY_BASEDIR));
	script.printProperty(tab, PROPERTY_FEATURE_BASE, destination.append("zip.sources.pdetemp").toString());
	script.printDeleteTask(tab, featurebase, null, null);
	script.printMkdirTask(tab, featurebase);
	Map params = new HashMap(1);
	params.put(PROPERTY_DESTINATION, featurebase);
	script.printAntCallTask(tab, TARGET_GATHER_SOURCES, null, params);
	params.put(PROPERTY_TARGET, TARGET_GATHER_SOURCES);
	script.printAntCallTask(tab, TARGET_ALL_CHILDREN, null, params);
	script.printZipTask(tab, destination.append("${feature}_src_${version}.zip").toString(), "${feature.base}");
	script.printDeleteTask(tab, featurebase, null, null);
	tab--;
	script.printString(tab, "</target>");
}
protected void generateGatherBinPartsTarget() throws CoreException {
	int tab = 1;
	script.println();
	script.printTargetDeclaration(tab++, TARGET_GATHER_BIN_PARTS, TARGET_INIT, PROPERTY_FEATURE_BASE, null, null);
	Map params = new HashMap(1);
	params.put(PROPERTY_TARGET, TARGET_GATHER_BIN_PARTS);
	params.put(PROPERTY_DESTINATION, getPropertyFormat(PROPERTY_FEATURE_BASE));
	script.printAntCallTask(tab, TARGET_CHILDREN, null, params);
	String include = (String) getBuildProperties(feature).get(PROPERTY_BIN_INCLUDES);
	String exclude = (String) getBuildProperties(feature).get(PROPERTY_BIN_EXCLUDES);
	String root = "${feature.base}/features/${feature}_${version}";
	script.printMkdirTask(tab, root);
	if (include != null || exclude != null) {
		FileSet fileSet = new FileSet(getPropertyFormat(PROPERTY_BASEDIR), null, include, null, exclude, null, null);
		script.printCopyTask(tab, null, root, new FileSet[]{ fileSet });
	}	
	script.printString(--tab, "</target>");
}
protected void generateBuildUpdateJarTarget() {
	int tab = 1;
	script.println();
	script.printTargetDeclaration(tab, TARGET_BUILD_UPDATE_JAR, TARGET_INIT, null, null, null);
	tab++;
	Map params = new HashMap(1);
	params.put(PROPERTY_TARGET, TARGET_BUILD_UPDATE_JAR);
	script.printAntCallTask(tab, TARGET_ALL_CHILDREN, null, params);
	script.printAntCallTask(tab, TARGET_BUILD_JARS, null, null);
	IPath destination = new Path(getPropertyFormat(PROPERTY_BASEDIR));
	script.printProperty(tab, PROPERTY_FEATURE_BASE, destination.append("bin.zip.pdetemp").toString());
	script.printDeleteTask(tab, getPropertyFormat(PROPERTY_FEATURE_BASE), null, null);
	script.printMkdirTask(tab, getPropertyFormat(PROPERTY_FEATURE_BASE));
	// be sure to call the gather with children turned off.  The only way to do this is 
	// to clear all inherited values.  Must remember to setup anything that is really expected.
	params.clear();
	params.put(PROPERTY_FEATURE_BASE, getPropertyFormat(PROPERTY_FEATURE_BASE));
	script.printAntCallTask(tab, TARGET_GATHER_BIN_PARTS, "false", params);
	script.printJarTask(tab, destination.append("${feature}_${version}.jar").toString(), "${feature.base}");
	script.printDeleteTask(tab, getPropertyFormat(PROPERTY_FEATURE_BASE), null, null);
	tab--;
	script.printString(tab, "</target>");
}
/**
 * Zip up the whole feature.
 */
protected void generateZipDistributionWholeTarget() {
	int tab = 1;
	script.println();
	script.printTargetDeclaration(tab, TARGET_ZIP_DISTRIBUTION, TARGET_INIT, null, null, null);
	tab++;
	IPath destination = new Path(getPropertyFormat(PROPERTY_BASEDIR));
	script.printProperty(tab, PROPERTY_FEATURE_BASE, destination.append("bin.zip.pdetemp").toString());
	script.printDeleteTask(tab, getPropertyFormat(PROPERTY_FEATURE_BASE), null, null);
	script.printMkdirTask(tab, getPropertyFormat(PROPERTY_FEATURE_BASE));
	Map params = new HashMap(1);
	params.put(PROPERTY_INCLUDE_CHILDREN, "true");
	script.printAntCallTask(tab, TARGET_GATHER_BIN_PARTS, null, params);
	script.printZipTask(tab, destination.append("${feature}_${version}.bin.dist.zip").toString(), getPropertyFormat(PROPERTY_FEATURE_BASE));
	script.printDeleteTask(tab, getPropertyFormat(PROPERTY_FEATURE_BASE), null, null);
	tab--;
	script.printString(tab, "</target>");
}
/**
 * Executes a given target in all children's script files.
 */
protected void generateAllChildrenTarget() {
	StringBuffer depends = new StringBuffer();
	depends.append(TARGET_INIT);
	depends.append(",");
	depends.append(TARGET_ALL_PLUGINS);
	depends.append(",");
	depends.append(TARGET_ALL_FRAGMENTS);
	
	script.println();
	script.printTargetDeclaration(1, TARGET_ALL_CHILDREN, depends.toString(), null, null, null);
	script.printString(1, "</target>");
}
/**
 * Target responsible for delegating target calls to plug-in's build.xml scripts.
 */
protected void generateAllPluginsTarget() throws CoreException {
	int tab = 1;
	List plugins = computeElements(false);
	String[][] sortedPlugins = Utils.computePrerequisiteOrder((PluginModel[]) plugins.toArray(new PluginModel[plugins.size()]));
	script.println();
	script.printTargetDeclaration(tab++, TARGET_ALL_PLUGINS, TARGET_INIT, null, null, null);
	for (int list = 0; list < 2; list++) {
		for (int i = 0; i < sortedPlugins[list].length; i++) {
			PluginModel plugin = getRegistry().getPlugin(sortedPlugins[list][i]);
			IPath location = Utils.makeRelative(new Path(getLocation(plugin)), new Path(getFeatureRootLocation()));
			script.printAntTask(tab, buildScriptName, location.toString(), getPropertyFormat(PROPERTY_TARGET), null, null, null);
		}
	}
	script.printString(--tab, "</target>");
}
/**
 * Target responsible for delegating target calls to fragments's build.xml scripts.
 */
protected void generateAllFragmentsTarget() throws CoreException {
	int tab = 1;
	List fragments = computeElements(true);
	script.println();
	script.printTargetDeclaration(tab++, TARGET_ALL_FRAGMENTS, TARGET_INIT, null, null, null);
	for (Iterator iterator = fragments.iterator(); iterator.hasNext();) {
		PluginModel fragment = (PluginModel) iterator.next();
		IPath location = Utils.makeRelative(new Path(getLocation(fragment)), new Path(getFeatureRootLocation()));
		script.printAntTask(tab, buildScriptName, location.toString(), getPropertyFormat(PROPERTY_TARGET), null, null, null);
	}
	script.printString(--tab, "</target>");
}





/**
 * Just ends the script.
 */
protected void generateEpilogue() {
	script.println();
	script.printString(0, "</project>");
}
/**
 * Defines, the XML declaration, Ant project and init target.
 */
protected void generatePrologue() {
	int tab = 1;
	script.printProjectDeclaration(feature.getFeatureIdentifier(), TARGET_BUILD_JARS, ".");
	script.println();
	script.printProperty(tab, PROPERTY_BUILD_COMPILER, JDT_COMPILER_ADAPTER);
	script.println();
	script.printTargetDeclaration(tab++, TARGET_INIT, null, null, null, null);
	script.printProperty(tab, PROPERTY_FEATURE, feature.getFeatureIdentifier());
	script.printProperty(tab, PROPERTY_VERSION, feature.getFeatureVersion());
	script.printString(--tab, "</target>");
}
protected void generateChildrenScripts() throws CoreException {
	generateModels(new PluginBuildScriptGenerator(), computeElements(false));
	generateModels(new PluginBuildScriptGenerator(), computeElements(true));
}
protected void generateModels(ModelBuildScriptGenerator generator, List models) throws CoreException {
	if (models.isEmpty())
		return;
	generator.setInstallLocation(installLocation);
	generator.setDevEntries(devEntries);
	generator.setPluginPath(getPluginPath());
	for (Iterator iterator = models.iterator(); iterator.hasNext();) {
		PluginModel model = (PluginModel) iterator.next();
		// setModel has to be called before configurePersistentProperties
		// because it reads the model's properties
		generator.setModel(model);
		configurePersistentProperties(generator, model);
		generator.generate();
	}
}
/**
 * Propagates properties that are set for this feature but should
 * overwrite any values set for the children.
 */
protected void configurePersistentProperties(AbstractBuildScriptGenerator generator, PluginModel model) throws CoreException {
	Properties props = generator.getBuildProperties(model);
	for (int i = 0; i < PERSISTENT_PROPERTIES.length; i++) {
		String key = PERSISTENT_PROPERTIES[i];
		String value = props.getProperty(key);
		if (value == null)
			continue;
		props.setProperty(key, value);
	}
}
public void setFeature(String featureID) throws CoreException {
	if (featureID == null)
		throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_FEATURE_MISSING, Policy.bind("error.missingFeatureId"), null));
	this.featureID = featureID;
}
/**
 * Reads the target feature from the specified location.
 */
protected void readFeature() throws CoreException {
	String location = getFeatureRootLocation();
	if (location == null)
		throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_FEATURE_MISSING, Policy.bind("error.missingFeatureLocation"), null));
	
	FeatureExecutableFactory factory = new FeatureExecutableFactory();
	File file = new File(location);
	try {
		feature = (Feature) factory.createFeature(file.toURL(), null);
		if (feature == null)
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_FEATURE_MISSING, Policy.bind("error.creatingFeature", new String[] {featureID}), null));	
	} catch (MalformedURLException e) {
		throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_FEATURE_MISSING, Policy.bind("error.creatingFeature", new String[] {featureID}), e));
	}
}
/**
 * If the feature location was not specified, use a default one.
 */
protected String getFeatureRootLocation() {
	if (featureRootLocation == null) {
		IPath location = new Path(installLocation);
		location = location.append(DEFAULT_FEATURE_LOCATION);
		location = location.append(featureID);
		featureRootLocation = location.addTrailingSeparator().toOSString();
	}
	return featureRootLocation;
}
/**
 *
 */
public void setFeatureRootLocation(String location) {
	this.featureRootLocation = location;
}

protected Properties getBuildProperties(Feature feature) throws CoreException {
	VersionedIdentifier identifier = feature.getVersionedIdentifier();
	Properties result = (Properties) buildProperties.get(identifier);
	if (result == null) {
		result = readBuildProperties(getFeatureRootLocation());
		buildProperties.put(identifier, result);
	}
	return result;
}
/**
 * Delegates some target call to all-template only if the property
 * includeChildren is set.
 */
protected void generateChildrenTarget() {
	script.println();
	script.printTargetDeclaration(1, TARGET_CHILDREN, null, PROPERTY_INCLUDE_CHILDREN, null, null);
	script.printAntCallTask(2, TARGET_ALL_CHILDREN, null, null);
	script.printString(1, "</target>");
}

/**
 * 
 */
protected void setUpAntBuildScript() throws CoreException {
	String external = (String) getBuildProperties(feature).get(PROPERTY_ZIP_EXTERNAL);
	if (external != null && external.equalsIgnoreCase("true"))
		script.setZipExternal(true);

	external = (String) getBuildProperties(feature).get(PROPERTY_JAR_EXTERNAL);
	if (external != null && external.equalsIgnoreCase("true"))
		script.setJarExternal(true);

	String executable = (String) getBuildProperties(feature).get(PROPERTY_ZIP_PROGRAM);
	if (executable != null)
		script.setZipExecutable(executable);
	
	String arg = (String) getBuildProperties(feature).get(PROPERTY_ZIP_ARGUMENT);
	if (arg != null)
		script.setZipArgument(arg);
}
}