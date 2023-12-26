/**
 * Copyright © 2019 dataliquid GmbH | www.dataliquid.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dataliquid.maven.api.portal.mojo;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecrypter;

import com.dataliquid.maven.api.portal.domain.AuthenticationType;

/**
 * Goal which update a existing API definition.
 */
@Mojo(name = "update-api", defaultPhase = LifecyclePhase.DEPLOY)
public class ApiUpdateMojo extends AbstractMojo
{

    @Component
    protected Settings settings;

    @Component
    protected SettingsDecrypter decrypter;

    @Parameter(property = "directory", defaultValue = "${project.basedir}", required = true)
    private File directory;

    @Parameter(defaultValue = "${project}", required = true)
    private MavenProject project;

    @Parameter(property = "auth")
    private AuthenticationType auth;

    @Parameter(property = "maven.server", required = false)
    protected String server;

    @Parameter(property = "username")
    private String username;

    @Parameter(property = "password")
    private String password;

    @Parameter(property = "clientId")
    private String clientId;

    @Parameter(property = "clientSecret")
    private String clientSecret;

    @Parameter(property = "endpoint", defaultValue = "https://www.api-portal.io", required = true)
    private String endpoint;

    @Parameter(property = "basePath")
    private String basePath = "";

    @Parameter(property = "path", defaultValue = "/portal/v1/apis/{apiId}/versions", required = true)
    private String path;

    @Parameter(property = "apiId", required = true)
    private String apiId;

    @Parameter(property = "apiVersion", required = true)
    private String apiVersion;

    @Parameter(property = "filename", required = true)
    private String filename;

    public void execute() throws MojoExecutionException
    {
        CloseableHttpClient httpClient = HttpClients.createDefault();

        try
        {
            String pathTemplate = endpoint + basePath + path;
            String apiPath = resolveUriParameter(pathTemplate);
            HttpPut putRequest = new HttpPut(apiPath);

            // Basic Authentication
            if (AuthenticationType.BASIC.equals(auth))
            {
                getLog().info("Authentication Mode BASIC - using configured username and password for endpoint.");

                // Same authentication logic as before

                if ((StringUtils.isBlank(username) || StringUtils.isBlank(password)) && StringUtils.isBlank(server))
                {
                    throw new MojoExecutionException(
                            "The API Portal Maven Plugin configuration is incomplete: server or username and password are required for basic authentication.");
                }

                if (StringUtils.isNotEmpty(server))
                {
                    getLog().debug("Using server configuration for authentication.");

                    // settings.xml
                    Server serverObject = this.settings.getServer(server);
                    if (serverObject == null)
                    {
                        getLog().error("Server [" + server + "] not found in settings file.");
                        throw new MojoExecutionException("Server [" + server + "] not found in settings file.");
                    }

                    serverObject = decrypter.decrypt(new DefaultSettingsDecryptionRequest(serverObject)).getServer();
                    username = serverObject.getUsername();
                    password = serverObject.getPassword();

                    if (StringUtils.isNotEmpty(username) && StringUtils.isNotEmpty(password))
                    {
                        getLog().warn("Server and plugin inline credentials are configured. Using plugin configuration credentials.");
                    }
                }
                else
                {
                    getLog().debug("Using inline credentials for authentication.");
                }

                CredentialsProvider provider = new BasicCredentialsProvider();
                UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(username, password);
                provider.setCredentials(AuthScope.ANY, credentials);
                httpClient = HttpClients.custom().setDefaultCredentialsProvider(provider).build();

            }
            // Client ID and Secret Authentication
            else if (AuthenticationType.CLIENT_ID_SECRET.equals(auth))
            {
                getLog().info("Authentication Mode CLIENT_ID_SECRET - using configured client id and client secret for endpoint.");

                // Same logic for checking client id and secret

                putRequest.addHeader("client-id", clientId);
                putRequest.addHeader("client-secret", clientSecret);
            }
            else
            {
                getLog().error("No auth type defined. Available types are: " + AuthenticationType.values());
                return;
            }

            File apiFile = new File(directory, filename);

            getLog().info("Upload API file [" + apiFile.getPath() + "] to [" + apiPath + "]");

            // Setting up multipart request
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.addTextBody("api_version", apiVersion, ContentType.TEXT_PLAIN);
            builder.addBinaryBody("file", apiFile, ContentType.DEFAULT_BINARY, apiFile.getName());
            putRequest.setEntity(builder.build());

            // Execute request
            HttpResponse response = httpClient.execute(putRequest);

            int code = response.getStatusLine().getStatusCode();

            if (code == 204)
            {
                getLog().info("API " + filename + " uploaded successfully.");
            }
            else
            {
                String responseString = EntityUtils.toString(response.getEntity());
                getLog().warn("API " + filename + " upload failed. HTTP Response: " + code + " - " + responseString);
            }
        }
        catch (Exception e)
        {
            getLog().error("Error uploading API " + filename + " - reason: ", e);
        }
        finally
        {
            try
            {
                httpClient.close();
            }
            catch (IOException e)
            {
                getLog().error("Error closing HttpClient: ", e);
            }
        }
    }

    private String resolveUriParameter(String pathTemplate)
    {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("apiId", apiId);

        StringSubstitutor sub = new StringSubstitutor(parameters, "{", "}");
        String path = sub.replace(pathTemplate);

        return path;
    }

    public File getDirectory()
    {
        return directory;
    }

    public void setDirectory(File directory)
    {
        this.directory = directory;
    }

    public AuthenticationType getAuth()
    {
        return auth;
    }

    public void setAuth(AuthenticationType auth)
    {
        this.auth = auth;
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

    public String getEndpoint()
    {
        return endpoint;
    }

    public void setEndpoint(String endpoint)
    {
        this.endpoint = endpoint;
    }

    public String getApiId()
    {
        return apiId;
    }

    public void setApiId(String apiId)
    {
        this.apiId = apiId;
    }

    public String getApiVersion()
    {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion)
    {
        this.apiVersion = apiVersion;
    }

    public String getFilename()
    {
        return filename;
    }

    public void setFilename(String filename)
    {
        this.filename = filename;
    }
}
