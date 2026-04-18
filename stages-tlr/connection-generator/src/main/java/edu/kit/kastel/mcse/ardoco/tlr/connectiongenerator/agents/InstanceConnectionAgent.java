/* Licensed under MIT 2026-2027. */
package edu.kit.kastel.mcse.ardoco.tlr.connectiongenerator.agents;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import edu.kit.kastel.mcse.ardoco.tlr.connectiongenerator.informants.InstantConnectionInformant;
import edu.kit.kastel.mcse.ardoco.core.data.DataRepository;
import edu.kit.kastel.mcse.ardoco.core.pipeline.agent.PipelineAgent;
import edu.kit.kastel.mcse.ardoco.core.pipeline.informant.Informant;

/**
 * This connector finds names of model instance in recommended instances.
 * Enhanced version with configurable informant behavior and filtering.
 */
public class InstanceConnectionAgent extends PipelineAgent {
    
    private static final String DEFAULT_AGENT_NAME = "InstanceConnectionAgent";
    private final ConnectionStrategy strategy;
    
    /**
     * Create the agent with default settings.
     *
     * @param dataRepository the {@link DataRepository}
     */
    public InstanceConnectionAgent(DataRepository dataRepository) {
        this(dataRepository, ConnectionStrategy.STANDARD);
    }
    
    /**
     * Create the agent with a specific connection strategy.
     *
     * @param dataRepository the {@link DataRepository}
     * @param strategy the connection strategy to use
     */
    public InstanceConnectionAgent(DataRepository dataRepository, ConnectionStrategy strategy) {
        super(List.of(createInformant(dataRepository, strategy)), 
              DEFAULT_AGENT_NAME + "-" + strategy.name(), 
              dataRepository);
        this.strategy = strategy;
    }
    
    private static Informant createInformant(DataRepository dataRepository, ConnectionStrategy strategy) {
        var informant = new InstantConnectionInformant(dataRepository);
        
        switch (strategy) {
            case STRICT:
                informant.setStrictMatching(true);
                break;
            case FUZZY:
                informant.setFuzzyMatching(true);
                informant.setSimilarityThreshold(0.7);
                break;
            case CASE_INSENSITIVE:
                informant.setCaseInsensitive(true);
                break;
            case STANDARD:
            default:
                // Use default settings
                break;
        }
        
        return informant;
    }
    
    /**
     * Connection strategy enumeration for different matching behaviors.
     */
    public enum ConnectionStrategy {
        /** Standard matching behavior */
        STANDARD,
        /** Strict exact matching only */
        STRICT,
        /** Fuzzy matching with similarity threshold */
        FUZZY,
        /** Case-insensitive matching */
        CASE_INSENSITIVE
    }
    
    public ConnectionStrategy getStrategy() {
        return strategy;
    }
}
