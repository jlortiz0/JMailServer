import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Write a description of class LogHandler here.
 * 
 * @author (your name) 
 * @version (a version number or a date)
 */
public class LogHandler extends Handler
{
    private BufferedWriter logFile;
    private static final SimpleDateFormat sdf = new SimpleDateFormat("[HH:mm:ss] ");
    private String newFileName;
    public LogHandler() {
        super();
        try {
            newFileName = System.getProperty("user.home")+"\\Documents\\JMail\\logs\\"+new SimpleDateFormat("yyyy-MM-dd-kk-mm-ss").format(new Date())+".log";
            logFile = new BufferedWriter(new FileWriter(System.getProperty("user.home")+"\\Documents\\JMail\\logs\\latest.log"));
        } catch (IOException e) {
            reportError(null, e, 4);
        }
    }
    
    @Override
    public void close() {
        try {
            logFile.close();
            new File(System.getProperty("user.home")+"\\Documents\\JMail\\logs\\latest.log").renameTo(new File(newFileName));
        } catch (IOException e) {
            reportError(null, e, 3);
        }
    }
    @Override
    public void flush() {
        try {
            logFile.flush();
        } catch (IOException e) {
            reportError(null, e, 2);
        }
    }
    @Override
    public void publish(LogRecord record) {
        String s = record.getMessage();
        if (s.isEmpty())
            return;
        try {
            logFile.write(sdf.format(new Date()));
            if (record.getLevel().intValue() >  800)
                logFile.write(record.getLevel().getName()+": ");
            logFile.write(s);
            this.flush();
            logFile.newLine();
        } catch (IOException e) {
            reportError(null, e, 1);
        }
    }
}
