package core;

import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Watcher implements Runnable {
    private List<Path> oldFiles = Collections.emptyList();
    private static final String PATH_TO_FFMPEG = "F:/ffmpeg/bin/ffmpeg.exe";
    private static final String PATH_TO_IMAGES = "F:/timestamp";
    private static final String PATH_TO_TEMP = "F:/timestamp/temp";
    private static final String OUTPUT_FILE_PATH = "F:/timestamp/Video/out.mp4";
    private int frameRate = 15;

    public void copyAndRenameFiles(List<Path> filesPaths) {
        try {
            FileUtils.forceMkdir(new File(PATH_TO_TEMP));
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (int i = 0; i < filesPaths.size(); i++) {
            try {
                FileUtils.copyFile(filesPaths.get(i).toFile(), new File(PATH_TO_TEMP + '/' + String.format("%03d",i) + ".jpg"));
            } catch (IOException ignored) {
            }
        }
    }

    private String getProcessOutput(Process proc) throws InterruptedException, IOException {
        StringBuilder strBuild = new StringBuilder();
        try (BufferedReader processOutputReader = new BufferedReader(new InputStreamReader(proc.getInputStream(), Charset.defaultCharset()));) {
            String line;
            while ((line = processOutputReader.readLine()) != null) {
                strBuild.append(line + System.lineSeparator());
            }
            proc.waitFor();
        }
        return strBuild.toString().trim();
    }

    private void makeVideo(List<Path> imgLst) {
        FileUtils.deleteQuietly(new File(PATH_TO_TEMP));
        copyAndRenameFiles(imgLst);
        try {
            FileUtils.deleteQuietly(new File(OUTPUT_FILE_PATH));
            Process proc = new ProcessBuilder(PATH_TO_FFMPEG, "-framerate", Integer.toString(frameRate), "-i", PATH_TO_TEMP + "/\"%03d.jpg\"", "-c:v", "libx264", "-pix_fmt", "yuv420p", OUTPUT_FILE_PATH).redirectErrorStream(true).start();
            System.out.println(getProcessOutput(proc));
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }


        private List<Path> getImagesPaths (Path directoryPath){
            try (Stream<Path> paths = Files.walk(Paths.get(directoryPath.toString() + '/' + getCurrentYYYYMMDD()))) {
                return paths.filter(Files::isRegularFile).filter(path -> {
                    try {
                        ImageScanner.ScannerResult scannerResult = ImageScanner.scan(path);
                        return scannerResult.isOk();
                    } catch (IOException e) {
                        return false;
                    }
                }).collect(Collectors.toList());
            } catch (IOException e) {
                return Collections.emptyList();
            }
        }

        private String getCurrentYYYYMMDD () {
            LocalDate localDate = LocalDate.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            return localDate.format(formatter);
        }

        @Override
        public void run () {
            System.out.println("Looking for new files...");
            List<Path> newFiles = getImagesPaths(Path.of(PATH_TO_IMAGES));
            if (oldFiles.size() != newFiles.size() || !oldFiles.equals(newFiles)) {
                System.out.println("New files are found!");
                makeVideo(newFiles);
            }
            oldFiles = newFiles;
        }
    }
