/*
 *  OmegaT - Computer Assisted Translation (CAT) tool
 *           with fuzzy matching, translation memory, keyword search,
 *           glossaries, and translation leveraging into updated projects.
 *
 *  Copyright (C) 2025 Hiroshi Miura
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

import java.util.Locale;
import java.util.Map;
import org.omegat.util.Language;

/**
 * Utility class for mapping OmegaT language codes to DeepL API language codes.
 *
 * @author Hiroshi Miura
 */
public final class DeepLLanguage {

    /**
     * Maps OmegaT target language codes to DeepL API target language codes.
     * DeepL target API supports regional variants for some languages.
     */
    private static final Map<String, String> TARGET_LANG_MAP = Map.of(
            // English variants
            "EN-US", "EN-US",
            "EN-GB", "EN-GB",
            "EN", "EN-US", // Default English to US
            // Portuguese variants
            "PT-BR", "PT-BR",
            "PT-PT", "PT-PT",
            "PT", "PT-BR", // Default Portuguese to Brazilian
            // Chinese variants (DeepL uses script-based codes)
            "ZH-CN", "ZH-HANS", // Simplified Chinese
            "ZH-TW", "ZH-HANT"); // Traditional Chinese

    /**
     * Maps OmegaT source language codes to DeepL API source language codes.
     * DeepL source API normalizes regional variants to base language codes.
     */
    private static final Map<String, String> SOURCE_LANG_MAP = Map.of(
            // English variants normalize to EN
            "EN-US", "EN",
            "EN-GB", "EN",
            // Portuguese variants normalize to PT
            "PT-BR", "PT",
            "PT-PT", "PT",
            // Chinese variants normalize to ZH
            "ZH-CN", "ZH",
            "ZH-TW", "ZH",
            "ZH-HANS", "ZH",
            "ZH-HANT", "ZH");

    /**
     * Maps an OmegaT language code to a DeepL API language code using the provided mapping.
     *
     * @param language the OmegaT language object
     * @param languageMap the mapping table (source or target)
     * @return the DeepL API language code
     */
    private String mapToDeepLLanguage(Language language, Map<String, String> languageMap) {
        String key = language.getLanguage().toUpperCase(Locale.ENGLISH);
        String langCode = language.getLanguageCode();
        return languageMap.getOrDefault(key, langCode);
    }

    /**
     * Returns the DeepL API language code for target_lang API parameter.
     * @param language the OmegaT language object.
     * @return the DeepL API language code corresponding to the provided target language.
     */
    public String getTargetLanguage(Language language) {
        return mapToDeepLLanguage(language, TARGET_LANG_MAP);
    }

    /**
     * Returns the DeepL API language code for the source_lang API parameter.
     *
     * @param language the OmegaT language object.
     * @return the DeepL API language code corresponding to the provided source language.
     */
    public String getSourceLanguage(Language language) {
        return mapToDeepLLanguage(language, SOURCE_LANG_MAP);
    }
}
