/* Licensed under MIT 2026. */
package edu.kit.kastel.mcse.ardoco.tlr.execution.runner;

import java.io.File;
import java.io.PrintWriter;
import java.util.SortedMap;
import java.util.TreeMap;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.kit.kastel.mcse.ardoco.tlr.execution.ArDoCoForSadCodeTraceabilityLinkRecovery;
import edu.kit.kastel.mcse.ardoco.core.execution.CodeRunnerBaseTest;
import edu.kit.kastel.mcse.ardoco.core.execution.ConfigurationHelper;

class ArDoCoForSadCodeTraceabilityLinkRecoveryTest extends CodeRunnerBaseTest {
    
    private static final Logger logger = LoggerFactory.getLogger(ArDoCoForSadCodeTraceabilityLinkRecoveryTest.class);
    
    // ADDED: Configuration constants
    private static final int DEFAULT_NUM_RUNS = 5;
    private static final String RESULTS_DIR = "results/sad_code_runs";
    
    // ADDED: Statistics storage
    private static final SortedMap<Integer, RunResult> runResults = new TreeMap<>();
    
    // ADDED: Inner class to store run results
    private static class RunResult {
        final int runNumber;
        final long durationMs;
        final boolean success;
        final String errorMessage;
        final long timestamp;
        
        RunResult(int runNumber, long durationMs, boolean success, String errorMessage) {
            this.runNumber = runNumber;
            this.durationMs = durationMs;
            this.success = success;
            this.errorMessage = errorMessage;
            this.timestamp = System.currentTimeMillis();
        }
        
        @Override
        public String toString() {
            return String.format("Run %d: %s - Duration: %d ms", runNumber, success ? "SUCCESS" : "FAILED", durationMs);
        }
        
        String toCSV() {
            return String.format("%d,%d,%b,%s,%d", runNumber, durationMs, success, 
                errorMessage != null ? errorMessage.replace(",", ";") : "", timestamp);
        }
        
        static String getCSVHeader() {
            return "RunNumber,DurationMs,Success,ErrorMessage,Timestamp";
        }
    }

    @Test
    @DisplayName("Test ArDoCo for SAD-Code-TLR (Single Run)")
    void testSadCodeTlr() {
        logger.info("========================================");
        logger.info("Starting SAD-Code TLR Test (Single Run)");
        logger.info("========================================");
        
        var runner = new ArDoCoForSadCodeTraceabilityLinkRecovery(projectName);
        var additionalConfigsMap = ConfigurationHelper.loadAdditionalConfigs(new File(additionalConfigs));
        
        // ADDED: Set live LLM mode from system property
        boolean useLiveLLM = Boolean.parseBoolean(System.getProperty("useLiveLLM", "false"));
        if (useLiveLLM) {
            additionalConfigsMap.put("useLiveLLM", "true");
            logger.warn("LIVE LLM MODE ENABLED - API calls will incur costs!");
        }
        
        runner.setUp(new File(inputText), new File(inputCodeModel), additionalConfigsMap, new File(outputDir));

        long startTime = System.currentTimeMillis();
        var result = runner.run();
        long duration = System.currentTimeMillis() - startTime;
        
        testRunnerAssertions(runner);
        Assertions.assertNotNull(result);
        
        logger.info("Single run completed - Duration: {} ms", duration);
        logger.info("========================================");
    }
    
    // ADDED: Repeated test for multiple runs
    @RepeatedTest(value = DEFAULT_NUM_RUNS, name = "Run {currentRepetition} of {totalRepetitions}")
    @DisplayName("Test ArDoCo for SAD-Code-TLR (Multiple Runs)")
    void testSadCodeTlrMultipleRuns(RepetitionInfo repetitionInfo, TestInfo testInfo) {
        int currentRun = repetitionInfo.getCurrentRepetition();
        int totalRuns = repetitionInfo.getTotalRepetitions();
        
        logger.info("========================================");
        logger.info("Starting SAD-Code TLR Test - Run {}/{}", currentRun, totalRuns);
        logger.info("========================================");
        
        var runner = new ArDoCoForSadCodeTraceabilityLinkRecovery(projectName);
        var additionalConfigsMap = ConfigurationHelper.loadAdditionalConfigs(new File(additionalConfigs));
        
        // ADDED: Set configuration for this run
        additionalConfigsMap.put("currentRun", String.valueOf(currentRun));
        additionalConfigsMap.put("totalRuns", String.valueOf(totalRuns));
        
        boolean useLiveLLM = Boolean.parseBoolean(System.getProperty("useLiveLLM", "false"));
        if (useLiveLLM) {
            additionalConfigsMap.put("useLiveLLM", "true");
            if (currentRun == 1) {
                logger.warn("LIVE LLM MODE ENABLED - API calls will incur costs!");
            }
        }
        
        runner.setUp(new File(inputText), new File(inputCodeModel), additionalConfigsMap, new File(outputDir));

        long startTime = System.currentTimeMillis();
        boolean success = true;
        String errorMessage = null;
        
        try {
            var result = runner.run();
            testRunnerAssertions(runner);
            Assertions.assertNotNull(result);
        } catch (Exception e) {
            success = false;
            errorMessage = e.getMessage();
            logger.error("Run {} failed: {}", currentRun, errorMessage);
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            runResults.put(currentRun, new RunResult(currentRun, duration, success, errorMessage));
            
            logger.info("Run {}/{} completed - Success: {}, Duration: {} ms", 
                currentRun, totalRuns, success, duration);
        }
        
        // ADDED: Small delay between runs to avoid rate limiting
        if (currentRun < totalRuns && useLiveLLM) {
            try {
                logger.info("Waiting 2 seconds before next run to avoid rate limiting...");
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Interrupted while waiting between runs");
            }
        }
    }
    
