public interface NodeDetection extends java.rmi.Remote {
    long getNodeLogicalTime(int nodeId) throws java.rmi.RemoteException;
    void setNodeLogicalTime(int nodeID, int value) throws java.rmi.RemoteException;
    void sendMessage(String message, int nodeID) throws java.rmi.RemoteException;
    String receiveMessage(int destinationNodeID, int senderNodeID) throws java.rmi.RemoteException;
    boolean nodesUpAndRunning(int nodeID, int numberOfNodes) throws java.rmi.RemoteException;
    void workComplete(int nodeID, boolean isDone) throws java.rmi.RemoteException;
}
