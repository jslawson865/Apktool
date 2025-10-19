package brut.androlib.smali;

import brut.androlib.exceptions.AndrolibException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.logging.Logger;

import static org.junit.Assert.assertTrue;

public class SmaliSyntaxValidatorTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private static final Logger LOGGER = Logger.getLogger(SmaliSyntaxValidatorTest.class.getName());

    @Test
    public void validSmaliFilePasses() throws Exception {
        File project = createProjectWithSmali(".class public LTest;\n" +
            ".super Ljava/lang/Object;\n" +
            ".method public example()V\n" +
            "    .locals 2\n" +
            "    const/4 v0, 0x1\n" +
            "    if-eqz v0, :cond_0\n" +
            "    return-void\n" +
            "    :cond_0\n" +
            "    goto :cond_0\n" +
            ".end method\n");

        SmaliSyntaxValidator.validate(project, LOGGER);
    }

    @Test(expected = AndrolibException.class)
    public void registerOverflowFailsValidation() throws Exception {
        File project = createProjectWithSmali(".class public LTest;\n" +
            ".super Ljava/lang/Object;\n" +
            ".method public example()V\n" +
            "    .locals 2\n" +
            "    const/4 v2, 0x1\n" +
            "    return-void\n" +
            ".end method\n");

        SmaliSyntaxValidator.validate(project, LOGGER);
    }

    @Test(expected = AndrolibException.class)
    public void missingEndMethodFailsValidation() throws Exception {
        File project = createProjectWithSmali(".class public LTest;\n" +
            ".super Ljava/lang/Object;\n" +
            ".method public example()V\n" +
            "    .locals 1\n" +
            "    return-void\n");

        SmaliSyntaxValidator.validate(project, LOGGER);
    }

    @Test(expected = AndrolibException.class)
    public void invalidOpcodeIsReported() throws Exception {
        File project = createProjectWithSmali(".class public LTest;\n" +
            ".super Ljava/lang/Object;\n" +
            ".method public example()V\n" +
            "    .locals 1\n" +
            "    move-oject v0, v0\n" +
            "    return-void\n" +
            ".end method\n");

        SmaliSyntaxValidator.validate(project, LOGGER);
    }

    private File createProjectWithSmali(String contents) throws IOException {
        File projectDir = temporaryFolder.newFolder("project");
        File smaliDir = new File(projectDir, "smali");
        assertTrue(smaliDir.mkdirs());
        File file = new File(smaliDir, "Test.smali");
        Files.write(file.toPath(), contents.getBytes(StandardCharsets.UTF_8));
        return projectDir;
    }
}
