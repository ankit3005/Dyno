/*
 * Ankit Agrawal
 * aagrawa5@ncsu.edu
 */

package org.opendaylight.controller.dyno.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.opendaylight.controller.dyno.IDynoService;
import org.opendaylight.controller.dyno.PSEPort;
import org.opendaylight.controller.dyno.PSESwitch;
import org.opendaylight.controller.sal.core.Bandwidth;
import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.Edge;
import org.opendaylight.controller.sal.core.Path;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.UpdateType;
import org.opendaylight.controller.switchmanager.IInventoryListener;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.opendaylight.controller.topologymanager.ITopologyManager;
import org.opendaylight.controller.topologymanager.ITopologyManagerAware;
//import org.opendaylight.controller.sal.routing.IListenRoutingUpdates;
import org.opendaylight.controller.sal.routing.IRouting;
//import org.opendaylight.controller.sal.topology.IListenTopoUpdates;
import org.opendaylight.controller.sal.topology.TopoEdgeUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.SparseMultigraph;
import edu.uci.ics.jung.graph.util.EdgeType;


public class DynoImpl implements IDynoService, IInventoryListener, ITopologyManagerAware, IRouting {

	private static final Logger logger = LoggerFactory
            .getLogger(DynoImpl.class);
	int thread_no = 0;
	
	private ConcurrentMap<Short, Graph<Node, Edge>> topologyBWAware;
	private ConcurrentMap<Short, DijkstraShortestPath<Node, Edge>> sptBWAware;
	DijkstraShortestPath<Node, Edge> mtp; // Max Throughput Path
	private static final long DEFAULT_LINK_SPEED = Bandwidth.BW1Gbps;
	
	/* External services */
	private ISwitchManager switchManager = null;
    private ITopologyManager topologyManager = null;
	private HashMap<String, Node> nodeList = null;
	private HashMap<String, String> portList = null;
    private HashMap<Node, Set<Edge>> topology;
	
	/* Default Constructor */
	public DynoImpl() {
		super();
		logger.info("Vnm getting instancetiated !");
	}

	/* Setter and UnSetter of External Services */
	
    void setSwitchManager(ISwitchManager s) {
        logger.info("SwitchManager is set!");
        this.switchManager = s;
    }

    void unsetSwitchManager(ISwitchManager s) {
        if (this.switchManager == s) {
            logger.info("SwitchManager is removed!");
            this.switchManager = null;
        }
    }

    void setTopologyManager(ITopologyManager s) {
        logger.info("TopologyManager is set!");
        this.topologyManager = s;
    }

    void unsetTopologyManager(ITopologyManager s) {
        if (this.topologyManager == s) {
            logger.info("TopologyManager is removed!");
            this.topologyManager = null;
        }
    }

    /* Function to be called by ODL */

    /**
     * Function called by the dependency manager when all the required
     * dependencies are satisfied
     *
     */
    public void init() {

    	/* Initialize Plugin components */    	
    	logger.info("Plugin getting Initialized by Dependency Manager!");
    	nodeList = new HashMap<String, Node>();
    	portList = new HashMap<String, String>();    	
    	
    	this.topologyBWAware = new ConcurrentHashMap<Short, Graph<Node, Edge>>();
        this.sptBWAware = new ConcurrentHashMap<Short, DijkstraShortestPath<Node, Edge>>();
        // Now create the default topology, which doesn't consider the
        // BW, also create the corresponding Dijkstra calculation
        Graph<Node, Edge> g = new SparseMultigraph();
        Short sZero = Short.valueOf((short) 0);
        this.topologyBWAware.put(sZero, g);
        this.sptBWAware.put(sZero, new DijkstraShortestPath(g));
        // Topologies for other BW will be added on a needed base
    }

	/**
     * Function called by the dependency manager when at least one
     * dependency become unsatisfied or when the component is shutting
     * down because for example bundle is being stopped.
     *
     */
    void destroy() {
        logger.error("Dyno : destroy() called!");
    }

