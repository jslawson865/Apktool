package brut.androlib.util;

import brut.directory.ZipRODirectory;
import java.io.File;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.Test;
import static org.junit.Assert.*;

public class ZipRODirectoryZipSlipTest {

    @Test
    public void testZipSlipVulnerability() throws Exception {
        File zipFile = File.createTempFile("test_zipslip", ".zip");
        zipFile.deleteOnExit();

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            zos.putNextEntry(new ZipEntry("../foo.txt"));
            zos.write("hello".getBytes());
            zos.closeEntry();

            zos.putNextEntry(new ZipEntry("/etc/passwd"));
            zos.write("world".getBytes());
            zos.closeEntry();

            zos.putNextEntry(new ZipEntry("C:\\Windows\\System32\\cmd.exe"));
            zos.write("windows".getBytes());
            zos.closeEntry();

            zos.putNextEntry(new ZipEntry("valid/bar.txt"));
            zos.write("valid".getBytes());
            zos.closeEntry();

            zos.putNextEntry(new ZipEntry("a/../../foo.txt"));
            zos.write("world".getBytes());
            zos.closeEntry();
        }

        try (ZipRODirectory dir = new ZipRODirectory(zipFile)) {
            java.util.Set<String> files = dir.getFiles(true);
            assertEquals(1, files.size());
            assertTrue(files.contains("valid/bar.txt"));
            assertFalse(files.contains("../foo.txt"));
            assertFalse(files.contains("/etc/passwd"));
            assertFalse(files.contains("C:\\Windows\\System32\\cmd.exe"));
            assertFalse(files.contains("a/../../foo.txt"));
        }
    }
}
