package com.abuscom.infisicalplugin.infisical.cache;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * applyLocalEnvironment() does real file I/O against the project's base directory (it needs to
 * create/read .infisical.local.json there), so - unlike CacheEnvironmentSwitchTest/
 * CacheVersionControlTest, which only need network I/O - this needs a real, writable Project.
 * BasePlatformTestCase provides that (same approach as InjectIntoGradleProcessTest).
 */
public class CacheLocalOverrideTest extends BasePlatformTestCase {

    private final Cache cache = Cache.getInstance();
    private Path localOverridePath;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        cache.getSecrets().clear();
        Path projectBasePath = Paths.get(Objects.requireNonNull(getProject().getBasePath()));
        Files.createDirectories(projectBasePath);
        localOverridePath = projectBasePath.resolve(".infisical.local.json");
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            cache.getSecrets().clear();
            Files.deleteIfExists(localOverridePath);
        } finally {
            super.tearDown();
        }
    }

    public void testApplyLocalEnvironment_noUserSpecificSecrets_neverCreatesFile() throws IOException {
        cache.getSecrets().put("PLAIN_KEY", "just-a-value");

        cache.applyLocalEnvironment(getProject());

        assertFalse(Files.exists(localOverridePath));
        assertEquals("just-a-value", cache.getSecrets().get("PLAIN_KEY"));
    }

    public void testApplyLocalEnvironment_noFileWithUserSpecificSecret_scaffoldsKeyAndThrows() {
        cache.getSecrets().put("IDASHX_CONFIG_ROOT", "C:/Users/Abuscom/workspace/idashx-config");

        assertThrows(IOException.class, () -> cache.applyLocalEnvironment(getProject()));

        Map<String, String> written = readLocalOverrideFile();
        assertTrue(written.containsKey("IDASHX_CONFIG_ROOT"));
        assertEquals("", written.get("IDASHX_CONFIG_ROOT"));
        // the run was cancelled before applying anything - the raw, user-specific value must
        // still be sitting in secrets untouched, not silently overwritten with the placeholder.
        assertEquals("C:/Users/Abuscom/workspace/idashx-config", cache.getSecrets().get("IDASHX_CONFIG_ROOT"));
    }

    public void testApplyLocalEnvironment_fileHasRealOverride_appliesItToSecrets() throws IOException {
        cache.getSecrets().put("IDASHX_CONFIG_ROOT", "C:/Users/Abuscom/workspace/idashx-config");
        writeLocalOverrideFile(Map.of("IDASHX_CONFIG_ROOT", "C:/Users/Fabian/workspace/idashx-config"));

        cache.applyLocalEnvironment(getProject());

        assertEquals("C:/Users/Fabian/workspace/idashx-config", cache.getSecrets().get("IDASHX_CONFIG_ROOT"));
    }

    public void testApplyLocalEnvironment_fileHasBlankOverride_doesNotOverwriteRawValue() throws IOException {
        cache.getSecrets().put("IDASHX_CONFIG_ROOT", "C:/Users/Abuscom/workspace/idashx-config");
        writeLocalOverrideFile(Map.of("IDASHX_CONFIG_ROOT", ""));

        cache.applyLocalEnvironment(getProject());

        assertEquals("C:/Users/Abuscom/workspace/idashx-config", cache.getSecrets().get("IDASHX_CONFIG_ROOT"));
    }

    public void testApplyLocalEnvironment_newUserSpecificKeyFromEnvironmentSwitch_appendsWithoutLosingExistingOverride() {
        writeLocalOverrideFile(Map.of("EXISTING_KEY", "C:/Users/Fabian/existing"));
        cache.getSecrets().put("EXISTING_KEY", "C:/Users/Abuscom/existing");
        cache.getSecrets().put("NEW_KEY", "C:/Users/Abuscom/new-from-other-environment");

        assertThrows(IOException.class, () -> cache.applyLocalEnvironment(getProject()));

        Map<String, String> written = readLocalOverrideFile();
        assertEquals("C:/Users/Fabian/existing", written.get("EXISTING_KEY"));
        assertTrue(written.containsKey("NEW_KEY"));
        assertEquals("", written.get("NEW_KEY"));
    }

    public void testApplyLocalEnvironment_nonPathSecret_isNeverRequiredInOverrideFile() throws IOException {
        cache.getSecrets().put("API_KEY", "sk-not-a-path-1234");

        cache.applyLocalEnvironment(getProject());

        assertEquals("sk-not-a-path-1234", cache.getSecrets().get("API_KEY"));
    }

    public void testLooksLikeUserSpecificPath_recognizesWindowsMacLinuxHomeDirs() {
        assertTrue(Cache.looksLikeUserSpecificPath("C:\\Users\\Abuscom\\workspace\\idashx-config"));
        assertTrue(Cache.looksLikeUserSpecificPath("C:/Users/Abuscom/workspace/idashx-config"));
        assertTrue(Cache.looksLikeUserSpecificPath("/Users/abuscom/workspace/idashx-config"));
        assertTrue(Cache.looksLikeUserSpecificPath("/home/abuscom/workspace/idashx-config"));
    }

    public void testLooksLikeUserSpecificPath_recognizesFileUriPrefixedPaths() {
        assertTrue(Cache.looksLikeUserSpecificPath("file:C:\\Users\\Abuscom\\workspace\\wobi-controlroom\\dev"));
        assertTrue(Cache.looksLikeUserSpecificPath("file:C:/Users/Abuscom/workspace/wobi-controlroom/dev"));
        assertTrue(Cache.looksLikeUserSpecificPath("file:/C:/Users/Abuscom/workspace/wobi-controlroom/dev"));
        assertTrue(Cache.looksLikeUserSpecificPath("file:///C:/Users/Abuscom/workspace/wobi-controlroom/dev"));
        assertTrue(Cache.looksLikeUserSpecificPath("file:/home/abuscom/workspace/idashx-config"));
    }

    public void testLooksLikeUserSpecificPath_ignoresPlainValuesAndNull() {
        assertFalse(Cache.looksLikeUserSpecificPath("sk-not-a-path-1234"));
        assertFalse(Cache.looksLikeUserSpecificPath("https://infisical.internal.abuscom.cloud"));
        assertFalse(Cache.looksLikeUserSpecificPath(null));
    }

    private void writeLocalOverrideFile(Map<String, String> content) {
        try {
            Files.writeString(localOverridePath, new Gson().toJson(content));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, String> readLocalOverrideFile() {
        try {
            Type type = new TypeToken<Map<String, String>>() {}.getType();
            return new Gson().fromJson(Files.readString(localOverridePath), type);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
