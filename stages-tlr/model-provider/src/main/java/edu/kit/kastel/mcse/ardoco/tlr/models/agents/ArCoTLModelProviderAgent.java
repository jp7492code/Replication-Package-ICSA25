/* Licensed under MIT 2026-2027. */
package edu.kit.kastel.mcse.ardoco.tlr.models.agents;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.SortedMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.kit.kastel.mcse.ardoco.core.data.DataRepository;
import edu.kit.kastel.mcse.ardoco.core.pipeline.agent.Informant;
import edu.kit.kastel.mcse.ardoco.core.pipeline.agent.PipelineAgent;
import edu.kit.kastel.mcse.ardoco.tlr.models.connectors.generators.Extractor;
import edu.kit.kastel.mcse.ardoco.tlr.models.informants.ArCoTLModelProviderInformant;

/**
 * Agent that provides information from models.
 * Enhanced version with improved validation and logging.
 */
public class ArCoTLModelProviderAgent extends PipelineAgent {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ArCoTLModelProviderAgent.class);
    private static final String AGENT_NAME = "ArCoTLModelProviderAgent";
    private static final String LEGACY_CODE_MODEL_FILENAME = "codeModel.acm";
    
    private final ArchitectureConfiguration architectureConfiguration;
    private final CodeConfiguration codeConfiguration;
    
    /**
     * Instantiates a new model provider agent.
     *
     * @param dataRepository           the DataRepository
     * @param architectureConfiguration the architecture configuration
     * @param codeConfiguration         the code configuration
     * @throws IllegalArgumentException if both configurations are null
     */
    public ArCoTLModelProviderAgent(DataRepository dataRepository, 
                                    ArchitectureConfiguration architectureConfiguration, 
                                    CodeConfiguration codeConfiguration) {
        super(createInformants(dataRepository, architectureConfiguration, codeConfiguration), 
              AGENT_NAME, 
              dataRepository);
        
        this.architectureConfiguration = architectureConfiguration;
        this.codeConfiguration = codeConfiguration;
        
        validateConfigurations();
        logConfiguration();
    }
    
    private void validateConfigurations() {
        if (architectureConfiguration == null && codeConfiguration == null) {
            throw new IllegalArgumentException("At least one configuration (architecture or code) must be provided");
        }
    }
    
    private void logConfiguration() {
        LOGGER.info("Initializing {} with:", AGENT_NAME);
        if (architectureConfiguration != null) {
            LOGGER.info("  - Architecture configuration: {}", architectureConfiguration);
        }
        if (codeConfiguration != null) {
            LOGGER.info("  - Code configuration: {} (type: {})", 
                       codeConfiguration.getSource(), 
                       codeConfiguration.getType());
        }
    }
    
    private static List<Informant> createInformants(DataRepository dataRepository,
                                                    ArchitectureConfiguration architectureConfiguration,
                                                    CodeConfiguration codeConfiguration) {
        List<Informant> informants = new ArrayList<>();
        
        // Add architecture informant if provided
        if (architectureConfiguration != null && architectureConfiguration.hasExtractor()) {
            informants.add(new ArCoTLModelProviderInformant(dataRepository, architectureConfiguration.getExtractor()));
            LOGGER.debug("Added architecture informant with extractor: {}", architectureConfiguration.getExtractor().getClass().getSimpleName());
        }
        
        // Add code informants based on configuration type
        if (codeConfiguration != null) {
            addCodeInformants(dataRepository, informants, codeConfiguration);
        }
        
        if (informants.isEmpty()) {
            throw new IllegalStateException("No informants could be created from the provided configurations");
        }
        
        LOGGER.info("Created {} informant(s)", informants.size());
        return informants;
    }
    
    private static void addCodeInformants(DataRepository dataRepository, 
                                          List<Informant> informants, 
                                          CodeConfiguration codeConfiguration) {
        switch (codeConfiguration.getType()) {
            case ACM_FILE:
                var file = codeConfiguration.getCodeFile();
                if (file != null && file.exists()) {
                    informants.add(new ArCoTLModelProviderInformant(dataRepository, file));
                    LOGGER.debug("Added ACM file informant from: {}", file.getAbsolutePath());
                } else {
                    LOGGER.warn("ACM file not found: {}", file);
                }
                break;
                
            case DIRECTORY:
                var extractors = codeConfiguration.getExtractors();
                if (extractors != null && !extractors.isEmpty()) {
                    for (Extractor extractor : extractors) {
                        informants.add(new ArCoTLModelProviderInformant(dataRepository, extractor));
                        LOGGER.debug("Added directory extractor informant: {}", extractor.getClass().getSimpleName());
                    }
                } else {
                    LOGGER.warn("No extractors provided for directory configuration");
                }
                break;
                
            default:
                LOGGER.warn("Unknown code configuration type: {}", codeConfiguration.getType());
        }
    }
    
    /**
     * Factory method for creating the agent with additional configurations.
     *
     * @param dataRepository         the DataRepository
     * @param additionalConfigs      additional configuration properties
     * @param architectureConfiguration the architecture configuration
     * @param codeConfiguration         the code configuration
     * @return a configured ArCoTLModelProviderAgent instance
     */
    public static ArCoTLModelProviderAgent create(DataRepository dataRepository, 
                                                   SortedMap<String, String> additionalConfigs,
                                                   ArchitectureConfiguration architectureConfiguration, 
                                                   CodeConfiguration codeConfiguration) {
        var agent = new ArCoTLModelProviderAgent(dataRepository, architectureConfiguration, codeConfiguration);
        agent.applyConfiguration(additionalConfigs);
        return agent;
    }
    
    /**
     * Creates a code configuration from a file or directory.
     *
     * @param inputCode the input code file or directory
     * @return a CodeConfiguration instance
     * @throws IllegalArgumentException if inputCode is null
     */
    public static CodeConfiguration createCodeConfiguration(File inputCode) {
        Objects.requireNonNull(inputCode, "Code file or directory must not be null");
        
        if (inputCode.isFile()) {
            LOGGER.debug("Creating ACM_FILE configuration from: {}", inputCode.getAbsolutePath());
            return new CodeConfiguration(inputCode, CodeConfiguration.CodeConfigurationType.ACM_FILE);
        }
        
        // Legacy Support for only ACM_FILE in a directory
        File legacyModelFile = new File(inputCode, LEGACY_CODE_MODEL_FILENAME);
        if (inputCode.isDirectory() && legacyModelFile.exists()) {
            LOGGER.warn("Legacy support for ACM_FILE in directory detected. Please use the ACM_FILE directly: {}", 
                       legacyModelFile.getAbsolutePath());
            return new CodeConfiguration(legacyModelFile, CodeConfiguration.CodeConfigurationType.ACM_FILE);
        }
        
        LOGGER.debug("Creating DIRECTORY configuration from: {}", inputCode.getAbsolutePath());
        return new CodeConfiguration(inputCode, CodeConfiguration.CodeConfigurationType.DIRECTORY);
    }
    
    @Override
    protected void delegateApplyConfigurationToInternalObjects(SortedMap<String, String> additionalConfiguration) {
        // Configuration is applied to informants through the pipeline
        LOGGER.debug("Applying configuration with {} properties", additionalConfiguration.size());
        // Additional configuration can be propagated to informants if needed
    }
    
    public ArchitectureConfiguration getArchitectureConfiguration() {
        return architectureConfiguration;
    }
    
    public CodeConfiguration getCodeConfiguration() {
        return codeConfiguration;
    }
}
