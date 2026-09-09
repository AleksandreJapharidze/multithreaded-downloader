import org.apache.tika.Tika;
import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;

import java.io.IOException;
import java.nio.file.Path;

public class TikaHelper {
    private static final Tika tika = new Tika();
    private static final MimeTypes mimeTypes = MimeTypes.getDefaultMimeTypes();

    private TikaHelper() {
    }

    public static String getExtension(Path path) {
        try {
            MimeType mimeType = mimeTypes.forName(tika.detect(path));
            return mimeType.getExtension();
        } catch (MimeTypeException | IOException e) {
            System.err.println("Error getting extension for file: " + path + ". Error: " + e.getMessage());
            return null;
        }
    }
}
