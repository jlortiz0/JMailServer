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
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.logging.Level;
/**
 * 
 * @author jlortiz
 */
public class SendMailThread extends ServerThread
{
    public SendMailThread(Socket sock) throws UnknownHostException,IOException {
        super(sock);
    }
    private void close() {
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
