package com.abuscom.infisicalplugin.infisical.cache.Secrets;

import java.util.List;

public record SecretEntry(String secretKey, String secretValue, int version, String id, List<TagResponse> tags) {
}
