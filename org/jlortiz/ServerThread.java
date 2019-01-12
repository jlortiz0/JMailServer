package org.jlortiz;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import static org.jlortiz.JMailServer.log;

/**
 * Write a description of class Client here.
 * 
 * @author (your name) 
 * @version (a version number or a date)
 */
public class ServerThread extends Thread
{
    protected final Socket socket;
    protected final DataInputStream input;
    protected final DataOutputStream output;
    private int nonce;
    private String cUser;

    private static HashMap<String,HashMap> tree(File fdir) {
        HashMap<String,HashMap> ls = new HashMap<>(fdir.list().length);
        for (File f: fdir.listFiles())
            if (f.isDirectory())
                ls.put(f.getName(), tree(f));
        return ls;
    }
    private static String serialize(Serializable o) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(o);
        }
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }
    private static void delDir(File dir) {
        if (!dir.exists())
            return;
        if (dir.isFile()) {
            dir.delete();
            return;
        }
        for (File f: dir.listFiles()) {
            if (f.isDirectory()) {
                delDir(f);
            } else {
                f.delete();
            }
        }
        dir.delete();
    }
    
    public ServerThread(Socket socket) throws UnknownHostException, IOException {
        this.socket=socket;
        output = new DataOutputStream(socket.getOutputStream());
        input = new DataInputStream(socket.getInputStream());
        log.log(Level.FINE, "Opened {0} connected to {1} / {2}", new String[]{this.getClass().getName(), socket.getInetAddress().getHostName(), socket.getInetAddress().getHostAddress()});
    }
    public void close() {
        ServerDaemon.remove(this.socket.getInetAddress().getHostAddress());
        try {
            output.close();
            input.close();
            socket.close();
        } catch (IOException e) {
            log.warning(e.toString());
            log.log(Level.WARNING, "Error on closing ServerThread connected to {0} / {1}", new String[]{socket.getInetAddress().getHostName(), socket.getInetAddress().getHostAddress()});
        }
    }
    //TODO: There's probably a better way to log exceptions than this.
    protected void send(String msg) {
        if (socket.isClosed())
            return;
        try {
            output.writeUTF(msg);
        } catch (IOException e) {
            log.warning(e.toString());
            log.log(Level.WARNING, "Error sending data in {0} connected to {1} / {2}", new String[]{this.getClass().getName(), socket.getInetAddress().getHostName(), socket.getInetAddress().getHostAddress()});
            close();
        }
    }
    protected String receive() {
        try {
            return input.readUTF();
        } catch (IOException e) {
            log.warning(e.toString());
            log.log(Level.WARNING, "Error getting data in {0} connected to {1} / {2}", new String[]{this.getClass().getName(), socket.getInetAddress().getHostName(), socket.getInetAddress().getHostAddress()});
        }
        return "QUIT";
    }
    protected void flush() {
        if (socket.isClosed())
            return;
        try {
            output.flush();
        } catch (IOException e) {
            log.warning(e.toString());
            log.log(Level.WARNING, "Error flushing data in {0} connected to {1} / {2}", new String[]{this.getClass().getName(), socket.getInetAddress().getHostName(), socket.getInetAddress().getHostAddress()});
            close();
        }
    }
    
    @Override
    public void run() {
        Scanner sc;
        boolean loggedIn = false;
        int rega = 0;
        int logina = 0;
        String usr;
        String pass;
        String filename;
        send("OK connect");
        while (!socket.isClosed()) {
            sc = new Scanner(receive());
            if (loggedIn) {
                switch (sc.next()) {
                    case "QUIT":
                        close();
                        return;
                    case "VRFY":
                        usr = sc.next();
                        if (!(usr.contains("\\") || usr.contains("/") || usr.contains(".."))) {
                            if (new File(cUser+"\\..\\"+usr).isDirectory()) {
                                send("true");
                                break;
                            }
                        }
                        send("false");
                        break;
                    case "DEL":
                        filename = sc.nextLine().substring(1);
                        if (filename.equals("..")) {
                            delDir(new File(cUser));
                            close();
                            return;
                        }
                        if (!filename.contains(".."))
                            delDir(new File(cUser+"\\mail\\"+filename));
                        break;
                    case "MV":
                        filename = sc.next();
                        pass = sc.next();
                        if (filename.contains("..") || pass.contains(".."))
                            break;
                        try {
                            Files.move(Paths.get(cUser+"\\mail\\"+filename), Paths.get(cUser+"\\mail\\"+pass));
                        } catch (IOException e) {
                            log.warning(e.toString());
                            log.log(Level.WARNING, "Failed to move {0}\\mail\\{1} to {2}\\mail\\{3}", new String[]{cUser, filename, cUser, pass});
                        }
                        break;
                    case "TREE":
                        try {
                            send(serialize(tree(new File(cUser+"\\mail\\"))));
                        } catch (IOException e) {
                            send("false");
                            log.warning(e.toString());
                            log.log(Level.WARNING, "Failed to make a tree in folder {0}\\mail\\", cUser);
                        }
                        break;
                    case "GET":
                        filename = sc.nextLine().substring(1);
                        if (filename.contains("..")) {
                            send("false");
                            break;
                        }
                        if (!(new File(cUser+"\\mail\\"+filename).exists())) {
                            send("false");
                            break;
                        } else if (new File(cUser+"\\mail\\"+filename).isDirectory()) {
                            pass="";
                            for (File f: new File(cUser+"\\mail\\"+filename).listFiles()) {
                                if (f.isFile())
                                    pass+=f.getName()+"\n";
                            }
                            send(pass);
                            break;
                        } else {
                            try (Scanner fil = new Scanner(new File(cUser+"\\mail\\"+filename))) {
                                fil.useDelimiter("\\A");
                                send(fil.next());
                                fil.close();
                            } catch (FileNotFoundException e) {
                                send("false");
                            }
                        }
                        break;
                    case "PC":
                        pass = sc.next();
                        filename = sc.next();
                        try (Scanner file = new Scanner(new File(cUser+"\\pass.txt"))) {
                            if (!pass.equals(file.next())) {
                                send("false");
                                SendMail.send("SERVER", new File(cUser).getName()+"@localhost\nPasschange FAILURE\nA computer at IP address "+socket.getInetAddress().getHostAddress()+" just tried to change your password. If this was not you, contact us immediately.");
                                break;
                            }
                        } catch (FileNotFoundException e) {
                            log.log(Level.WARNING, "{0} password file was missing!", new File(cUser).getName());
                        }
                        try (FileOutputStream out = new FileOutputStream(new File(cUser+"\\pass.txt"), false)) {
                            out.write(filename.getBytes());
                        } catch (IOException e) {
                            log.warning(e.toString());
                            log.log(Level.WARNING, "Failed to change {0} password.", new File(cUser).getName());
                            send("QUIT There was an unexpected error.\nYour password may not have been changed.\nIf you are unable to log in, please contact an admin.");
                            close();
                            return;
                        }
                        send("true");
                        SendMail.send("SERVER", new File(cUser).getName()+"@localhost\nPassword Changed\nYour password was just changed by a computer at IP address "+socket.getInetAddress().getHostAddress()+". If this was not you, contact us immediately.");
                        break;
                    case "NEW":
                        pass=sc.next();
                        try {
                            Files.createDirectories(Paths.get(cUser+"\\mail\\"+pass));
                        } catch (IOException e) {
                            log.warning(e.toString());
                            log.log(Level.WARNING, "Failed to create directory {0}", pass);
                        }
                        break;
                    case "DATA":
                        if (!(Boolean)JMailServer.get("sendmailremote")) {
                            send("sendmail");
                            break;
                        }
                        sc.useDelimiter("\\A");
                        send(SendMail.send(new File(cUser).getName(), sc.next()));
                        break;
                    default:
                        send("false");
                        break;
                    }
            } else {
                switch (sc.next()) {
                    case "QUIT":
                        close();
                        return;
                    case "REGA":
                        send(String.valueOf(JMailServer.get("allownewuser")));
                        break;
                    case "NONCE":
                        do {
                            nonce = new Random().nextInt();
                        } while(nonce==0);
                        send(String.valueOf(nonce));
                        break;
                    case "VRFY":
                        usr = sc.next();
                        if (!(usr.contains("\\") || usr.contains("/") || usr.contains(".."))) {
                            if (new File(System.getProperty("user.home")+"\\Documents\\JMail\\users\\"+usr).isDirectory()) {
                                send("true");
                                break;
                            }
                        }
                        send("false");
                        break;
                    case "REG":
                        if (!(Boolean)JMailServer.get("allownewuser") || (new File(System.getProperty("user.home")+"\\Documents\\JMail\\users\\").list().length>(Integer)JMailServer.get("maxusers")) && (Integer)JMailServer.get("maxusers") > 1) {
                            rega++;
                            if (rega>(Integer)JMailServer.get("rega") && (Integer)JMailServer.get("rega")>1) {
                                send("QUIT Too many registration attempts!");
                                close();
                                return;
                            }
                            send("false");
                            break;
                        }
                        usr = sc.next();
                        if (usr.contains("\\") || usr.contains("/") || usr.contains("..") || ((List)JMailServer.get("bannedun")).contains(usr.toLowerCase()) || new File(System.getProperty("user.home")+"\\Documents\\JMail\\users\\"+usr).isDirectory()) {
                            rega++;
                            if (rega>(Integer)JMailServer.get("rega") && (Integer)JMailServer.get("rega")>1) {
                                send("QUIT Too many registration attempts!");
                                close();
                                return;
                            }
                            send("false");
                            break;
                        }
                        pass = sc.next();
                        try {
                            new File(System.getProperty("user.home")+"\\Documents\\JMail\\users\\"+usr).mkdir();
                            new File(System.getProperty("user.home")+"\\Documents\\JMail\\users\\"+usr+"\\pass.txt").createNewFile();
                            try (PrintWriter out = new PrintWriter(System.getProperty("user.home")+"\\Documents\\JMail\\users\\"+usr+"\\pass.txt")) {
                                out.print(pass);
                            }
                            new File(System.getProperty("user.home")+"\\Documents\\JMail\\users\\"+usr+"\\mail").mkdir();
                            SendMail.send("SERVER", usr+"@localhost\nWelcome to "+InetAddress.getLocalHost().getHostName()+"\n"+JMailServer.get("welcomemsg"));
                        } catch (IOException e) {
                            System.out.println(e);
                            send("false");
                            new File(System.getProperty("user.home")+"\\Documents\\JMail\\users\\"+usr).delete();
                        }
                        send("true");
                        break;
                    case "AUTH":
                        if (nonce==0) {
                            logina++;
                            if (logina>(Integer)JMailServer.get("logina") && (Integer)JMailServer.get("logina")>1) {
                                send("QUIT Too many login attempts!");
                                close();
                                return;
                            }
                            send("false");
                            break;
                        }
                        usr = sc.next();
                        if (usr.contains("\\") || usr.contains("/") || usr.contains("..") || !(new File(System.getProperty("user.home")+"\\Documents\\JMail\\users\\"+usr).isDirectory())) {
                            logina++;
                            if (logina>(Integer)JMailServer.get("logina") && (Integer)JMailServer.get("logina")>1) {
                                send("QUIT Too many login attempts!");
                                close();
                                return;
                            }
                            send("false");
                            break;
                        }
                        pass = sc.next();
                        try (Scanner sf = new Scanner(new File(System.getProperty("user.home")+"\\Documents\\JMail\\users\\"+usr+"\\pass.txt"))) {
                            if (!Blake.hash(sf.nextLine(), String.valueOf(this.nonce)).equals(pass)) {
                                sf.close();
                                logina++;
                                if (logina>(Integer)JMailServer.get("logina") && (Integer)JMailServer.get("logina")>1) {
                                    send("QUIT Too many login attempts!");
                                    close();
                                    return;
                                }
                                send("false");
                                break;
                            }
                        } catch (FileNotFoundException e) {
                            logina++;
                            if (logina>(Integer)JMailServer.get("logina") && (Integer)JMailServer.get("logina")>1) {
                                send("QUIT Too many login attempts!");
                                close();
                                return;
                            }
                            send("false");
                            break;
                        }
                        this.cUser=System.getProperty("user.home")+"\\Documents\\JMail\\users\\"+usr;
                        send("true");
                        loggedIn=true;
                        break;
                    case "HOST":
                        try {
                            send(InetAddress.getLocalHost().getHostName());
                        } catch (UnknownHostException e) {
                            log.warning("The server was unable to find itself on the network!");
                            send("Unknown JMailServer");
                        }
                        break;
                    case "SOFT":
                        send("JMailServer Java 1.2 by jlortiz");
                        break;
                    default:
                        send("false");
                        break;
                }
            }
            flush();
        }
    }
}
