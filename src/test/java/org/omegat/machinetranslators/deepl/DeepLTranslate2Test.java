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

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.omegat.core.data.ProjectProperties;
import org.omegat.util.Language;
import org.omegat.util.Preferences;
import org.omegat.util.PreferencesImpl;
import org.omegat.util.PreferencesXML;
import org.omegat.util.RuntimePreferences;

@WireMockTest
public class DeepLTranslate2Test {

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
        prefs.setPreference(DeepLTranslate2.ALLOW_DEEPL_TRANSLATE, true);
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
    void testResponse(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        String key = "deepl8api8key";

        WireMock.stubFor(WireMock.post(WireMock.urlPathEqualTo("/v2/translate"))
                .withHeader("Authorization", WireMock.equalTo("DeepL-Auth-Key " + key))
                .withRequestBody(containing("text=source+text"))
                .withRequestBody(containing("source_lang=de-DE"))
                .withRequestBody(containing("target_lang=en-US"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{ \"translations\":[ "
                                + "{ \"detected_source_language\": \"DE\", \"text\": \"Hello World!\", \"billed_characters\": 11 }"
                                + " ] }")));
        WireMock.stubFor(
                WireMock.get(WireMock.anyUrl()).willReturn(WireMock.aResponse().withStatus(404)));

        int port = wireMockRuntimeInfo.getHttpPort();
        String url = String.format("http://localhost:%d", port);
        String sourceText = "source text";
        DeepLTranslate2 deepLTranslate = new DeepLTranslate2TestStub(url, key);
        String result = deepLTranslate.translate(new Language("de-DE"), new Language("en-US"), sourceText);
        assertEquals("Hello World!", result);
    }

    @Test
    void testLanguageMappingRestriction(WireMockRuntimeInfo wireMockRuntimeInfo) throws Exception {
        String key = "deepl8api8key";

        WireMock.stubFor(WireMock.post(WireMock.urlPathEqualTo("/v2/translate"))
                .withHeader("Authorization", WireMock.equalTo("DeepL-Auth-Key " + key))
                .withRequestBody(containing("text=source+text"))
                .withRequestBody(containing("source_lang=en-GB"))
                .withRequestBody(containing("target_lang=pt-BR"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{ \"translations\":[ "
                                + "{ \"detected_source_language\": \"EN-GB\", \"text\": \"Ola Mundo!\", \"billed_characters\": 11 }"
                                + " ] }")));
        WireMock.stubFor(
                WireMock.get(WireMock.anyUrl()).willReturn(WireMock.aResponse().withStatus(404)));

        int port = wireMockRuntimeInfo.getHttpPort();
        String url = String.format("http://localhost:%d", port);
        String sourceText = "source text";
        DeepLTranslate2 deepLTranslate = new DeepLTranslate2TestStub(url, key);
        String result = deepLTranslate.translate(new Language("en"), new Language("pt"), sourceText);
        assertEquals("Ola Mundo!", result);
    }

    static class DeepLTranslate2TestStub extends DeepLTranslate2 {

        DeepLTranslate2TestStub(String url, String key) {
            super(url, key);
        }

        @Override
        public ProjectProperties getProjectProperties() {
            return null;
        }
    }
}
