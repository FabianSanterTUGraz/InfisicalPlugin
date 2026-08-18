package com.abuscom.infisicalplugin.infisical.cache.Secrets;

import com.abuscom.infisicalplugin.infisical.cache.Secrets.Tagging.TagListRequest;

import java.util.List;

public record SecretEntry(String secretKey, String secretValue, int version, String id, List<TagListRequest> tags) {
}