    /**
     * Function called by dependency manager after "init ()" is called
     * and after the services provided by the class are registered in
     * the service registry
     *
     */
    void start() {
    	logger.info("Dyno : start() called!");
    	
    	// build the routing database from the topology if it exists.
        Map<Edge, Set<Property>> edges = topologyManager.getEdges();
        if (edges.isEmpty()) {
            return;
        }
        
        logger.info("ANKIT>>> Found edges: {}", edges); 
        
        List<TopoEdgeUpdate> topoedgeupdateList = new ArrayList<TopoEdgeUpdate>();
        logger.info("ANKIT>>>> Creating Dyno's routing database from the topology");
        for (Iterator<Map.Entry<Edge, Set<Property>>> i = edges.entrySet()
                .iterator(); i.hasNext();) {
            Map.Entry<Edge, Set<Property>> entry = i.next();
            Edge e = entry.getKey();
            Set<Property> props = entry.getValue();
            TopoEdgeUpdate topoedgeupdate = new TopoEdgeUpdate(e, props,
                    UpdateType.ADDED);
            topoedgeupdateList.add(topoedgeupdate);
        }
        edgeUpdate(topoedgeupdateList);
    }

    /**
     * Function called by the dependency manager before the services
     * exported by the component are unregistered, this will be
     * followed by a "destroy ()" calls
     *
     */
    void stop() {
        logger.info("Stopped");
    }


    /* InventoryListener service Interface - internal use only, exposed To ODL */

	@Override /* ODL NODE notification */
	public void notifyNode(Node node, UpdateType type, Map<String, Property> propMap) {

		String switchId;
		
        if(node == null) {
            logger.warn("New Node Notification : Node is null ");
            return;
        }
        
        if(type == null) {
        	logger.warn("New Node Notification : Type is null ");
            return;
        }
        
        /* Extract dpId from node */
        switchId = OdlUtil.getDpIdFromNode(node);
        if(switchId == null){
			logger.error("Switch Id could not be extracted !");
			return;
		}
        
        /* Check type of switch notification */
		switch (type) {
        	case ADDED:
        		this.nodeList.put(switchId, node);
        		break;

        	case CHANGED:
	            this.nodeList.put(switchId, node);
	            break;

        	case REMOVED:
	        	this.nodeList.remove(switchId);
	        	break;

        	default:
        		logger.error("Unknown Type of Switch Notification!");
		}
		
		//TODO remove this log
		//logger.info("ANKIT >>>> nodeList: "+nodeList);
	}

	@Override /* ODL NODECONNECTOR notification */
	public void notifyNodeConnector(NodeConnector nodeConnector, UpdateType type, Map<String, Property> propMap) {

		
		/* Port name is determined later */

		String portName = null;
		String portNo = null;
		PSEPort port = null;
		
        if (nodeConnector == null) {
            logger.warn("New NodeConnector Notification : NodeConnector is null");
            return;
        }
        
        if(type == null){
        	logger.warn("New NodeConnector Notification : Type is null");
            return;
        }

        if(propMap == null){
        	logger.warn("New NodeConnector Notification : Property Map is null");
            return;
        }
        
        /* Extract port No */
		portNo = OdlUtil.getPortNo(nodeConnector);
		if(portNo == null){
			logger.error("Port No could not be extracted !");
			return;
		}

		
		/* Extract dpId from Node */
		switch (type) {
	        case ADDED:
	        	/* Extract port Name */
	    		portName = OdlUtil.getPortName(propMap);
	    		portList.put(portNo, portName);
	        	break;
	
	        case CHANGED:
	        	/* Extract port Name */
	        	portName = OdlUtil.getPortName(propMap);
	    		portList.put(portNo, portName);
	        	break;
	
	        case REMOVED:
	        	
	        	portList.remove(portNo);
	        	break;
	
	        default:
	            logger.error("Unknown NodeConnector notification received");
		}
		
		//TODO remove this log
		//logger.info("ANKIT >>>> portList: " + portList);
	}

	
	
