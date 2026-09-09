import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.Callable;

public class DownloadTask implements Callable<FileDownloadResult> {
    private final String downloadUrl;
    private final String fileName;
    private final Path destinationPath;
    private final HttpClient httpClient;

    public DownloadTask(String downloadUrl, String fileName, Path destinationPath, HttpClient httpClient) {
        this.downloadUrl = downloadUrl;
        this.fileName = fileName;
        this.destinationPath = destinationPath;
        this.httpClient = httpClient;
    }

    @Override
    public FileDownloadResult call() {
        HttpRequest request;

        try {
            request = HttpRequest.newBuilder()
                    .uri(URI.create(downloadUrl))
                    .build();
        } catch (IllegalArgumentException e) {
            return new FileDownloadResult(downloadUrl, fileName, destinationPath, false, "Invalid URL was provided");
        }

        Path tempFilePath = null;
        Path finalPath = null;
        try {
            tempFilePath = createTempFile();
            if (tempFilePath == null) {
                return new FileDownloadResult(downloadUrl, fileName, destinationPath, false, "Error creating temporary file for download");
            }

            HttpResponse<Path> response = httpClient.send(request, HttpResponse.BodyHandlers.ofFile(tempFilePath));

            int statusCode = response.statusCode();
            if (statusCode < 200 || statusCode >= 300) {
                return new FileDownloadResult(downloadUrl, fileName, destinationPath, false, "Download failed with status code: " + statusCode);
            }

            String extension = TikaHelper.getExtension(tempFilePath);
            if (extension == null) {
                return new FileDownloadResult(downloadUrl, fileName, destinationPath, false, "Error figuring out the extension of the file");
            }

            String sanitizedFileName = sanitizeFileName(fileName);
            finalPath = destinationPath.resolve(sanitizedFileName + "-" + System.currentTimeMillis() + extension);

            return tryMoveFileAndGetFinalResult(tempFilePath, finalPath);
        } catch (IOException e) {
            deleteFileIfExists(finalPath);
            return new FileDownloadResult(downloadUrl, fileName, destinationPath, false, "Error downloading file: " + e.getMessage());
        } catch (InterruptedException e) {
            deleteFileIfExists(finalPath);
            Thread.currentThread().interrupt();
            return new FileDownloadResult(downloadUrl, fileName, destinationPath, false, "Download task interrupted");
        } finally {
            if (tempFilePath != null) {
                deleteFileIfExists(tempFilePath);
            }
        }
    }

    private void deleteFileIfExists(Path path) {
        if (path == null) {
            return;
        }

        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            System.err.println("Error deleting the file: " + path + ". Consider manually deleting the file.");
        }
    }

    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private Path createTempFile() {
        try {
            return Files.createTempFile("download-", ".tmp");
        } catch (IOException e) {
            return null;
        }
    }

    private FileDownloadResult tryMoveFileAndGetFinalResult(Path source, Path destination) {
        try {
            Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE);
            return new FileDownloadResult(downloadUrl, fileName, destinationPath, true, "File downloaded successfully");
        } catch (IOException e) {
            return new FileDownloadResult(downloadUrl, fileName, destinationPath, false, "File was downloaded to a temporary location but couldn't be moved: " + e.getMessage());
        }
    }
}
