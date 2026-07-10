# Repository Guidelines & Agent Instructions

## Overview

This document provides comprehensive guidelines for repository management, development workflows, and collaboration standards for Maven-based Java projects. It serves as a reference for both human developers and AI agents working with this codebase.

## 🏛️ Repository Structure & Organization

### Project Layout

```
project-root/
├── .github/                          # GitHub workflows and automation
│   ├── ISSUE_TEMPLATE/               # Issue templates
│   ├── workflows/                    # CI/CD pipeline definitions
├── .husky/                           # Git hooks
├── .mvn/                             # Maven wrapper
├── docker/                           # Docker configs for local dev
│   ├── dbgate/                       # Database UI
│   ├── grafana/                      # Observability dashboards
│   ├── postgres/                     # Database init scripts
│   ├── prometheus/                   # Metrics config
├── docs/                             # Architecture, deployment, API docs
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/iqkv/foundation/billingservice/
│   │   │       ├── gateway/          # Payment gateway integration (commands, events, ports, REST resources)
│   │   │       ├── infrastructure/   # Infrastructure layer
│   │   │       │   ├── config/       # Spring configuration, properties classes
│   │   │       │   ├── messaging/    # RabbitMQ event consumers/publishers
│   │   │       │   ├── mybatis/      # MyBatis type handlers
│   │   │       │   ├── persistence/  # MyBatis mapper interfaces
│   │   │       │   ├── security/     # Security filters, config
│   │   │       ├── plan/             # Plan management bounded context
│   │   │       ├── settings/         # Billing settings bounded context
│   │   │       ├── shared/           # Shared kernel (exceptions, utils)
│   │   │       ├── subscription/     # Subscription management bounded context
│   │   │       ├── tenancy/          # Multi-tenancy support
│   │   │       ├── userbilling/      # User billing settings bounded context
│   │   │       ├── webhook/          # Payment gateway webhook handlers
│   │   │       └── BillingServiceApplication.java  # Main class
│   │   └── resources/
│   │       ├── META-INF/
│   │       │   └── spring-configuration-metadata.json
│   │       ├── db/changelog/         # Liquibase migrations
│   │       ├── i18n/                 # Internationalization
│   │       ├── keys/                 # JWT keys
│   │       ├── mappers/              # MyBatis XML mappers
│   │       ├── templates/email/      # Email templates
│   │       ├── application.yml       # Base configuration
│   │       ├── application-local.yml # Local dev
│   │       ├── application-sit.yml   # SIT
│   │       ├── application-uat.yml   # UAT
│   │       ├── application-prd.yml   # Production
│   │       ├── banner.txt
│   │       └── logback-spring.xml
│   └── test/
│       └── java/com/iqkv/foundation/billingservice/
│           ├── domain/               # Domain tests
│           ├── gateway/              # Gateway tests
│           ├── infrastructure/       # Infrastructure tests
│           ├── plan/                 # Plan tests
│           ├── settings/             # Settings tests
│           ├── subscription/         # Subscription tests
│           ├── userbilling/          # User billing tests
│           ├── webhook/              # Webhook tests
│           ├── BillingServiceApplicationTests.java
│           ├── IntegrationTest.java
│           └── TechnicalStructureTest.java
├── .gitignore
├── pom.xml                           # Maven build config
├── README.md
├── AGENTS.md                         # This file
```

## 🤖 AI Agent Guidelines

### AI Communication Standards

**CRITICAL: Agents must communicate concisely and avoid unnecessary verbosity.**

#### Response Length Guidelines

```yaml
summaries:
    max_length: "2-3 sentences"
    focus: "What was done, not how it was done"
    avoid: "Bullet point lists, detailed recaps, obvious statements"

explanations:
    when_detailed: "Complex technical concepts, architecture decisions, security implications"
    when_brief: "Simple changes, routine operations, status updates"

verification:
    format: "Minimal wording - state outcome only"
    example: "Tests pass. Coverage at 90%."
    avoid: "Lengthy descriptions of what was verified"
```

#### Prohibited Outputs

**NEVER create these files:**

- ❌ `SUMMARY.md` - Auto-generated work summaries
- ❌ `CHANGES.md` - Change documentation (use commit messages)
- ❌ `REVIEW.md` - Code review summaries
- ❌ `ANALYSIS.md` - Analysis reports (communicate directly)
- ❌ Any markdown file documenting agent work unless explicitly requested

**Why:** These files create noise, clutter the repository, and provide no value. Use commit messages and direct communication instead.

