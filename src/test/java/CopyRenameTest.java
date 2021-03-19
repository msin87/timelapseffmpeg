import core.ImageScanner;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CopyRenameTest {
    private static final String PATH_TO_IMAGES = "F:/timestamp";
    private static final String PATH_TO_TEMP = "F:/timestamp/temp";

    private String getCurrentYYYYMMDD() {
        LocalDate localDate = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return localDate.format(formatter);
    }

    private List<Path> getImagesPaths(Path directoryPath) {
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


    public void copyAndRenameFiles(List<Path> filesPaths){
        try {
            FileUtils.forceMkdir(new File(PATH_TO_TEMP));
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (int i = 0; i < filesPaths.size(); i++) {
            try {
                String fileName = PATH_TO_TEMP + '/' + String.format("%03d",i);

                FileUtils.copyFile(filesPaths.get(i).toFile(), new File(fileName + ".jpg"));
            } catch (IOException ignored) {
            }
        }
    }

    @Test
    void copyAndRenameTest() throws IOException {
        FileUtils.deleteDirectory(new File(PATH_TO_TEMP));
        List<Path> files = getImagesPaths(Path.of(PATH_TO_IMAGES));
        copyAndRenameFiles(files);
    }
    @Test
    void makeVideo() {
        String PATH_TO_FFMPEG = "F:/ffmpeg/bin/ffmpeg.exe";
        String OUTPUT_FILE_PATH = "F:/timestamp/Video/out.mp4";
        String CMD = " -framerate 1 -i "+ PATH_TO_TEMP+ "/\"%03d.jpg\" -c:v libx264 -pix_fmt yuv420p " + OUTPUT_FILE_PATH;
        List<Path> files = getImagesPaths(Path.of(PATH_TO_IMAGES));
        copyAndRenameFiles(files);
        try {
            Process proc = new ProcessBuilder(PATH_TO_FFMPEG,"-framerate","1","-i",PATH_TO_TEMP+"/\"%03d.jpg\"","-c:v", "libx264", "-pix_fmt", "yuv420p", OUTPUT_FILE_PATH).redirectErrorStream(true).start();
            StringBuilder strBuild = new StringBuilder();
            try (BufferedReader processOutputReader = new BufferedReader(new InputStreamReader(proc.getInputStream(), Charset.defaultCharset()));) {
                String line;
                while ((line = processOutputReader.readLine()) != null) {
                    strBuild.append(line + System.lineSeparator());
                }
                proc.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            String outputJson = strBuild.toString().trim();
            System.out.println(outputJson);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
