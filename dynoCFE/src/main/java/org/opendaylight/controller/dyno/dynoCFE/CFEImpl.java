package org.opendaylight.controller.dyno.dynoCFE;


import java.util.HashMap;

import org.opendaylight.controller.dyno.IDynoService;
import org.opendaylight.controller.dyno.PSEPort;
import org.opendaylight.controller.dyno.PSESwitch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CFEImpl implements DynoCFEApi{

	private static final Logger logger = LoggerFactory
            .getLogger(CFEImpl.class);

	/* External services */
	static private IDynoService dynoService = null;


	/* Default Constructor */
	public CFEImpl() {
		super();
		logger.info("Configuraton FroentEnd getting instancetiated !");
	}

	/* Setter and UnSetter of External Services */

	void setDynoService(IDynoService s) {
		logger.info("PseServices is set!");
		dynoService = s;
	}

	void unsetDynoService(IDynoService s) {
		if (dynoService == s) {
            logger.info("PseServices is removed!");
            dynoService = null;
        }
	}


	void init() {
		logger.info("Configuration Front End has Initialized !");
	}

	void destroy() {
		logger.info("Configuration Front End has destroyed!");
	}

	 void start() {
	    logger.info("Configuration Front End has started !");


	 }

	 void stop() {
		logger.info("CFE Stopped!!");
	 }

	@Override
	public HashMap<String, PSESwitch> getSwitch() {
		
		HashMap<String, PSESwitch> switchMap = new HashMap<String, PSESwitch>();
		switchMap=dynoService.getSwitch();
		return switchMap;
	}

	@Override
	public HashMap<String, PSEPort> getPort(String switchId) {
		
		HashMap<String, PSEPort> portMap  = new HashMap<String, PSEPort>();
		portMap=dynoService.getPort(switchId);
		return portMap;
	}

	

	
	
}

