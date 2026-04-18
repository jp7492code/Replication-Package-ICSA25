/* Licensed under MIT 2026-2027. */
package edu.kit.kastel.mcse.ardoco.tlr.connectiongenerator;

import java.util.EnumMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.kit.kastel.mcse.ardoco.core.api.connectiongenerator.ConnectionStates;
import edu.kit.kastel.mcse.ardoco.core.api.models.Metamodel;

public class ConnectionStatesImpl implements ConnectionStates {
    
    private static final Logger logger = LoggerFactory.getLogger(ConnectionStatesImpl.class);
    
    private final EnumMap<Metamodel, ConnectionStateImpl> connectionStates;
    
    // ADDED: Statistics tracking fields
    private long creationTime;
    private int totalAccessCount = 0;
    private boolean useLiveLLM = false;
    private int currentRun = 1;
    private String currentProject = "unknown";
    private String currentModel = "unknown";

    private ConnectionStatesImpl() {
        connectionStates = new EnumMap<>(Metamodel.class);
        creationTime = System.currentTimeMillis();
        logger.debug("ConnectionStatesImpl created at timestamp: {}", creationTime);
    }
    
    // ADDED: Configuration setter methods
    public void setUseLiveLLM(boolean useLiveLLM) {
        this.useLiveLLM = useLiveLLM;
        // Propagate to all connection states
        for (var entry : connectionStates.entrySet()) {
            entry.getValue().setUseLiveLLM(useLiveLLM);
        }
        logger.info("ConnectionStatesImpl: Live LLM mode set to {}", useLiveLLM);
    }
    
    public void setCurrentRun(int currentRun) {
        this.currentRun = currentRun;
        // Propagate to all connection states
        for (var entry : connectionStates.entrySet()) {
            entry.getValue().setCurrentRun(currentRun);
        }
        logger.debug("ConnectionStatesImpl: Current run set to {}", currentRun);
    }
    
    public void setCurrentProject(String currentProject) {
        this.currentProject = currentProject;
        // Propagate to all connection states
        for (var entry : connectionStates.entrySet()) {
            entry.getValue().setCurrentProject(currentProject);
        }
        logger.debug("ConnectionStatesImpl: Current project set to {}", currentProject);
    }
    
    public void setCurrentModel(String currentModel) {
        this.currentModel = currentModel;
        // Propagate to all connection states
        for (var entry : connectionStates.entrySet()) {
            entry.getValue().setCurrentModel(currentModel);
        }
        logger.debug("ConnectionStatesImpl: Current model set to {}", currentModel);
    }

    public static ConnectionStatesImpl build() {
        var recStates = new ConnectionStatesImpl();
        for (Metamodel mm : Metamodel.values()) {
            recStates.connectionStates.put(mm, new ConnectionStateImpl());
        }
        logger.info("ConnectionStatesImpl built with {} metamodels", Metamodel.values().length);
        return recStates;
    }
    
    // ADDED: Builder with configuration
    public static ConnectionStatesImpl buildWithConfig(boolean useLiveLLM, int currentRun, String currentProject, String currentModel) {
        var states = build();
        states.setUseLiveLLM(useLiveLLM);
        states.setCurrentRun(currentRun);
        states.setCurrentProject(currentProject);
        states.setCurrentModel(currentModel);
        return states;
    }

    @Override
    public ConnectionStateImpl getConnectionState(Metamodel mm) {
        totalAccessCount++;
        var state = connectionStates.get(mm);
        
        if (useLiveLLM && totalAccessCount % 100 == 0) {
            logger.debug("ConnectionStatesImpl access count: {} (Run: {}, Project: {}, Model: {})", 
                totalAccessCount, currentRun, currentProject, currentModel);
        }
        
        return state;
    }
    
    // ADDED: Get all connection states
    public EnumMap<Metamodel, ConnectionStateImpl> getAllConnectionStates() {
        return connectionStates;
    }
    
    // ADDED: Get total access count
    public int getTotalAccessCount() {
        return totalAccessCount;
    }
    
    // ADDED: Get creation time
    public long getCreationTime() {
        return creationTime;
    }
    
    // ADDED: Get age in milliseconds
    public long getAgeMs() {
        return System.currentTimeMillis() - creationTime;
    }
    
    // ADDED: Check if a specific metamodel has connection state
    public boolean hasConnectionState(Metamodel mm) {
        return connectionStates.containsKey(mm);
    }
    
    // ADDED: Reset all connection states
    public void resetAllStates() {
        for (var entry : connectionStates.entrySet()) {
            entry.getValue().resetStatistics();
        }
        totalAccessCount = 0;
        logger.info("ConnectionStatesImpl: All connection states reset for run {}", currentRun);
    }
    
