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

import com.deepl.api.DeepLApiVersion;
import com.deepl.api.DeepLClient;
import com.deepl.api.DeepLClientOptions;
import com.deepl.api.DeepLException;
import com.deepl.api.SentenceSplittingMode;
import com.deepl.api.TextResult;
import com.deepl.api.TextTranslationOptions;
import java.awt.Window;
import java.util.ResourceBundle;
import org.omegat.core.Core;
import org.omegat.core.data.ProjectProperties;
import org.omegat.core.machinetranslators.BaseCachedTranslate;
import org.omegat.core.machinetranslators.BaseTranslate;
import org.omegat.core.machinetranslators.MachineTranslateError;
import org.omegat.gui.exttrans.MTConfigDialog;
import org.omegat.util.Language;

/**
 * Support of DeepL machine translation.
 *
 * @author Alex Buloichik (alex73mail@gmail.com)
 * @author Didier Briel
 * @author Briac Pilpre
 * @author Aaron Madlon-Kay
 * @author Hiroshi Miura
 *
 * @see <a href="https://www.deepl.com/api.html">Translation API</a>
 * @see <a href="https://github.com/DeepLcom/deepl-java">DeepL Java library</a>
 */
public class DeepLTranslate2 extends BaseCachedTranslate {

    public static final String ALLOW_DEEPL_TRANSLATE = "allow_deepl_translate";

    protected static final String PROPERTY_API_KEY = "deepl.api.key";
    private static final String BUNDLE_BASENAME = "org.omegat.machinetranslators.deepl.DeepLBundle";
    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle(BUNDLE_BASENAME);

    protected static final String DEEPL_URL = "https://api.deepl.com";
    protected static final String DEEPL_URL_FREE = "https://api-free.deepl.com";

    protected final String deepLServerUrl;
    private String temporaryKey = null;

    /*
     * Register plugins into OmegaT.
     */
    @SuppressWarnings("unused")
    public static void loadPlugins() {
        Core.registerMachineTranslationClass(DeepLTranslate2.class);
    }

    @SuppressWarnings("unused")
    public static void unloadPlugins() {}

    @SuppressWarnings("unused")
    public DeepLTranslate2() {
        deepLServerUrl = DEEPL_URL;
    }

    public DeepLTranslate2(Boolean freeApi) {
        if (freeApi) {
            deepLServerUrl = DEEPL_URL_FREE;
        } else {
            deepLServerUrl = DEEPL_URL;
        }
    }

    /**
     * Constructor for tests.
     *
     * @param baseUrl
     *            custom base url
     * @param key
     *            temporary api key
     */
    public DeepLTranslate2(String baseUrl, String key) {
        deepLServerUrl = baseUrl;
        temporaryKey = key;
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
        String apiKey = getCredential(PROPERTY_API_KEY);
        if (apiKey == null || apiKey.isEmpty()) {
            if (temporaryKey == null) {
                throw new MachineTranslateError(BUNDLE.getString("DEEPL_API_KEY_NOTFOUND"));
            }
            apiKey = temporaryKey;
        }
        DeepLClientOptions deepLClientOptions = new DeepLClientOptions();
        deepLClientOptions.setApiVersion(DeepLApiVersion.VERSION_1).setServerUrl(deepLServerUrl);
        ProjectProperties projectProperties = getProjectProperties();
        DeepLClient client = new DeepLClient(apiKey, deepLClientOptions);

        String sourceLang = sLang.getLanguage();
        String targetLang = tLang.getLanguage();
        TextTranslationOptions textTranslationOptions = new TextTranslationOptions();

        if (projectProperties != null && projectProperties.isSentenceSegmentingEnabled()) {
            textTranslationOptions.setSentenceSplittingMode(SentenceSplittingMode.All);
        }

        TextResult result;
        try {
            result = client.translateText(text, sourceLang, targetLang, textTranslationOptions);
        } catch (DeepLException | InterruptedException e) {
            throw new MachineTranslateError(BUNDLE.getString("DEEPL_CONNECTION_ERROR"));
        }
        String tr = result.getText();
        tr = BaseTranslate.unescapeHTML(tr);
        return cleanSpacesAroundTags(tr, text);
    }

    // for test stub
    protected ProjectProperties getProjectProperties() {
        return Core.getProject().getProjectProperties();
    }

    /**
     * Engine is configurable.
     *
     * @return true
     */
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
}
