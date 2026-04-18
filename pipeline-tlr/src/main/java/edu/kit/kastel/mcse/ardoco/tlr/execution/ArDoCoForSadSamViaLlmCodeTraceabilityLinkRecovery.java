/* Licensed under MIT 2026-2027. */
package edu.kit.kastel.mcse.ardoco.tlr.execution;

import java.io.File;
import java.util.SortedMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.kit.kastel.mcse.ardoco.core.common.util.CommonUtilities;
import edu.kit.kastel.mcse.ardoco.core.common.util.DataRepositoryHelper;
import edu.kit.kastel.mcse.ardoco.core.execution.ArDoCo;
import edu.kit.kastel.mcse.ardoco.core.execution.runner.ArDoCoRunner;
import edu.kit.kastel.mcse.ardoco.tlr.codetraceability.SadSamCodeTraceabilityLinkRecovery;
import edu.kit.kastel.mcse.ardoco.tlr.codetraceability.SamCodeTraceabilityLinkRecovery;
import edu.kit.kastel.mcse.ardoco.tlr.connectiongenerator.ConnectionGenerator;
import edu.kit.kastel.mcse.ardoco.tlr.models.agents.ArCoTLModelProviderAgent;
import edu.kit.kastel.mcse.ardoco.tlr.models.agents.LLMArchitectureProviderAgent;
import edu.kit.kastel.mcse.ardoco.tlr.models.informants.LLMArchitecturePrompt;
import edu.kit.kastel.mcse.ardoco.tlr.models.informants.LargeLanguageModel;
import edu.kit.kastel.mcse.ardoco.tlr.recommendationgenerator.RecommendationGenerator;
import edu.kit.kastel.mcse.ardoco.tlr.text.providers.TextPreprocessingAgent;
import edu.kit.kastel.mcse.ardoco.tlr.textextraction.TextExtraction;

public class ArDoCoForSadSamViaLlmCodeTraceabilityLinkRecovery extends ArDoCoRunner {

    private static final Logger logger = LoggerFactory.getLogger(ArDoCoForSadSamViaLlmCodeTraceabilityLinkRecovery.class);

    // ADDED: Configuration fields for live LLM tracking
    private boolean useLiveLLM = false;
    private int currentRun = 1;
    private int totalRuns = 1;
    private LargeLanguageModel currentModel;
    private String currentProjectName;

    // ADDED: Statistics tracking fields
    private long pipelineStartTime;
    private long pipelineEndTime;
    private int totalAgentsExecuted = 0;

    public ArDoCoForSadSamViaLlmCodeTraceabilityLinkRecovery(String projectName) {
        super(projectName);
        this.currentProjectName = projectName;
        logger.info("Initialized ArDoCoForSadSamViaLlmCodeTraceabilityLinkRecovery for project: {}", projectName);
    }

    public void setUp(File inputText, File inputCode, SortedMap<String, String> additionalConfigs, File outputDir, LargeLanguageModel largeLanguageModel,
            LLMArchitecturePrompt documentationExtractionPrompt, LLMArchitecturePrompt codeExtractionPrompt, LLMArchitecturePrompt.Features codeFeatures,
            LLMArchitecturePrompt aggregationPrompt) {
        
        // ADDED: Extract configuration values
        extractConfiguration(additionalConfigs, largeLanguageModel);
        
        logger.info("=== Setting up Pipeline ===");
        logger.info("Project: {}", currentProjectName);
        logger.info("Model: {}", currentModel != null ? currentModel.name() : "unknown");
        logger.info("Live LLM Mode: {}", useLiveLLM);
        logger.info("Current Run: {}/{}", currentRun, totalRuns);
        logger.info("Output Directory: {}", outputDir.getAbsolutePath());
        
        definePipeline(inputText, inputCode, additionalConfigs, largeLanguageModel, documentationExtractionPrompt, codeExtractionPrompt, codeFeatures,
                aggregationPrompt);
        setOutputDirectory(outputDir);
        isSetUp = true;
        
        logger.info("Pipeline setup complete");
    }

    // ADDED: Configuration extraction method
    private void extractConfiguration(SortedMap<String, String> additionalConfigs, LargeLanguageModel largeLanguageModel) {
        this.currentModel = largeLanguageModel;
        
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
            if (additionalConfigs.containsKey("currentProject")) {
                this.currentProjectName = additionalConfigs.get("currentProject");
            }
        }
        
