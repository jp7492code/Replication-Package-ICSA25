# Replication Package for "Enabling Architecture Traceability by LLM-based Architecture Component Name Extraction"

**by Johnathan Pham, Max Nanasy, Psy**

---

## Quickstart

### Docker (Easiest)
```bash
docker run -it --rm ghcr.io/ardoco/icsa25
mvn -q test -Dsurefire.failIfNoSpecifiedTests=false -Dtest=TraceLinkEvaluationSadSamViaLlmCodeIT
Local (with Maven)
bash
git clone https://github.com/jp7492code/Replication-Package-ICSA25.git
cd Replication-Package-ICSA25
mvn -q test -Dsurefire.failIfNoSpecifiedTests=false -Dtest=TraceLinkEvaluationSadSamViaLlmCodeIT
The test takes 25-30 minutes on a MacBook Air M2 using cached responses.

Requirements
Component	Minimum	Recommended
OS	macOS 12+, Linux (Ubuntu 20.04+), Windows 10+	macOS 13+, Ubuntu 22.04+, Windows 11
CPU	4 cores	8+ cores
RAM	8 GB	16+ GB
Storage	50 GB free space	100+ GB free space
Java	JDK 17	JDK 17 or 21
Maven	3.9+	3.9+
Installation
macOS (Apple Silicon M1/M2/M3)
bash
# Install Homebrew
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# Install Java and Maven
brew install openjdk@17 maven

# Set JAVA_HOME
echo 'export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"' >> ~/.zshrc
echo 'export JAVA_HOME="/opt/homebrew/opt/openjdk@17"' >> ~/.zshrc
source ~/.zshrc

# Clone repository
git clone https://github.com/jp7492code/Replication-Package-ICSA25.git
cd Replication-Package-ICSA25
macOS (Intel)
bash
# Install Homebrew
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# Install Java and Maven
brew install openjdk@17 maven

# Set JAVA_HOME
echo 'export JAVA_HOME="/usr/local/opt/openjdk@17"' >> ~/.zshrc
source ~/.zshrc

# Clone repository
git clone https://github.com/jp7492code/Replication-Package-ICSA25.git
cd Replication-Package-ICSA25
Linux (Ubuntu/Debian)
bash
# Install Java and Maven
sudo apt update
sudo apt install openjdk-17-jdk maven git -y

# Clone repository
git clone https://github.com/jp7492code/Replication-Package-ICSA25.git
cd Replication-Package-ICSA25
Windows (PowerShell as Administrator)
powershell
# Install Chocolatey
Set-ExecutionPolicy Bypass -Scope Process -Force
iex ((New-Object System.Net.WebClient).DownloadString('https://chocolatey.org/install.ps1'))

# Install Java, Maven, Git
choco install openjdk17 maven git -y

# Clone repository
git clone https://github.com/jp7492code/Replication-Package-ICSA25.git
cd Replication-Package-ICSA25
Environment Variables
Variable	Purpose	Default
OPENAI_API_KEY	OpenAI API key for GPT models	sk-DUMMY
OPENAI_ORG_ID	OpenAI organization ID (optional)	empty
OLLAMA_HOST	Host for Ollama local LLMs	http://localhost:11434
MAVEN_OPTS	Maven memory options	-Xmx4g
Running the Evaluation
Using Cached Results (No API Calls, No Cost)
bash
export OPENAI_API_KEY="sk-DUMMY"
mvn -q test -Dsurefire.failIfNoSpecifiedTests=false -Dtest=TraceLinkEvaluationSadSamViaLlmCodeIT
Using Docker
bash
docker run -it --rm ghcr.io/ardoco/icsa25
mvn -q test -Dsurefire.failIfNoSpecifiedTests=false -Dtest=TraceLinkEvaluationSadSamViaLlmCodeIT
With GPU Support (Docker)
bash
docker run -it --rm --gpus all ghcr.io/ardoco/icsa25
Running Specific Tests
bash
# Run only for MediaStore
mvn test -Dtest=TraceLinkEvaluationSadSamViaLlmCodeIT#testMediaStore

# Verbose output
mvn test -Dsurefire.failIfNoSpecifiedTests=false -Dtest=TraceLinkEvaluationSadSamViaLlmCodeIT
Live LLM Support
To use live LLM API calls (incurs costs):

Step 1: Set API Keys
bash
# For OpenAI models
export OPENAI_API_KEY="sk-proj-your-actual-key"

# For local Ollama models
brew install ollama
ollama serve
export OLLAMA_HOST="http://localhost:11434"
Step 2: Remove Cache
bash
rm -rf cache-llm
Step 3: Run with Live Flag
bash
mvn test -Dtest=TraceLinkEvaluationSadSamViaLlmCodeIT -Dlive=true
Step 4: Multiple Iterations for Statistics
bash
mvn test -Dtest=TraceLinkEvaluationSadSamViaLlmCodeIT -Druns=5
Repository Structure
Directory	Description
pipeline-tlr/	Core source code for TransArC and LLM integration
tests/	Test classes for evaluation
cache-llm/	Cached LLM requests/responses in JSON format
results/	Evaluation results in human-readable logging format
stages-tlr/	Pipeline stage configurations
Key Classes
Class	Purpose
TraceLinkEvaluationSadSamViaLlmCodeIT	Main test class for TLR evaluation
LLMArchitectureProviderInformant	Core logic for extracting component names via LLMs
LargeLanguageModel	Enum defining supported LLMs
LLMStatisticsCollector	Collects statistics across multiple runs
Results Format
Each evaluation produces output like:

text
Evaluating project MEDIASTORE with LLM 'GPT_4_O_MINI'
MEDIASTORE (SadSamViaLlmCodeTraceabilityLinkRecoveryEvaluation):
    Precision:    0.49
    Recall:       0.52
    F1:           0.50
    Accuracy:     0.99
Statistics Summary (Multiple Runs)
text
========== LLM STATISTICS SUMMARY ==========
--- MEDIASTORE:GPT-4o (5 runs) ---
F1:        mean=0.8623, std=0.0234, min=0.8234, max=0.8912
95% CI:    [0.8478, 0.8768]
✅ Variation: LOW - Results are stable
=============================================
Extending the Package
Adding New LLMs
Edit LargeLanguageModel.java:

java
public enum LargeLanguageModel {
    GPT_4_O("gpt-4o", "OpenAI GPT-4o", "https://api.openai.com/v1"),
    
    // ADD NEW MODELS:
    GPT_4_1("gpt-4.1", "OpenAI GPT-4.1", "https://api.openai.com/v1"),
    LLAMA_3_3_70B("llama3.3:70b", "Meta Llama 3.3 70B", "http://localhost:11434"),
    LLAMA_4_70B("llama4:70b", "Meta Llama 4 70B", "http://localhost:11434"),
    ;
}
Adding New Projects
Edit CodeProject.java:

java
public enum CodeProject {
    MEDIASTORE, TEASTORE, TEAMMATES, BIGBLUEBUTTON, JABREF,
    YOUR_NEW_PROJECT,  // ADD NEW PROJECTS HERE
}
Custom Pipeline Execution
java
ArDoCoForSadSamViaLlmCodeTraceabilityLinkRecovery runner = 
    new ArDoCoForSadSamViaLlmCodeTraceabilityLinkRecovery("MyProject");

runner.setUp(
    inputTextFile,           // SAD text file
    inputCodeDirectory,      // Source code directory
    additionalConfigs,       // Map of configs
    outputDirectory,         // Output directory
    LargeLanguageModel.GPT_4_1,  // LLM to use
    documentationPrompt,     // Prompt for doc extraction (or null)
    codeExtractionPrompt,    // Prompt for code extraction (or null)
    codeFeatures,            // Code features (or null)
    aggregationPrompt        // Aggregation prompt (or null)
);

runner.run();
Non-determinism in LLM Results
LLMs are inherently non-deterministic. Mitigation strategies:

Run multiple iterations (recommended: 5-10 runs)

Report statistics (mean, standard deviation, confidence intervals)

Use cached responses for exact replication

Expected Variation Range
Metric	Typical Variation
Precision	±0.02-0.05
Recall	±0.01-0.03
F1	±0.02-0.04
Troubleshooting
Error	Solution
mvn: command not found	brew install maven (macOS) or apt install maven (Linux)
Java not found	Install Java 17: brew install openjdk@17
JAVA_HOME not set	export JAVA_HOME=$(/usr/libexec/java_home -v 17)
OutOfMemoryError	export MAVEN_OPTS="-Xmx4g"
OPENAI_API_KEY not set	Set key or use sk-DUMMY for cached mode
Docker permission denied	sudo usermod -aG docker $USER (Linux)
Tests take too long	mvn test -DskipSlowTests=true
Citation
bibtex
@inproceedings{pham2025replication,
  title={Replication Package for "Enabling Architecture Traceability by LLM-based Architecture Component Name Extraction"},
  author={Pham, Johnathan and Nanasy, Max and Psy},
  booktitle={ICSA 2025 Replication Package},
  year={2025},
  url={https://github.com/jp7492code/Replication-Package-ICSA25}
}
License
MIT License

Acknowledgments
This replication package is forked from the original ICSA25 replication package (DOI: 10.5281/zenodo.14506935) and extended with:

Live LLM support (GPT-4.1, Llama 3.3, Llama 4, Mistral Large 2)

Multiple run statistics (mean, std dev, confidence intervals)

Nondeterminism handling (variation analysis)

Cost tracking for API calls

Enhanced prompt strategies

Contact
Open an issue: https://github.com/jp7492code/Replication-Package-ICSA25/issues

text

---

## How to Save

```bash
cd ~/Downloads/Replication-Package-ICSA25
nano README.md
Then:

Delete all existing content

Copy and paste the entire markdown above

Ctrl+O, Enter, Ctrl+X to save and exit

Then commit:

bash
git add README.md
git commit -m "Update README with comprehensive documentation"
git push origin main
