package org.opendaylight.controller.dyno;

import java.util.HashMap;



public interface IDynoService {

	/* Interface To be exposed*/
	
	/* getSwitch() : To get all the switches details mapped against the switchId  */
	public HashMap<String, PSESwitch> getSwitch();
	
	/* getPort() : To get all the port info for a switch mapped against portNo */
	public HashMap<String, PSEPort> getPort(String switchId);
	
	public boolean provisionFlow(HashMap<String, String> flowAttributes);
}
