import java.nio.file.Path;

public record FileDownloadResult(String downloadUrl, String fileName, Path destinationPath, boolean success, String status) {
}
