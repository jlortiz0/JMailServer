import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.IOException;
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
    private SimpleDateFormat sdf = new SimpleDateFormat("[HH:mm:ss] ");
    public LogHandler() {
        super();
        try {
            logFile = new BufferedWriter(new FileWriter(System.getProperty("user.home")+"\\Documents\\JMail\\log.log"));
        } catch (IOException e) {
            reportError(null, e, 4);
        }
    }
    
    @Override
    public void close() {
        try {
            logFile.close();
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
            if (record.getLevel()!=java.util.logging.Level.INFO)
                logFile.write(record.getLevel().getName()+": ");
            logFile.write(s);
            this.flush();
            logFile.newLine();
        } catch (IOException e) {
            reportError(null, e, 1);
        }
    }
}
