/* Licensed under MIT 2026-2027. */
package edu.kit.kastel.mcse.ardoco.tlr.connectiongenerator;

import java.util.List;
import java.util.SortedMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.kit.kastel.mcse.ardoco.core.api.connectiongenerator.ConnectionStates;
import edu.kit.kastel.mcse.ardoco.tlr.connectiongenerator.agents.InitialConnectionAgent;
import edu.kit.kastel.mcse.ardoco.tlr.connectiongenerator.agents.InstanceConnectionAgent;
import edu.kit.kastel.mcse.ardoco.tlr.connectiongenerator.agents.ProjectNameFilterAgent;
import edu.kit.kastel.mcse.ardoco.tlr.connectiongenerator.agents.ReferenceAgent;
import edu.kit.kastel.mcse.ardoco.core.data.DataRepository;
import edu.kit.kastel.mcse.ardoco.core.pipeline.AbstractExecutionStage;

/**
 * The ModelConnectionAgent runs different analyzers and solvers. This agent creates recommendations as well as matchings between text and model. The order is
 * important: All connections should run after the recommendations have been made.
 * 
 * MODIFIED: Added replication support, statistics tracking, and detailed logging.
 */
public class ConnectionGenerator extends AbstractExecutionStage {
    
    private static final Logger logger = LoggerFactory.getLogger(ConnectionGenerator.class);
    
    // ADDED: Statistics tracking fields
    private long startTime;
    private long endTime;
    private int totalAgentsExecuted = 0;
    private boolean useLiveLLM = false;
    private int currentRun = 1;
    private String currentProject = "unknown";
    private String currentModel = "unknown";

    /**
     * Create the module.
     *
     * @param dataRepository the {@link DataRepository}
     */
    public ConnectionGenerator(DataRepository dataRepository) {
        super(List.of(new InitialConnectionAgent(dataRepository), 
                      new ReferenceAgent(dataRepository), 
                      new ProjectNameFilterAgent(dataRepository),
                      new InstanceConnectionAgent(dataRepository)), 
              "ConnectionGenerator", dataRepository);
        
        logger.debug("ConnectionGenerator initialized");
    }

    /**
     * Creates a {@link ConnectionGenerator} and applies the additional configuration to it.
     *
     * @param additionalConfigs the additional configuration
     * @param dataRepository    the data repository
     * @return an instance of connectionGenerator
     */
    public static ConnectionGenerator get(SortedMap<String, String> additionalConfigs, DataRepository dataRepository) {
        var connectionGenerator = new ConnectionGenerator(dataRepository);
        connectionGenerator.applyConfiguration(additionalConfigs);
        
        // ADDED: Extract configuration for tracking
        connectionGenerator.extractConfiguration(additionalConfigs);
        
        return connectionGenerator;
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
            if (additionalConfigs.containsKey("currentProject")) {
                this.currentProject = additionalConfigs.get("currentProject");
            }
            if (additionalConfigs.containsKey("currentModel")) {
                this.currentModel = additionalConfigs.get("currentModel");
            }
        }
        
        logger.info("ConnectionGenerator configured - Run: {}, Project: {}, Model: {}, LiveLLM: {}", 
            currentRun, currentProject, currentModel, useLiveLLM);
    }

    @Override
    protected void initializeState() {
        startTime = System.currentTimeMillis();
        
        logger.info("========================================");
        logger.info("Initializing ConnectionGenerator State");
        logger.info("========================================");
        logger.info("Run: {}/{}", currentRun, getTotalRuns());
        logger.info("Project: {}", currentProject);
        logger.info("Model: {}", currentModel);
        logger.info("Live LLM Mode: {}", useLiveLLM);
        logger.info("========================================");
        
        var connectionStates = ConnectionStatesImpl.build();
        getDataRepository().addData(ConnectionStates.ID, connectionStates);
        
        // ADDED: Store configuration in data repository
        getDataRepository().addData("connectionGenerator.startTime", startTime);
        getDataRepository().addData("connectionGenerator.currentRun", currentRun);
        getDataRepository().addData("connectionGenerator.currentProject", currentProject);
        getDataRepository().addData("connectionGenerator.currentModel", currentModel);
        getDataRepository().addData("connectionGenerator.useLiveLLM", useLiveLLM);
        
        logger.info("ConnectionGenerator state initialized successfully");
    }
    
    // ADDED: Override run method for statistics tracking
    @Override
    public void run() {
        logger.info("========================================");
        logger.info("Starting ConnectionGenerator Execution");
        logger.info("========================================");
        logger.info("Run: {}", currentRun);
        logger.info("Project: {}", currentProject);
        logger.info("Model: {}", currentModel);
        logger.info("========================================");
        
        long executionStart = System.currentTimeMillis();
        
        try {
            super.run();
            endTime = System.currentTimeMillis();
            long duration = endTime - executionStart;
            
            logger.info("========================================");
            logger.info("ConnectionGenerator Execution Complete");
            logger.info("========================================");
            logger.info("Duration: {} ms ({} seconds)", duration, duration / 1000.0);
            logger.info("Total agents: {}", getAgents().size());
            logger.info("========================================");
            
        } catch (Exception e) {
            logger.error("ConnectionGenerator execution failed: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    // ADDED: Method to get total runs from system property
    private int getTotalRuns() {
        try {
            return Integer.parseInt(System.getProperty("test.runs", "1"));
        } catch (NumberFormatException e) {
            return 1;
        }
    }
    
    // ADDED: Method to get execution statistics
    public String getStatistics() {
        long duration = endTime - startTime;
        return String.format(
            "ConnectionGenerator Stats - Run: %d, Project: %s, Model: %s, LiveLLM: %s, Duration: %d ms, Agents: %d",
            currentRun, currentProject, currentModel, useLiveLLM, duration, getAgents().size()
        );
    }
    
    // ADDED: Method to reset statistics
    public void resetStatistics() {
        this.startTime = 0;
        this.endTime = 0;
        this.totalAgentsExecuted = 0;
        logger.info("ConnectionGenerator statistics reset for run {}", currentRun);
    }
    
    // ADDED: Getters for configuration
    public boolean isUseLiveLLM() {
        return useLiveLLM;
    }
    
    public int getCurrentRun() {
        return currentRun;
    }
    
    public String getCurrentProject() {
        return currentProject;
    }
    
    public String getCurrentModel() {
        return currentModel;
    }
    
    public long getStartTime() {
        return startTime;
    }
    
    public long getEndTime() {
        return endTime;
    }
    
    public long getDuration() {
        return endTime - startTime;
    }
    
    // ADDED: Method to log connection statistics
    public void logConnectionStatistics() {
        var dataRepository = getDataRepository();
        var connectionStates = dataRepository.getData(ConnectionStates.ID, ConnectionStates.class);
        
        if (connectionStates != null) {
            int totalConnections = connectionStates.getConnectionStates().size();
            logger.info("Connection Statistics - Total connections: {}", totalConnections);
        } else {
            logger.warn("ConnectionStates not found in data repository");
        }
    }
}
