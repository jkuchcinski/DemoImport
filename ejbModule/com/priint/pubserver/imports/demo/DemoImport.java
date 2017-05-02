package com.priint.pubserver.imports.demo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.shiro.session.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.priint.pubserver.plugin.PluginControlDefault;
import com.priint.pubserver.plugin.annotation.PubServerPlugin;
import com.priint.pubserver.plugin.entitydata.Bucket;
import com.priint.pubserver.plugin.entitydata.EntityData;
import com.priint.pubserver.plugin.exception.ConnectorException;
import com.priint.pubserver.plugin.interfaces.CometServer4Job;
import com.priint.pubserver.plugin.interfaces.ConnectorPersistLocal;
import com.priint.pubserver.session.SessionManagerLocal;
import com.priint.pubserver.tracing.Tracer;
import com.priint.pubserver.tracing.TracerFactory;
import com.werkii.cas.server.CometServer;
import com.werkii.server.exception.ServerException;

@Stateless(mappedName = "com.priint.pubserver.imports.demo.DemoImport")
@LocalBean
@PubServerPlugin
public class DemoImport extends PluginControlDefault implements CometServer4Job {
	
	private static final String IMPORT_USER = "import";

	private static final String DEMO_IMPORT_START_JOB_WITH_PARAMS_S = "DemoImport: startJob with params: %s";

	private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
	
	private static final Tracer TRACER = TracerFactory.getTracer(DemoImport.class,"DemoImport");



	@Override
	public boolean removeAfterExecute() {
		return false;
	}

	@Override
	public boolean showStartInfo() {
		return true;
	}

	@Override
	public Result startJob(HashMap<String, Object> params) throws Exception {
		
		Result result = new Result();

		if (params!=null) {
			LOGGER.debug(String.format(DEMO_IMPORT_START_JOB_WITH_PARAMS_S, params));
		} else {
			result.errorMessage="params is null but should not be null";
			result.resultCode=-1;			
			return result;
		}

		Session session = getSession(); //only if your class extends PluginControlDefault
		session.setAttribute(SessionManagerLocal.SESSION_TRACE_ACTIVE_PROPERTY, Boolean.TRUE.toString());

		CometServer cometServer = (CometServer)params.get("COMETSERVER");
		String importId = (String)params.get("IMPORTID");

	    cometServer.logInfo(DemoImport.class.getName(), String.format(DEMO_IMPORT_START_JOB_WITH_PARAMS_S, params), getSessionId(), importId);
	    TRACER.info("DemoImport","",String.format(DEMO_IMPORT_START_JOB_WITH_PARAMS_S, params));
		
	    List<Object> parsedData = parseImportFile(new java.io.File((String)params.get("IMPPATH")), cometServer, importId);
		List<EntityData> data2save = prepareData(parsedData, cometServer, importId);
		saveData(data2save, cometServer, importId);

	    cometServer.logInfo(DemoImport.class.getName(), "DemoImport has been finished.", getSessionId(), importId);

		
		return result;
	}
	
	public List<Object> parseImportFile(java.io.File importFile, CometServer cometServer,  String importID) throws ServerException {
		String progressLabel = "Step 1/3 - Parsing "+importFile.getAbsolutePath()+"file...";
		cometServer.progressInfo(importID, progressLabel, 0.0f, Boolean.FALSE);
		int i=0;
		while (i<1000) {
			cometServer.progressInfo(importID, progressLabel, ((float)i/1000.0f)*100.0f, Boolean.FALSE);
			sleep(20);
			i++;
		}
		cometServer.progressInfo(importID, progressLabel, 100.0f, Boolean.TRUE);

		return new ArrayList<>();
	}
	
	public List<EntityData> prepareData(List<Object> parsedData, CometServer cometServer,  String importID) throws ServerException {
		String progressLabel = "Step 1/2 - Preparing data...";
		cometServer.progressInfo(importID, progressLabel, 0.0f, Boolean.FALSE);
		cometServer.logInfo(DemoImport.class.getName(), progressLabel, getSessionId(), importID);
		sleep(10);
	    ArrayList<EntityData> result = new ArrayList<>();
	    
	    Bucket b = new Bucket();
	    b.setIdentifier(importID);
	    b.setLabel("From Import "+new java.util.Date().toString()+" "+importID);
	    b.setCreatedBy(IMPORT_USER);
	    b.setUpdatedBy(IMPORT_USER);
	    b.setCreatedOn(new java.util.Date());
	    b.setUpdatedOn(new java.util.Date());
	    b.setEntityBucketId("category");
	    b.setVersion(importID);
	    result.add(b);
	    
	    int i=1;
	    while(i<=50) {
	    	cometServer.progressInfo(importID, progressLabel, ((float)i/50.0f)*100.0f, Boolean.FALSE);
	    	sleep(25);
	    	Bucket b1 = new Bucket();
		    b1.setIdentifier(UUID.randomUUID().toString());
		    b1.setLabel("Product "+importID+" "+i+" "+b1.getIdentifier());
		    b1.setCreatedBy(IMPORT_USER);
		    b1.setUpdatedBy(IMPORT_USER);
		    b1.setCreatedOn(new java.util.Date());
		    b1.setUpdatedOn(new java.util.Date());
		    b1.setEntityBucketId("product");
		    b1.setVersion(importID);
	    	b1.setParentBucketId(b.getIdentifier());
	    	b1.setSequence(i);
	    	i++;
	    	result.add(b1);
	    }
	    cometServer.progressInfo(importID, progressLabel, 100.0f, Boolean.TRUE);
	    cometServer.logInfo(DemoImport.class.getName(), progressLabel+" Finished.", getSessionId(), importID);
		return result;
	}


	
	public void saveData(List<EntityData> data2save, CometServer cometServer,  String importID) throws ServerException, NamingException, ConnectorException {
		sleep(10);
		String progressLabel = "Step 1/3 - Saving data...";
		cometServer.progressInfo(importID, progressLabel, 0.0f, Boolean.FALSE);
		try {
			ConnectorPersistLocal connector = getConnector();
			if (connector!=null) {
				for (EntityData bucket : data2save) {				
					cometServer.progressInfo(importID, progressLabel, ((float)((Bucket)bucket).getSequence()/(float)data2save.size())*100.0f, Boolean.FALSE);
					connector.persist(bucket);
				}
			}
		} catch (NamingException | ConnectorException e) {
			throw e;
		}
		cometServer.progressInfo(importID, progressLabel, 100.0f, Boolean.TRUE);
		
	}

	private ConnectorPersistLocal getConnector() throws NamingException {
		javax.naming.Context ctx = new InitialContext();
		Object o = ctx.lookup("java:global/PublishingHubDBConnector/PublishingHubDBConnector!com.priint.pubserver.connector.jpa.api.PublishingHubDBConnector");
		if (o!=null && o instanceof ConnectorPersistLocal) {
			return (ConnectorPersistLocal)o;
		}
		return null;
	}
	
	private void sleep(int timeInMs) {
		try {
			Thread.sleep(timeInMs);			
		} catch (Exception e) {
			LOGGER.error("Error: "+e.getMessage(),e);
		}
	}
	
}
