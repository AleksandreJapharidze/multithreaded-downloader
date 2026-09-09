import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;

public class MultiDownloader {
    private final int maxConcurrentDownloads;

    public MultiDownloader(int maxConcurrentDownloads) {
        if (maxConcurrentDownloads <= 0) {
            throw new IllegalArgumentException("Max concurrent downloads must be greater than 0");
        }
        this.maxConcurrentDownloads = maxConcurrentDownloads;
    }

    public void startMultithreadedDownload(Map<String, String> downloadAndFileNames, Path destinationFolder) {
        Objects.requireNonNull(downloadAndFileNames, "downloadAndFileNames cannot be null");
        Objects.requireNonNull(destinationFolder, "destinationFolder cannot be null");

        if (!noNullElements(downloadAndFileNames)) {
            System.err.println("Null elements found in downloadAndFileNames.");
            return;
        }

        if (!allRulesPassed(downloadAndFileNames, destinationFolder)) {
            return;
        }

        try (ExecutorService es = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("Task-", 0).factory());
             HttpClient httpClient = HttpClient.newHttpClient()
        ) {
            System.out.println("Starting downloads...");
            List<DownloadTask> downloadTasks = downloadAndFileNames.entrySet()
                    .stream()
                    .map(entry -> new DownloadTask(entry.getKey(), entry.getValue(), destinationFolder, httpClient))
                    .toList();

            List<Future<FileDownloadResult>> results = es.invokeAll(downloadTasks);

            System.out.println("Results:\n");
            for (Future<FileDownloadResult> result : results) {
                System.out.println(result.resultNow());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("An error occurred on higher level while executing the downloads: " + e.getMessage());
        }
    }

    private boolean destinationPathExistsAndIsDirectory(Path destinationFolder) {
        if (Files.notExists(destinationFolder)) {
            System.err.println("Destination folder does not exist: " + destinationFolder);
            return false;
        }

        if (Files.exists(destinationFolder) && !Files.isDirectory(destinationFolder)) {
            System.err.println("Destination path is not a directory: " + destinationFolder);
            return false;
        }

        return true;
    }

    private boolean allRulesPassed(Map<String, String> downloadAndFileNames, Path destinationFolder) {
        if (!destinationPathExistsAndIsDirectory(destinationFolder)) {
            return false;
        }

        if (downloadAndFileNames.isEmpty()) {
            System.err.println("No URLs to download.");
            return false;
        }

        if (downloadAndFileNames.size() > maxConcurrentDownloads) {
            System.err.println("Number of URLs exceeds maximum concurrent downloads: " + maxConcurrentDownloads);
            return false;
        }

        return true;
    }

    private boolean noNullElements(Map<String, String> downloadAndFileNames) {
        for (Map.Entry<String, String> entry : downloadAndFileNames.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                return false;
            }
        }

        return true;
    }
}
