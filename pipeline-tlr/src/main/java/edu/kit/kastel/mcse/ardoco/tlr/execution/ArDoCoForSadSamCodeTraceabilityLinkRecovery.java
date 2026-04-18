/* Licensed under MIT 2023-2024. */
package edu.kit.kastel.mcse.ardoco.tlr.execution;

import java.io.File;
import java.util.SortedMap;

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
    
    // ADDED: Support for multiple runs and statistics
    private static final int DEFAULT_NUM_RUNS = 5;
    private static boolean USE_LIVE_LLM = true;
    private static int currentRun = 1;

    public ArDoCoForSadSamViaLlmCodeTraceabilityLinkRecovery(String projectName) {
        super(projectName);
    }

    public void setUp(File inputText, File inputCode, SortedMap<String, String> additionalConfigs, File outputDir, LargeLanguageModel largeLanguageModel,
            LLMArchitecturePrompt documentationExtractionPrompt, LLMArchitecturePrompt codeExtractionPrompt, LLMArchitecturePrompt.Features codeFeatures,
            LLMArchitecturePrompt aggregationPrompt) {
        
        // ADDED: Check for live LLM mode
        if (additionalConfigs.containsKey("useLiveLLM")) {
            USE_LIVE_LLM = Boolean.parseBoolean(additionalConfigs.get("useLiveLLM"));
        }
        
        // ADDED: Check for current run number
        if (additionalConfigs.containsKey("currentRun")) {
            currentRun = Integer.parseInt(additionalConfigs.get("currentRun"));
        }
        
        definePipeline(inputText, inputCode, additionalConfigs, largeLanguageModel, documentationExtractionPrompt, codeExtractionPrompt, codeFeatures,
                aggregationPrompt);
        setOutputDirectory(outputDir);
        isSetUp = true;
    }

    private void definePipeline(File inputText, File inputCode, SortedMap<String, String> additionalConfigs, LargeLanguageModel largeLanguageModel,
            LLMArchitecturePrompt documentationExtractionPrompt, LLMArchitecturePrompt codeExtractionPrompt, LLMArchitecturePrompt.Features codeFeatures,
            LLMArchitecturePrompt aggregationPrompt) {
        ArDoCo arDoCo = this.getArDoCo();
        var dataRepository = arDoCo.getDataRepository();

        var text = CommonUtilities.readInputText(inputText);
        if (text.isBlank()) {
            throw new IllegalArgumentException("Cannot deal with empty input text. Maybe there was an error reading the file.");
        }
        DataRepositoryHelper.putInputText(dataRepository, text);
        
        // ADDED: Log current run information
        org.slf4j.LoggerFactory.getLogger(ArDoCoForSadSamViaLlmCodeTraceabilityLinkRecovery.class)
            .info("=== Starting run {}/{} with LLM: {} (Live mode: {}) ===", 
                  currentRun, DEFAULT_NUM_RUNS, largeLanguageModel.name(), USE_LIVE_LLM);

        arDoCo.addPipelineStep(TextPreprocessingAgent.get(additionalConfigs, dataRepository));

        var codeConfiguration = ArCoTLModelProviderAgent.getCodeConfiguration(inputCode);

        ArCoTLModelProviderAgent arCoTLModelProviderAgent = ArCoTLModelProviderAgent.getArCoTLModelProviderAgent(dataRepository, additionalConfigs, null,
                codeConfiguration);
        arDoCo.addPipelineStep(arCoTLModelProviderAgent);

        // ADDED: Pass live LLM flag to the agent
        LLMArchitectureProviderAgent llmArchitectureProviderAgent = new LLMArchitectureProviderAgent(dataRepository, largeLanguageModel,
                documentationExtractionPrompt, codeExtractionPrompt, codeFeatures, aggregationPrompt);
        llmArchitectureProviderAgent.setUseLiveLLM(USE_LIVE_LLM);
        llmArchitectureProviderAgent.setCurrentRun(currentRun);
        arDoCo.addPipelineStep(llmArchitectureProviderAgent);

        arDoCo.addPipelineStep(TextExtraction.get(additionalConfigs, dataRepository));
        arDoCo.addPipelineStep(RecommendationGenerator.get(additionalConfigs, dataRepository));
        arDoCo.addPipelineStep(ConnectionGenerator.get(additionalConfigs, dataRepository));

        arDoCo.addPipelineStep(SamCodeTraceabilityLinkRecovery.get(additionalConfigs, dataRepository));
        arDoCo.addPipelineStep(SadSamCodeTraceabilityLinkRecovery.get(additionalConfigs, dataRepository));
    }
    
    // ADDED: Setter methods for configuration
    public static void setUseLiveLLM(boolean useLive) {
        USE_LIVE_LLM = useLive;
    }
    
    public static void setNumRuns(int numRuns) {
        // This would require changes to the test class
        System.setProperty("test.runs", String.valueOf(numRuns));
    }
}