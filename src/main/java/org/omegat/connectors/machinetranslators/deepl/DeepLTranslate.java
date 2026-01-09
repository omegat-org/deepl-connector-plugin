/*
 *  OmegaT - Computer Assisted Translation (CAT) tool
 *           with fuzzy matching, translation memory, keyword search,
 *           glossaries, and translation leveraging into updated projects.
 *
 *  Copyright (C) 2010 Alex Buloichik, Didier Briel
 *                2011 Briac Pilpre, Alex Buloichik
 *                2013 Didier Briel
 *                2016 Aaron Madlon-Kay
 *                2021-2023 Hiroshi Miura
 *                2025 Hiroshi Miura
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

package org.omegat.connectors.machinetranslators.deepl;

import java.awt.Window;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.omegat.core.Core;
import org.omegat.core.data.ProjectProperties;
import org.omegat.core.machinetranslators.BaseCachedTranslate;
import org.omegat.core.machinetranslators.BaseTranslate;
import org.omegat.core.machinetranslators.MachineTranslateError;
import org.omegat.gui.exttrans.MTConfigDialog;
import org.omegat.util.HttpConnectionUtils;
import org.omegat.util.Language;

/**
 * Support of DeepL machine translation.
 *
 * @author Alex Buloichik (alex73mail@gmail.com)
 * @author Didier Briel
 * @author Briac Pilpre
 * @author Aaron Madlon-Kay
 * @author Hiroshi Miura
 * @author Yong Choi
 *
 * @see <a href="https://developers.deepl.com/docs/">DeepL API Documentation</a>
 */
public class DeepLTranslate extends BaseCachedTranslate {

    public static final String ALLOW_DEEPL_TRANSLATE = "allow_deepl_translate";

    protected static final String PROPERTY_API_KEY = "deepl.api.key";
    private static final String BUNDLE_BASENAME = "org.omegat.machinetranslators.deepl.DeepLBundle";
    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle(BUNDLE_BASENAME);

    protected static final String DEEPL_URL = "https://api.deepl.com";
    protected static final String DEEPL_URL_FREE = "https://api-free.deepl.com";
    private static final String TRANSLATE_PATH = "/v2/translate";

    protected String temporaryKey;

    /*
     * Register plugins into OmegaT.
     */
    @SuppressWarnings("unused")
    public static void loadPlugins() {
        Core.registerMachineTranslationClass(DeepLTranslate.class);
    }

    @SuppressWarnings("unused")
    public static void unloadPlugins() {}

    @SuppressWarnings("unused")
    public DeepLTranslate() {
        this(null);
    }

    public DeepLTranslate(String tmpKey) {
        this.temporaryKey = tmpKey;
    }

    @Override
    protected String getPreferenceName() {
        return ALLOW_DEEPL_TRANSLATE;
    }

    @Override
    public String getName() {
        return BUNDLE.getString("MT_ENGINE_DEEPL");
    }

    @Override
    protected String translate(Language sLang, Language tLang, String text) throws MachineTranslateError {
        String cached = getCachedTranslation(sLang, tLang, text);
        if (cached != null) {
            return cached;
        }

        String apiKey = resolveApiKey();
        String baseUrl = resolveBaseUrl(apiKey);

        Map<String, String> params = buildRequestParams(sLang, tLang, text);

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "DeepL-Auth-Key " + apiKey);

        String response;
        try {
            response = HttpConnectionUtils.post(baseUrl + TRANSLATE_PATH, params, headers);
        } catch (IOException e) {
            throw new MachineTranslateError(BUNDLE.getString("DEEPL_CONNECTION_ERROR"));
        }

        String cleaned = parseAndCleanResponse(response, text);

        putToCache(sLang, tLang, text, cleaned);

        return cleaned;
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public void showConfigurationUI(Window parent) {
        MTConfigDialog dialog = new MTConfigDialog(parent, getName()) {
            @Override
            protected void onConfirm() {
                String key = panel.valueField1.getText().trim();
                boolean temporary = panel.temporaryCheckBox.isSelected();
                setCredential(PROPERTY_API_KEY, key, temporary);
            }
        };

        dialog.panel.valueLabel1.setText(BUNDLE.getString("MT_ENGINE_DEEPL_API_KEY_LABEL"));
        dialog.panel.valueField1.setText(getCredential(PROPERTY_API_KEY));

        dialog.panel.valueLabel2.setVisible(false);
        dialog.panel.valueField2.setVisible(false);

        dialog.panel.temporaryCheckBox.setSelected(isCredentialStoredTemporarily(PROPERTY_API_KEY));

        dialog.show();
    }

