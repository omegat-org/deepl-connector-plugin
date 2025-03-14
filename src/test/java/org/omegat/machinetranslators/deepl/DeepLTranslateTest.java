/*
 *  OmegaT - Computer Assisted Translation (CAT) tool
 *           with fuzzy matching, translation memory, keyword search,
 *           glossaries, and translation leveraging into updated projects.
 *
 *  Copyright (C) 2021,2023 Hiroshi Miura
 *                Home page: https://www.omegat.org/
 *                Support center: https://omegat.org/support
 *
 *  This file is part of OmegaT.
 *
 *  OmegaT is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  OmegaT is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.omegat.machinetranslators.deepl;

import static net.javacrumbs.jsonunit.JsonMatchers.jsonEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omegat.core.data.ProjectProperties;
import org.omegat.core.machinetranslators.MachineTranslateError;
import org.omegat.util.Language;
import org.omegat.util.Preferences;
import org.omegat.util.PreferencesImpl;
import org.omegat.util.PreferencesXML;
import org.omegat.util.RuntimePreferences;

@WireMockTest
public class DeepLTranslateTest {

    private File tmpDir;

    /**
     * Prepare a temporary directory.
     * @throws IOException when I/O error.
     */
    @BeforeEach
    public final void setUp() throws IOException {
        tmpDir = Files.createTempDirectory("omegat").toFile();
        Assertions.assertTrue(tmpDir.isDirectory());
        File prefsFile = new File(tmpDir, Preferences.FILE_PREFERENCES);
        Preferences.IPreferences prefs = new PreferencesImpl(new PreferencesXML(null, prefsFile));
        prefs.setPreference(DeepLTranslate.ALLOW_DEEPL_TRANSLATE, true);
        RuntimePreferences.setConfigDir(prefsFile.getAbsolutePath());
        Preferences.init();
        Preferences.initFilters();
        Preferences.initSegmentation();
    }

    /**
     * Clean up a temporary directory.
     * @throws IOException when I/O error.
     */
    @AfterEach
    public final void tearDown() throws IOException {
        FileUtils.deleteDirectory(tmpDir);
    }

    @Test
    void testCreateRequest() throws MachineTranslateError {
        DeepLTranslate deepLTranslate = new DeepLTranslateTestStub("", "key");
        String text = "Hello";
        Language sLang = new Language("EN");
        Language tLang = new Language("FR");
        Map<String, String> params = deepLTranslate.createRequest(sLang, tLang, text);
        String json = deepLTranslate.createJson(params);
        assertThat(
                json,
                jsonEquals("{\"text\":\"Hello\",\"target_lang\":\"FR\",\"source_lang\":\"EN\","
                        + "\"split_sentences\":\"0\",\"preserve_formatting\":\"1\",\"tag_handling\":\"xml\"}"));
    }

    @Test
    void testGetJsonResults() throws Exception {
        DeepLTranslate deepLTranslate = new DeepLTranslateTestStub();
        String json = "{ \"translations\": [ { \"detected_source_language\": \"DE\", \"text\": \"Hello World!\" } ] }";
        String result = deepLTranslate.getJsonResults(json);
        assertEquals("Hello World!", result);
    }

    @Test
    void testGetJsonResultsWithWrongJson() {
        DeepLTranslate deepLTranslate = new DeepLTranslateTestStub();
        String json = "{ \"response\": \"failed\" }";
        assertThrows(Exception.class, () -> deepLTranslate.getJsonResults(json));
    }

    @Test
    void testResponse(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        String key = "deepl8api8key";

        WireMock.stubFor(WireMock.post(WireMock.urlPathEqualTo("/v1/translate"))
                .withHeader("Authorization", WireMock.equalTo("DeepL-Auth-Key " + key))
                .withRequestBody(WireMock.matchingJsonPath("$.text", WireMock.equalTo("source text")))
                .withRequestBody(WireMock.matchingJsonPath("$.source_lang", WireMock.equalTo("DE")))
                .withRequestBody(WireMock.matchingJsonPath("$.target_lang", WireMock.equalTo("EN")))
                .withRequestBody(WireMock.matchingJsonPath("$.tag_handling", WireMock.equalTo("xml")))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{ \"translations\":[ "
                                + "{ \"detected_source_language\": \"DE\", \"text\": \"Hello World!\" }"
                                + " ] }")));
        WireMock.stubFor(
                WireMock.get(WireMock.anyUrl()).willReturn(WireMock.aResponse().withStatus(404)));

        int port = wireMockRuntimeInfo.getHttpPort();
        String url = String.format("http://localhost:%d", port);
        String sourceText = "source text";
        DeepLTranslate deepLTranslate = new DeepLTranslateTestStub(url, key);
        String result = deepLTranslate.translate(new Language("DE"), new Language("EN"), sourceText);
        assertEquals("Hello World!", result);
    }

    static class DeepLTranslateTestStub extends DeepLTranslate {

        DeepLTranslateTestStub() {
            super("", "key");
        }

        DeepLTranslateTestStub(String url, String key) {
            super(url, key);
        }

        @Override
        public ProjectProperties getProjectProperties() {
            return null;
        }
    }
}
