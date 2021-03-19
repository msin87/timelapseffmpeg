package core;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;

/**
 * A utility class that scans damaged image files
 *
 * This is based on BadPeggy GUI Tool https://github.com/llaith-oss/BadPeggy
 *
 * @author Clyde Velasquez
 * @version 1.0
 */
public final class ImageScanner {

    private ImageScanner() {
    }

    /**
     * Scans an image and returns the Result object
     *
     * @param imgInputStream InputStream of an image file
     * @param imgFormat ImageFormat enum type
     * @return Result object
     */
    public static ScannerResult scan(InputStream imgInputStream, ImageFormat imgFormat) {

        ScannerResult scannerResult = new ScannerResult();
        ImageReader imgReader = ImageIO.getImageReadersByFormatName(imgFormat.toString()).next();

        try (imgInputStream) {

            // Remove all Listener objects
            imgReader.removeAllIIOReadProgressListeners();
            imgReader.removeAllIIOReadUpdateListeners();
            imgReader.removeAllIIOReadWarningListeners();

            imgReader.addIIOReadWarningListener((final ImageReader source, final String warning) -> {
                scannerResult.messagesSb.append(warning).append("\n");
                scannerResult.isOk = false;
                scannerResult.resultType = ScannerResult.Type.WARNING;
            });
            imgReader.setInput(ImageIO.createImageInputStream(imgInputStream));

            int imgCount = imgReader.getNumImages(true);
            for (int i = 0; i < imgCount; i++) {
                imgReader.read(i);
            }
        } catch (NegativeArraySizeException ex) {
            scannerResult.messagesSb.append("Internal decoder error 1");
            scannerResult.messagesSb.append(ex.getMessage()).append("\n");
            scannerResult.isOk = false;
            scannerResult.resultType = ScannerResult.Type.ERROR;
        } catch (ArrayIndexOutOfBoundsException ex) {
            scannerResult.messagesSb.append("Internal decoder error 2");
            scannerResult.messagesSb.append(ex.getMessage()).append("\n");
            scannerResult.isOk = false;
            scannerResult.resultType = ScannerResult.Type.ERROR;
        } catch (IOException ex) {
            scannerResult.messagesSb.append(ex.getMessage()).append("\n");
            scannerResult.isOk = false;
            scannerResult.resultType = ScannerResult.Type.ERROR;
        } catch (Exception e) {
            scannerResult.messagesSb.append(e.getMessage()).append("\n");
            scannerResult.isOk = false;
            scannerResult.resultType = ScannerResult.Type.UNEXPECTED_ERROR;
        } finally {
            imgReader.dispose();
        }

        return scannerResult;
    }

    /**
     * Scans an image and returns the Result object
     *
     * @param imgPath Path of an image
     * @return Result object
     * @throws IOException If I/O error occurs when opening a Path then creating
     * a new InputStream from it
     */
    public static ScannerResult scan(Path imgPath) throws IOException {
        return scan(Files.newInputStream(imgPath),
                Objects.requireNonNull(ImageFormat.fromFileName(imgPath.getFileName().toString())));
    }

    /**
     * Result object that contains some information after scanning an image
     */
    public static class ScannerResult {

        public enum Type {
            OK,
            WARNING,
            ERROR,
            UNEXPECTED_ERROR
        }

        private final StringBuilder messagesSb;
        private Boolean isOk;
        private Type resultType;

        public ScannerResult() {
            this.messagesSb = new StringBuilder();
            this.isOk = true;
            this.resultType = Type.OK;
        }

        public StringBuilder getMessages() {
            return messagesSb;
        }

        public Boolean isOk() {
            return isOk;
        }

        public Type resultType() {
            return resultType;
        }

        public Boolean isCorrupt() {
            return !isOk;
        }

        @Override
        public String toString() {
            return "Is image OK? " + isOk +
                    ", Result type: " + resultType +
                    "\n" + messagesSb.toString();
        }
    }
}
