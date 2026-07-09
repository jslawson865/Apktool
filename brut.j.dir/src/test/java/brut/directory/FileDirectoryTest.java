package brut.directory;

import brut.util.OS;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Set;

import static org.junit.Assert.*;

public class FileDirectoryTest {
    private static File sTmpDir;

    @BeforeClass
    public static void beforeClass() throws Exception {
        sTmpDir = OS.createTempDirectory();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        OS.rmdir(sTmpDir);
    }

    @Test
    public void testConstructorWithValidDir() throws Exception {
        File subDir = new File(sTmpDir, "valid_dir");
        OS.mkdir(subDir);
        FileDirectory dir1 = new FileDirectory(subDir);
        assertNotNull(dir1);

        FileDirectory dir2 = new FileDirectory(subDir.getAbsolutePath());
        assertNotNull(dir2);
    }

    @Test(expected = DirectoryException.class)
    public void testConstructorWithFile() throws Exception {
        File file = new File(sTmpDir, "test_file.txt");
        Files.write(file.toPath(), "test".getBytes());
        new FileDirectory(file); // Should throw DirectoryException
    }

    @Test
    public void testLoadAndGetFiles() throws Exception {
        File testDir = new File(sTmpDir, "load_test_dir");
        OS.mkdir(testDir);

        File file1 = new File(testDir, "file1.txt");
        Files.write(file1.toPath(), "file1".getBytes());

        File file2 = new File(testDir, "file2.txt");
        Files.write(file2.toPath(), "file2".getBytes());

        File subDir = new File(testDir, "subdir");
        OS.mkdir(subDir);

        FileDirectory dir = new FileDirectory(testDir);

        Set<String> files = dir.getFiles();
        assertTrue(files.contains("file1.txt"));
        assertTrue(files.contains("file2.txt"));
        assertFalse(files.contains("subdir")); // Only files

        assertTrue(dir.containsFile("file1.txt"));
        assertFalse(dir.containsFile("subdir"));
        assertTrue(dir.containsDir("subdir"));

        Set<String> allFiles = dir.getFiles(true); // recursive
        assertTrue(allFiles.contains("file1.txt"));

        assertTrue(dir.getDirs().containsKey("subdir"));
    }

    @Test
    public void testFileInputOutputAndRemove() throws Exception {
        File testDir = new File(sTmpDir, "io_test_dir");
        OS.mkdir(testDir);
        FileDirectory dir = new FileDirectory(testDir);

        // Test output
        try (OutputStream os = dir.getFileOutput("test_io.txt")) {
            os.write("hello".getBytes());
        }

        assertTrue(dir.containsFile("test_io.txt"));

        // Test size
        assertEquals(5, dir.getSize("test_io.txt"));
        assertEquals(5, dir.getCompressedSize("test_io.txt"));
        assertEquals(0, dir.getCompressionLevel("test_io.txt"));

        // Test input
        try (InputStream is = dir.getFileInput("test_io.txt")) {
            byte[] buf = new byte[5];
            int read = is.read(buf);
            assertEquals(5, read);
            assertEquals("hello", new String(buf));
        }

        // Test remove
        assertTrue(dir.removeFile("test_io.txt"));
        assertFalse(dir.containsFile("test_io.txt"));
    }

    @Test
    public void testCreateDir() throws Exception {
        File testDir = new File(sTmpDir, "create_dir_test");
        OS.mkdir(testDir);
        FileDirectory dir = new FileDirectory(testDir);

        Directory subDir = dir.createDir("new_subdir");
        assertNotNull(subDir);
        assertTrue(dir.containsDir("new_subdir"));
        assertTrue(new File(testDir, "new_subdir").isDirectory());
    }

    @Test(expected = DirectoryException.class)
    public void testGetSizeOnDirectory() throws Exception {
        File testDir = new File(sTmpDir, "size_test_dir");
        OS.mkdir(testDir);

        File subDir = new File(testDir, "subdir");
        OS.mkdir(subDir);

        FileDirectory dir = new FileDirectory(testDir);
        dir.getSize("subdir"); // Should throw DirectoryException
    }
}
