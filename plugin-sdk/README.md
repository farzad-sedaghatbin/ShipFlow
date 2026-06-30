# ShipFlow Plugin SDK

Extend ShipFlow with custom risk calculators, report generators, and integration providers.

## Quick start

Generate a skeleton plugin project with the Maven archetype:

```bash
mvn archetype:generate \
  -DarchetypeGroupId=com.github.farzadsedaghatbin.shipflow \
  -DarchetypeArtifactId=shipflow-plugin-archetype \
  -DarchetypeVersion=1.0.0 \
  -DgroupId=com.example \
  -DartifactId=my-shipflow-plugin \
  -Dversion=1.0.0-SNAPSHOT
```

## Available SPI interfaces

| Interface | Package | Purpose |
|-----------|---------|---------|
| `RiskCalculatorPlugin` | `com.github.farzadsedaghatbin.shipflow.plugin` | Augment AI risk scoring for pitches |
| `ReportGeneratorPlugin` | `com.github.farzadsedaghatbin.shipflow.plugin` | Generate downloadable reports (PDF, CSV, XLSX) |
| `IntegrationProviderPlugin` | `com.github.farzadsedaghatbin.shipflow.plugin` | Send/receive data from external services |

## Minimum `pom.xml`

```xml
<dependency>
  <groupId>com.github.farzadsedaghatbin.shipflow</groupId>
  <artifactId>shipflow-plugin-api</artifactId>
  <version>1.6.0</version>
  <scope>provided</scope>
</dependency>
```

## Example: custom risk calculator

```java
import com.github.farzadsedaghatbin.shipflow.plugin.RiskCalculatorPlugin;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class MyRiskCalculator implements RiskCalculatorPlugin {

    @Override
    public String getPluginId() { return "my-risk-calculator"; }

    @Override
    public String getDisplayName() { return "My Custom Risk Calculator"; }

    @Override
    public Double calculateRisk(Map<String, Object> context) {
        // context keys: pitchId, pitchTitle, appetiteDays, cycleId,
        //               taskCount, completedTaskCount, daysRemaining
        Integer daysRemaining = (Integer) context.get("daysRemaining");
        Integer taskCount     = (Integer) context.get("taskCount");
        Integer completed     = (Integer) context.get("completedTaskCount");
        if (taskCount == null || taskCount == 0) return null;
        double progress = completed.doubleValue() / taskCount;
        // Return null to abstain; return [0.0, 1.0] to vote
        return daysRemaining != null && daysRemaining < 7 && progress < 0.5 ? 0.9 : 0.2;
    }
}
```

## Deployment

Drop the plugin JAR (and any transitive dependencies) into the `plugins/` directory alongside the
ShipFlow backend JAR. The `PluginAutoConfiguration` runner discovers all Spring beans on the
classpath that implement a plugin interface and registers them automatically.

Enable/disable individual plugins at runtime from **Organization Settings → Plugins** without
restarting the application.

## Admin API

```
GET  /api/v1/admin/plugins                       — list all registered plugins
PATCH /api/v1/admin/plugins/{pluginId}/enabled   — toggle enabled state
```

Both endpoints require the `ADMIN` role.
