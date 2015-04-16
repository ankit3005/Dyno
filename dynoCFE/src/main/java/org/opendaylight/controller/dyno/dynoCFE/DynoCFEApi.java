package org.opendaylight.controller.dyno.dynoCFE;

import java.util.HashMap;

import org.opendaylight.controller.dyno.PSEPort;
import org.opendaylight.controller.dyno.PSESwitch;

public interface DynoCFEApi {

/* Interface To be exposed*/
	
	/* getSwitch() : To get all the switches details mapped against the switchId  */
	public HashMap<String, PSESwitch> getSwitch();
	
	/* getPort() : To get all the port info for a switch mapped against portNo */
	public HashMap<String, PSEPort> getPort(String switchId);

	
	public boolean provisionFlow(HashMap<String, String> flowAttributes);

}
