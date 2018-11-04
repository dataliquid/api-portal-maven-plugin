package com.dataliquid.maven.api.portal.mojo;

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import com.dataliquid.maven.api.portal.domain.AuthenticationType;
import com.github.kevinsawicki.http.HttpRequest;

/**
 * Goal which upload api via REST endpoint.
 */
@Mojo(name = "api-upload", defaultPhase = LifecyclePhase.DEPLOY)
public class ApiUploadMojo extends AbstractMojo
{
    @Parameter(defaultValue = "${project.basedir}", property = "directory", required = true)
    private File directory;

    @Parameter(defaultValue = "${project}", required = true)
    private MavenProject project;

    @Parameter(property = "auth", defaultValue = "BASIC")
    private AuthenticationType auth;

    @Parameter(property = "username")
    private String username;

    @Parameter(property = "password")
    private String password;

    @Parameter(property = "endpoint", required = true)
    private String endpoint;

    private String basePath = "/api-portal-services/app/api/web/management/interfaces";

    @Parameter(property = "api-id", required = true)
    private String apiId;

    @Parameter(property = "api-version", required = true)
    private String apiVersion;

    @Parameter(property = "fileName", required = true)
    private String fileName;

    public void execute() throws MojoExecutionException
    {
        try
        {

            if (AuthenticationType.BASIC.equals(auth) && (username == null || username.isEmpty())
                    || (password == null || password.isEmpty()))
            {
                throw new MojoExecutionException(
                        "The API Portal Maven Plugin configuration is incomplete: username and password are required for basic authentication type.");
            }

            String apiPath = endpoint + basePath + "/" + apiId + "/update";
            HttpRequest request = HttpRequest.put(apiPath);

            if (AuthenticationType.BASIC.equals(auth))
            {
                getLog().info("Authentication Mode BASIC - using configured username and password for endpoint.");
                request.basic(username, password);
            }

            File apiFile = new File(directory, fileName);

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
                getLog().info("API uploaded successfully.");
            }
            else
            {
                getLog().info("API upload failed. HTTP Response: " + code + " - " + request.message());
            }
        }
        catch (Exception e)
        {
            getLog().error("Error uploading API. Reason: ", e);
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

    public String getFileName()
    {
        return fileName;
    }

    public void setFileName(String fileName)
    {
        this.fileName = fileName;
    }
}