	@SuppressWarnings({ "unchecked" })
    private synchronized boolean updateTopo(Edge edge, Short bw, UpdateType type) {
        Graph<Node, Edge> topo = this.topologyBWAware.get(bw);
        DijkstraShortestPath<Node, Edge> spt = this.sptBWAware.get(bw);
        boolean edgePresentInGraph = false;
        Short baseBW = Short.valueOf((short) 0);

        if (topo == null) {
            // Create topology for this BW
            Graph<Node, Edge> g = new SparseMultigraph();
            this.topologyBWAware.put(bw, g);
            topo = this.topologyBWAware.get(bw);
            this.sptBWAware.put(bw, new DijkstraShortestPath(g));
            spt = this.sptBWAware.get(bw);
        }

        if (topo != null) {
            NodeConnector src = edge.getTailNodeConnector();
            NodeConnector dst = edge.getHeadNodeConnector();
            if (spt == null) {
                spt = new DijkstraShortestPath(topo);
                this.sptBWAware.put(bw, spt);
            }

            switch (type) {
            case ADDED:
                // Make sure the vertex are there before adding the edge
                topo.addVertex(src.getNode());
                topo.addVertex(dst.getNode());
                // Add the link between
                edgePresentInGraph = topo.containsEdge(edge);
                if (edgePresentInGraph == false) {
                    try {
                        topo.addEdge(new Edge(src, dst), src.getNode(), dst.getNode(), EdgeType.DIRECTED);
                        
                        //TODO remove me!
                        logger.info("ANKIT>>>> edge added: " + edge);
                    } catch (final ConstructionException e) {
                        logger.error("", e);
                        return edgePresentInGraph;
                    }
                }
            case CHANGED:
                // Mainly raised only on properties update, so not really useful
                // in this case
                break;
            case REMOVED:
                // Remove the edge
                try {
                    topo.removeEdge(new Edge(src, dst));
                } catch (final ConstructionException e) {
                    logger.error("", e);
                    return edgePresentInGraph;
                }

                // If the src and dst vertex don't have incoming or
                // outgoing links we can get ride of them
                if (topo.containsVertex(src.getNode()) && (topo.inDegree(src.getNode()) == 0)
                        && (topo.outDegree(src.getNode()) == 0)) {
                    logger.debug("Removing vertex {}", src);
                    topo.removeVertex(src.getNode());
                }

                if (topo.containsVertex(dst.getNode()) && (topo.inDegree(dst.getNode()) == 0)
                        && (topo.outDegree(dst.getNode()) == 0)) {
                    logger.debug("Removing vertex {}", dst);
                    topo.removeVertex(dst.getNode());
                }
                break;
            }
            spt.reset();
            if (bw.equals(baseBW)) {
                clearMaxThroughput();
            }
        } else {
            logger.error("Cannot find topology for BW {} this is unexpected!", bw);
        }
        return edgePresentInGraph;
    }

    private boolean edgeUpdate(Edge e, UpdateType type, Set<Property> props, boolean local) {
        String srcType = null;
        String dstType = null;

        logger.info("Got an edgeUpdate: {} props: {} update type: {} local: {}", new Object[] { e, props, type, local });

        if ((e == null) || (type == null)) {
            logger.error("Edge or Update type are null!");
            return false;
        } else {
            srcType = e.getTailNodeConnector().getType();
            dstType = e.getHeadNodeConnector().getType();

            if (srcType.equals(NodeConnector.NodeConnectorIDType.PRODUCTION)) {
                logger.debug("Skip updates for {}", e);
                return false;
            }

            if (dstType.equals(NodeConnector.NodeConnectorIDType.PRODUCTION)) {
                logger.debug("Skip updates for {}", e);
                return false;
            }
        }

        Bandwidth bw = new Bandwidth(0);
        boolean newEdge = false;
        if (props != null) {
            props.remove(bw);
        }

        Short baseBW = Short.valueOf((short) 0);
        // Update base topo
        newEdge = !updateTopo(e, baseBW, type);
        if (newEdge == true) {
            if (bw.getValue() != baseBW) {
                // Update BW topo
                updateTopo(e, (short) bw.getValue(), type);
            }
        }
        return newEdge;
    }

