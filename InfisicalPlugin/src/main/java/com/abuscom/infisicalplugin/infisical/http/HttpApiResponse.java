package com.abuscom.infisicalplugin.infisical.http;

import java.util.Map;

public record HttpApiResponse(int statusCode, Map<String, String> headers, String body) {
}
