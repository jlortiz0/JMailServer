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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.*;
/**
 *
 * @author jlortiz
 */
public class SendMail {
    public static String send(String usr, String s) {
        long unixTime = Instant.now().getEpochSecond();
        try {
            String from = usr+"@"+InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {}
        Scanner sc = new Scanner(s);
        String recpts[];
        try (Scanner sf = new Scanner(sc.nextLine())) {
            ArrayList<String> ls = new ArrayList<>(15);
            while (sf.hasNext()) {
                ls.add(sf.next());
            }
            recpts = (String[])ls.toArray();
        }
        sc.useDelimiter("\\A");
        String subject = sc.nextLine();
        String message = sc.next();
        HashMap<String,ArrayList<String>> servers = new HashMap<>();
        for (String a: recpts) {
            if (!servers.containsKey(a.split("@")[1]))
                servers.put(a.split("@")[1], new ArrayList<String>());
            servers.get(a.split("@")[1]).add(a.split("@")[0]);
        }
        return "true";
    }
    public static String get(InetAddress addr, String s) {
        if (!(Boolean)JMailServer.get("getmailremote")) {
            return "getmail";
        }
        Scanner sc = new Scanner(s);
        sc.useDelimiter("\\A");
        String from = sc.nextLine();
        long date = sc.nextLong();
        String to = sc.nextLine();
        String recpts[];
        try (Scanner sf = new Scanner(sc.nextLine())) {
            ArrayList<String> ls = new ArrayList<>(15);
            while (sf.hasNext()) {
                ls.add(sf.next());
            }
            recpts = (String[])ls.toArray();
        }
        String subject = sc.nextLine();
        String message = sc.next();
        if (!(new File(System.getProperty("user.home")+"\\Documents\\JMail\\users\\"+to.substring(0, to.indexOf("@")-1)).isDirectory())) {
            return "user";
        }
        if (new File(System.getProperty("user.home")+"\\Documents\\JMail\\users\\"+to.substring(0, to.indexOf("@")-1)+"\\"+subject+" from "+from).exists()) {
        return "exists";
        }
        try (FileOutputStream f = new FileOutputStream(System.getProperty("user.home")+"\\Documents\\JMail\\users\\"+to.substring(0, to.indexOf("@")-1)+"\\"+subject+" from "+from)) {
            f.write(("Date: "+Date.from(Instant.ofEpochSecond(date))).getBytes());
            f.write(("From: "+from).getBytes());
            f.write(("To: "+Arrays.toString(recpts)).getBytes());
            f.write(("Subject: "+subject).getBytes());
            f.write(("\n\n"+message).getBytes());
            f.flush();
        } catch (IOException e) {
            return "write";
        }
        return "true";
    }
}
