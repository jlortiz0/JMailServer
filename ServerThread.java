import java.io.*;
import java.net.Socket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

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
    
    private static HashMap tree(String dir) {
         return tree(new File(dir));
    }
    private static HashMap<String,HashMap> tree(File fdir) {
        HashMap<String,HashMap> ls = new HashMap<>(fdir.list().length);
        for (File f: fdir.listFiles())
            if (f.isDirectory())
                ls.put(f.getName(), tree(f));
        return ls;
    }
    private static String serialize(Serializable o) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(o);
        oos.close();
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
    }
    public void close() {
        ServerDaemon.remove(this.socket.getInetAddress().getHostAddress());
        try {
            output.close();
            input.close();
            socket.close();
        } catch (IOException e) {}
    }
    protected void send(String msg) {
        try {
            output.writeUTF(msg);
        } catch (IOException e) {
            System.out.println("Error sending data: "+e);
        }
    }
    protected String receive() {
        try {
            return input.readUTF();
        } catch (IOException e) {
            System.out.println("Error getting data: "+e);
        }
        return "";
    }
    protected void flush() {
        try {
            output.flush();
        } catch (IOException e) {
            System.out.println("Error flushing data: "+e);
        }
    }
    
    @Override
    public void run() {
        //TODO: Add exception handling so that it stops softlocking
        Scanner sc;
        boolean loggedIn = false;
        int rega = 0;
        int logina = 0;
        String usr;
        String pass;
        String filename;
        boolean b;
        Scanner sf;
        send("OK connect");
        while (true) {
            sc = new Scanner(receive());
            if (loggedIn) {
                switch (sc.next()) {
                    case "QUIT":
                        close();
                        return;
                    case "VRFY":
                        usr = sc.next();
                        b = false;
                        if (!(usr.contains("\\") || usr.contains("/") || usr.contains("..")))
                            if (new File(cUser+"\\..\\"+usr).isDirectory())
                                b=true;
                        send(String.valueOf(b));
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
                        if (filename.contains(".."))
                            break;
                        if (pass.contains(".."))
                            break;
                        try {
                            Files.move(Paths.get(cUser+"\\mail\\"+filename), Paths.get(cUser+"\\mail\\"+pass));
                        } catch (IOException e) {}
                        break;
                    case "TREE":
                        try {
                            send(serialize(tree(cUser+"\\mail\\")));
                        } catch (IOException e) {
                            send("false");
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
                            //Shame on this statement for requiring a new variable
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
                        //TODO: Send user email telling them of sucessful (or unsucessful) passchange
                        pass = sc.next();
                        filename = sc.next();
                        try (Scanner file = new Scanner(new File(cUser+"\\pass.txt"))) {
                            if (!pass.equals(file.next())) {
                                send("false");
                                break;
                            }
                        } catch (FileNotFoundException e) {
                            send("false");
                            break;
                        }
                        try (FileOutputStream out = new FileOutputStream(new File(cUser+"\\pass.txt"), false)) {
                            out.write(filename.getBytes());
                        } catch (IOException e) {
                            send("false");
                            break;
                        }
                        send("true");
                        break;
                    case "NEW":
                        try {
                            Files.createDirectories(Paths.get(cUser+"\\mail\\"+sc.next()));
                        } catch (IOException e) {}
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
                        b = false;
                        if (!(usr.contains("\\") || usr.contains("/") || usr.contains("..")))
                            if (new File(System.getProperty("user.home")+"\\Documents\\JMail\\users\\"+usr).isDirectory())
                                b=true;
                        send(String.valueOf(b));
                        break;
                    case "REG":
                        if (!(Boolean)JMailServer.get("allownewuser") || (new File(System.getProperty("user.home")+"\\Documents\\JMail\\users\\").list().length>(Integer)JMailServer.get("maxusers")) && (Integer)JMailServer.get("maxusers") > 1) {
                            rega++;
                            if (rega>(Integer)JMailServer.get("rega") && rega>1) {
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
                            if (rega>(Integer)JMailServer.get("rega") && rega>1) {
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
                            PrintWriter out = new PrintWriter(System.getProperty("user.home")+"\\Documents\\JMail\\users\\"+usr+"\\pass.txt");
                            out.print(pass);
                            out.close();
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
                            if (logina>(Integer)JMailServer.get("logina") && logina>1) {
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
                            if (logina>(Integer)JMailServer.get("logina") && logina>1) {
                                send("QUIT Too many login attempts!");
                                close();
                                return;
                            }
                            send("false");
                            break;
                        }
                        pass = sc.next();
                        try {
                            sf = new Scanner(new File(System.getProperty("user.home")+"\\Documents\\JMail\\users\\"+usr+"\\pass.txt"));
                        } catch (FileNotFoundException e) {
                            logina++;
                            if (logina>(Integer)JMailServer.get("logina") && logina>1) {
                                send("QUIT Too many login attempts!");
                                close();
                                return;
                            }
                            send("false");
                            break;
                        }
                        if (!Blake.hash(sf.nextLine(), String.valueOf(this.nonce)).equals(pass)) {
                            sf.close();
                            logina++;
                            if (logina>(Integer)JMailServer.get("logina") && logina>1) {
                                send("QUIT Too many login attempts!");
                                close();
                                return;
                            }
                            send("false");
                            break;
                        }
                        sf.close();
                        this.cUser=System.getProperty("user.home")+"\\Documents\\JMail\\users\\"+usr;
                        send("true");
                        loggedIn=true;
                        break;
                    case "HOST":
                        try {
                            send(InetAddress.getLocalHost().getHostName());
                        } catch (UnknownHostException e) {
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
