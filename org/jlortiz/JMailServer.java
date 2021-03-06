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
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.yaml.snakeyaml.Yaml;

/**
 *
 * @author jlortiz
 */
public class JMailServer {
    private static Map<String,Object> options;
    private static final Yaml yml = new Yaml();
    public static final Logger log = Logger.getLogger("JMailServer");
    
    public static void main(String[] args) throws IOException {
        Blake.hash("");
        if (!new File(System.getProperty("user.home")+"\\Documents\\JMail\\config.yml").exists()) {
            new File(System.getProperty("user.home")+"\\Documents\\JMail").mkdir();
            new File(System.getProperty("user.home")+"\\Documents\\JMail\\logs").mkdir();
            new File(System.getProperty("user.home")+"\\Documents\\JMail\\users").mkdir();
            new File(System.getProperty("user.home")+"\\Documents\\JMail\\config.yml").createNewFile();
            copy(JMailServer.class.getClassLoader().getResourceAsStream("config.yml"), new FileOutputStream(System.getProperty("user.home")+"\\Documents\\JMail\\config.yml"));
            System.out.println("The config file was created! Please modify it, then run the server again.");
            return;
        }
        reloadCfg();
        log.setLevel(Level.parse((String)get("loglevel")));
        LogHandler h = new LogHandler();
        log.addHandler(h);
        new ServerConsole().start();
        new ServerDaemon((Integer)get("port")).run();
        log.info("Server shutting down.");
        h.close();
        new File(System.getProperty("user.home")+"\\Documents\\JMail\\logs\\latest.log").renameTo(new File(h.newFileName));
    }
    public static Object get(String key) {
        return options.get(key);
    }
    public static void set(String key, Object value) {
        options.put(key, value);
    }
    public static void flush() throws IOException {
        try (FileWriter write = new FileWriter(System.getProperty("user.home")+"\\Documents\\JMail\\config.yml")) {
            yml.dump(options, write);
            log.info("Config file flushed to disk.");
        }
    }
    public static void reloadCfg() throws IOException {
        options = yml.load(new FileInputStream(new File(System.getProperty("user.home")+"\\Documents\\JMail\\config.yml")));
        if ((Boolean)get("useblist") && ((List)get("blist")).isEmpty())
            set("useblist", false);
        if ((Boolean)get("usewlist") && ((List)get("wlist")).isEmpty())
            set("usewlist", false);
        log.info("Config file reloaded.");
    }
    private static void copy(InputStream in, FileOutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        while (true) {
            int bytesRead = in.read(buffer);
            if (bytesRead == -1)
                break;
            out.write(buffer, 0, bytesRead);
        }
    }
}
