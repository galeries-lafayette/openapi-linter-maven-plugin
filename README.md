[![Kotlin version](https://img.shields.io/badge/Kotlin-1.4-blue)](https://kotlinlang.org/docs/reference/whatsnew14.html)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

# OPENAPI-LINTER-MAVEN-PLUGIN

![zally logo](./documentation/zally.png) ![maven logo](./documentation/maven.png)

This plugin provides oas3 schema validation using maven.
It checks configured rules on a specified rule server.

Actually supported :
- [Zally](https://github.com/zalando/zally/tree/master/server)

## Usage

In your `pom.xml`

Add repository
```xml
<repository>
  <id>github</id>
  <url>https://maven.pkg.github.com/galeries-lafayette/*</url>
</repository>
```

Add the plugin `openapi-linter-maven-plugin`
```xml
<build>
    <plugins>

        <plugin>
            <groupId>com.ggl</groupId>
            <artifactId>openapi-linter-maven-plugin</artifactId>
            <version>1.0.0</version>
            <configuration>
                <schema>/path/to/schema.yaml</schema>
                <server>http://localhost:8000</server>
                <linter>zally</linter>
                <thresholds>
                    <must>5</must>
                    <should>7</should>
                </thresholds>
                <displayViolations>true</displayViolations>
            </configuration>
            <executions>
                <execution>
                    <id>oas-linting</id>
                    <goals>
                        <goal>validate</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>

    </plugins>
</build>
```

In your `settings.xml` add github server (a personal access token can be added [here](https://github.com/settings/tokens/new), it needs at minimal `read:packages` rights)
```xml
<server>
  <id>github</id>
  <username>USER</username>
  <password>USER_ACCESS_TOKEN</password>
</server>
```

### Configuration

| Name              |      Description                       |  Required | Values                  | Default   |
|-------------------|:--------------------------------------:|----------:|------------------------:|----------:|
| schema            | The file path of the oas schema        | Y         |                         |           |
| server            | The validating server url              | Y         |                         |           |
| linter            | The linter for validation              | Y         | zally                   |           |
| thresholds        | key-value map of threshold by severity | N         | must, may, should, hint |           |
| displayViolations | Display all violations ?               | N         |                         | false     |

### Properties
__-DskipSchemaValidation__: property for disabling the schema validation

### Logs
```
[INFO] --- openapi-linter-maven-plugin:1.0.0:validate (oas-linting) @ api-customer-contract ---
[INFO] Validate plugin configuration
[INFO] Processing with thresholds (must, 5), (should, 7)
[INFO] Reading file `/Users/fabienrecco/workspace/GL/ggl-customer-api-contract/src/main/resources/api-customer.yaml`
[INFO] Process schema validation with ZALLY
[INFO] Validate schema on server http://localhost:8000
[INFO] Comparing violations to thresholds
[ERROR] MUST: 6 errors [5 maximum]
[ERROR] SHOULD: 8 errors [7 maximum]
[WARNING] MAY: 1 errors
[INFO] Violations per threshold
[WARNING] -------------------------------
[WARNING] 6 violations found for severity `MUST`:
[WARNING] #1 https://zalando.github.io/restful-api-guidelines/#219 [1 violations]
[WARNING] /info/x-audience
[WARNING]
[WARNING] #2 https://zalando.github.io/restful-api-guidelines/#215 [1 violations]
[WARNING] /info/x-api-id
[WARNING]
[WARNING] #3 https://zalando.github.io/restful-api-guidelines/#218 [4 violations]
[WARNING] /info/contact/url
[WARNING] /info/contact/name
[WARNING] /info/description
[WARNING] /info/contact/email
[ERROR] too many MUST violations: 6 is greater than 5
[WARNING] -------------------------------
[WARNING] 8 violations found for severity `SHOULD`:
[WARNING] #1 https://zalando.github.io/restful-api-guidelines/#235 [2 violations]
[WARNING] /components/schemas/CustomerContract/properties/birth_date
[WARNING] /components/schemas/KnowledgeContract/properties/last_purchase_date
[WARNING]
[WARNING] #2 https://github.com/zalando/zally/blob/master/server/rules.md#s006-define-bounds-for-numeric-properties [6 violations for 3 paths]
[WARNING] /components/schemas/KnowledgeContract/properties/number_days_store_purchases_12m
[WARNING] /components/schemas/PurchaseKpisContract/properties/net_amount_12m
[WARNING] /components/schemas/PurchaseKpisContract/properties/gfg_rate_12m
[ERROR] too many SHOULD violations: 8 is greater than 7
[INFO] ------------------------------------------------------------------------
[INFO] BUILD FAILURE
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  1.619 s
[INFO] Finished at: 2020-11-07T01:42:13+01:00
[INFO] ------------------------------------------------------------------------
[ERROR] Failed to execute goal com.ggl:openapi-linter-maven-plugin:1.0.0:validate (oas-linting) on project api-customer-contract: Schema not validated.
[ERROR] MUST: 6 errors [5 maximum]
[ERROR] SHOULD: 8 errors [7 maximum]
```

### Code format
The project contains a shared `.editorconfig`.
The code format is verified during compilation.

To apply code formatter:
```
./mvnw ktlint:format
```

More informations https://gantsign.com/ktlint-maven-plugin/usage.html
Plugin available for IntelliJ: https://plugins.jetbrains.com/plugin/15057-ktlint-unofficial-
