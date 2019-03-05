package org.jlortiz;

/*
 * Copyright (C) 2019 jlortiz
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import static org.jlortiz.JMailServer.log;

/**
 * 
 * @author jlortiz
 */
public class ServerDaemon
{
    private static ServerSocket listener;
    private static final HashMap<String, ServerThread> sockets = new HashMap<>((Integer)JMailServer.get("maxconnections"));
    public ServerDaemon(int port) throws IOException {
        listener = new ServerSocket(port);
        log.log(Level.INFO, "Started ServerDaemon on port {0}", String.valueOf(port));
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
                    Thread.sleep(1000);
                Socket sock = listener.accept();
                if ((Boolean)JMailServer.get("useblist")) {
                    if (((List)JMailServer.get("blist")).contains(sock.getInetAddress().getHostName()) || ((List)JMailServer.get("blist")).contains(sock.getInetAddress().getHostAddress())) {
                        new DataOutputStream(sock.getOutputStream()).writeUTF("QUIT Blacklisted!");
                        log.log(Level.FINE, "Kicked {0} / {1} for blacklist.", new String[]{sock.getInetAddress().getHostName(), sock.getInetAddress().getHostAddress()});
                        sock.close();
                        continue;
                    }
                }
                if ((Boolean)JMailServer.get("usewlist")) {
                    if (!((List)JMailServer.get("wlist")).contains(sock.getInetAddress().getHostName()) || !((List)JMailServer.get("wlist")).contains(sock.getInetAddress().getHostAddress())) {
                        new DataOutputStream(sock.getOutputStream()).writeUTF("QUIT Not whitelisted!");
                        log.log(Level.FINE, "Kicked {0} / {1} for whitelist.", new String[]{sock.getInetAddress().getHostName(), sock.getInetAddress().getHostAddress()});
                        sock.close();
                        continue;
                    }
                }
                try {
                    if (new DataInputStream(sock.getInputStream()).readUTF().equals("SEND")) {
                        if (!(Boolean)JMailServer.get("getmailremote")) {
                             new DataOutputStream(sock.getOutputStream()).writeUTF("getmail");
                             log.log(Level.FINE, "Kicked {0} / {1} for getmail.", new String[]{sock.getInetAddress().getHostName(), sock.getInetAddress().getHostAddress()});
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
                    log.log(Level.FINE, "Kicked {0} / {1} for already connected.", new String[]{sock.getInetAddress().getHostName(), sock.getInetAddress().getHostAddress()});
                    sock.close();
                    continue;
                }
                if (Thread.activeCount()>(Integer)JMailServer.get("maxconnections")) {
                    new DataOutputStream(sock.getOutputStream()).writeUTF("Server is overloaded, try again later");
                    log.log(Level.FINE, "Kicked {0} / {1} for overload.", new String[]{sock.getInetAddress().getHostName(), sock.getInetAddress().getHostAddress()});
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
                log.warning(e.toString());
                log.warning("Error on closing ServerDaemon.");
            }
        }
        try {
            while (!sockets.isEmpty())
                if ((Boolean)JMailServer.get("shkick"))
                    sockets.values().iterator().next().close(true);
                else
                    sockets.values().iterator().next().join();
        } catch (InterruptedException e) {}
    }
}
