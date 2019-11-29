import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.lang.SecurityManager;
import java.rmi.RemoteException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.TimeUnit;


//This is the class that is doing all the work: generates the events and processed them
public class DetectionNodeProcessor {
    private int nodeId;
    private static NodeDetection dns;
    private List<String> eventsJournal;
    private Queue<String> receivedMessages;
    private long numberOfIterations;
    //randomMessages variable stores 5 different strings/words used to create random 2 words messages
    private String[] randomMessages;

    public DetectionNodeProcessor(int nodeId, long numberOfIterations) {
        this.nodeId = nodeId;
        this.numberOfIterations = numberOfIterations;
        this.randomMessages = new String[]{"aaaaaaa bbbbbbb", "ccccccc ddddddd", "eeeeeee fffffff", "ggggggg hhhhhhh", "iiiiiii jjjjjjj"};
        this.eventsJournal = new ArrayList<>();
        this.receivedMessages = new ArrayDeque<>();
    }

    private int getNodeId() {
        return nodeId;
    }

    private long getNumberOfIterations() {
        return numberOfIterations;
    }

    private String[] getRandomMessages() {
        return randomMessages;
    }

    private List<String> getEventsJournal() {
        return eventsJournal;
    }

    public void setEventsJournal(List<String> eventsJournal) {
        this.eventsJournal = eventsJournal;
    }

    private Queue<String> getReceivedMessages() {
        return receivedMessages;
    }

    public void setReceivedMessages(Queue<String> receivedMessages) {
        this.receivedMessages = receivedMessages;
    }

