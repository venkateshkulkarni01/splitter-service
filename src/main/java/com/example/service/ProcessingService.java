package com.example.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;

@Slf4j
@Service
public class ProcessingService {

    @Value("${app.processed-dir-path}")
    private String processedDirPath;

    private static final Object LOCK = new Object();

    public void processFile(String inputFilePath) {
        try {
            Path outputDir = Paths.get(processedDirPath);
            if (!Files.exists(outputDir)) {
                Files.createDirectories(outputDir);
            }

            String finalOutputFile = processedDirPath + "final-processed-output.txt";

            try (
                BufferedReader reader = new BufferedReader(new FileReader(inputFilePath));
            ) {
                String todayDate = LocalDate.now().toString();
                String line;

                while ((line = reader.readLine()) != null) {
                    String enriched = line + ",DATE:" + todayDate;

                    // Append to the final shared file in a synchronized block
                    synchronized (LOCK) {
                        try (BufferedWriter writer = new BufferedWriter(new FileWriter(finalOutputFile, true))) {
                            writer.write(enriched);
                            writer.newLine();
                        }
                    }
                }
            }

            log.info("Finished processing and appending to: {}", finalOutputFile);

        } catch (IOException e) {
            log.error("Error processing file: " + inputFilePath, e);
        }
    }
}