#### Concise vs Verbose Examples

**❌ VERBOSE (Don't do this):**

```
I have successfully completed the implementation of the new user authentication
endpoint. Here's a comprehensive summary of everything I did:

• Created a new UserAuthenticationController class
• Implemented the login method with proper validation
• Added JWT token generation logic
• Created comprehensive unit tests
• Updated the OpenAPI documentation

All tests are passing successfully and the code is ready for review.
```

**✅ CONCISE (Do this):**

```
Added user authentication endpoint with JWT token generation. Tests pass, coverage at 90%.
```

#### When to Be Detailed

Provide detailed explanations ONLY for:

1. **Complex Architecture Decisions**
2. **Security Implications**
3. **Breaking Changes**
4. **Non-Obvious Technical Choices**

#### Response Templates

**For Simple Changes:**

```
Changed X to Y. Tests pass.
```

**For Bug Fixes:**

```
Fixed [issue]. Root cause: [brief explanation]. Added regression test.
```

**For New Features:**

```
Implemented [feature]. Includes [key components]. Tests pass, coverage [X]%.
```

**For Refactoring:**

```
Refactored [component] to [improvement]. No behavior changes. Tests pass.
```

#### Communication Principles

1. **Action-Oriented**: Focus on what was done, not the process
2. **Results-First**: State the outcome immediately
3. **No Redundancy**: Don't repeat what's obvious from the code
4. **No Meta-Commentary**: Don't describe your own actions
5. **Trust the User**: They can read code; don't explain obvious changes
6. **Verification is Brief**: "Tests pass" is sufficient

### Technology Stack Context

Before making recommendations, agents should understand the project's technology stack:

**Runtime & Framework**

- Java 25 with modern features (records, pattern matching, text blocks, var)
- Spring Boot 3.x
- Maven for build management

**Data & Persistence**

- Database: PostgreSQL
- MyBatis for data access
- Liquibase for database migrations

**Messaging & Integration**

- RabbitMQ for event-driven architecture
- Stripe and Lemon Squeezy payment gateways
- ShedLock for distributed locking

**Observability**

- Spring Boot Actuator
- Prometheus for metrics
- Grafana for dashboards
- Logback with Logstash encoder

**Security**

- Spring Security
- OAuth2 Resource Server (JWT)
- Multi-tenancy support via foundation-tenancy library

**Testing**

- JUnit 5 for unit and integration tests
- Mockito for mocking
- Testcontainers (PostgreSQL and RabbitMQ)
- ArchUnit for architecture validation
- JaCoCo for code coverage (90% threshold)

**API Documentation**

- SpringDoc OpenAPI 3
- Swagger UI

**Build & Deployment**

- Maven 3.9.0+
- Docker
- Environment profiles: local, sit, uat, prd

## 📋 Development Standards

### Branch Strategy

```
main (production-ready code)
├── develop (main development branch)
├── feature/* (new features)
├── bugfix/* (bug fixes)
├── improvement/* (enhancements)
├── hotfix/* (production fixes)
└── rfc/* (request for comments)
```

### Branch Naming Conventions

```bash
# Feature branches
feature/add-user-authentication
feature/implement-caching

# Bug fixes
bugfix/fix-null-pointer-exception
bugfix/resolve-connection-leak

# Improvements
improvement/optimize-database-queries
improvement/enhance-error-messages

# Hotfixes
hotfix/critical-security-patch

# RFC (Request for Comments)
rfc/new-authentication-flow
```

### Commit Message Format (Conventional Commits)

**Format:**

```
type(scope): subject

[optional body]

[optional footer]
```

**Allowed Types:**

- `feat`: New feature
- `fix`: Bug fix
- `rfc`: Request for comments / architectural proposal
- `docs`: Documentation changes
- `style`: Code style changes
- `improvement`: Enhancements to existing features
- `refactor`: Code refactoring
- `perf`: Performance improvements
- `test`: Adding or updating tests
- `chore`: Maintenance tasks
- `build`: Build system changes
- `ci`: CI/CD pipeline changes
- `revert`: Reverting previous commits

**Rules:**

- Subject line: 6-220 characters
- Use lowercase for type
- Use imperative mood ("add" not "added")
- No period at end of subject line

**Examples:**

```bash
feat(auth): add JWT authentication endpoint

fix(user): resolve null pointer in user service

The service was not checking for null values before
accessing user properties.

Closes #123

perf(database): optimize user search query with indexes

docs(readme): update installation instructions

refactor(service): extract validation logic to separate class
```

### AI Commit Message Generation

**When AI Should Generate Commit Messages:**

AI agents should automatically generate and present commit messages after:

- Completing multi-file changes (3+ files modified)
- Implementing new features or bug fixes
- Performing refactoring across multiple components
- Making configuration or infrastructure changes

**AI Workflow for Commit Message Generation:**

```yaml
after_completing_changes:
    1. analyze_changes: "Review all modified files and understand the scope"
    2. identify_type: "Determine the appropriate commit type"
    3. determine_scope: "Identify the affected component"
    4. craft_subject: "Write concise subject line (6-220 chars)"
    5. add_body_if_needed: "Include body for complex changes"
    6. present_to_user: "Show the generated commit message for review"
    7. wait_for_approval: "User can accept, modify, or reject"
```

**Example Presentation:**

```
I've completed the changes. Here's the suggested commit message:

---
feat(auth): add JWT authentication endpoint

Implements JWT-based authentication with token generation
and validation. Includes rate limiting and comprehensive tests.
---

Would you like me to use this commit message, or would you prefer to modify it?
```

### Code Quality Standards

#### Java Code Standards (Java 25)

**Use Modern Java Features:**

```java
// Records for immutable DTOs
public record UserDto(@NotBlank String username, @Email String email, Instant createdAt) {}

// Pattern matching with switch expressions
public String getUserRole(User user) {
  return switch (user.getRole()) {
    case ADMIN -> "Administrator";
    case USER -> "Regular User";
    default -> "Unknown";
  };
}

// Text blocks for multi-line strings
var query = """
  SELECT u FROM User u
  WHERE u.email = :email
  AND u.enabled = true
  """;

// var for local variables (when type is obvious)
var users = userRepository.findAll();
```

**Import Order (Checkstyle Enforced):**

```java
// 1. Static imports (alphabetically sorted)
import static org.assertj.core.api.Assertions.assertThat;

// 2. Standard Java/Jakarta packages (alphabetically sorted)
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
// 3. Third-party packages (alphabetically sorted)
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
```

**Import Rules:**

```yaml
import_formatting:
    - "Separate each group with a blank line"
    - "Sort imports alphabetically within each group"
    - "No wildcard imports (import java.util.*) - use explicit imports"
    - "No unused imports"
    - "Package and import statements must not be line-wrapped"

checkstyle_modules:
    AvoidStarImport:
        description: "Prohibits wildcard imports (import java.util.*)"
        enforcement: "Build fails on star imports"
        rationale: "Explicit imports improve code clarity and prevent naming conflicts"

    UnusedImports:
        description: "Detects and removes unused import statements"
        enforcement: "Build fails on unused imports"
```

*_REST Resource Pattern (named *RestResource):*_

```java
@RestController
@RequestMapping("/api/v1/plans")
@RequiredArgsConstructor
@Tag(name = "Plan Management")
public class PlanCatalogRestResource {

  private final PlanCatalogService planCatalogService;

  @GetMapping
  @Operation(summary = "Get all active plans")
  public ResponseEntity<List<PublicPlanEntry>> getActivePlans() {
    return ResponseEntity.ok(planCatalogService.getActivePlans());
  }
}
```

**Service Layer Pattern:**

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class PlanCatalogService {

  private final PlanMapper planMapper;

  public List<PublicPlanEntry> getActivePlans() {
    return planMapper.findAllActive().stream()
        .map(plan -> new PublicPlanEntry(plan.getPlanCode(), plan.getDisplayName(), plan.getPriceMinor(), plan.getCurrency()))
        .toList();
  }
}
```

**MyBatis Mapper Pattern:**

```java
@Mapper
public interface PlanMapper {
  List<Plan> findAllActive();
  Optional<Plan> findByPlanCode(String planCode);
  void insert(Plan plan);
  void update(Plan plan);
}
```

#### Testing Standards

**Unit Tests - AAA Pattern:**

```java
@ExtendWith(MockitoExtension.class)
class PlanCatalogServiceTest {

  @Mock
  private PlanMapper planMapper;

  @InjectMocks
  private PlanCatalogService planCatalogService;

  @Test
  @DisplayName("Should return active plans")
  void shouldReturnActivePlans() {
    // Arrange
    var plan = new Plan();
    plan.setPlanCode("pro-monthly");
    plan.setDisplayName("Pro Monthly");
    plan.setPriceMinor(999);
    plan.setCurrency("USD");
    plan.setActive(true);
    when(planMapper.findAllActive()).thenReturn(List.of(plan));

    // Act
    var result = planCatalogService.getActivePlans();

    // Assert
    assertThat(result).hasSize(1);
    assertThat(result.get(0).planCode()).isEqualTo("pro-monthly");
  }
}
```

**Integration Tests with Testcontainers:**

```java
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class PlanCatalogServiceIntegrationTest {

  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

  @Container
  static RabbitMQContainer rabbitMQ = new RabbitMQContainer("rabbitmq:3.12-alpine");

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("spring.rabbitmq.host", rabbitMQ::getHost);
    registry.add("spring.rabbitmq.port", rabbitMQ::getAmqpPort::toString);
    registry.add("spring.rabbitmq.username", rabbitMQ::getAdminUsername);
    registry.add("spring.rabbitmq.password", rabbitMQ::getAdminPassword);
  }

  @Autowired
  private PlanCatalogService planCatalogService;

  @Test
  void shouldCreateAndRetrievePlan() {
    // Test implementation
  }
}
```

### Maven Command Best Practices for AI Agents

**STRICT RECOMMENDATION: Always use `-Dcheckstyle.skip=true` when running Maven commands during development.**

```yaml
maven_commands:
    development_phase:
        recommended: "mvn clean verify -Dcheckstyle.skip=true"
        reason: "Focus on functionality and tests without style blocking"

    testing_phase:
        recommended: "mvn test -Dcheckstyle.skip=true"
        reason: "Rapid test iteration without style checks"

    style_check_phase:
        explicit: "mvn checkstyle:check"
        when: "Before committing or when explicitly requested"

