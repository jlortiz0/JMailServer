package org.jlortiz;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

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
    public final String newFileName;
    public LogHandler() {
        super();
        newFileName = System.getProperty("user.home")+"\\Documents\\JMail\\logs\\"+new SimpleDateFormat("yyyy-MM-dd-kk-mm-ss").format(new Date())+".log";
        if (JMailServer.log.getLevel()==Level.OFF)
            return;
        try {
            logFile = new BufferedWriter(new FileWriter(System.getProperty("user.home")+"\\Documents\\JMail\\logs\\latest.log"));
        } catch (IOException e) {
            reportError(null, e, 4);
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.DAY_OF_YEAR, 0-(Integer)JMailServer.get("logkeep"));
        for (File f: new File(System.getProperty("user.home")+"\\Documents\\JMail\\logs\\").listFiles())
            try {
                if (cal.getTime().after(sdf.parse(f.getName())))
                    f.delete();
            } catch (ParseException e) {}
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
        try {
            if (record.getThrown()!=null) {
                logFile.write(sdf.format(new Date()));
                logFile.write(record.getThrown().toString());
                logFile.newLine();
            }
            logFile.write(sdf.format(new Date()));
            if (record.getLevel().intValue() > Level.INFO.intValue())
                logFile.write(record.getLevel().getName()+": ");
            logFile.write(record.getMessage());
            this.flush();
            logFile.newLine();
        } catch (IOException e) {
            reportError(null, e, 1);
        }
    }
}
