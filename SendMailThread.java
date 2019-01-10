import java.net.Socket;
import java.net.UnknownHostException;
import java.io.IOException;
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
    public void close() {
        try {
            output.close();
            input.close();
            socket.close();
        } catch (IOException e) {
            JMailServer.log.warning(e.toString());
            JMailServer.log.warning("Error on closing SendMailThread connected to "+socket.getInetAddress().getHostName()+" / "+socket.getInetAddress().getHostAddress());
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
