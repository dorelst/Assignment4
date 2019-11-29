import java.io.Serializable;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;

public class BankServer extends UnicastRemoteObject implements Bank, Serializable {
    private static final long serialVersionUID = 123456789L;
    private float value = 10000;
    private int id = 0;
    private String name;
    public BankServer(String s) throws RemoteException {
        super();
        name = s;
    }

    public synchronized int get_id() throws RemoteException {
        id++;
        return id;
    }

    public synchronized void deposit(int id, float amount) throws RemoteException {
        System.out.println("Deposit by: "+id);
        value += amount;
        System.out.println("Balance: "+value);
    }

    public synchronized void withdraw(int id, float amount) throws RemoteException {
        System.out.println("Withdrawn by: " + id);
        if (value < amount)
            System.out.println("Not Allowed!");
        else {
            value -= amount;
            System.out.println("Balance: "+  value);
        }
    }

    public synchronized void check(int id) throws RemoteException {
        System.out.println("Checked by: " + id);
        System.out.println("Balance: " + value);
    }

    public static void main(String[] args) throws RemoteException {
        LocateRegistry.createRegistry(1099);
        System.setProperty("java.security.policy", "file:./server.policy");
        System.setSecurityManager(new SecurityManager());
        try {

            System.out.println("Creating a Bank Server!");
            String name = "BankServer";
            BankServer bank = new BankServer(name);
            System.out.println("BankServer: binding it to name: " + name);
            Naming.rebind(name, bank);
            System.out.println("Bank Server Ready!");

        } catch (RemoteException | MalformedURLException e) {
            System.out.println("Exception Occured: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
