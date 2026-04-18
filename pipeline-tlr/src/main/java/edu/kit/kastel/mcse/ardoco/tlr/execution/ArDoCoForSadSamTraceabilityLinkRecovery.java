/* Licensed under MIT 2026-2027. */
package edu.kit.kastel.mcse.ardoco.tlr.execution;

import java.io.File;
import java.util.SortedMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.kit.kastel.mcse.ardoco.core.api.models.ArchitectureModelType;
import edu.kit.kastel.mcse.ardoco.core.common.util.CommonUtilities;
import edu.kit.kastel.mcse.ardoco.core.common.util.DataRepositoryHelper;
import edu.kit.kastel.mcse.ardoco.core.execution.runner.ArDoCoRunner;
import edu.kit.kastel.mcse.ardoco.tlr.connectiongenerator.ConnectionGenerator;
import edu.kit.kastel.mcse.ardoco.tlr.models.agents.ArCoTLModelProviderAgent;
import edu.kit.kastel.mcse.ardoco.tlr.models.agents.ArchitectureConfiguration;
import edu.kit.kastel.mcse.ardoco.tlr.recommendationgenerator.RecommendationGenerator;
import edu.kit.kastel.mcse.ardoco.tlr.text.providers.TextPreprocessingAgent;
import edu.kit.kastel.mcse.ardoco.tlr.textextraction.TextExtraction;

