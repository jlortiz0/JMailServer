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
 * 
 * @author jlortiz
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
                if (cal.getTime().after(new SimpleDateFormat("yyyy-MM-dd-kk-mm-ss").parse(f.getName().substring(0, f.getName().length()-4)))) {
                    f.delete();
                    publish(new LogRecord(Level.INFO, "Deleted old log file "+f.getName()));
                }
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