    private String resolveApiKey() throws MachineTranslateError {
        String apiKey = getCredential(PROPERTY_API_KEY);
        if (apiKey == null || apiKey.isEmpty()) {
            if (temporaryKey == null || temporaryKey.isEmpty()) {
                throw new MachineTranslateError(BUNDLE.getString("DEEPL_API_KEY_NOTFOUND"));
            }
            apiKey = temporaryKey;
        }
        return apiKey;
    }

    protected String resolveBaseUrl(String apiKey) {
        return apiKey.endsWith(":fx") ? DEEPL_URL_FREE : DEEPL_URL;
    }

    private Map<String, String> buildRequestParams(Language sLang, Language tLang, String text)
            throws MachineTranslateError {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("text", text);

        String targetLang = toDeepLLanguage(tLang, true);
        if (targetLang == null) {
            throw new MachineTranslateError(BUNDLE.getString("DEEPL_CONNECTION_ERROR"));
        }
        params.put("target_lang", targetLang);

        String sourceLang = toDeepLLanguage(sLang, false);
        if (sourceLang != null) {
            params.put("source_lang", sourceLang);
        }

        ProjectProperties props = getProjectProperties();
        if (props != null && !props.isSentenceSegmentingEnabled()) {
            params.put("split_sentences", "0");
        }

        return params;
    }

    private String parseAndCleanResponse(String response, String originalText) throws MachineTranslateError {
        try {
            String translatedText = extractTranslationText(response);
            String unescaped = BaseTranslate.unescapeHTML(translatedText);
            return cleanSpacesAroundTags(unescaped, originalText);
        } catch (IllegalArgumentException ex) {
            throw new MachineTranslateError(BUNDLE.getString("DEEPL_CONNECTION_ERROR"));
        }
    }

    protected ProjectProperties getProjectProperties() {
        return Core.getProject().getProjectProperties();
    }

    private static String toDeepLLanguage(Language language, boolean allowRegion) {
        if (language == null) {
            return null;
        }
        String languageCode = language.getLanguageCode();
        if (languageCode == null || languageCode.isEmpty()) {
            return null;
        }
        StringBuilder builder = new StringBuilder(languageCode.toUpperCase(Locale.ENGLISH));
        if (allowRegion) {
            String countryCode = language.getCountryCode();
            if (countryCode != null && !countryCode.isEmpty()) {
                builder.append('-').append(countryCode.toUpperCase(Locale.ENGLISH));
            }
        }
        return builder.toString();
    }

    private static final Pattern TEXT_PATTERN = Pattern.compile("\"text\"\\s*:\\s*\"((?:\\\\.|[^\\\\\"])*)\"");

    private static String extractTranslationText(String response) {
        Matcher matcher = TEXT_PATTERN.matcher(response);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Missing translation text");
        }
        return decodeJsonString(matcher.group(1));
    }

    private static String decodeJsonString(String value) {
        StringBuilder builder = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '\\') {
                if (++i >= value.length()) {
                    throw new IllegalArgumentException("Invalid escape sequence");
                }
                char escape = value.charAt(i);
                switch (escape) {
                    case '\"':
                    case '\\':
                    case '/':
                        builder.append(escape);
                        break;
                    case 'b':
                        builder.append('\b');
                        break;
                    case 'f':
                        builder.append('\f');
                        break;
                    case 'n':
                        builder.append('\n');
                        break;
                    case 'r':
                        builder.append('\r');
                        break;
                    case 't':
                        builder.append('\t');
                        break;
                    case 'u':
                        if (i + 4 >= value.length()) {
                            throw new IllegalArgumentException("Invalid unicode escape");
                        }
                        String hex = value.substring(i + 1, i + 5);
                        try {
                            builder.append((char) Integer.parseInt(hex, 16));
                        } catch (NumberFormatException ex) {
                            throw new IllegalArgumentException("Invalid unicode escape", ex);
                        }
                        i += 4;
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported escape sequence");
                }
            } else {
                builder.append(ch);
            }
        }
        return builder.toString();
    }
}