    //This run method runs the activities of the detection node, for the number of iterations that was set when the
    //detection node (detectionNodeProcessor) object was created in the Assignment1 class
    private void run(int numberOfNodes) {
        int senderNodeId;
        int typeOfEvent;
        long nodeLC = 0;
        System.out.println("NodeID = "+getNodeId());
        for (int i = 0; i < getNumberOfIterations(); i++) {
/*
            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }rmi
*/
            System.out.println("i="+i);
            //There are three type of events that generateTypeOfEvent might randomly return
            typeOfEvent = generateTypeOfEvent();

            try {
                dns.setNodeLogicalTime(getNodeId(), 1);
                //The arbitraryFailure method will change the logical clock, if one of the arbitrary failures implemented happens
                arbitraryFailure(getNodeId(), false);
                nodeLC = dns.getNodeLogicalTime(getNodeId());
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            System.out.println("DN logical clock = "+nodeLC);
            System.out.println("typeofevent = "+typeOfEvent);
            switch (typeOfEvent) {
                case 0:
                    internalEvent(nodeLC);
                    break;
                case 1:
                    senderNodeId = generateNodeId(numberOfNodes);
                    receivingEvent(senderNodeId, nodeLC);
                    break;
                case 2:
                    int destinationNodeId = generateNodeId(numberOfNodes);
                    sendingEvent(destinationNodeId, nodeLC);
                    break;
            }
        }

        String fileName="";
        try {
            fileName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        saveResultsToFile(fileName);

        try {
            System.out.println("Logical clock = " + dns.getNodeLogicalTime(this.getNodeId()));
            dns.workComplete(getNodeId(), true);
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }

    }

    //This method simulates different random types of arbitrary failures
    private int arbitraryFailure(int nodeId, boolean advance) throws RemoteException {

        //af variable controls the chances that an arbitrary failure to happen
        int af = (int) (Math.random() * 500);

        //This simulates a failure where the node doesn't advance its internal logical clock when an event happens.
        //Since the LC was incremented before the arbitraryFailure was called, the value of the LC is reduced by 1 unit
        // to simulate this
        if ((af == 222) && (!advance)) {
            dns.setNodeLogicalTime(nodeId, -1);
        }

        //This simulates a failure where the node advances its LC two units when an event happens
        if ((af == 333) && (!advance)) {
            dns.setNodeLogicalTime(nodeId, 1);
        }

        //This simulates a failure where the node doesn't advance its LC to have a greater value than the LC associated
        //with a message received from another detection node
        //it returns -1 to signal the receivingEvent method to skip the logical clock synchronization
        if ((af == 444) && (advance)) {
            return -1;
        }
        return 1;
    }

    //This method generates a random node id that will be used either as the destination node id of a sending event
    //or in case of a receiving event the sender node id for which a receiving event will look to see if a message
    //was received in its receiving que
    private int generateNodeId(int numberOfNodes) {
        int nodeId;
        do {
            nodeId = ((int) (Math.random() * numberOfNodes));
        } while (nodeId == getNodeId());

        return nodeId;
    }

    //This method generates a random type of event for the run method to process
    private int generateTypeOfEvent() {
        int typeOfEvent = (int) (Math.random() * 100);
        if ((typeOfEvent >= 0) && (typeOfEvent <= 24)) {
            return 0;
        }
        if ((typeOfEvent >= 25) && (typeOfEvent <= 75)) {
            return 1;
        }
        return 2;
    }

    //This method generates a random internal event (create message or anomaly detection) for the internalEvent method
    //to process
    private void internalEvent(long nodeLC) {
        int typeOfInternalEvent = (int) (Math.random() * 2);
        if (typeOfInternalEvent == 0) {
            generateMessage(nodeLC);
        } else {
            anomalyDetection(nodeLC);
        }
    }

    //This method generates a random message following the pattern:
    //node_Id+" "+type_of_Event+" "+message
    private String createRandomMessage(String eventType) {
        String message;
        int chooseMessage = (int) (Math.random() * 5);
        message = getNodeId() + " " + eventType + " " + getRandomMessages()[chooseMessage];
        return message;
    }

    //This method handle the internal event of creating a message
    private void generateMessage(long nodeLC) {
        String message = nodeLC + " " + createRandomMessage("message_generation");
        getEventsJournal().add(message);
    }

    //This method handle the internal event of detecting an anomaly in a received message
    private void anomalyDetection(long nodeLC) {
        String message;

        //The method analysis the messages that were received (from the receivedMessages ques)
        //If the que is empty it logs this is the eventsJournal list
        if (getReceivedMessages().isEmpty()) {
            message = nodeLC + " " + getNodeId() + " " + "detection_event" + " " + "No messages available to analyze";
            getEventsJournal().add(message);
            return;
        }

        //abnormalMessage controls the chances that a message is considered normal or abnormal
        //since the assignment requirements said this is not relevant for the assignment I implemented this as a
        //random decision
        int abnormalMessage = (int) (Math.random() * 10);

        //If the receivedMessages que is not empty it reads the first element from the que and it removes it also
        message = nodeLC + " " + getNodeId() + " " + "detection_event" + " " + getReceivedMessages().poll();

        //The message analysis was implemented here as random decision to classify a received message as normal or abnormal
        if (abnormalMessage == 6) {
            message = message + " " + "ABNORMAL_MESSAGE";
        } else {
            message = message + " " + "NORMAL_MESSAGE";
        }
        getEventsJournal().add(message);
    }

    //This method implements a receiving event
    private void receivingEvent(int senderNodeId, long nodeLC) {
        String message;
        int senderLogicalClock;
        message = nodeLC + " " + getNodeId() + " " + "receiving_event";

        //The method looks in the receiving que to process a message from specific detection node
        //If the receiving que is empty or no message has been received from the specified node it logs this in the eventsJournal and exits

        String messageReceived="";
        try {
            messageReceived = dns.receiveMessage(getNodeId(), senderNodeId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        if (messageReceived.isEmpty()) {
            message = message + " " + "Receiving que is empty or no message received from node "+senderNodeId;
            getEventsJournal().add(message);
            return;
        }

        //Method getNodeLogicalClockFromIncomingMessage extracts the logical clock of the sender node from
        // the received message
        senderLogicalClock = getNodeLogicalClockFromIncomingMessage(messageReceived);

        // the sender logical clock is compared to the internal logical clock of the working node and if senders
        //value is greater or equal to the receiver one, the receiver LC is updated
        int abrflr = 0;
        try {
            abrflr = arbitraryFailure(getNodeId(), true);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        if ((senderLogicalClock > nodeLC) && (abrflr != -1)) {
            System.out.println("Sender LC, "+senderLogicalClock+" is greater than destination LC, "+getNodeId());
            //System.out.println("DN "+getNodeId()+" LC was "+processorQues.getLogicalClock()+" and it advanced to "+senderLogicalClock+"+1 because senderNode "+senderNodeId+" LC was "+senderLogicalClock);
            try {
                long value = senderLogicalClock - nodeLC+1;
                dns.setNodeLogicalTime(getNodeId(), (int)value);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            nodeLC = senderLogicalClock+1;
            System.out.println("Destination LC time is now: "+nodeLC);
            message = nodeLC + " " + getNodeId()+ " " + "receiving_event";

        }

        message = message + " " + messageReceived;

        //Adds the message to receivedMessages que and also to the eventsJournal list
        getReceivedMessages().add(message);
        getEventsJournal().add(message);
    }

    //This method implements a sending event
    private void sendingEvent(int destinationNodeId, long nodeLC) {

        //A random message is created to be sent to a detection node.
        //From the discussions in the class with Dr Raje and from the assigment directions the message sent does't need
        //to be in response to a received message for this assignment
        String message = nodeLC + " " + createRandomMessage("sending_event to DN " + destinationNodeId);

        //The access to the destination receiving que is synchronized to avoid a race condition between this write operation
        //and an internal read operation from the destination node.


/*
        synchronized (getOtherProcessorsQues().get(destinationNodeId).getReceivingQue()){
            getOtherProcessorsQues().get(destinationNodeId).addMessageToReceivingQue(message);
        }
*/

        //Adds the sending message to sending que of the destination node and also to the eventsJournal list
        try {
            dns.sendMessage(message, destinationNodeId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        getEventsJournal().add(message);
    }

    private int getNodeLogicalClockFromIncomingMessage(String message) {
        String[] splitMessage = message.split(" ", 2);
        return (Integer.parseInt(splitMessage[0]));
    }

    private void saveResultsToFile(String fileName) {
        String name = "Report-" + fileName + ".txt";
        try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(name), "UTF-8"))) {
            out.write("-------------------------------------------------------\n");
            out.write("-------------------------------------------------------\n");

            for (String jn : getEventsJournal()) {
                out.write(jn);
                out.newLine();
            }
            out.write("++++++++++++++++++++++++++++++++++++++++++++++\n");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    public static void main(String[] args) {
        int numberOfNodes = 1;
        int numberOfIterations = 100;
        System.out.println("Set security policy file");
        System.setProperty("java.security.policy", "file:./server.policy");
        System.out.println("Set Security Manager");
        System.setSecurityManager(new SecurityManager());
        try {
            System.out.println("Looking for the Server");
            String name = "//DStoian-LEN/DetectionNodeServer";
            //String name = "//Doru-PC/DetectionNodeServer";
            //String name = "//LAPTOP-GDTMA4IQ/DetectionNodeServer";
            dns = (NodeDetection) Naming.lookup(name);
            System.out.println("Binding to the server");
            long i=0;
            while (!dns.nodesUpAndRunning(Integer.parseInt(args[0]), numberOfNodes)) {
                if (i == 0) {
                    System.out.println("Not all nodes are up and running! Please wait!");
                }
                i++;
            }
            System.out.println("All nodes are up and running!");

            DetectionNodeProcessor dnp = new DetectionNodeProcessor(Integer.parseInt(args[0]), numberOfIterations);
            dnp.run(numberOfNodes);
        } catch (RemoteException | NotBoundException | MalformedURLException ex) {
            ex.printStackTrace();
        }
    }
}