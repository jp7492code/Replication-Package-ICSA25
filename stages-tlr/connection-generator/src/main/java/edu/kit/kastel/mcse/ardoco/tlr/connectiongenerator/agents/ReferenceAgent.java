/* Licensed under MIT 2026-2027. */
package edu.kit.kastel.mcse.ardoco.tlr.connectiongenerator.agents;

import java.util.List;

import edu.kit.kastel.mcse.ardoco.tlr.connectiongenerator.informants.ReferenceInformant;
import edu.kit.kastel.mcse.ardoco.core.data.DataRepository;
import edu.kit.kastel.mcse.ardoco.core.pipeline.agent.PipelineAgent;

/**
 * The reference solver finds instances mentioned in the text extraction state as names. 
 * If it finds some similar names it creates recommendations.
 * Enhanced version with configurable matching strategies.
 */
public class ReferenceAgent extends PipelineAgent {
    
    private static final String DEFAULT_AGENT_NAME = "ReferenceAgent";
    private final MatchingStrategy matchingStrategy;
    private final double similarityThreshold;
    
    /**
     * Create the agent with default settings.
     *
     * @param dataRepository the {@link DataRepository}
     */
    public ReferenceAgent(DataRepository dataRepository) {
        this(dataRepository, MatchingStrategy.SIMILARITY, 0.7);
    }
    
    /**
     * Create the agent with custom matching strategy.
     *
     * @param dataRepository the {@link DataRepository}
     * @param matchingStrategy the strategy for matching names
     */
    public ReferenceAgent(DataRepository dataRepository, MatchingStrategy matchingStrategy) {
        this(dataRepository, matchingStrategy, matchingStrategy.getDefaultThreshold());
    }
    
    /**
     * Create the agent with full customization.
     *
     * @param dataRepository the {@link DataRepository}
     * @param matchingStrategy the strategy for matching names
     * @param similarityThreshold the threshold for similarity matching (0.0-1.0)
     */
    public ReferenceAgent(DataRepository dataRepository, MatchingStrategy matchingStrategy, double similarityThreshold) {
        super(List.of(createInformant(dataRepository, matchingStrategy, similarityThreshold)), 
              DEFAULT_AGENT_NAME + "-" + matchingStrategy.name(), 
              dataRepository);
        this.matchingStrategy = matchingStrategy;
        this.similarityThreshold = similarityThreshold;
    }
    
    private static ReferenceInformant createInformant(DataRepository dataRepository, 
                                                       MatchingStrategy strategy, 
                                                       double threshold) {
        var informant = new ReferenceInformant(dataRepository);
        informant.setMatchingStrategy(strategy);
        informant.setSimilarityThreshold(threshold);
        
        switch (strategy) {
            case EXACT:
                informant.setExactMatching(true);
                break;
            case CASE_INSENSITIVE:
                informant.setCaseInsensitive(true);
                break;
            case CONTAINS:
                informant.setContainsMatching(true);
                break;
            case SIMILARITY:
            default:
                informant.setSimilarityThreshold(threshold);
                break;
        }
        
        return informant;
    }
    
    /**
     * Matching strategy enumeration for different name matching behaviors.
     */
    public enum MatchingStrategy {
        /** Exact string matching */
        EXACT(1.0),
        /** Case-insensitive matching */
        CASE_INSENSITIVE(0.9),
        /** Contains substring matching */
        CONTAINS(0.8),
        /** Fuzzy similarity matching */
        SIMILARITY(0.7),
        /** Levenshtein distance based matching */
        LEVENSHTEIN(0.75),
        /** Jaro-Winkler distance based matching */
        JARO_WINKLER(0.8),
        /** Hybrid approach combining multiple strategies */
        HYBRID(0.7);
        
        private final double defaultThreshold;
        
        MatchingStrategy(double defaultThreshold) {
            this.defaultThreshold = defaultThreshold;
        }
        
        public double getDefaultThreshold() {
            return defaultThreshold;
        }
    }
    
    public MatchingStrategy getMatchingStrategy() {
        return matchingStrategy;
    }
    
    public double getSimilarityThreshold() {
        return similarityThreshold;
    }
}
