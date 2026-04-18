# Installation Instructions

This file guides you in setting up everything you need to run the Replication Package.

Everything was tested on Linux (amd64) and macOS (arm64). Windows should work as well, but we did not test it.

---

## Hardware / Service Requirements

| Requirement | Minimum | Recommended |
|-------------|---------|-------------|
| **RAM** | 8 GB | 16+ GB |
| **Storage** | 50 GB free space | 100+ GB free space |
| **CPU** | 4 cores | 8+ cores |
| **Internet** | Required for downloads | Required for API calls |

### For Live LLM Execution (New Projects)

If you want to run the LLMs with **new** projects (not using cached responses), you need:

- **Ollama instance** capable of running:
  - Llama 3.1 70B (requires ~40GB RAM)
  - Llama 3.3 70B (requires ~40GB RAM)
  - Llama 4 70B (requires ~42GB RAM)
  - Mistral Large 2 (requires ~80GB RAM)
- **OpenAI access token** and organization ID for:
  - GPT-4o
  - GPT-4.1
  - GPT-4.5

### For Replication (Using Cached Responses)

For the projects used in the paper, we provide cached LLM responses in the `cache-llm/` directory. 
**You do NOT need**:
- Ollama instance
- OpenAI access token
- Any API keys

Simply set `OPENAI_API_KEY=sk-DUMMY` to use cached responses.

---

## Prerequisites (Docker Image with all bundled dependencies)

### Install Docker