workflow:
    1. develop: "Implement features with -Dcheckstyle.skip=true"
    2. test: "Run tests with -Dcheckstyle.skip=true"
    3. verify: "Ensure functionality works correctly"
    4. style: "Run mvn checkstyle:check separately"
    5. fix_style: "Address Checkstyle violations in focused pass"
    6. commit: "CI/CD enforces Checkstyle automatically"

rationale:
    - "Checkstyle violations should not block functional development"
    - "Style issues are better addressed in dedicated cleanup phase"
    - "CI/CD pipeline enforces style checks before merge"
    - "Faster iteration cycle for AI-assisted development"
```

**Example Commands:**

```bash
# ✅ RECOMMENDED: Development and testing
mvn clean verify -Dcheckstyle.skip=true
mvn test -Dcheckstyle.skip=true
mvn clean install -Dcheckstyle.skip=true

# ✅ RECOMMENDED: Explicit style check when ready
mvn checkstyle:check

# ❌ NOT RECOMMENDED: Running verify without skip during active development
mvn clean verify  # May fail due to style issues, blocking progress
```

## 🔄 Workflow Management

### Pull Request Guidelines

#### PR Title Format

PR titles must follow Conventional Commits format:

```
type(scope): description

Examples:
feat(auth): add JWT authentication
fix(user): resolve null pointer exception
docs(readme): update installation guide
refactor(service): extract validation logic
```

#### PR Description Template

```markdown
## Description

