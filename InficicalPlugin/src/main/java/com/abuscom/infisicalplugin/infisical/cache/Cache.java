package com.abuscom.infisicalplugin.infisical.cache;

import com.abuscom.infisicalplugin.infisical.auth.AccessToken;
import com.abuscom.infisicalplugin.infisical.auth.UniversalAuthClient;
import com.abuscom.infisicalplugin.infisical.cache.SecretClient;
import com.abuscom.infisicalplugin.infisical.http.InfisicalHttpClient;
import com.abuscom.infisicalplugin.infisical.http.InfisicalHttpException;
import com.abuscom.infisicalplugin.toolwindow.login.TokenManager;
import com.intellij.notification.NotificationType;

import java.time.Instant;

import java.util.HashMap;
import java.util.Map;

import static com.intellij.platform.feedback.impl.FeedbackSurveyUtilsKt.showNotification;

public class Cache {
    private static final Cache INSTANCE = new Cache();
    private final Map<String,String> secrets = new HashMap<>();
    private Instant timeStamp = Instant.now();

    private Cache(){}

    public static Cache getInstance()
    {
        return INSTANCE;
    }

    // make the actual secrets api call and copy .env into cache but only of the current enviroment.
    public void setCache(String enviroment)
    {
        String projectID = "7f200104-3b40-4466-b2e3-6624808e9228";
        enviroment = "dev";
        String token = TokenManager.getInstance().getTokenFromKeypass();

        secrets.clear();

        try {
            InfisicalHttpClient httpClient = new InfisicalHttpClient(InfisicalHttpClient.DEFAULT_BASE_URL);
            SecretClient secretsClient = new SecretClient(httpClient);
            SecretsAPICallResponse response = secretsClient.secrets(projectID, enviroment,token);

            for(SecretEntry entry : response.secrets()){
                secrets.put(entry.secretKey(),entry.secretValue());
            }
        }
        catch (InfisicalHttpException e) {
            System.out.println("Error in fetching the secrets!!");
        }
    }

    public Map<String,String> getSecrets()
    {
        return secrets;
    }

    public Instant getTimeStamp()
    {
        return timeStamp;
    }

    public void clearCache()
    {
        secrets.clear();
    }

    //security issue nur debug:
    public void printCache()
    {
        //secrets.forEach((key,value) -> System.out.println(key + "==" + value));
    }
}
