/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.pde.internal.build.publisher;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.equinox.internal.p2.publisher.FileSetDescriptor;
import org.eclipse.equinox.p2.publisher.AbstractAdvice;
import org.eclipse.pde.internal.build.Config;
import org.osgi.framework.Version;

public class FeatureRootAdvice extends AbstractAdvice {
	private static final int IDX_COMPUTER = 0;
	private static final int IDX_DESCRIPTOR = 1;

	// String config -> Object[] { GatheringComputer, Map: permission -> Set, String }
	private final Map advice = new HashMap();
	private String featureId;
	private Version featureVersion;

	public boolean isApplicable(String configSpec, boolean includeDefault, String id, Version version) {
		if (configSpec == null)
			return true;
		if (advice.containsKey(configSpec)) {
			if (featureId != null) {
				if (!featureId.equals(id))
					return false;
				return (featureVersion != null) ? featureVersion.equals(version) : true;
			}
			return true;
		}
		return false;
	}

	/**
	 * Return the configs for which we have advice
	 * @return String[]
	 */
	public String[] getConfigs() {
		return (String[]) advice.keySet().toArray(new String[advice.size()]);
	}

	/**
	 * Return the GatheringComputer containing the set of rootfiles to include for the given config
	 * Returns null if we have no advice for the given config.
	 * @param config
	 * @return GatheringComputer
	 */
	public GatheringComputer getRootFileComputer(String config) {
		if (advice.containsKey(config))
			return (GatheringComputer) ((Object[]) advice.get(config))[IDX_COMPUTER];
		return null;
	}

	public void addRootfiles(String config, GatheringComputer computer) {
		Object[] configAdvice = getConfigAdvice(config);

		if (configAdvice[IDX_COMPUTER] == null)
			configAdvice[IDX_COMPUTER] = computer;
		else {
			GatheringComputer existing = (GatheringComputer) configAdvice[IDX_COMPUTER];
			existing.addAll(computer);
		}
	}

	public void addPermissions(String config, String permissions, String[] files) {
		FileSetDescriptor descriptor = getDescriptor(config);
		for (int i = 0; i < files.length; i++) {
			descriptor.addPermissions(new String[] {permissions, files[i]});
		}
	}

	public void addLinks(String config, String links) {
		FileSetDescriptor descriptor = getDescriptor(config);
		descriptor.setLinks(links);
	}

	private Object[] getConfigAdvice(String config) {
		Object[] configAdvice = (Object[]) advice.get(config);
		if (configAdvice == null) {
			configAdvice = new Object[3];
			advice.put(config, configAdvice);
		}
		return configAdvice;
	}

	public FileSetDescriptor getDescriptor(String config) {
		Object[] configAdvice = getConfigAdvice(config);
		FileSetDescriptor descriptor = null;

		if (configAdvice[IDX_DESCRIPTOR] != null)
			descriptor = (FileSetDescriptor) configAdvice[IDX_DESCRIPTOR];
		else {
			String key = "root"; //$NON-NLS-1$
			if (!config.equals(Config.ANY))
				key += "." + config; //$NON-NLS-1$
			descriptor = new FileSetDescriptor(key, config.equals(Config.ANY) ? null : config);
			descriptor.setFiles(""); //$NON-NLS-1$
			configAdvice[IDX_DESCRIPTOR] = descriptor;
		}
		return descriptor;
	}

	public void setFeatureId(String featureId) {
		this.featureId = featureId;
	}

	public void setFeatureVersion(Version featureVersion) {
		this.featureVersion = featureVersion;
	}
}