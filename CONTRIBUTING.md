# Contributing to JavaSense

Thank you for your interest in contributing to JavaSense! This document provides guidelines and instructions for contributing.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [How to Contribute](#how-to-contribute)
- [Coding Standards](#coding-standards)
- [Testing Guidelines](#testing-guidelines)
- [Pull Request Process](#pull-request-process)
- [Issue Guidelines](#issue-guidelines)

## Code of Conduct

Be respectful and professional in all interactions. We are building an inclusive community where everyone feels welcome to contribute.

## Getting Started

1. **Fork the repository** on GitHub
2. **Clone your fork** locally:
   ```bash
   git clone https://github.com/yourusername/javasense.git
   cd javasense
   ```
3. **Add upstream remote**:
   ```bash
   git remote add upstream https://github.com/original/javasense.git
   ```
4. **Create a branch** for your changes:
   ```bash
   git checkout -b feature/your-feature-name
   ```

## Development Setup

### Prerequisites

- Java 17 or higher
- Maven 3.6+
- IDE (IntelliJ IDEA, Eclipse, or VS Code recommended)

### Build the Project

```bash
cd demo
mvn clean install
```

### Run Tests

```bash
mvn test
```

### Run Examples

```bash
mvn exec:java -Dexec.mainClass="com.example.Main"
```

## How to Contribute

### Reporting Bugs

Found a bug? Please create an issue with:

- **Clear title**: Summarize the problem
- **Description**: What happened vs. what you expected
- **Steps to reproduce**: Minimal example to demonstrate the bug
- **Environment**: Java version, OS, JavaSense version
- **Stack trace** (if applicable)

**Example:**

```markdown
**Bug**: NullPointerException when loading rules from file

**Steps**:
1. Create rules.txt with empty line
2. Call `JavaSense.addRulesFromFile("rules.txt")`
3. Exception thrown

**Environment**: Java 17, Windows 11, JavaSense 1.0
```

### Suggesting Features

Have an idea? Create an issue with:

- **Use case**: Why is this feature needed?
- **Proposed solution**: How should it work?
- **Alternatives**: Other approaches you considered
- **Examples**: Code examples showing the proposed API

### Contributing Code

1. **Find or create an issue** describing what you'll work on
2. **Discuss the approach** in the issue before starting
3. **Write code** following our coding standards
4. **Add tests** for new functionality
5. **Update documentation** (JavaDoc, README, User Guide)
6. **Submit a pull request**

## Coding Standards

### Java Code Style

- **Indentation**: 4 spaces (no tabs)
- **Line length**: Max 120 characters
- **Naming**:
  - Classes: `PascalCase`
  - Methods/variables: `camelCase`
  - Constants: `UPPER_SNAKE_CASE`
- **Braces**: Opening brace on same line
  ```java
  if (condition) {
      // code
  }
  ```

### JavaDoc Requirements

All public classes, methods, and fields must have JavaDoc:

```java
/**
 * Brief description of what this method does.
 *
 * <p>Longer description with more details about behavior,
 * edge cases, and usage examples.</p>
 *
 * @param paramName description of parameter
 * @return description of return value
 * @throws ExceptionType when this exception is thrown
 */
public ReturnType methodName(ParamType paramName) {
    // implementation
}
```

### Code Organization

- **Package structure**: Keep related classes together
- **Single Responsibility**: Each class should have one clear purpose
- **DRY principle**: Don't repeat yourself
- **YAGNI**: You aren't gonna need it (don't over-engineer)

### Error Handling

- **Validate inputs**: Check for null, negative values, etc.
- **Meaningful exceptions**: Include context in error messages
- **Use logging**: Log warnings and errors appropriately

```java
public void addFact(Fact fact) {
    if (fact == null) {
        throw new IllegalArgumentException("Fact cannot be null");
    }
    if (fact.getStartTime() < 0) {
        throw new IllegalArgumentException(
            "Start time cannot be negative: " + fact.getStartTime()
        );
    }
    // ... rest of method
}
```

### Logging

Use SLF4J with appropriate log levels:

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

private static final Logger logger = LoggerFactory.getLogger(ClassName.class);

logger.debug("Detailed debug info: {}", variable);
logger.info("General information: {}", message);
logger.warn("Warning about {}", issue);
logger.error("Error occurred", exception);
```

## Testing Guidelines

### Test Coverage

- **Aim for 80%+ coverage** for new code
- **Test edge cases**: null values, empty collections, boundary conditions
- **Test error paths**: Verify exceptions are thrown correctly

### Writing Tests

Use JUnit 5:

```java
@Test
@DisplayName("Should throw exception when fact is null")
void testNullFact() {
    assertThrows(IllegalArgumentException.class, () -> {
        JavaSense.addFact(null);
    });
}

@Test
@DisplayName("Should add fact successfully")
void testAddFact() {
    Fact fact = new Fact("popular(Mary)", "test", 0, 5);
    JavaSense.addFact(fact);

    // Assertions
    assertNotNull(fact);
    assertEquals(0, fact.getStartTime());
}
```

### Test Organization

- **Unit tests**: Test individual methods in isolation
- **Integration tests**: Test interactions between components
- **Test naming**: Use descriptive names (`testAddFactWithValidInput`)
- **Test data**: Use meaningful test data, not random strings

### Running Tests Locally

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=JavaSenseTest

# Run with coverage
mvn test jacoco:report
```

## Pull Request Process

### Before Submitting

- [ ] Code builds successfully (`mvn clean install`)
- [ ] All tests pass (`mvn test`)
- [ ] New tests added for new functionality
- [ ] JavaDoc updated for public APIs
- [ ] README updated (if needed)
- [ ] No debug code or print statements left in
- [ ] Code follows style guidelines

### PR Description Template

```markdown
## Summary
Brief description of what this PR does.

## Related Issue
Closes #123

## Changes
- Added feature X
- Fixed bug Y
- Improved performance of Z

## Testing
- Added unit tests for ...
- Tested manually by ...

## Screenshots (if applicable)
[Add screenshots for UI changes]

## Checklist
- [ ] Tests pass
- [ ] Documentation updated
- [ ] No breaking changes (or documented)
```

### Review Process

1. Submit PR against `main` branch
2. Automated checks will run (tests, linting)
3. Maintainers will review (usually within 1 week)
4. Address any feedback
5. Once approved, maintainers will merge

### After Merge

- Delete your feature branch
- Update your fork:
  ```bash
  git checkout main
  git pull upstream main
  git push origin main
  ```

## Issue Guidelines

### Issue Labels

- `bug`: Something isn't working
- `enhancement`: New feature or request
- `documentation`: Documentation improvements
- `good first issue`: Good for newcomers
- `help wanted`: Extra attention needed
- `question`: Further information requested

### Working on Issues

1. **Comment on the issue** to let others know you're working on it
2. **Reference the issue** in commits: `git commit -m "Fix #123: Description"`
3. **Close the issue** in PR description: `Closes #123`

## Development Tips

### Useful Maven Commands

```bash
# Clean and rebuild
mvn clean install

# Run specific example
mvn exec:java -Dexec.mainClass="com.example.MainAdvanced"

# Skip tests (not recommended)
mvn install -DskipTests

# Generate JavaDoc
mvn javadoc:javadoc
```

### Debugging

- Use IDE debugger instead of print statements
- Check logs in `javasense.log`
- Enable debug logging in `logback.xml`:
  ```xml
  <logger name="com.example" level="DEBUG" />
  ```

### Performance Profiling

- Use JProfiler or YourKit for profiling
- Measure before optimizing
- Add performance tests for critical paths

## Need Help?

- **Questions**: Open an issue with `question` label
- **Discussions**: Use GitHub Discussions
- **Chat**: [Join our Discord/Slack] (if available)

## License

By contributing, you agree that your contributions will be licensed under the Apache License 2.0.

---

Thank you for contributing to JavaSense! 🎉
