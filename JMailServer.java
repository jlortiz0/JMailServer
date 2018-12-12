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
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

/**
 *
 * @author jlortiz
 */
public class JMailServer {
    private static Map<String,Object> options;
    private static final Yaml yml = new Yaml();
    
    public static void main(String[] args) throws IOException {
        blake.hash("");
        if (!new File(System.getProperty("user.home")+"\\Documents\\JMail\\config.yml").exists()) {
            new File(System.getProperty("user.home")+"\\Documents\\JMail").mkdir();
            new File(System.getProperty("user.home")+"\\Documents\\JMail\\users").mkdir();
            new File(System.getProperty("user.home")+"\\Documents\\JMail\\config.yml").createNewFile();
            copy(JMailServer.class.getClassLoader().getResourceAsStream("config.yml"), new FileOutputStream(System.getProperty("user.home")+"\\Documents\\JMail\\config.yml"));
            System.out.println("The config file was created! Please modify it, then run the server again.");
            return;
        }
        reloadCfg();
        ServerDaemon dmon = new ServerDaemon((Integer)get("port"));
        new ServerConsole().start();
        dmon.run();
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
        }
    }
    public static void reloadCfg() throws IOException {
        options = yml.load(new FileInputStream(new File(System.getProperty("user.home")+"\\Documents\\JMail\\config.yml")));
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