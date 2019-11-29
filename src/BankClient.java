import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

public class BankClient {
    static int my_id;

    public static void main(String[] args) {
        System.setProperty("java.security.policy", "file:./server.policy");
        System.setSecurityManager(new SecurityManager());
        try {
            String name = "//DStoian-LEN/BankServer";
            //String name = "//192.168.1.101/BankServer";
            Bank myBank = (Bank) Naming.lookup(name);
            my_id = myBank.get_id();
            myBank.check(my_id);
            myBank.deposit(my_id, 500);
            myBank.withdraw(my_id, 200);
        } catch (RemoteException | MalformedURLException | NotBoundException e) {
            System.out.println("BankClient: an exception occured " + e.getMessage());
            e.printStackTrace();
        }
        System.exit(0);
    }
}
