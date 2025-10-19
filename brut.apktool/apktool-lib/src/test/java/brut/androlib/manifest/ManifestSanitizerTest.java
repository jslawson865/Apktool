package brut.androlib.manifest;

import brut.androlib.apk.ApkInfo;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import static org.junit.Assert.*;

public class ManifestSanitizerTest {
    private Path tempDir;
    private Path manifestPath;

    @Before
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("manifestSanitizerTest");
        manifestPath = tempDir.resolve("AndroidManifest.xml");
        try (InputStream stream = getClass().getResourceAsStream("/manifest/AndroidManifest_blacklist.xml")) {
            assertNotNull("Test manifest resource should exist", stream);
            Files.copy(stream, manifestPath);
        }
    }

    @After
    public void tearDown() throws IOException {
        if (tempDir != null) {
            try (Stream<Path> paths = Files.walk(tempDir)) {
                paths.sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(java.io.File::delete);
            }
        }
    }

    @Test
    public void removesBlacklistedEntries() throws Exception {
        ApkInfo apkInfo = new ApkInfo();

        boolean sanitized = ManifestSanitizer.sanitize(manifestPath.toFile(), apkInfo);

        assertTrue("Expected manifest sanitizer to modify the manifest", sanitized);
        assertEquals("Sanitizer should record four entries", 4, apkInfo.manifestSanitizerHistory.size());
        assertTrue(apkInfo.manifestSanitizerHistory.stream()
                .anyMatch(entry -> entry.contains("android:allowCrossUidActivitySwitchFromBelow")));
        assertTrue(apkInfo.manifestSanitizerHistory.stream()
                .anyMatch(entry -> entry.contains("android:dataExtractionRules")));
        assertTrue(apkInfo.manifestSanitizerHistory.stream()
                .anyMatch(entry -> entry.contains("android:maxAspectRatio")));
        assertTrue(apkInfo.manifestSanitizerHistory.stream()
                .anyMatch(entry -> entry.contains("Removed tag <queries>")));

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document document = factory.newDocumentBuilder().parse(manifestPath.toFile());
        Element application = (Element) document.getElementsByTagName("application").item(0);
        assertNotNull(application);
        assertFalse(application.hasAttribute("android:allowCrossUidActivitySwitchFromBelow"));
        assertFalse(application.hasAttribute("android:dataExtractionRules"));
        assertFalse(application.hasAttribute("android:maxAspectRatio"));

        NodeList queriesNodes = document.getElementsByTagName("queries");
        assertEquals(0, queriesNodes.getLength());
    }
}
