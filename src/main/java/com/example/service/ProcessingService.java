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

   

    public void processFile(String inputFilePath) {
        try {
            Path outputDir = Paths.get(processedDirPath);
            if (!Files.exists(outputDir)) {
                Files.createDirectories(outputDir);
            }

            String outputFileName = processedDirPath + new File(inputFilePath).getName().replace("split", "processed");

            BufferedReader reader = new BufferedReader(new FileReader(inputFilePath));
            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFileName));

            String line;
              String todayDate = LocalDate.now().toString();
            while ((line = reader.readLine()) != null) {
                // Enrichment logic — sample: add timestamp
                String enriched = line + ",DATE:" + todayDate;
                writer.write(enriched);
                writer.newLine();
            }

            reader.close();
            writer.close();
            log.info("Processed file written: {}", outputFileName);

        } catch (IOException e) {
            log.error("Error processing file: " + inputFilePath, e);
        }
    }
}

