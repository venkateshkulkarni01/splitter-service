package com.example.service;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


@Slf4j
@Service
public class SplitterService {

    @Value("${app.input-file-path}")
    private String inputFilePath;

    @Value("${app.output-dir-path}")
    private String outputDirPath;

    @Value("${app.split-size}")
    private int splitSize;

    @Value("${app.processor-job-url}")
    private String processorJobUrl;

//private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    private  ProcessingService processingService;

    public void splitAndTriggerJobs() {
        try {
            Path outputDir = Paths.get(outputDirPath);
            if (!Files.exists(outputDir)) {
                Files.createDirectories(outputDir);
            }

            BufferedReader reader = new BufferedReader(new FileReader(inputFilePath));
            int fileCounter = 1;
            int lineCounter = 0;
            BufferedWriter writer = createNewSplitWriter(fileCounter);

            String line;
            while ((line = reader.readLine()) != null) {
                writer.write(line);
                writer.newLine();
                lineCounter++;

                if (lineCounter >= splitSize) {
                    writer.close();
                    String splitFileName = outputDirPath + "split_" + fileCounter + ".txt";
                    triggerProcessingJob(splitFileName);
                    fileCounter++;
                    lineCounter = 0;
                    writer = createNewSplitWriter(fileCounter);
                }
            }
            if (writer != null) {
                writer.close();
                String splitFileName = outputDirPath + "split_" + fileCounter + ".txt";
                System.out.println("file created as :  " + splitFileName);
                triggerProcessingJob(splitFileName);
            }
            reader.close();
            log.info("Splitting completed!");
        } catch (IOException e) {
            log.error("Error during splitting", e);
        }
    }

    private BufferedWriter createNewSplitWriter(int fileCounter) throws IOException {
        String splitFileName = outputDirPath + "split_" + fileCounter + ".txt";
        return new BufferedWriter(new FileWriter(splitFileName));
    }

    private void triggerProcessingJob(String filePath) {
        try {
            log.info("Triggering Processor Job for: {}", filePath);
           // restTemplate.postForObject(processorJobUrl, filePath, String.class);
           processingService.processFile(filePath);
            log.info("Processor Job triggered successfully for: {}", filePath);
        } catch (Exception e) {
            log.error("Failed to trigger job for: {}", filePath, e);
        }
    }
}
