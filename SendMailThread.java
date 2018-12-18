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
        } catch (IOException e) {}
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
            send(SendMail.get(this.socket.getInetAddress(), msg));
        }
    }
}
