import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

/**
 * @author jlortiz
 */
public class ServerDaemon
{
    private static ServerSocket listener;
    private static final HashMap<String, ServerThread> sockets = new HashMap<>((Integer)JMailServer.get("maxconnections"));
    public ServerDaemon(int port) throws IOException {
        listener = new ServerSocket(port);
    }
    public static void stop() throws IOException {
        listener.close();
    }
    public static HashMap<String, ServerThread> getSockets() {
        return sockets;
    }
    public static void remove(String ip) {
        sockets.remove(ip);
    }
    
    public void run() {
        try {
            while (!listener.isClosed()) {
                while (Thread.activeCount()>(Integer)JMailServer.get("maxconnections"))
                    wait(1000);
                Socket sock = listener.accept();
                //TODO: implement blacklist/whitelist
                ServerThread runner = new ServerThread(sock);
                //TODO: find better way to deal with same-connection issues
                if (!sockets.containsKey(sock.getInetAddress().getHostAddress())) {
                    sockets.put(sock.getInetAddress().getHostAddress(), runner);
                }
                runner.start();
            }
        } catch (InterruptedException | IOException e) {
            if (!e.getMessage().equals("socket closed")) {
                System.out.println(e);
            }
        }
        try {
            while (!sockets.isEmpty())
                sockets.values().iterator().next().join();
        } catch (InterruptedException e) {}
    }
}
