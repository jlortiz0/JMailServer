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
        } catch (IOException e) {
            JMailServer.log.warning(e.toString());
            JMailServer.log.warning("Error on closing ServerThread connected to "+socket.getInetAddress().getHostName()+" / "+socket.getInetAddress().getHostAddress());
        }
    }
    //TODO: What should be done if there is an exception here?
    protected void send(String msg) {
        try {
            output.writeUTF(msg);
        } catch (IOException e) {
            System.out.println("Error sending data: "+e);
            JMailServer.log.warning(e.toString());
            JMailServer.log.warning("Error sending data in "+this.getClass().getName()+" connected to "+socket.getInetAddress().getHostName()+" / "+socket.getInetAddress().getHostAddress());
        }
    }
    protected String receive() {
        try {
            return input.readUTF();
        } catch (IOException e) {
            System.out.println("Error getting data: "+e);
            JMailServer.log.warning(e.toString());
            JMailServer.log.warning("Error getting data in "+this.getClass().getName()+" connected to "+socket.getInetAddress().getHostName()+" / "+socket.getInetAddress().getHostAddress());
        }
        return "ERROR";
    }
    protected void flush() {
        try {
            output.flush();
        } catch (IOException e) {
            System.out.println("Error flushing data: "+e);
            JMailServer.log.warning(e.toString());
            JMailServer.log.warning("Error flushing data in "+this.getClass().getName()+" connected to "+socket.getInetAddress().getHostName()+" / "+socket.getInetAddress().getHostAddress());
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
                            //TODO: Log this
                        }
                        break;
                    case "TREE":
                        try {
                            send(serialize(tree(new File(cUser+"\\mail\\"))));
                        } catch (IOException e) {
                            send("false");
                            //TODO: Log this
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
                        //TODO: Send user email telling them of sucessful (or unsucessful) passchange
                        pass = sc.next();
                        filename = sc.next();
                        try (Scanner file = new Scanner(new File(cUser+"\\pass.txt"))) {
                            if (!pass.equals(file.next())) {
                                send("false");
                                SendMail.send("SERVER", new File(cUser).getName()+"@localhost\nPasschange FAILURE\nA computer at IP address "+socket.getInetAddress().getHostAddress()+" just tried to change your password. If this was not you, contact us immediately.");
                                break;
                            }
                        } catch (FileNotFoundException e) {
                            //TODO: This may be fatal
                            send("false");
                            break;
                        }
                        try (FileOutputStream out = new FileOutputStream(new File(cUser+"\\pass.txt"), false)) {
                            out.write(filename.getBytes());
                        } catch (IOException e) {
                            ///TODO: Log this. Also, if the file was opened, this may have destroyed some data.
                            send("false");
                            break;
                        }
                        send("true");
                        SendMail.send("SERVER", new File(cUser).getName()+"@localhost\nPassword Changed\nYour password was just changed by a computer at IP address "+socket.getInetAddress().getHostAddress()+". If this was not you, contact us immediately.");
                        break;
                    case "NEW":
                        try {
                            Files.createDirectories(Paths.get(cUser+"\\mail\\"+sc.next()));
                        } catch (IOException e) {
                            //TODO: Log this
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
                            //TODO: This may be fatal
                            send("Unknown JMailServer");
                        }
                        break;
                    case "SOFT":
                        //TODO: Make this configurable
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