| OS | Installation Command / Link |
|----|----------------------------|
| **macOS (Apple Silicon)** | [Download Docker Desktop for ARM64](https://desktop.docker.com/mac/main/arm64/Docker.dmg) |
| **macOS (Intel)** | [Download Docker Desktop for x86_64](https://desktop.docker.com/mac/main/amd64/Docker.dmg) |
| **Linux** | `curl -fsSL https://get.docker.com -o get-docker.sh && sudo sh get-docker.sh` |
| **Windows** | [Download Docker Desktop for Windows](https://desktop.docker.com/win/main/amd64/Docker%20Desktop%20Installer.exe) |

### Verify Docker Installation

```bash
docker --version
# Expected: Docker version 24.x.x or higher
Pull the Docker Image
bash
docker pull ghcr.io/ardoco/icsa25
Run with Docker
bash
docker run -it --rm ghcr.io/ardoco/icsa25
# Inside the container, run:
mvn -q test -Dsurefire.failIfNoSpecifiedTests=false -Dtest=TraceLinkEvaluationSadSamViaLlmCodeIT
Run with GPU Support (Linux only)
bash
docker run -it --rm --gpus all ghcr.io/ardoco/icsa25
Prerequisites (Local Installation)
Java JDK 17
The project requires Java JDK 17 (not 21, as the code is compiled for Java 17).

macOS (Apple Silicon)
bash
brew install openjdk@17

# Add to PATH
echo 'export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"' >> ~/.zshrc
echo 'export JAVA_HOME="/opt/homebrew/opt/openjdk@17"' >> ~/.zshrc
source ~/.zshrc
macOS (Intel)
bash
brew install openjdk@17

# Add to PATH
echo 'export PATH="/usr/local/opt/openjdk@17/bin:$PATH"' >> ~/.zshrc
echo 'export JAVA_HOME="/usr/local/opt/openjdk@17"' >> ~/.zshrc
source ~/.zshrc
Linux (Ubuntu/Debian)
bash
# Option A: OpenJDK
sudo apt update
sudo apt install openjdk-17-jdk openjdk-17-jre -y

# Option B: Adoptium (for newer versions)
wget -O - https://packages.adoptium.net/artifactory/api/gpg/key/public | sudo apt-key add -
echo "deb https://packages.adoptium.net/artifactory/deb $(lsb_release -sc) main" | sudo tee /etc/apt/sources.list.d/adoptium.list
sudo apt update
sudo apt install temurin-17-jdk -y
Windows
powershell
# Using Chocolatey
choco install openjdk17

# Or download from: https://adoptium.net/
Verify Java Installation
bash
java -version
# Expected: openjdk version "17.0.x"
Maven 3.9+
macOS
bash
brew install maven
Linux (Ubuntu/Debian)
bash
sudo apt install maven -y
Windows
powershell
choco install maven
Verify Maven Installation
bash
mvn -version
# Expected: Apache Maven 3.9.x
Git
macOS
bash
brew install git
Linux
bash
sudo apt install git -y
Windows
powershell
choco install git
Verify Git Installation
bash
git --version
# Expected: git version 2.x.x
Optional: Live LLM Setup
OpenAI API Setup
Go to OpenAI Platform

Sign in or create an account

Click "Create new secret key"

Copy the key (starts with sk-proj-)

bash
export OPENAI_API_KEY="sk-proj-your-actual-key"
export OPENAI_ORG_ID="your-org-id"  # Optional
Ollama Setup (Local LLMs)
Install Ollama
OS	Command
macOS	brew install ollama
Linux	curl -fsSL https://ollama.com/install.sh | sh
Windows	Download from ollama.com
Start Ollama Service
bash
ollama serve
Pull Required Models
Model	Size	Command
Llama 3.1 70B	40GB	ollama pull llama3.1:70b
Llama 3.3 70B	40GB	ollama pull llama3.3:70b
Llama 4 70B	42GB	ollama pull llama4:70b
Mistral Large 2	80GB	ollama pull mistral-large:2407
Set Ollama Host
bash
export OLLAMA_HOST="http://localhost:11434"
Cloning the Repository
bash
git clone https://github.com/jp7492code/Replication-Package-ICSA25.git
cd Replication-Package-ICSA25
Running the Replication
Using Cached Results (No API Keys Required)
bash
export OPENAI_API_KEY="sk-DUMMY"
mvn -q test -Dsurefire.failIfNoSpecifiedTests=false -Dtest=TraceLinkEvaluationSadSamViaLlmCodeIT
Using Docker (No Local Setup)
bash
docker run -it --rm ghcr.io/ardoco/icsa25
mvn -q test -Dsurefire.failIfNoSpecifiedTests=false -Dtest=TraceLinkEvaluationSadSamViaLlmCodeIT
Using Live LLMs (Requires API Keys)
bash
export OPENAI_API_KEY="sk-proj-your-actual-key"
rm -rf cache-llm
mvn test -Dtest=TraceLinkEvaluationSadSamViaLlmCodeIT -Dlive=true
Running Multiple Iterations for Statistics
bash
mvn test -Dtest=TraceLinkEvaluationSadSamViaLlmCodeIT -Druns=5
Verifying Installation
Run this quick test to verify everything is working:

bash
cd Replication-Package-ICSA25
mvn clean compile
echo "✅ Compilation successful"
Then run a small test:

bash
mvn test -Dtest=TraceLinkEvaluationSadSamViaLlmCodeIT#testMediaStore
Common Issues and Solutions
Issue	Solution
mvn: command not found	Install Maven: brew install maven (macOS) or apt install maven (Linux)
Java version mismatch	Install Java 17: brew install openjdk@17 (macOS) or apt install openjdk-17-jdk (Linux)
JAVA_HOME not set	export JAVA_HOME=$(/usr/libexec/java_home -v 17) (macOS)
OutOfMemoryError	export MAVEN_OPTS="-Xmx4g"
OPENAI_API_KEY not set	Set to sk-DUMMY for cached mode, or use your actual key for live mode
Docker permission denied	sudo usermod -aG docker $USER && newgrp docker (Linux)
Cache directory not found	Normal for first run; cache will be created
Tests take too long	mvn test -DskipSlowTests=true
Uninstalling
Remove Local Installation
bash
# Remove repository
rm -rf ~/Downloads/Replication-Package-ICSA25

# Remove Maven cache (optional)
rm -rf ~/.m2/repository/edu/kit/kastel/mcse/ardoco
Remove Docker Image
bash
docker rmi ghcr.io/ardoco/icsa25
Remove Ollama (if installed)
bash
# macOS
brew uninstall ollama

# Linux
sudo apt remove ollama -y

# Remove models (optional)
rm -rf ~/.ollama
Quick Reference Card
Task	Command
Clone repo	git clone https://github.com/jp7492code/Replication-Package-ICSA25.git
Enter directory	cd Replication-Package-ICSA25
Compile	mvn clean compile
Run test (cached)	export OPENAI_API_KEY="sk-DUMMY" && mvn -q test -Dsurefire.failIfNoSpecifiedTests=false -Dtest=TraceLinkEvaluationSadSamViaLlmCodeIT
Run test (live)	export OPENAI_API_KEY="your-key" && mvn test -Dlive=true
Run with Docker	docker run -it --rm ghcr.io/ardoco/icsa25
Multiple runs	mvn test -Druns=5
View results	cat results/*.csv
Clean build	mvn clean
Support
If you encounter issues:

Check the GitHub Issues

Open a new issue with:

Your operating system and version

Java version (java -version)

Maven version (mvn -version)

Full error log

Steps to reproduce

Last Updated: April 18, 2026

text

---

## How to Save and Commit

```bash
cd ~/Downloads/Replication-Package-ICSA25
nano INSTALL.md
Then:

Delete all existing content

Copy and paste the entire markdown above

Ctrl+O, Enter, Ctrl+X to save and exit

Then commit:

bash
git add INSTALL.md
git commit -m "Update INSTALL.md with Java 17 requirement and live LLM setup instructions"
git push origin main
