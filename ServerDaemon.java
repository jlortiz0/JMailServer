import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;

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
                if ((Boolean)JMailServer.get("useblist")) {
                    if (((List)JMailServer.get("blist")).contains(sock.getInetAddress().getHostName()) || ((List)JMailServer.get("blist")).contains(sock.getInetAddress().getHostAddress())) {
                        new DataOutputStream(sock.getOutputStream()).writeUTF("QUIT Blacklisted!");
                        sock.close();
                        continue;
                    }
                }
                if ((Boolean)JMailServer.get("usewlist")) {
                    if (!((List)JMailServer.get("wlist")).contains(sock.getInetAddress().getHostName()) || !((List)JMailServer.get("wlist")).contains(sock.getInetAddress().getHostAddress())) {
                        new DataOutputStream(sock.getOutputStream()).writeUTF("QUIT Not whitelisted!");
                        sock.close();
                        continue;
                    }
                }
                try {
                    byte b[] = new byte[8];
                    sock.getInputStream().read(b);
                    if ((new String(b)).equals(" SEND  ")) {
                        if (!(Boolean)JMailServer.get("getmailremote")) {
                             new DataOutputStream(sock.getOutputStream()).writeUTF("getmail");
                             sock.close();
                             continue;
                        }
                        new SendMailThread(sock).start();
                        continue;
                    }
                } catch (IOException e) {
                    sock.close();
                    continue;
                }
                if (sockets.containsKey(sock.getInetAddress().getHostAddress())) {
                    new DataOutputStream(sock.getOutputStream()).writeUTF("Already connected");
                    sock.close();
                    continue;
                }
                ServerThread runner = new ServerThread(sock);
                sockets.put(sock.getInetAddress().getHostAddress(), runner);
                runner.start();
            }
        } catch (InterruptedException | IOException e) {
            if (!e.getMessage().equals("socket closed")) {
                System.out.println(e);
            }
        }
        try {
            //TODO: Implement shkick without causing ConcurrentModificationException
            while (!sockets.isEmpty())
                sockets.values().iterator().next().join();
        } catch (InterruptedException e) {}
    }
}
