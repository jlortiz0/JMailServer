package org.jlortiz;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.logging.Level;
/**
 * Write a description of class SendMailThread here.
 * 
 * @author (your name) 
 * @version (a version number or a date)
 */
public class SendMailThread extends ServerThread
{
    public SendMailThread(Socket sock) throws UnknownHostException,IOException {
        super(sock);
    }
    @Override
    public void close() {
        try {
            output.close();
            input.close();
            socket.close();
        } catch (IOException e) {
            JMailServer.log.warning(e.toString());
            JMailServer.log.log(Level.WARNING, "Error on closing SendMailThread connected to {0} / {1}", new String[]{socket.getInetAddress().getHostName(), socket.getInetAddress().getHostAddress()});
        }
    }
    @Override
    public void run() {
        send("true");
        while (true) {
            flush();
            String msg = receive();
            if (msg.equals("QUIT")) {
                close();
                return;
            }
            send(SendMail.get(socket.getInetAddress(), msg));
        }
    }
}