    /* IListenTopoUpdates service Interface - internal use only, exposed To ODL */
    @Override
    public void edgeUpdate(List<TopoEdgeUpdate> topoedgeupdateList) {
        logger.info("Start of a Bulk EdgeUpdate with " + topoedgeupdateList.size() + " elements");
        boolean callListeners = false;
        for (int i = 0; i < topoedgeupdateList.size(); i++) {
            Edge e = topoedgeupdateList.get(i).getEdge();
            Set<Property> p = topoedgeupdateList.get(i)
                    .getProperty();
            UpdateType type = topoedgeupdateList.get(i)
                    .getUpdateType();
            boolean isLocal = topoedgeupdateList.get(i)
                    .isLocal();
            if ((edgeUpdate(e, type, p, isLocal)) && (!callListeners)) {
                callListeners = true;
            }
        }

        logger.info("End of a Bulk EdgeUpdate");
    }
	

	@Override
	public void edgeOverUtilized(Edge edge) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void edgeUtilBackToNormal(Edge edge) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean provisionFlow(HashMap<String, String> flowAttributes) {
		// TODO Auto-generated method stub
		return false;
	}
	
	/* Plugin service Interface - exposed as a service */
	
	@Override
	public HashMap<String, PSESwitch> getSwitch() {
		
		HashMap<String, PSESwitch> switchMap = new HashMap<String, PSESwitch>();
		Node switchNode = null;
		PSESwitch switchInfo = null;
		
		/* For all switch in the  */
		for(String switchId: nodeList.keySet()) {
			
			switchNode = nodeList.get(switchId);
			switchInfo = new PSESwitch();
			switchInfo.setSwitchId(switchId);
			switchInfo.setOpenFlow(OdlUtil.isOpenFlowSwitch(switchNode));
			Set<NodeConnector> allNodeConnectors = switchManager.getNodeConnectors(switchNode);
			/* check if no node is attached */
			if(allNodeConnectors == null){
				logger.info("No node connector is attached to node: {}", switchNode);
			}
			else {
				for(NodeConnector nodeConnector : allNodeConnectors) {
					String portNo = OdlUtil.getPortNo(nodeConnector);
				    switchInfo.addPortNo(portNo);
				}
			}
			switchMap.put(switchId, switchInfo);
		}
		
		return switchMap;
	}

	@Override
	public HashMap<String, PSEPort> getPort(String switchId) {
		
		Node switchNode = null;
		HashMap<String, PSEPort> portMap  = new HashMap<String, PSEPort>();
		
		if(switchId == null) {
			logger.info("User must specifiy a valid switch ID");
		}
		
		switchNode = nodeList.get(switchId);
		Set<NodeConnector> allNodeConnectors = switchManager.getNodeConnectors(switchNode);
		/* check if no node is attached */
		if(allNodeConnectors == null){
			logger.info("No node connector is attached to node: {}", switchNode);
		}
		else {
			for(NodeConnector nodeConnector : allNodeConnectors) {
				PSEPort port = new PSEPort();
				
				String portNo = OdlUtil.getPortNo(nodeConnector);
				port.setPortNo(portNo);
				/* TODO: port name is not being added */
			    String portName = portList.get(portNo);
			    port.setPortName(portName);
			    
			    portMap.put(portNo, port);
			}
		}
		return portMap;
	}

	@Override
	public Path getRoute(Node src, Node dst) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Path getMaxThroughputRoute(Node src, Node dst) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Path getRoute(Node src, Node dst, Short Bw) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void clear() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void clearMaxThroughput() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void initMaxThroughput(Map<Edge, Number> EdgeWeightMap) {
		// TODO Auto-generated method stub
		
	}

	
	
	
	
}
