package FileWrite;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class FileSystemWriter {
	private static final String DATE_FORMAT = "[yyyy][MM][dd]";
    private static final String TIMESTAMP_FORMAT = "HHmmss";
    private static final String LOG_TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss";
    
    private static final ReadWriteLock lock = new ReentrantReadWriteLock();

    public static void writeLog(String logType, String log, String directoryPath, long maxSizeBytes, int timezoneOffsetHours) {
        File latestFile = null;

        try {
            File directory = new File(directoryPath);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            latestFile = getLatestLogFile(directory, logType);
            String todayDate = getFormattedDate(DATE_FORMAT, timezoneOffsetHours);

            if (latestFile == null || latestFile.length() >= maxSizeBytes || !latestFile.getName().contains(todayDate)) {
                lock.writeLock().lock();
                try {
                    latestFile = createNewLogFile(directoryPath, logType, timezoneOffsetHours);
                } finally {
                    lock.writeLock().unlock();
                }
            }

            String timestamp = getFormattedDate(LOG_TIMESTAMP_FORMAT, timezoneOffsetHours);
            String logWithTimestamp = timestamp + System.lineSeparator() + log;

            try (FileWriter writer = new FileWriter(latestFile, true)) {
                writer.write(logWithTimestamp + System.lineSeparator());
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static File getLatestLogFile(File directory, String logType) {
        File[] files = directory.listFiles((dir, name) -> name.startsWith(logType));
        if (files == null || files.length == 0) {
            return null;
        }

        return Arrays.stream(files)
                .filter(File::isFile)
                .max(Comparator.comparingLong(File::lastModified))
                .orElse(null);
    }

    private static File createNewLogFile(String directoryPath, String logType, int timezoneOffsetHours) throws IOException {
        String date = getFormattedDate(DATE_FORMAT, timezoneOffsetHours);
        String timeStamp = getFormattedDate(TIMESTAMP_FORMAT, timezoneOffsetHours);
        String newFileName = logType + "_" + date + "_" + timeStamp + ".txt";

        Path newFilePath = Paths.get(directoryPath, newFileName);
        
        if (!Files.exists(newFilePath)) {
            Files.createFile(newFilePath);
        }

        return newFilePath.toFile();
    }

    private static String getFormattedDate(String format, int timezoneOffsetHours) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(format);
        TimeZone timeZone = TimeZone.getTimeZone("UTC");
        timeZone.setRawOffset(timezoneOffsetHours * 3600 * 1000);
        dateFormat.setTimeZone(timeZone);
        return dateFormat.format(new Date());
    }

}
