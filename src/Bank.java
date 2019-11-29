public interface Bank extends java.rmi.Remote {
    int get_id() throws java.rmi.RemoteException;
    void deposit(int id, float amount) throws java.rmi.RemoteException;
    void withdraw(int id, float amount) throws java.rmi.RemoteException;
    void check(int id) throws java.rmi.RemoteException;
}
