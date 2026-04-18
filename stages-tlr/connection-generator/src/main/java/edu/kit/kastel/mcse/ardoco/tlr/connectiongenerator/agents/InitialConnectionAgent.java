/* Licensed under MIT 2026-2027. */
package edu.kit.kastel.mcse.ardoco.tlr.connectiongenerator.agents;

import java.util.List;
import java.util.Set;

import edu.kit.kastel.mcse.ardoco.tlr.connectiongenerator.informants.ExtractionDependentOccurrenceInformant;
import edu.kit.kastel.mcse.ardoco.tlr.connectiongenerator.informants.NameTypeConnectionInformant;
import edu.kit.kastel.mcse.ardoco.core.data.DataRepository;
import edu.kit.kastel.mcse.ardoco.core.pipeline.agent.PipelineAgent;
import edu.kit.kastel.mcse.ardoco.core.pipeline.informant.Informant;

/**
 * The agent that executes the extractors of this stage.
 * Enhanced version with configurable informants and logging.
 */
public class InitialConnectionAgent extends PipelineAgent {
    
    private static final String AGENT_NAME = "InitialConnectionAgent";
    
    /**
     * Create the agent with default informants.
     *
     * @param dataRepository the {@link DataRepository}
     */
    public InitialConnectionAgent(DataRepository dataRepository) {
        this(dataRepository, true, true);
    }
    
    /**
     * Create the agent with configurable informant activation.
     *
     * @param dataRepository the {@link DataRepository}
     * @param enableNameType whether to enable NameTypeConnectionInformant
     * @param enableExtraction whether to enable ExtractionDependentOccurrenceInformant
     */
    public InitialConnectionAgent(DataRepository dataRepository, boolean enableNameType, boolean enableExtraction) {
        super(buildInformants(dataRepository, enableNameType, enableExtraction), 
              AGENT_NAME, 
              dataRepository);
    }
    
    private static List<Informant> buildInformants(DataRepository dataRepository, 
                                                    boolean enableNameType, 
                                                    boolean enableExtraction) {
        var informants = new java.util.ArrayList<Informant>();
        
        if (enableNameType) {
            informants.add(new NameTypeConnectionInformant(dataRepository));
        }
        
        if (enableExtraction) {
            informants.add(new ExtractionDependentOccurrenceInformant(dataRepository));
        }
        
        if (informants.isEmpty()) {
            throw new IllegalArgumentException("At least one informant must be enabled");
        }
        
        return informants;
    }
}
