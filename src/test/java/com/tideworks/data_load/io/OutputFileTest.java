package com.tideworks.data_load.io;

import com.google.common.io.Files;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;

import static org.junit.Assert.*;

public class OutputFileTest {

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Rule
    public TemporaryFolder folder= new TemporaryFolder();


    @Test
    public void testCreate_ErrorIfFileCreated() throws IOException {
        File file = folder.newFile("test.file");

        exceptionRule.expect(FileAlreadyExistsException.class);
        OutputFile.nioPathToOutputFile(file.toPath()).create(10);
    }

    @Test
    public void testCreateOrOverride_Truncate() throws IOException {
        File file = folder.newFile("override.file");
        Files.write(new byte[10], file);
        OutputFile.nioPathToOutputFile(file.toPath()).createOrOverwrite(10);
        assertEquals(0, file.length());
    }
}