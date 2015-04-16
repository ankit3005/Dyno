
/*
 * Ankit Agrawal
 * aagrawa5@ncsu.edu
 */

package org.opendaylight.controller.dyno.internal;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.dm.Component;
import org.opendaylight.controller.dyno.IDynoService;
import org.opendaylight.controller.sal.core.ComponentActivatorAbstractBase;
import org.opendaylight.controller.switchmanager.IInventoryListener;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.opendaylight.controller.topologymanager.ITopologyManager;
import org.opendaylight.controller.topologymanager.ITopologyManagerAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Activator extends ComponentActivatorAbstractBase {
	
    /** 
     * Logger for the class 
     **/
    protected static final Logger logger = LoggerFactory.getLogger(Activator.class);

    /**
     * Function called when the activator starts just after some
     * initializations are done by the
     * ComponentActivatorAbstractBase.
     *
     */
    @Override
	public void init() {
    }

    /**
     * Function called when the activator stops just before the
     * cleanup done by ComponentActivatorAbstractBase
     *
     */
    @Override
	public void destroy() {
    }

    /**
     * Function that is used to communicate to dependency manager the
     * list of known implementations for services inside a container
     *
     *
     * @return An array containing all the CLASS objects that will be
     * instantiated in order to get an fully working implementation
     * Object
     */
    @Override
    public Object[] getImplementations() {
    	logger.debug("Bundle getting YAON implementation info!");
        Object[] res = { DynoImpl.class};
        return res;
    }

    /**
     * Function that is called when configuration of the dependencies
     * is required.
     *
     * @param c dependency manager Component object, used for
     * configuring the dependencies exported and imported
     * @param imp Implementation class that is being configured,
     * needed as long as the same routine can configure multiple
     * implementations
     * @param containerName The containerName being configured, this allow
     * also optional per-container different behavior if needed, usually
     * should not be the case though.
     */
    @Override
    public void configureInstance(Component c, Object imp, String containerName) {

        if (imp.equals(DynoImpl.class)) {

            logger.debug("Exporting the PSE services");

            Dictionary<String, String> props = new Hashtable<String, String>();
            props.put("salListenerName", "Dyno");
            c.setInterface(new String[] { IDynoService.class.getName(), IInventoryListener.class.getName(), ITopologyManagerAware.class.getName()}, props);

            logger.debug("Registering dependent services");

            c.add(createContainerServiceDependency(containerName).setService(
                    ISwitchManager.class).setCallbacks("setSwitchManager",
                    "unsetSwitchManager").setRequired(true));

            c.add(createContainerServiceDependency(containerName).setService(
                    ITopologyManager.class).setCallbacks("setTopologyManager",
                    "unsetTopologyManager").setRequired(true));
        }
    }

}
