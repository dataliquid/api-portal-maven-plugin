package com.dataliquid.maven.api.portal.mojo;

import java.io.File;

import org.apache.commons.lang3.StringUtils;
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
import com.github.kevinsawicki.http.HttpRequest;

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

    @Parameter(property = "endpoint", defaultValue = "https://www.api-portal.de", required = true)
    private String endpoint;

    @Parameter(property = "basePath", defaultValue = "/services/app/api/v1/interfaces", required = true)
    private String basePath;

    @Parameter(property = "apiId", required = true)
    private String apiId;

    @Parameter(property = "apiVersion", required = true)
    private String apiVersion;

    @Parameter(property = "filename", required = true)
    private String filename;

    public void execute() throws MojoExecutionException
    {
        try
        {
            String apiPath = endpoint + basePath + "/" + apiId + "/update";
            HttpRequest request = HttpRequest.put(apiPath);

            if (AuthenticationType.BASIC.equals(auth))
            {
                getLog().info("Authentication Mode BASIC - using configured username and password for endpoint.");

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

                request.basic(username, password);
            }
            else
            {
                getLog().error("No auth type defined. Available types are: " + AuthenticationType.values());
            }

            File apiFile = new File(directory, filename);

            if (!getLog().isDebugEnabled())
            {
                getLog().info("Upload API file [" + apiFile.getPath() + "]");
            }
            else
            {
                getLog().debug("Upload API file [" + apiFile.getPath() + "] to [" + apiPath + "]");
            }

            request.part("api_version", apiVersion);
            request.part("file", "file", apiFile);

            int code = request.code();
            if (request.noContent())
            {
                getLog().info("API " + filename + " uploaded successfully.");
            }
            else
            {
                getLog().info("API " + filename + " upload failed. HTTP Response: " + code + " - " + request.message());
            }
        }
        catch (Exception e)
        {
            getLog().error("Error uploading API " + filename + " . Reason: ", e);
        }

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