    // ADDED: Test with live LLM mode
    @Test
    @DisplayName("Test ArDoCo for SAD-Code-TLR (Live LLM Mode)")
    void testSadCodeTlrLiveLLM() {
        logger.info("========================================");
        logger.info("Starting SAD-Code TLR Test (LIVE LLM MODE)");
        logger.info("========================================");
        logger.warn("⚠️  This test will make actual API calls and incur costs! ⚠️");
        
        System.setProperty("useLiveLLM", "true");
        
        try {
            testSadCodeTlr();
        } finally {
            System.clearProperty("useLiveLLM");
        }
    }
    
    // ADDED: Test with multiple runs configuration
    @Test
    @DisplayName("Test ArDoCo for SAD-Code-TLR (Configured Multiple Runs)")
    void testSadCodeTlrWithMultipleRuns() {
        int numRuns = Integer.parseInt(System.getProperty("test.runs", String.valueOf(DEFAULT_NUM_RUNS)));
        logger.info("========================================");
        logger.info("Starting SAD-Code TLR Test - {} runs configured", numRuns);
        logger.info("========================================");
        
        for (int run = 1; run <= numRuns; run++) {
            var runner = new ArDoCoForSadCodeTraceabilityLinkRecovery(projectName);
            var additionalConfigsMap = ConfigurationHelper.loadAdditionalConfigs(new File(additionalConfigs));
            
            additionalConfigsMap.put("currentRun", String.valueOf(run));
            additionalConfigsMap.put("totalRuns", String.valueOf(numRuns));
            
            boolean useLiveLLM = Boolean.parseBoolean(System.getProperty("useLiveLLM", "false"));
            if (useLiveLLM) {
                additionalConfigsMap.put("useLiveLLM", "true");
            }
            
            runner.setUp(new File(inputText), new File(inputCodeModel), additionalConfigsMap, new File(outputDir));
            
            long startTime = System.currentTimeMillis();
            var result = runner.run();
            long duration = System.currentTimeMillis() - startTime;
            
            testRunnerAssertions(runner);
            Assertions.assertNotNull(result);
            
            logger.info("Run {} completed - Duration: {} ms", run, duration);
            
            if (run < numRuns && useLiveLLM) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        
        logger.info("All {} runs completed successfully", numRuns);
    }
    
    // ADDED: Method to print summary after all runs
    @org.junit.jupiter.api.AfterAll
    static void printSummary() {
        if (runResults.isEmpty()) {
            logger.info("No multiple run results to summarize");
            return;
        }
        
        logger.info("\n\n");
        logger.info("########################################");
        logger.info("#     SAD-CODE TLR TEST SUMMARY       #");
        logger.info("########################################");
        logger.info("Total Runs: {}", runResults.size());
        logger.info("----------------------------------------");
        
        long totalDuration = 0;
        int successCount = 0;
        int failCount = 0;
        
        for (RunResult result : runResults.values()) {
            totalDuration += result.durationMs;
            if (result.success) {
                successCount++;
            } else {
                failCount++;
            }
            logger.info("  {}", result);
        }
        
        long avgDuration = runResults.isEmpty() ? 0 : totalDuration / runResults.size();
        logger.info("----------------------------------------");
        logger.info("Successful Runs: {}", successCount);
        logger.info("Failed Runs: {}", failCount);
        logger.info("Total Duration: {} ms", totalDuration);
        logger.info("Average Duration: {} ms", avgDuration);
        logger.info("########################################\n\n");
        
        // Export results to CSV
        exportResultsToCSV();
    }
    
    // ADDED: Export results to CSV
    private static void exportResultsToCSV() {
        try {
            File resultsDir = new File(RESULTS_DIR);
            if (!resultsDir.exists()) {
                resultsDir.mkdirs();
            }
            
            String timestamp = String.valueOf(System.currentTimeMillis());
            File csvFile = new File(resultsDir, "sad_code_results_" + timestamp + ".csv");
            
            try (PrintWriter writer = new PrintWriter(csvFile)) {
                writer.println(RunResult.getCSVHeader());
                for (RunResult result : runResults.values()) {
                    writer.println(result.toCSV());
                }
            }
            
            logger.info("Results exported to: {}", csvFile.getAbsolutePath());
        } catch (Exception e) {
            logger.error("Failed to export results to CSV: {}", e.getMessage());
        }
    }
    
    // ADDED: Helper method to clear results
    public static void clearResults() {
        runResults.clear();
        logger.info("Cleared all stored test results");
    }
}