    // ADDED: Clear all connection states
    public void clearAllStates() {
        for (var entry : connectionStates.entrySet()) {
            entry.getValue().clear();
        }
        totalAccessCount = 0;
        logger.info("ConnectionStatesImpl: All connection states cleared for run {}", currentRun);
    }
    
    // ADDED: Print statistics for all states
    public void printAllStatistics() {
        logger.info("========================================");
        logger.info("ConnectionStatesImpl Global Statistics");
        logger.info("========================================");
        logger.info("Project: {}", currentProject);
        logger.info("Model: {}", currentModel);
        logger.info("Run: {}", currentRun);
        logger.info("Live LLM Mode: {}", useLiveLLM);
        logger.info("----------------------------------------");
        logger.info("Total Access Count: {}", totalAccessCount);
        logger.info("Age: {} ms", getAgeMs());
        logger.info("Number of Metamodels: {}", connectionStates.size());
        logger.info("----------------------------------------");
        
        for (var entry : connectionStates.entrySet()) {
            logger.info("Metamodel: {}", entry.getKey());
            entry.getValue().printStatistics();
        }
        logger.info("========================================");
    }
    
    // ADDED: Get statistics as string
    public String getStatisticsString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("ConnectionStates Stats - Run: %d, Project: %s, Model: %s, AccessCount: %d, Age: %d ms\n",
            currentRun, currentProject, currentModel, totalAccessCount, getAgeMs()));
        
        for (var entry : connectionStates.entrySet()) {
            sb.append(String.format("  %s: %s\n", entry.getKey(), entry.getValue().getStatisticsString()));
        }
        return sb.toString();
    }
    
    // ADDED: Export all statistics to CSV
    public String toCSV() {
        StringBuilder sb = new StringBuilder();
        for (var entry : connectionStates.entrySet()) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append(String.format("%d,%s,%s,%s,%d,%d",
                currentRun, currentProject, currentModel, entry.getKey(),
                totalAccessCount, entry.getValue().getInstanceLinksCount()));
        }
        return sb.toString();
    }
    
    // ADDED: Get CSV header
    public static String getCSVHeader() {
        return "Run,Project,Model,Metamodel,TotalAccessCount,InstanceLinksCount";
    }
    
    // ADDED: Get aggregated statistics
    public AggregatedStats getAggregatedStats() {
        int totalInstanceLinks = 0;
        int totalAddAttempts = 0;
        int totalSuccessfulAdds = 0;
        int totalDuplicateAttempts = 0;
        int totalRemovals = 0;
        
        for (var entry : connectionStates.entrySet()) {
            var state = entry.getValue();
            totalInstanceLinks += state.getInstanceLinksCount();
            totalAddAttempts += state.getTotalAddAttempts();
            totalSuccessfulAdds += state.getSuccessfulAdds();
            totalDuplicateAttempts += state.getDuplicateAttempts();
            totalRemovals += state.getRemoveCount();
        }
        
        return new AggregatedStats(totalInstanceLinks, totalAddAttempts, totalSuccessfulAdds, 
                                   totalDuplicateAttempts, totalRemovals);
    }
    
    /**
     * Inner class for aggregated statistics.
     */
    public static class AggregatedStats {
        public final int totalInstanceLinks;
        public final int totalAddAttempts;
        public final int totalSuccessfulAdds;
        public final int totalDuplicateAttempts;
        public final int totalRemovals;
        
        public AggregatedStats(int totalInstanceLinks, int totalAddAttempts, int totalSuccessfulAdds,
                               int totalDuplicateAttempts, int totalRemovals) {
            this.totalInstanceLinks = totalInstanceLinks;
            this.totalAddAttempts = totalAddAttempts;
            this.totalSuccessfulAdds = totalSuccessfulAdds;
            this.totalDuplicateAttempts = totalDuplicateAttempts;
            this.totalRemovals = totalRemovals;
        }
        
        public double getSuccessRate() {
            if (totalAddAttempts == 0) return 0.0;
            return (double) totalSuccessfulAdds / totalAddAttempts;
        }
        
        @Override
        public String toString() {
            return String.format(
                "AggregatedStats - Links: %d, Attempts: %d, Success: %d, Duplicates: %d, Removals: %d, SuccessRate: %.2f%%",
                totalInstanceLinks, totalAddAttempts, totalSuccessfulAdds, totalDuplicateAttempts, totalRemovals, getSuccessRate() * 100
            );
        }
    }
}