public class ArDoCoForSadSamTraceabilityLinkRecovery extends ArDoCoRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(ArDoCoForSadSamTraceabilityLinkRecovery.class);
    
    // ADDED: Configuration fields for tracking
    private boolean useLiveLLM = false;
    private int currentRun = 1;
    private int totalRuns = 1;
    private String currentModel = "unknown";
    private String currentProjectName;
    private ArchitectureModelType architectureModelType;
    
    // ADDED: Statistics tracking fields
    private long pipelineStartTime;
    private long pipelineEndTime;
    private int totalAgentsExecuted = 0;
    
    public ArDoCoForSadSamTraceabilityLinkRecovery(String projectName) {
        super(projectName);
        this.currentProjectName = projectName;
        logger.info("Initialized ArDoCoForSadSamTraceabilityLinkRecovery for project: {}", projectName);
    }

    public void setUp(File inputText, File inputArchitectureModel, ArchitectureModelType architectureModelType, SortedMap<String, String> additionalConfigs,
            File outputDir) {
        
        // ADDED: Store architecture model type
        this.architectureModelType = architectureModelType;
        
        // ADDED: Extract configuration values
        extractConfiguration(additionalConfigs);
        
        logger.info("=== Setting up SAD-SAM Pipeline ===");
        logger.info("Project: {}", currentProjectName);
        logger.info("Model: {}", currentModel);
        logger.info("Architecture Model Type: {}", architectureModelType);
        logger.info("Live LLM Mode: {}", useLiveLLM);
        logger.info("Current Run: {}/{}", currentRun, totalRuns);
        logger.info("Input Text: {}", inputText.getAbsolutePath());
        logger.info("Input Model: {}", inputArchitectureModel.getAbsolutePath());
        logger.info("Output Directory: {}", outputDir.getAbsolutePath());
        
        definePipeline(inputText, inputArchitectureModel, architectureModelType, additionalConfigs);
        setOutputDirectory(outputDir);
        isSetUp = true;
        
        logger.info("SAD-SAM pipeline setup complete");
    }

    public void setUp(String inputTextLocation, String inputArchitectureModelLocation, ArchitectureModelType architectureModelType,
            SortedMap<String, String> additionalConfigs, String outputDirectory) {
        setUp(new File(inputTextLocation), new File(inputArchitectureModelLocation), architectureModelType, additionalConfigs, new File(outputDirectory));
    }
    
    // ADDED: Configuration extraction method
    private void extractConfiguration(SortedMap<String, String> additionalConfigs) {
        if (additionalConfigs != null) {
            if (additionalConfigs.containsKey("useLiveLLM")) {
                this.useLiveLLM = Boolean.parseBoolean(additionalConfigs.get("useLiveLLM"));
            }
            if (additionalConfigs.containsKey("currentRun")) {
                this.currentRun = Integer.parseInt(additionalConfigs.get("currentRun"));
            }
            if (additionalConfigs.containsKey("totalRuns")) {
                this.totalRuns = Integer.parseInt(additionalConfigs.get("totalRuns"));
            }
            if (additionalConfigs.containsKey("currentModel")) {
                this.currentModel = additionalConfigs.get("currentModel");
            }
            if (additionalConfigs.containsKey("currentProject")) {
                this.currentProjectName = additionalConfigs.get("currentProject");
            }
        }
        
        logger.debug("Configuration extracted - LiveLLM: {}, Run: {}/{}, Model: {}", 
            useLiveLLM, currentRun, totalRuns, currentModel);
    }

    private void definePipeline(File inputText, File inputArchitectureModel, ArchitectureModelType architectureModelType,
            SortedMap<String, String> additionalConfigs) {
        
        pipelineStartTime = System.currentTimeMillis();
        
        var dataRepository = this.getArDoCo().getDataRepository();
        
        // ADDED: Store configuration in data repository for other components
        dataRepository.addData("config.useLiveLLM", useLiveLLM);
        dataRepository.addData("config.currentRun", currentRun);
        dataRepository.addData("config.currentModel", currentModel);
        dataRepository.addData("config.currentProject", currentProjectName);
        dataRepository.addData("config.task", "SAD-SAM");
        
        logger.info("Building SAD-SAM traceability pipeline...");
        
        var text = CommonUtilities.readInputText(inputText);
        if (text.isBlank()) {
            throw new IllegalArgumentException("Cannot deal with empty input text. Maybe there was an error reading the file.");
        }
        DataRepositoryHelper.putInputText(dataRepository, text);
        logger.info("Input text loaded: {} characters", text.length());

        this.getArDoCo().addPipelineStep(TextPreprocessingAgent.get(additionalConfigs, dataRepository));
        totalAgentsExecuted++;
        logger.debug("Added TextPreprocessingAgent");

        var architectureConfiguration = new ArchitectureConfiguration(inputArchitectureModel, architectureModelType);
        ArCoTLModelProviderAgent arCoTLModelProviderAgent = //
                ArCoTLModelProviderAgent.getArCoTLModelProviderAgent(dataRepository, additionalConfigs, architectureConfiguration, null);
        this.getArDoCo().addPipelineStep(arCoTLModelProviderAgent);
        totalAgentsExecuted++;
        logger.debug("Added ArCoTLModelProviderAgent");

        this.getArDoCo().addPipelineStep(TextExtraction.get(additionalConfigs, dataRepository));
        totalAgentsExecuted++;
        logger.debug("Added TextExtraction");

        this.getArDoCo().addPipelineStep(RecommendationGenerator.get(additionalConfigs, dataRepository));
        totalAgentsExecuted++;
        logger.debug("Added RecommendationGenerator");

        this.getArDoCo().addPipelineStep(ConnectionGenerator.get(additionalConfigs, dataRepository));
        totalAgentsExecuted++;
        logger.debug("Added ConnectionGenerator");

        logger.info("Pipeline defined with {} agents", totalAgentsExecuted);
    }
    
    // ADDED: Method to run pipeline with statistics
    @Override
    public void run() {
        logger.info("========================================");
        logger.info("Starting SAD-SAM Pipeline Execution");
        logger.info("========================================");
        logger.info("Project: {}", currentProjectName);
        logger.info("Model: {}", currentModel);
        logger.info("Architecture Type: {}", architectureModelType);
        logger.info("Live LLM Mode: {}", useLiveLLM);
        logger.info("Run: {}/{}", currentRun, totalRuns);
        logger.info("========================================");
        
        long executionStartTime = System.currentTimeMillis();
        
        try {
            super.run();
            pipelineEndTime = System.currentTimeMillis();
            long executionDuration = pipelineEndTime - executionStartTime;
            
            logger.info("========================================");
            logger.info("SAD-SAM Pipeline Execution Complete");
            logger.info("========================================");
            logger.info("Total execution time: {} ms ({} seconds)", 
                executionDuration, executionDuration / 1000.0);
            logger.info("Total agents executed: {}", totalAgentsExecuted);
            logger.info("Average time per agent: {} ms", executionDuration / totalAgentsExecuted);
            logger.info("========================================");
            
        } catch (Exception e) {
            logger.error("SAD-SAM pipeline execution failed: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    // ADDED: Method to get execution statistics
    public String getStatistics() {
        long totalDuration = pipelineEndTime - pipelineStartTime;
        return String.format(
            "Task: SAD-SAM, Project: %s, Model: %s, LiveLLM: %s, Run: %d/%d, Duration: %d ms, Agents: %d",
            currentProjectName,
            currentModel,
            useLiveLLM,
            currentRun,
            totalRuns,
            totalDuration,
            totalAgentsExecuted
        );
    }
    
    // ADDED: Method to get CSV formatted statistics
    public String toCSV() {
        long totalDuration = pipelineEndTime - pipelineStartTime;
        return String.format(
            "%s,%s,%s,%b,%d,%d,%d,%d",
            "SAD-SAM",
            currentProjectName,
            currentModel,
            useLiveLLM,
            currentRun,
            totalRuns,
            totalDuration,
            totalAgentsExecuted
        );
    }
    
    // ADDED: Static method to get CSV header
    public static String getCSVHeader() {
        return "Task,Project,Model,LiveLLM,CurrentRun,TotalRuns,DurationMs,AgentsExecuted";
    }
    
    // ADDED: Getters for configuration
    public boolean isUseLiveLLM() {
        return useLiveLLM;
    }
    
    public int getCurrentRun() {
        return currentRun;
    }
    
    public int getTotalRuns() {
        return totalRuns;
    }
    
    public String getCurrentModel() {
        return currentModel;
    }
    
    public String getCurrentProjectName() {
        return currentProjectName;
    }
    
    public ArchitectureModelType getArchitectureModelType() {
        return architectureModelType;
    }
    
    // ADDED: Setters for programmatic configuration
    public void setUseLiveLLM(boolean useLiveLLM) {
        this.useLiveLLM = useLiveLLM;
        logger.info("Live LLM mode set to: {}", useLiveLLM);
    }
    
    public void setCurrentRun(int currentRun) {
        this.currentRun = currentRun;
    }
    
    public void setTotalRuns(int totalRuns) {
        this.totalRuns = totalRuns;
    }
    
    public void setCurrentModel(String currentModel) {
        this.currentModel = currentModel;
    }
    
    // ADDED: Reset method for multiple runs
    public void reset() {
        this.pipelineStartTime = 0;
        this.pipelineEndTime = 0;
        this.totalAgentsExecuted = 0;
        logger.info("SAD-SAM pipeline statistics reset for run {}", currentRun);
    }
}
