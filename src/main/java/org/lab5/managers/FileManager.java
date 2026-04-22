package org.lab5.managers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.lab5.exceptions.FileReadException;
import org.lab5.exceptions.FileWriteException;
import org.lab5.exceptions.ValidationException;
import org.lab5.models.Organization;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

public class FileManager {
    private final Path dataFile;
    private final ObjectMapper objectMapper;

    public FileManager(Path dataFile, ObjectMapper objectMapper) {
        this.dataFile = dataFile;
        this.objectMapper = objectMapper;
    }

    public Path getDataFile() {
        return dataFile;
    }

    public List<Organization> loadCollection() throws FileReadException {
        if (!Files.exists(dataFile)) {
            return Collections.emptyList();
        }

        if (!Files.isReadable(dataFile)) {
            throw new FileReadException("cannot read file: " + dataFile);
        }

        try (Scanner scanner = new Scanner(dataFile, StandardCharsets.UTF_8)) {
            scanner.useDelimiter("\\A");
            String content = scanner.hasNext() ? scanner.next() : "";

            if (content.isBlank()) {
                return Collections.emptyList();
            }

            List<Organization> list = objectMapper.readValue(content, new TypeReference<List<Organization>>() {
            });

            for (Organization organization : list) {
                try {
                    organization.validate();
                } catch (ValidationException e) {
                    throw new FileReadException("invalid organization in file: " + e.getMessage());
                }
            }

            return list;
        } catch (IOException e) {
            throw new FileReadException("failed to read file: " + e.getMessage());
        }
    }

    public void saveCollection(List<Organization> organizations) throws FileWriteException {
        Path parent = dataFile.getParent();

        if (parent != null) {
            try {
                Files.createDirectories(parent);
            } catch (IOException e) {
                throw new FileWriteException("cannot create directories: " + e.getMessage());
            }
        }

        if (Files.exists(dataFile) && !Files.isWritable(dataFile)) {
            throw new FileWriteException("cannot write file: " + dataFile);
        }

        try (BufferedWriter writer = Files.newBufferedWriter(dataFile, StandardCharsets.UTF_8)) {
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(organizations);
            writer.write(json);
        } catch (IOException e) {
            throw new FileWriteException("failed to write file: " + e.getMessage());
        }
    }
}