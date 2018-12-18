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
                        sock.getOutputStream().write("QUIT Blacklisted!\n".getBytes());
                        sock.getOutputStream().flush();
                        sock.close();
                        continue;
                    }
                }
                if ((Boolean)JMailServer.get("usewlist")) {
                    if (!((List)JMailServer.get("wlist")).contains(sock.getInetAddress().getHostName()) || !((List)JMailServer.get("wlist")).contains(sock.getInetAddress().getHostAddress())) {
                        sock.getOutputStream().write("QUIT Not whitelisted!\n".getBytes());
                        sock.getOutputStream().flush();
                        sock.close();
                        continue;
                    }
                }
                try {
                    byte b[] = new byte[8];
                    sock.getInputStream().read(b);
                    if ((new String(b)).equals(" SEND  ")) {
                        new SendMailThread(sock).start();
                        System.out.println("Sendmail started!");
                        continue;
                    }
                } catch (IOException e) {
                    sock.close();
                    continue;
                }
                if (sockets.containsKey(sock.getInetAddress().getHostAddress())) {
                    sock.getOutputStream().write("Already connected\n".getBytes());
                    sock.getOutputStream().flush();
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
            while (!sockets.isEmpty())
                sockets.values().iterator().next().join();
        } catch (InterruptedException e) {}
    }
}
