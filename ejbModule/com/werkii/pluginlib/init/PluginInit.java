package com.werkii.pluginlib.init;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;

import com.priint.pubserver.plugin.PluginLibraryControl;
import com.priint.pubserver.plugin.PluginLibraryControl.PluginType;
import com.priint.pubserver.plugin.annotation.PubServerPluginLibrary;

/**
 * Register and initializes the plug-ins of the plug-in library after deployment or server start
 *
 */
@Singleton
@Startup
@PubServerPluginLibrary(
	// required properties
	id = "DemoImportPluginLibrary",
	type = PluginType.SERVICE,
	vendor = "WERK II GmbH",
	version = "1.0",
	// optional properties
	configClasses = {},		
	description = "this is a demo library for import module",
	label = "Demo Import Plug-in Library",
	url = "http://support.url"
)
public class PluginInit extends PluginLibraryControl {

	@Override
	@PostConstruct
	public void startup() {
		//Register and initialize all plug-ins
		super.startup();
	}
	
	@Override
	@PreDestroy
	public void shutdown() {
		//Unregister all plug-ins
		super.shutdown();
	}
}