        // ADDED: Set system properties for deterministic behavior
        if (!useLiveLLM) {
            System.setProperty("OPENAI_API_KEY", "sk-DUMMY");
            logger.info("Using cached LLM responses (API key set to sk-DUMMY)");
        } else {
            logger.warn("Using LIVE LLM mode - API calls will incur costs!");
            String apiKey = System.getenv("OPENAI_API_KEY");
            if (apiKey == null || apiKey.equals("sk-DUMMY")) {
                logger.error("OPENAI_API_KEY environment variable not set properly for live mode!");
            } else {
                logger.info("OpenAI API key detected (length: {} chars)", apiKey.length());
            }
        }
    }

    private void definePipeline(File inputText, File inputCode, SortedMap<String, String> additionalConfigs, LargeLanguageModel largeLanguageModel,
            LLMArchitecturePrompt documentationExtractionPrompt, LLMArchitecturePrompt codeExtractionPrompt, LLMArchitecturePrompt.Features codeFeatures,
            LLMArchitecturePrompt aggregationPrompt) {
        
        pipelineStartTime = System.currentTimeMillis();
        
        ArDoCo arDoCo = this.getArDoCo();
        var dataRepository = arDoCo.getDataRepository();

        // ADDED: Store configuration in data repository for other components
        dataRepository.addData("config.useLiveLLM", useLiveLLM);
        dataRepository.addData("config.currentRun", currentRun);
        dataRepository.addData("config.currentModel", currentModel != null ? currentModel.name() : "unknown");
        dataRepository.addData("config.currentProject", currentProjectName);

        var text = CommonUtilities.readInputText(inputText);
        if (text.isBlank()) {
            throw new IllegalArgumentException("Cannot deal with empty input text. Maybe there was an error reading the file.");
        }
        DataRepositoryHelper.putInputText(dataRepository, text);
        
        logger.info("Input text loaded: {} characters", text.length());

        // ADDED: Log pipeline steps being added
        logger.info("Adding pipeline steps...");

        arDoCo.addPipelineStep(TextPreprocessingAgent.get(additionalConfigs, dataRepository));
        totalAgentsExecuted++;
        logger.debug("Added TextPreprocessingAgent");

        var codeConfiguration = ArCoTLModelProviderAgent.getCodeConfiguration(inputCode);
        ArCoTLModelProviderAgent arCoTLModelProviderAgent = ArCoTLModelProviderAgent.getArCoTLModelProviderAgent(dataRepository, additionalConfigs, null,
                codeConfiguration);
        arDoCo.addPipelineStep(arCoTLModelProviderAgent);
        totalAgentsExecuted++;
        logger.debug("Added ArCoTLModelProviderAgent");

        // ADDED: Create LLM agent with live mode configuration
        LLMArchitectureProviderAgent llmArchitectureProviderAgent = new LLMArchitectureProviderAgent(dataRepository, largeLanguageModel,
                documentationExtractionPrompt, codeExtractionPrompt, codeFeatures, aggregationPrompt);
        
        // ADDED: Configure the LLM agent with live mode settings
        llmArchitectureProviderAgent.setUseLiveLLM(useLiveLLM);
        llmArchitectureProviderAgent.setCurrentRun(currentRun);
        
        arDoCo.addPipelineStep(llmArchitectureProviderAgent);
        totalAgentsExecuted++;
        logger.info("Added LLMArchitectureProviderAgent with model: {}", largeLanguageModel.name());

        arDoCo.addPipelineStep(TextExtraction.get(additionalConfigs, dataRepository));
        totalAgentsExecuted++;
        logger.debug("Added TextExtraction");

        arDoCo.addPipelineStep(RecommendationGenerator.get(additionalConfigs, dataRepository));
        totalAgentsExecuted++;
        logger.debug("Added RecommendationGenerator");

        arDoCo.addPipelineStep(ConnectionGenerator.get(additionalConfigs, dataRepository));
        totalAgentsExecuted++;
        logger.debug("Added ConnectionGenerator");

        arDoCo.addPipelineStep(SamCodeTraceabilityLinkRecovery.get(additionalConfigs, dataRepository));
        totalAgentsExecuted++;
        logger.debug("Added SamCodeTraceabilityLinkRecovery");

        arDoCo.addPipelineStep(SadSamCodeTraceabilityLinkRecovery.get(additionalConfigs, dataRepository));
        totalAgentsExecuted++;
        logger.debug("Added SadSamCodeTraceabilityLinkRecovery");

        logger.info("Pipeline defined with {} agents", totalAgentsExecuted);
    }

    // ADDED: Method to run pipeline with statistics
    @Override
    public void run() {
        logger.info("========================================");
        logger.info("Starting ArDoCo Pipeline Execution");
        logger.info("========================================");
        logger.info("Project: {}", currentProjectName);
        logger.info("Model: {}", currentModel != null ? currentModel.name() : "unknown");
        logger.info("Live LLM Mode: {}", useLiveLLM);
        logger.info("Run: {}/{}", currentRun, totalRuns);
        logger.info("========================================");
        
        long executionStartTime = System.currentTimeMillis();
        
        try {
            super.run();
            pipelineEndTime = System.currentTimeMillis();
            long executionDuration = pipelineEndTime - executionStartTime;
            
            logger.info("========================================");
            logger.info("Pipeline Execution Complete");
            logger.info("========================================");
            logger.info("Total execution time: {} ms ({} seconds)", 
                executionDuration, executionDuration / 1000.0);
            logger.info("Total agents executed: {}", totalAgentsExecuted);
            logger.info("Average time per agent: {} ms", executionDuration / totalAgentsExecuted);
            logger.info("========================================");
            
        } catch (Exception e) {
            logger.error("Pipeline execution failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    // ADDED: Method to get execution statistics
    public String getStatistics() {
        long totalDuration = pipelineEndTime - pipelineStartTime;
        return String.format(
            "Project: %s, Model: %s, LiveLLM: %s, Run: %d/%d, Duration: %d ms, Agents: %d",
            currentProjectName,
            currentModel != null ? currentModel.name() : "unknown",
            useLiveLLM,
            currentRun,
            totalRuns,
            totalDuration,
            totalAgentsExecuted
        );
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

    public LargeLanguageModel getCurrentModel() {
        return currentModel;
    }

    public String getCurrentProjectName() {
        return currentProjectName;
    }

    // ADDED: Setter for live LLM mode (for programmatic configuration)
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
}