Brief description of changes and motivation.

## Type of Change

- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update
- [ ] Performance improvement
- [ ] Refactoring

## Changes Made

- List specific changes
- Include affected components

## How Has This Been Tested?

- [ ] Unit tests added/updated
- [ ] Integration tests added/updated
- [ ] Manual testing performed

## Test Coverage

- Current coverage: X%
- Coverage change: +/-X%

## Checklist

- [ ] Code follows style guidelines
- [ ] Commit messages follow Conventional Commits
- [ ] Self-review completed
- [ ] Tests cover new/modified code
- [ ] All tests pass locally
- [ ] Documentation updated

## Security Considerations

- [ ] No sensitive data in logs
- [ ] Input validation implemented
- [ ] Authorization checks in place

## Additional Notes
```

#### Review Criteria

**Automated Checks (Must Pass):**

- ✅ All tests pass
- ✅ Checkstyle validation passes
- ✅ Code coverage meets minimum threshold (90%)
- ✅ Commit messages follow Conventional Commits
- ✅ No merge conflicts

**Manual Review Focus:**

```yaml
review_checklist:
    code_quality:
        - Modern Java features used appropriately
        - Proper exception handling
        - No code duplication

    security:
        - Input validation with @Valid
        - No SQL injection vulnerabilities (MyBatis parameterized queries)
        - No sensitive data exposure

    testing:
        - AAA pattern in unit tests
        - Edge cases covered
        - Integration tests for critical paths

    documentation:
        - OpenAPI annotations on endpoints
        - Complex logic documented
        - README updated if needed
