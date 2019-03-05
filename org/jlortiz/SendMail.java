package org.jlortiz;

/*
 * Copyright (C) 2018 jlortiz
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
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.*;
import java.util.logging.Level;
import static org.jlortiz.JMailServer.log;
/**
 * 
 * @author jlortiz
 */
public class SendMail {
    public static String send(String usr, String s) {
        long unixTime = Instant.now().getEpochSecond();
        String from;
        try {
            from = usr+"@"+InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "host";
        }
        Scanner sc = new Scanner(s);
        String recptsRaw=sc.nextLine();
        String recpts[];
        try (Scanner sf = new Scanner(recptsRaw)) {
            HashSet<String> ls = new HashSet<>(15);
            while (sf.hasNext()) {
                ls.add(sf.next());
            }
            recpts = ls.toArray(new String[0]);
        }
        sc.useDelimiter("\\A");
        String subject = sc.nextLine();
        String message = sc.next();
        HashMap<String,ArrayList<String>> servers = new HashMap<>();
        ArrayList<String> responses = new ArrayList(recpts.length);
        for (String a: recpts) {
            if (!a.contains("@")) {
                responses.add("localhost: fmt  "+a);
                continue;
            }
            if (!servers.containsKey(a.split("@")[1]))
                servers.put(a.split("@")[1], new ArrayList<>());
            servers.get(a.split("@")[1]).add(a.split("@")[0]);
        }
        for (String a: servers.keySet()) {
            try {
                Socket sock;
                if (a.contains(":")) {
                    sock = new Socket(a.split(":")[0], Integer.parseInt(a.split(":")[1]));
                    a=a.split(":")[0];
                } else {
                    sock = new Socket(a, 5000);
                }
                DataOutputStream output = new DataOutputStream(sock.getOutputStream());
                DataInputStream input = new DataInputStream(sock.getInputStream());
                output.writeUTF("SEND");
                output.flush();
                while (input.available()<1)
                    Thread.sleep(100);
                if (!input.readUTF().equals("true")) {
                    output.close();
                    input.close();
                    sock.close();
                    continue;
                }
                for (String b: servers.get(a)) {
                    output.writeUTF(from+"\n"+unixTime+"\n"+b+"\n"+recptsRaw+"\n"+subject+"\n"+message);
                    b = input.readUTF();
                    if (b.equals("true"))
                        continue;
                    responses.add(a+": "+b);
                    if (b.equals("getmail"))
                        break;
                }
                output.writeUTF("QUIT");
                output.close();
                input.close();
                sock.close();
            } catch (UnknownHostException e) {
                responses.add(a+": badHost");
            } catch (IOException | InterruptedException e) {
                responses.add(a+": io");
                log.warning(e.toString());
            }
        }
        responses.trimToSize();
        StringBuilder resp = new StringBuilder();
        responses.stream().map((a) -> {
            resp.append(a);
            return a;
        }).forEachOrdered((_item) -> {
            resp.append("\n");
        });
        return resp.toString();
    }
    public static String get(InetAddress addr, String s) {
        Scanner sc = new Scanner(s);
        String from = sc.nextLine();
        long date = sc.nextLong();
        sc.nextLine();
        String to = sc.nextLine();
        String recpts[]=new String[0];
        try (Scanner sf = new Scanner(sc.nextLine())) {
            ArrayList<String> ls = new ArrayList<>(15);
            while (sf.hasNext()) {
                ls.add(sf.next());
            }
            recpts = ls.toArray(recpts);
        }
        String subject = sc.nextLine();
        sc.useDelimiter("\\A");
        String message = sc.next();
        if (!(new File(System.getProperty("user.home")+"\\Documents\\JMail\\users\\"+to).isDirectory())) {
            return "User "+to;
        }
        if (new File(System.getProperty("user.home")+"\\Documents\\JMail\\users\\"+to+"\\mail\\"+subject+" from "+from).exists()) {
            return "exists "+to;
        }
        try (FileOutputStream f = new FileOutputStream(System.getProperty("user.home")+"\\Documents\\JMail\\users\\"+to+"\\mail\\"+subject+" from "+from)) {
            f.write(("Date: "+Date.from(Instant.ofEpochSecond(date))).getBytes());
            f.write(("\nFrom: "+from).getBytes());
            f.write(("\nTo: "+String.join(", ", recpts)).getBytes());
            f.write(("\nSubject: "+subject).getBytes());
            f.write(("\n\n"+message).getBytes());
            f.flush();
        } catch (IOException e) {
            log.warning(e.toString());
            log.log(Level.WARNING, "Error sending mail to {0}", to);
            return "write "+to;
        }
        return "true";
    }
}
