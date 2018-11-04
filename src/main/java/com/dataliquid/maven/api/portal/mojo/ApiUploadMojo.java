package com.dataliquid.maven.api.portal.mojo;

import java.io.File;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
    @Parameter(defaultValue = "${project.build.directory}", property = "outputDir", required = true)
    private File outputDirectory;

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

    @Parameter(property = "api-id", defaultValue = "${project.version}")
    private String apiId;

    @Parameter(property = "api-version", defaultValue = "${project.version}")
    private String apiVersion;

    @Parameter(property = "fileName", defaultValue = "${project.artifactId}-${project.version}.${project.packaging}")
    private String fileName;

    public void execute() throws MojoExecutionException
    {
        if (AuthenticationType.BASIC.equals(auth) && (username == null || username.isEmpty()) || (password == null || password.isEmpty()))
        {
            throw new MojoExecutionException(
                    "The API Portal Maven Plugin configuration is incomplete: username and password are required for basic authentication type.");
        }

        getLog().info("Checking the accessibility of the endpoint [ " + endpoint + " ] ...");
        HttpRequest request = HttpRequest.get(endpoint);
        if (!request.ok())
        {
            getLog().warn("Endpoint is not reachable!");
            throw new MojoExecutionException("The Endpoint " + endpoint + " is not reachable.");
        }

        getLog().info("The endpoint is reachable.");

        if (username != null && !username.isEmpty() && password != null)
        {
            getLog().info("Using configured username and password for endpoint.");
            if (!request.basic(username, password).ok())
            {
                getLog().info("Failed to login to endpoint!");
            }
            else
            {
                getLog().info("Logged in successfully.");
            }
        }

        File toPublish = new File(outputDirectory, fileName);
        getLog().info("Upload API file [" + toPublish.getPath() + "]");

        request = HttpRequest.post(endpoint);
        request.part("file", toPublish);
        if (request.ok())
        {
            getLog().info("API uploaded successfully.");
        }
        else
        {
            getLog().info("Failed to upload API.");
        }

    }

    public File getOutputDirectory()
    {
        return outputDirectory;
    }

    public void setOutputDirectory(File outputDirectory)
    {
        this.outputDirectory = outputDirectory;
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