```

## 🔒 Security Guidelines

### Security Checklist

```yaml
authentication:
  - [ ] Secure authentication mechanism implemented (OAuth2 Resource Server)
  - [ ] JWT validation
  - [ ] Token expiration configured
  - [ ] Account lockout protection

authorization:
  - [ ] Role-based access control
  - [ ] Method-level security with @PreAuthorize
  - [ ] User can only access own data (multi-tenancy)

input_validation:
  - [ ] Bean Validation annotations (@Valid, @NotBlank)
  - [ ] SQL injection prevention (MyBatis parameterized queries)
  - [ ] XSS prevention
  - [ ] Request size limits configured

data_protection:
  - [ ] Passwords never logged
  - [ ] Sensitive data encrypted
  - [ ] HTTPS enforced in production
  - [ ] Secure headers configured

secrets_management:
  - [ ] No hardcoded secrets
  - [ ] Environment variables for configuration
  - [ ] .env files gitignored
```

### Security Testing

```java
@Test
void shouldRejectInvalidToken() {
  var invalidToken = "invalid.token";
  assertThrows(AuthenticationException.class, () -> authService.validateToken(invalidToken));
}

@Test
@WithMockUser(roles = "USER")
void shouldDenyAccessToAdminEndpoint() throws Exception {
  mockMvc.perform(get("/api/v1/admin/users")).andExpect(status().isForbidden());
}
```

## 📊 Quality Assurance

### Code Quality Metrics

```yaml
test_coverage:
    minimum_threshold: ">= 90%"
    target: ">= 95%"

code_style:
    tool: "Checkstyle"
    enforcement: "Maven build fails on violations"

architecture:
    tool: "ArchUnit"
    rules:
        - "Proper package structure"
        - "No cyclic dependencies"
```

### Quality Gates

```bash
# Run all quality checks locally
mvn clean verify

# Run only tests
mvn test

# Check code coverage (JaCoCo)
mvn jacoco:report
# View: target/site/jacoco/index.html

# Check code style
mvn checkstyle:check

# Run architecture tests
mvn test -Dtest=TechnicalStructureTest
```

## 🚀 CI/CD Pipeline

### GitHub Actions Workflows

The project has existing workflows in `.github/workflows`:

- `auto-approve-dependabot-pr.yml`
- `build-nodejs-project.yml`
- `check-commit-message.yml`
- `check-markdown-links.yml`
- `check-pr-title.yml`
- `use-template.yml`

## 📚 Documentation Standards

### API Documentation with OpenAPI

```java
@RestController
@RequestMapping("/api/v1/plans")
@Tag(name = "Plan Management", description = "Plan operations")
public class PlanCatalogRestResource {

  @GetMapping("/{planCode}")
  @Operation(summary = "Get plan by code", description = "Retrieves a plan by its code")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Plan found"),
    @ApiResponse(responseCode = "404", description = "Plan not found")
  })
  public ResponseEntity<Plan> getPlanByCode(@Parameter(description = "Plan code", example = "pro-monthly") @PathVariable String planCode) {
    return ResponseEntity.ok().build();
  }
}
```

### Code Documentation

**When to Document:**

- Complex business logic
- Non-obvious algorithms
- Security-critical code
- Public APIs

**When NOT to Document:**

- Self-explanatory code
- Simple getters/setters
- Obvious implementations

## 🎯 Agent Decision Framework

### User Confirmation Policy

**CRITICAL RULE: Always ask for user confirmation before applying changes.**

```yaml
before_making_changes:
    1. analyze: "Understand the request and identify required changes"
    2. explain: "Describe what changes will be made and why"
    3. assess_impact: "Evaluate impact, effort, and risk"
    4. present_options: "Offer alternatives if applicable"
    5. wait_for_approval: "STOP and wait for explicit user confirmation"
    6. apply_changes: "Only after user approves"
    7. verify: "Confirm changes work as expected"

exceptions:
    - read_only_operations: "Reading files, searching, analyzing"
    - information_requests: "Answering questions, explaining concepts"
    - recommendations: "Suggesting approaches without implementing"

