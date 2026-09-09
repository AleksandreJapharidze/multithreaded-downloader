void main() {
    final Map<String, String> downloadAndFileNames = Map.of(
            "https://cdn.jsdelivr.net/gh/belaviyo/download-with/samples/sample.png", "example1",
            "https://cdn.jsdelivr.net/gh/belaviyo/download-with/samples/sample.jpg", "example2",
            "https://cdn.jsdelivr.net/gh/belaviyo/download-with/samples/sample", "example3",
            "https://cdn.jsdelivr.net/gh/belaviyo/download-with/samples/sample.pdf", "example4",
            "https://cdn.jsdelivr.net/gh/belaviyo/download-with/samples/sample.html", "example5",
            "https://cdn.jsdelivr.net/gh/belaviyo/download-with/samples/sample.zip", "example6"
    );

    try {
        MultiDownloader multiDownloader = new MultiDownloader(10);
        multiDownloader.startMultithreadedDownload(downloadAndFileNames, Path.of(System.getProperty("user.home"), "Downloads"));
    } catch (Exception e) {
        System.err.println("Error starting download: " + e.getMessage());
    }
}