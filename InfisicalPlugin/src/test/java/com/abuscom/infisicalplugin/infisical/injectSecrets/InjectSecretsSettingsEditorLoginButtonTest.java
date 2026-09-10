package com.abuscom.infisicalplugin.infisical.injectSecrets;

import com.abuscom.infisicalplugin.infisical.injectSecrets.gradle.InjectSecretsRunConfigurationExtensionTest;
import com.abuscom.infisicalplugin.infisical.login.TokenManager;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import javax.swing.JButton;
import javax.swing.JComponent;
import java.awt.Component;
import java.awt.Container;

/**
 * Covers the "expired token forces a fresh login" path end-to-end:
 * TokenManager.isTokenValid() must detect an expired exp claim, clear the stored token, and -
 * via the TokenChangeListener wiring - make InjectSecretsSettingsEditor's login button
 * reappear. See TokenManager.isTokenValid()/clearKeypass() and
 * InjectSecretsSettingsEditor.updateLoginButtonVisibility().
 */
public class InjectSecretsSettingsEditorLoginButtonTest extends BasePlatformTestCase {

    private InjectSecretsSettingsEditor editor;

    @Override
    protected void tearDown() throws Exception {
        try {
            if (editor != null) {
                editor.disposeEditor();
            }
            TokenManager.getInstance().clearKeypass();
        } finally {
            super.tearDown();
        }
    }

    public void testExpiredToken_isClearedByIsTokenValid_andLoginButtonReappears() {
        TokenManager.getInstance().setTokenInKeypass(InjectSecretsRunConfigurationExtensionTest.fakeJwt(-3600));

        editor = new InjectSecretsSettingsEditor();
        JButton loginButton = findLoginButton(editor);

        // a token is present in the keypass (even though expired), so at construction time the
        // button starts out hidden - updateLoginButtonVisibility only checks for null, not
        // actual validity.
        assertFalse(loginButton.isVisible());

        boolean isValid = TokenManager.getInstance().isTokenValid();

        assertFalse(isValid);
        assertNull(TokenManager.getInstance().getTokenFromKeypass());
        assertTrue(loginButton.isVisible());
    }

    public void testValidToken_isTokenValid_doesNotClearTokenOrShowLoginButton() {
        TokenManager.getInstance().setTokenInKeypass(InjectSecretsRunConfigurationExtensionTest.fakeJwt(3600));

        editor = new InjectSecretsSettingsEditor();
        JButton loginButton = findLoginButton(editor);
        assertFalse(loginButton.isVisible());

        boolean isValid = TokenManager.getInstance().isTokenValid();

        assertTrue(isValid);
        assertNotNull(TokenManager.getInstance().getTokenFromKeypass());
        assertFalse(loginButton.isVisible());
    }

    private static JButton findLoginButton(InjectSecretsSettingsEditor editor) {
        JComponent panel = editor.createEditor();
        JButton loginButton = findButtonByText(panel, "Login");
        if (loginButton == null) {
            throw new IllegalStateException("No JButton found in InjectSecretsSettingsEditor's panel");
        }
        return loginButton;
    }

    // Der Login-Button steckt seit der topRow/bottomRow-Aufteilung in createEditor() in einem
    // verschachtelten Panel, nicht mehr direkt im zurückgegebenen Root-Panel - daher rekursiv suchen.
    private static JButton findButtonByText(Container container, String text) {
        for (Component component : container.getComponents()) {
            if (component instanceof JButton button && text.equals(button.getText())) {
                return button;
            }
            if (component instanceof Container childContainer) {
                JButton found = findButtonByText(childContainer, text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
