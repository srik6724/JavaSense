# Quick Fix for IDE Errors

## Issue
You're seeing errors in:
- `ExampleNewFeatures.java`
- `ExampleOptimization.java`
- `Neo4jReasoner.java`
- Other files using Kafka/Neo4j

## Root Cause
Maven dependencies (Kafka, Neo4j, Gson) haven't been downloaded yet. The IDE can't find these classes.

## Solution

### Option 1: Maven Install (Recommended)
```bash
cd demo
mvn clean install
```

This will:
- Download all dependencies (Kafka, Neo4j, Gson)
- Compile all source files
- Run tests
- Clear all IDE errors

### Option 2: Maven Compile Only
```bash
cd demo
mvn clean compile
```

This will download dependencies and compile without running tests.

### Option 3: IDE Refresh

**IntelliJ IDEA:**
1. Right-click on `pom.xml`
2. Select "Maven" → "Reload Project"

**VS Code:**
1. Open Command Palette (Ctrl+Shift+P / Cmd+Shift+P)
2. Type "Java: Clean Java Language Server Workspace"
3. Restart VS Code

**Eclipse:**
1. Right-click on project
2. Select "Maven" → "Update Project"

## Expected Dependencies

After running Maven, these dependencies will be downloaded:

```xml
<!-- Kafka Integration -->
org.apache.kafka:kafka-clients:3.6.1

<!-- Neo4j Integration -->
org.neo4j.driver:neo4j-java-driver:5.15.0

<!-- JSON Parsing -->
com.google.code.gson:gson:2.10.1
```

## Verify Fix

After running Maven, compile should succeed:

```bash
mvn compile
# Should show: BUILD SUCCESS
```

Then you can run examples:

```bash
# Probabilistic Reasoning
mvn exec:java -Dexec.mainClass="com.example.ExampleProbabilistic"

# Continuous Time
mvn exec:java -Dexec.mainClass="com.example.ExampleContinuousTime"

# Optimization
mvn exec:java -Dexec.mainClass="com.example.ExampleOptimization"
```

## Still Having Issues?

If errors persist after Maven install:

1. **Clean Maven cache:**
   ```bash
   mvn dependency:purge-local-repository
   mvn clean install
   ```

2. **Check Java version:**
   ```bash
   java -version
   # Should be Java 17 or higher
   ```

3. **Verify pom.xml** has the dependencies (it does - we added them in v1.1)

## What's Working

All the **core JavaSense features** work without external dependencies:
- Standard reasoning (OptimizedReasoner)
- Probabilistic reasoning (ProbabilisticReasoner) ✅
- Continuous time (ContinuousTimeReasoner) ✅
- Constraint optimization (OptimizationReasoner) ✅
- All basic examples

**Only Kafka/Neo4j integration** requires external dependencies.

## Quick Test

To verify everything works, run a v1.2 example (no external dependencies):

```bash
mvn exec:java -Dexec.mainClass="com.example.ExampleV12Combined"
```

This should work immediately after `mvn clean install`!
