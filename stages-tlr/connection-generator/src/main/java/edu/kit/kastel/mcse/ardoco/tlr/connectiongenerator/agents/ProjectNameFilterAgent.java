/* Licensed under MIT 2026-2027. */
package edu.kit.kastel.mcse.ardoco.tlr.connectiongenerator.agents;

import java.util.List;

import edu.kit.kastel.mcse.ardoco.core.api.recommendationgenerator.RecommendedInstance;
import edu.kit.kastel.mcse.ardoco.tlr.connectiongenerator.informants.ProjectNameInformant;
import edu.kit.kastel.mcse.ardoco.core.data.DataRepository;
import edu.kit.kastel.mcse.ardoco.core.pipeline.agent.PipelineAgent;

/**
 * This agent should look for {@link RecommendedInstance RecommendedInstances} that contain the project's name and "filters" them by adding a heavy negative
 * probability, thus making the {@link RecommendedInstance} extremely improbable.
 * Enhanced version with configurable filtering strategies.
 */
public class ProjectNameFilterAgent extends PipelineAgent {
    
    private static final String DEFAULT_AGENT_NAME = "ProjectNameFilterAgent";
    private final FilterStrength filterStrength;
    private final boolean enableLogging;
    
    /**
     * Create the agent with default settings.
     *
     * @param dataRepository the {@link DataRepository}
     */
    public ProjectNameFilterAgent(DataRepository dataRepository) {
        this(dataRepository, FilterStrength.STANDARD, true);
    }
    
    /**
     * Create the agent with custom filter strength.
     *
     * @param dataRepository the {@link DataRepository}
     * @param filterStrength the strength of the filter
     */
    public ProjectNameFilterAgent(DataRepository dataRepository, FilterStrength filterStrength) {
        this(dataRepository, filterStrength, true);
    }
    
    /**
     * Create the agent with full customization.
     *
     * @param dataRepository the {@link DataRepository}
     * @param filterStrength the strength of the filter
     * @param enableLogging whether to enable logging
     */
    public ProjectNameFilterAgent(DataRepository dataRepository, FilterStrength filterStrength, boolean enableLogging) {
        super(List.of(createInformant(dataRepository, filterStrength, enableLogging)), 
              DEFAULT_AGENT_NAME + "-" + filterStrength.name(), 
              dataRepository);
        this.filterStrength = filterStrength;
        this.enableLogging = enableLogging;
    }
    
    private static ProjectNameInformant createInformant(DataRepository dataRepository, 
                                                        FilterStrength filterStrength, 
                                                        boolean enableLogging) {
        var informant = new ProjectNameInformant(dataRepository);
        informant.setFilterStrength(filterStrength.getProbabilityPenalty());
        informant.setEnableLogging(enableLogging);
        
        if (filterStrength.isStrict()) {
            informant.setStrictMatching(true);
        }
        
        if (filterStrength.isCaseSensitive()) {
            informant.setCaseSensitive(true);
        }
        
        return informant;
    }
    
    /**
     * Filter strength enumeration defining different penalty levels.
     */
    public enum FilterStrength {
        /** Light filtering with small penalty */
        LIGHT(-0.3, false, false),
        /** Standard filtering with moderate penalty */
        STANDARD(-0.7, false, false),
        /** Strong filtering with high penalty */
        STRONG(-0.9, false, false),
        /** Extreme filtering with maximum penalty */
        EXTREME(-1.0, false, false),
        /** Strict exact matching with high penalty */
        STRICT(-0.95, true, false),
        /** Case-sensitive matching with standard penalty */
        CASE_SENSITIVE(-0.7, false, true);
        
        private final double probabilityPenalty;
        private final boolean strict;
        private final boolean caseSensitive;
        
        FilterStrength(double probabilityPenalty, boolean strict, boolean caseSensitive) {
            this.probabilityPenalty = probabilityPenalty;
            this.strict = strict;
            this.caseSensitive = caseSensitive;
        }
        
        public double getProbabilityPenalty() {
            return probabilityPenalty;
        }
        
        public boolean isStrict() {
            return strict;
        }
        
        public boolean isCaseSensitive() {
            return caseSensitive;
        }
    }
    
    public FilterStrength getFilterStrength() {
        return filterStrength;
    }
    
    public boolean isLoggingEnabled() {
        return enableLogging;
    }
}
