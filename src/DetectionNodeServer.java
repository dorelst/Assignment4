import java.io.Serializable;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.lang.SecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class DetectionNodeServer extends UnicastRemoteObject implements NodeDetection, Serializable {
    private static final long serialVersionUID = 987654321L;
    private List<Long> logicalClocks;
    private List<List<String>> messagesQue;
    private Set<Integer> runningDNs;
    private List<Boolean> workCompleted;

    private static ExecutorService detectionNodesExecutor = Executors.newFixedThreadPool(1);

    public DetectionNodeServer(String name, int numberOfNodes) throws RemoteException {
        super();
        this.logicalClocks = new ArrayList<>();
        this.messagesQue = new ArrayList<>();
        this.workCompleted = new ArrayList<>();
        for(int i=0; i<numberOfNodes; i++) {
            this.logicalClocks.add(0L);
            this.messagesQue.add(new ArrayList<String>());
            this.workCompleted.add(false);
        }
        this.runningDNs = new HashSet<>();
    }

    @Override
    public long getNodeLogicalTime(int nodeID) throws RemoteException {
        long lc = getLogicalClocks().get(nodeID);
        System.out.println("getNodeLogicalTime NodeId = "+nodeID+" which is "+lc);

        return lc;
    }

    @Override
    public void setNodeLogicalTime(int nodeID, int value) throws RemoteException {
        long lcValue = getLogicalClocks().get(nodeID);
        System.out.println("setNodeLogicalTime NodeId = "+nodeID+" has LC: "+lcValue);
        lcValue = lcValue + value;
        System.out.println("Node "+nodeID+" had its LC advanced " + value + " units to = "+lcValue);
        getLogicalClocks().set(nodeID, lcValue);
    }

    @Override
    public synchronized void sendMessage(String message, int nodeID) throws RemoteException {
        System.out.println("SendMessage NodeId = "+nodeID);
        getMessagesQue().get(nodeID).add(message);
    }

    @Override
    public synchronized String receiveMessage(int destinationNodeID, int senderNodeID) {
        System.out.println("Receiving message destination nodeId = "+destinationNodeID+" sender node ID = "+senderNodeID);
        String message = "";
            if (!getMessagesQue().get(destinationNodeID).isEmpty()) {

                //If the receiving que is not empty it iterates through its entries to search for a message sent
                //by the node specified by the senderNodeId parameter
                Iterator<String> iterator = getMessagesQue().get(destinationNodeID).iterator();
                int nodeId;
                while (iterator.hasNext()) {
                    message = iterator.next();
                    nodeId = getNodeIdFromIncomingMessage(message);

                    //If a message from the detection node specified by the senderNodeId parameter is found
                    //then it remove it from the receiving que, gets out from while loop and returns the message it founded
                    if (nodeId == senderNodeID) {
                        iterator.remove();
                        break;
                    }
                    message = "";
                }
            }

        return message;
    }

    @Override
    public boolean nodesUpAndRunning(int nodeID, int numberOfNodes) throws java.rmi.RemoteException {
        if ((nodeID > -1) && (nodeID <4)) {
            System.out.println("nodeUpAndRunning NodeId = "+nodeID);
            getRunningDNs().add(nodeID);
            if (getRunningDNs().size()>(numberOfNodes-1))
                return true;
        }
        return false;
    }

    @Override
    public void workComplete(int nodeID, boolean isDone) throws java.rmi.RemoteException {
        getWorkCompleted().set(nodeID, isDone);
    }

    public List<Long> getLogicalClocks() {
        return logicalClocks;
    }

    public List<List<String>> getMessagesQue() {
        return messagesQue;
    }

    public Set<Integer> getRunningDNs() {
        return runningDNs;
    }

    public List<Boolean> getWorkCompleted() {
        return workCompleted;
    }

    private int getNodeIdFromIncomingMessage(String message) {
        String[] splitMessage = message.split(" ", 3);
        return (Integer.parseInt(splitMessage[1]));
    }

    public static void main(String[] args) {
        int numberOfNodes = 1;
        try {
            LocateRegistry.createRegistry(1099);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        System.setProperty("java.security.policy", "file:./server.policy");
        System.setSecurityManager(new SecurityManager());
        DetectionNodeServer dns=null;

        try {
            System.out.println("Creating Detection Nodes Server!");
            String name = "DetectionNodeServer";
            dns = new DetectionNodeServer(name, numberOfNodes);
            System.out.println("Detection Node: binding it to name: " + name);
            Naming.rebind(name, dns);
            System.out.println("Detection Node Server Ready!");
            Sampler sampler = new Sampler(dns.getLogicalClocks(), dns.getWorkCompleted(), dns.getRunningDNs());
            detectionNodesExecutor.submit(sampler);
        } catch (RemoteException | MalformedURLException e) {
            System.out.println("Exception Occured: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
