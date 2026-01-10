/*
 *  Integration test for DeepL API - requires a real API key
 *  Run with: DEEPL_API_KEY=your-key ./gradlew testIntegration
 */

package org.omegat.machinetranslators.deepl;

import static org.junit.jupiter.api.Assertions.*;

import com.deepl.api.DeepLApiVersion;
import com.deepl.api.DeepLClient;
import com.deepl.api.DeepLClientOptions;
import com.deepl.api.TextResult;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Integration tests that call the real DeepL API.
 * Only runs when DEEPL_API_KEY environment variable is set.
 * Run with:
 * DEEPL_API_KEY=your-key ./gradlew testIntegration
 */
@Disabled("Integration tests require a real DeepL API key - run manually")
public class DeepLIntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeepLIntegrationTest.class);

    @Test
    @EnabledIfEnvironmentVariable(named = "DEEPL_API_KEY", matches = ".+")
    void testLiveApiCall() throws Exception {
        String apiKey = System.getenv("DEEPL_API_KEY");
        LOGGER.info("Testing with API key: " + apiKey.substring(0, 8) + "...");
        LOGGER.info("Key ends with :fx (Free API): " + apiKey.endsWith(":fx"));

        // Create client with V2 API (same as the plugin)
        DeepLClientOptions options = new DeepLClientOptions();
        options.setApiVersion(DeepLApiVersion.VERSION_2);

        DeepLClient client = new DeepLClient(apiKey, options);

        // Test translation
        String sourceText = "Hello, world!";
        LOGGER.info("Translating: \"" + sourceText + "\" from EN to DE");

        TextResult result = client.translateText(sourceText, "EN", "DE");

        LOGGER.info("Result: \"" + result.getText() + "\"");
        LOGGER.info("Detected source language: " + result.getDetectedSourceLanguage());
        LOGGER.info("Billed characters: " + result.getBilledCharacters());

        assertNotNull(result.getText());
        assertFalse(result.getText().isEmpty());
        assertTrue(result.getText().toLowerCase().contains("hallo")
                || result.getText().toLowerCase().contains("welt"));

        LOGGER.info("✅ Integration test passed!");
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "DEEPL_API_KEY", matches = ".+")
    void testUsage() throws Exception {
        String apiKey = System.getenv("DEEPL_API_KEY");

        DeepLClientOptions options = new DeepLClientOptions();
        options.setApiVersion(DeepLApiVersion.VERSION_2);

        DeepLClient client = new DeepLClient(apiKey, options);

        var usage = client.getUsage();
        LOGGER.info("API Usage: " + usage);

        var detail = usage.getCharacter();
        if (detail != null) {
            LOGGER.info("Characters used: {}/{} ", detail.getCount(), detail.getLimit());
        }

        LOGGER.info("✅ Usage check passed!");
    }
}
