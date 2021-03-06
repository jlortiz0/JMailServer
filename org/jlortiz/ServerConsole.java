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
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import static org.jlortiz.JMailServer.log;

/**
 * 
 * @author jlortiz
 */
public class ServerConsole extends Thread
{
    @Override
    public void run() {
        Scanner sc = new Scanner(System.in);
        Scanner sl;
        boolean locked = !(((String)JMailServer.get("adminpass")).isEmpty());
        String user;
        List<String> blist;
        while (true) {
            if (locked) {
                clearScreen();
                String pass;
                if (System.console()==null) {
                    System.out.print("Terminal is locked\nEnter password: ");
                    pass = sc.nextLine();
                } else {
                    pass = new String(System.console().readPassword("Terminal is locked\nEnter password: "));
                    sc.skip(".*");
                }
                if (pass.equals(JMailServer.get("adminpass")))
                    locked=false;
                else 
                    log.severe("Incorrect admin password given!");
                continue;
            }
            System.out.print("> ");
            sl = new Scanner(sc.nextLine());
            if (!sl.hasNext())
                continue;
            switch (sl.next()) {
                case "lock":
                    if (!(((String)JMailServer.get("adminpass")).isEmpty()))
                        locked=true;
                    break;
                case "stop":
                case "exit":
                    try {
                        ServerDaemon.stop();
                    } catch (IOException e) {
                        System.out.println("FATAL: The server will not stop! Will now crash!");
                        log.severe(e.toString());
                        log.severe("The server produced an exception while stopping! Will now crash.");
                        System.exit(1);
                    }
                    return;
                case "connected":
                    ServerDaemon.getSockets().keySet().forEach((s) -> {
                        System.out.print(s+" ");
                    });
                    System.out.println();
                    break;
                case "ble":
                    System.out.println("Blacklist is now "+String.valueOf(!(Boolean)JMailServer.get("useblist")));
                    JMailServer.set("useblist", !(Boolean)JMailServer.get("useblist"));
                    log.log(Level.INFO, "Blacklist {0}", (Boolean)JMailServer.get("useblist") ? "enabled" : "disabled");
                    break;
                case "wle":
                    System.out.println("Whitelist is now "+String.valueOf(!(Boolean)JMailServer.get("usewlist")));
                    JMailServer.set("usewlist", !(Boolean)JMailServer.get("usewlist"));
                    log.log(Level.INFO, "Whitelist {0}", (Boolean)JMailServer.get("useblist") ? "enabled" : "disabled");
                    break;
                case "cls":
                case "clear":
                    clearScreen();
                    break;
                case "rlcfg":
                    try {
                        JMailServer.reloadCfg();
                        System.out.println("Reloaded the config file. Note that not all changes will take place immediately.");
                    } catch (IOException e) {
                        log.severe("Reloading config file FAILED! Server is unstable!");
                        System.out.println("SEVERE: Reloading config failed! Server is now unstable!");
                    }
                    break;
                case "locfg":
                    System.out.println(System.getProperty("user.home")+"\\Documents\\JMail\\config.yml");
                    break;
                case "savecfg":
                    try {
                        JMailServer.flush();
                        System.out.println("Current config has been flushed to disk.");
                    } catch (IOException e) {
                        log.severe("Saving config file failed! Corruption possible.");
                        System.out.println("ERROR: Saving config failed! Please check the config file for corruption.");
                        break;
                    }
                    System.out.println("Updated config file was saved to disk.");
                    break;
                case "bl":
                    if (sl.hasNext()) {
                        user=sl.next();
                        blist = (List<String>)JMailServer.get("blist");
                        if (blist.contains(user)) {
                            blist.remove(user);
                            System.out.println(user+" is no longer blacklisted.");
                        } else {
                            blist.add(user);
                            System.out.println(user+" is now blacklisted.");
                        }
                        JMailServer.set("blist", blist);
                    } else {
                        ((List<String>)JMailServer.get("blist")).forEach((s) -> {
                            System.out.println(s);
                        });
                    }
                    break;
                case "wl":
                    if (sl.hasNext()) {
                        user=sl.next();
                        blist = (List<String>)JMailServer.get("wlist");
                        if (blist.contains(user)) {
                            blist.remove(user);
                            System.out.println(user+" is no longer whitelisted.");
                        } else {
                            blist.add(user);
                            System.out.println(user+" is now whitelisted.");
                        }
                        JMailServer.set("wlist", blist);
                    } else {
                        ((List<String>)JMailServer.get("wlist")).forEach((s) -> {
                            System.out.println(s);
                        });
                    }
                    break;
                case "dc":
                    if (sl.hasNext()) {
                        user = sl.next();
                        if (ServerDaemon.getSockets().containsKey(user)) {
                            ServerDaemon.getSockets().get(user).close(true);
                            ServerDaemon.getSockets().remove(user);
                        } else {
                            System.out.println("This IP is not connected!");
                        }
                    } else {
                        System.out.println("Usage: dc <ip>");
                    }
                    break;
                default:
                    System.out.println("Unknown command!");
            }
        }
    }
    public static void clearScreen() {
        try {
            if (System.getProperty("os.name").contains("Windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                new ProcessBuilder("clear").inheritIO().start().waitFor();
            }
        } catch (IOException | InterruptedException e) {
            log.warning(e.toString());
            log.warning("Failed to clear screen!");
        }
    }
}
