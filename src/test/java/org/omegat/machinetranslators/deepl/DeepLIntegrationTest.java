/*
 *  Integration test for DeepL API - requires a real API key
 *  Run with: DEEPL_API_KEY=your-key ./gradlew test --tests "*DeepLIntegrationTest*"
 */

package org.omegat.machinetranslators.deepl;

import com.deepl.api.DeepLClient;
import com.deepl.api.DeepLClientOptions;
import com.deepl.api.DeepLApiVersion;
import com.deepl.api.TextResult;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests that call the real DeepL API.
 * Only runs when DEEPL_API_KEY environment variable is set.
 * 
 * Disabled by default for CI. Run manually with:
 * DEEPL_API_KEY=your-key ./gradlew test --tests "*DeepLIntegrationTest*"
 */
@Disabled("Integration tests require a real DeepL API key - run manually")
public class DeepLIntegrationTest {

    @Test
    @EnabledIfEnvironmentVariable(named = "DEEPL_API_KEY", matches = ".+")
    void testLiveApiCall() throws Exception {
        String apiKey = System.getenv("DEEPL_API_KEY");
        System.out.println("Testing with API key: " + apiKey.substring(0, 8) + "...");
        System.out.println("Key ends with :fx (Free API): " + apiKey.endsWith(":fx"));

        // Create client with V2 API (same as the plugin)
        DeepLClientOptions options = new DeepLClientOptions();
        options.setApiVersion(DeepLApiVersion.VERSION_2);
        
        DeepLClient client = new DeepLClient(apiKey, options);

        // Test translation
        String sourceText = "Hello, world!";
        System.out.println("Translating: \"" + sourceText + "\" from EN to DE");
        
        TextResult result = client.translateText(sourceText, "EN", "DE");
        
        System.out.println("Result: \"" + result.getText() + "\"");
        System.out.println("Detected source language: " + result.getDetectedSourceLanguage());
        System.out.println("Billed characters: " + result.getBilledCharacters());

        assertNotNull(result.getText());
        assertFalse(result.getText().isEmpty());
        assertTrue(result.getText().toLowerCase().contains("hallo") || 
                   result.getText().toLowerCase().contains("welt"));
        
        System.out.println("✅ Integration test passed!");
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "DEEPL_API_KEY", matches = ".+")
    void testUsage() throws Exception {
        String apiKey = System.getenv("DEEPL_API_KEY");
        
        DeepLClientOptions options = new DeepLClientOptions();
        options.setApiVersion(DeepLApiVersion.VERSION_2);
        
        DeepLClient client = new DeepLClient(apiKey, options);

        var usage = client.getUsage();
        System.out.println("API Usage: " + usage);
        
        if (usage.getCharacter() != null) {
            System.out.println("Characters used: " + usage.getCharacter().getCount() + 
                             " / " + usage.getCharacter().getLimit());
        }
        
        System.out.println("✅ Usage check passed!");
    }
}
