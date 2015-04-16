Dyno
=================

###Build steps
git clone https://github.com/ankit3005/Dyno.git
cd Dyno/Dyno
mvn clean package -Dcheckstyle.skip=true


Dyno is a sample of ADSAL application on OpenDaylight to provide list of all active ports for switches. 

###Dyno Service:

The port state examiner service plugin use ISwitchManager service implementaion, and implement the IInventoryListener callback and IDynoService API.

####ISwitchManager: 
Used to get the list of active ports for a switch identified by a switchId.

####IInventoryListener: 
IInventoryListener callbacks are implemented to receive Switch and Port notification in Dyno plguin.

####IDynoService: 
IDynoService API is implemented to use the Dyno Services from other plugins.


The Dyno service used to store the list of all active Switchs. The list is populated by the Node notification receivded from SAL.
It also stores the list of active ports Name for port No.

IDynoService exposes two API : 

######public HashMap<String, PSESwitch> getSwitch()

Used to get list of all the active switches mapped againest a switch id (in case of Openflow switch it is DPid).

######public HashMap<String, PSEPort> getPort(String switchId)

Used to get all the ports for a switch mapped againest port no. 

PSESwitch and PSEPort used to hold infoormation of switch and port respectivly. 