never_auto_apply:
    - code_changes: "Any modification to source files"
    - configuration_changes: "application.yml, pom.xml, etc."
    - dependency_updates: "Adding or updating dependencies"
    - refactoring: "Code restructuring"
    - deletions: "Removing files or code"
    - security_changes: "Authentication, authorization, secrets"
```

### When to Intervene

**High Priority (Immediate Action - Still Requires Approval):**

- 🚨 Security vulnerabilities
- 🔴 Build failures blocking development
- 💥 Critical bugs affecting core functionality
- ⚠️ Authentication/authorization failures

**Medium Priority (Plan and Execute - Requires Approval):**

- 📉 Code quality degradation
- 🐌 Performance regressions
- ❌ Test failures
- 📦 Dependency updates

**Low Priority (Continuous Improvement - Requires Approval):**

- 📝 Documentation gaps
- ♻️ Refactoring opportunities
- 🎨 Code style improvements
- 🧪 Test coverage improvements

### Decision Matrix

```yaml
impact_assessment:
    critical: "Service unavailable, data loss risk"
    high: "Major feature broken, workaround exists"
    medium: "Minor feature affected"
    low: "Internal improvement, no user impact"

effort_estimation:
    small: "< 1 day"
    medium: "1-3 days"
    large: "1-2 weeks"
    xlarge: "> 2 weeks"

risk_evaluation:
    low: "Well-understood change, easy rollback"
    medium: "New pattern, tested rollback"
    high: "Complex change, difficult rollback"
    critical: "Breaking change, irreversible"
```

### Common Patterns and Solutions

**Pattern: Adding a new REST endpoint**

```java
// 1. Define DTO
public record CreatePlanRequest(@NotBlank String planCode, @NotBlank String displayName) {}

// 2. Add service method
@Service
public class PlanService {

  public Plan createPlan(CreatePlanRequest request) {
    /* ... */
  }
}

// 3. Add REST resource endpoint
@RestController
public class PlanRestResource {

  @PostMapping
  @Operation(summary = "Create plan")
  public ResponseEntity<Plan> createPlan(@Valid @RequestBody CreatePlanRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(planService.createPlan(request));
  }
}

// 4. Add tests
@Test
void shouldCreatePlan() {
  /* ... */
}
```

**Pattern: Adding database migration (Liquibase)**

```xml
<changeSet id="20260710120000" author="developer">
  <createTable tableName="plans">
    <column name="id" type="UUID">
      <constraints primaryKey="true"/>
    </column>
    <column name="plan_code" type="varchar(100)">
      <constraints nullable="false" unique="true"/>
    </column>
    <column name="created_at" type="timestamp"
            defaultValueComputed="CURRENT_TIMESTAMP"/>
  </createTable>
</changeSet>
```

**Pattern: Adding MyBatis mapper**

1. Create Java interface in `infrastructure/persistence`:

```java
@Mapper
public interface PlanMapper {
  void insert(Plan plan);
  Optional<Plan> findByPlanCode(String planCode);
  List<Plan> findAll();
}
```

2. Create XML mapper in `resources/mappers`:

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
  PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
  "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.iqkv.foundation.billingservice.infrastructure.persistence.PlanMapper">

  <resultMap id="PlanResultMap" type="com.iqkv.foundation.billingservice.plan.Plan">
    <id property="id" column="id"/>
    <result property="planCode" column="plan_code"/>
    <result property="displayName" column="display_name"/>
    <result property="createdAt" column="created_at"/>
  </resultMap>

  <insert id="insert" parameterType="com.iqkv.foundation.billingservice.plan.Plan">
    INSERT INTO plans (id, plan_code, display_name, created_at)
    VALUES (#{id}, #{planCode}, #{displayName}, #{createdAt})
  </insert>

  <select id="findByPlanCode" resultMap="PlanResultMap">
    SELECT * FROM plans WHERE plan_code = #{planCode}
  </select>

  <select id="findAll" resultMap="PlanResultMap">
    SELECT * FROM plans
  </select>
</mapper>
```

**Pattern: Adding RabbitMQ event listener**

```java
@Component
@RequiredArgsConstructor
public class TenantEventConsumer {

  private final PlanService planService;

  @RabbitListener(queues = "${iqkv.messaging.rabbitmq.tenant-events-queue}")
  public void handleTenantCreatedEvent(TenantCreatedEvent event) {
    log.info("Handling tenant created event: {}", event.getTenantId());
    // Process event
  }
}
```

---

This document serves as a comprehensive reference for maintaining high-quality, secure, and well-organized Java projects while facilitating effective collaboration between human developers and AI agents.
