/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2018-2019 Andres Almiray.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kordamp.gradle.plugin.publishing

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.plugins.signing.SigningPlugin
import org.kordamp.gradle.plugin.AbstractKordampPlugin
import org.kordamp.gradle.plugin.apidoc.ApidocPlugin
import org.kordamp.gradle.plugin.base.BasePlugin
import org.kordamp.gradle.plugin.base.ProjectConfigurationExtension
import org.kordamp.gradle.plugin.base.model.Credentials
import org.kordamp.gradle.plugin.base.model.Person
import org.kordamp.gradle.plugin.base.model.Repository
import org.kordamp.gradle.plugin.buildinfo.BuildInfoPlugin
import org.kordamp.gradle.plugin.groovydoc.GroovydocPlugin
import org.kordamp.gradle.plugin.jar.JarPlugin
import org.kordamp.gradle.plugin.javadoc.JavadocPlugin
import org.kordamp.gradle.plugin.source.SourceJarPlugin

import static org.kordamp.gradle.StringUtils.isBlank
import static org.kordamp.gradle.plugin.base.BasePlugin.isRootProject

/**
 * Configures artifact publication.
 *
 * @author Andres Almiray
 * @since 0.2.0
 */
class PublishingPlugin extends AbstractKordampPlugin {
    Project project

    void apply(Project project) {
        this.project = project

        if (isRootProject(project)) {
            if (project.childProjects.size()) {
                project.childProjects.values().each {
                    configureProject(it)
                }
            } else {
                configureProject(project)
            }
        } else {
            configureProject(project)
        }
    }

    static void applyIfMissing(Project project) {
        if (!project.plugins.findPlugin(PublishingPlugin)) {
            project.plugins.apply(PublishingPlugin)
        }
        if (!project.plugins.findPlugin(SigningPlugin)) {
            project.plugins.apply(SigningPlugin)
        }
    }

    private void configureProject(Project project) {
        if (hasBeenVisited(project)) {
            return
        }
        setVisited(project, true)

        BasePlugin.applyIfMissing(project)
        BuildInfoPlugin.applyIfMissing(project)
        SourceJarPlugin.applyIfMissing(project)
        ApidocPlugin.applyIfMissing(project)
        JarPlugin.applyIfMissing(project)

        project.plugins.withType(JavaBasePlugin) {
            if (!project.plugins.findPlugin(MavenPublishPlugin)) {
                project.plugins.apply(MavenPublishPlugin)
            }
        }

        project.afterEvaluate {
            project.plugins.withType(JavaBasePlugin) {
                updatePublications(project)
            }
        }
    }

    private void updatePublications(Project project) {
        ProjectConfigurationExtension effectiveConfig = resolveEffectiveConfig(project)

        if (!effectiveConfig.publishing.enabled || !project.sourceSets.findByName('main')) {
            setEnabled(false)
            return
        }

        Task javadocJar = project.tasks.findByName(JavadocPlugin.JAVADOC_JAR_TASK_NAME)
        Task groovydocJar = project.tasks.findByName(GroovydocPlugin.GROOVYDOC_JAR_TASK_NAME)
        Task sourceJar = project.tasks.findByName(SourceJarPlugin.SOURCE_JAR_TASK_NAME)

        project.publishing {
            publications {
                mainPublication(MavenPublication) {
                    from project.components.java

                    if (javadocJar?.enabled) artifact javadocJar
                    if (groovydocJar?.enabled) artifact groovydocJar
                    if (sourceJar?.enabled) artifact sourceJar

                    pom {
                        name = effectiveConfig.info.name
                        description = effectiveConfig.info.description
                        url = effectiveConfig.info.url
                        inceptionYear = effectiveConfig.info.inceptionYear
                        licenses {
                            effectiveConfig.license.licenses.forEach { lic ->
                                license {
                                    name = lic.name
                                    url = lic.url
                                    distribution = lic.distribution
                                    if (lic.comments) comments = lic.comments
                                }
                            }
                        }
                        if (!isBlank(effectiveConfig.info.scm.url)) {
                            scm {
                                url = effectiveConfig.info.scm.url
                                if (effectiveConfig.info.scm.connection) {
                                    connection = effectiveConfig.info.scm.connection
                                }
                                if (effectiveConfig.info.scm.connection) {
                                    developerConnection = effectiveConfig.info.scm.developerConnection
                                }
                            }
                        } else if (effectiveConfig.info.links.scm) {
                            scm {
                                url = effectiveConfig.info.links.scm
                            }
                        }
                        if (!effectiveConfig.info.organization.isEmpty()) {
                            organization {
                                name = effectiveConfig.info.organization.name
                                url = effectiveConfig.info.organization.url
                            }
                        }
                        developers {
                            effectiveConfig.info.people.forEach { Person person ->
                                if ('developer' in person.roles*.toLowerCase()) {
                                    developer {
                                        if (person.id) id = person.id
                                        if (person.name) name = person.name
                                        if (person.url) url = person.url
                                        if (person.email) email = person.email
                                        if (person.organization?.name) organizationName = person.organization.name
                                        if (person.organization?.url) organizationUrl = person.organization.url
                                        if (person.roles) roles = person.roles as Set
                                        if (person.properties) properties.set(person.properties)
                                    }
                                }
                            }
                        }
                        contributors {
                            effectiveConfig.info.people.forEach { Person person ->
                                if ('contributor' in person.roles*.toLowerCase()) {
                                    contributor {
                                        if (person.name) name = person.name
                                        if (person.url) url = person.url
                                        if (person.email) email = person.email
                                        if (person.organization?.name) organizationName = person.organization.name
                                        if (person.organization?.url) organizationUrl = person.organization.url
                                        if (person.roles) roles = person.roles as Set
                                        if (person.properties) properties.set(person.properties)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            String repositoryName = effectiveConfig.release ? effectiveConfig.publishing.releasesRepository : effectiveConfig.publishing.snapshotsRepository
            if (!isBlank(repositoryName)) {
                Repository repo = effectiveConfig.info.repositories.getRepository(repositoryName)
                if (repo == null) {
                    throw new IllegalStateException("Repository '${repositoryName}' was not found")
                }

                repositories {
                    maven {
                        url = repo.url
                        Credentials creds = effectiveConfig.info.credentials.getCredentials(repo.name)
                        if (repo.credentials) {
                            credentials {
                                username = repo.credentials.username
                                password = repo.credentials.password
                            }
                        } else if (creds) {
                            credentials {
                                username = creds.username
                                password = creds.password
                            }
                        }
                    }
                }
            }
        }

        if (effectiveConfig.publishing.signing) {
            project.signing {
                sign project.publishing.publications.mainPublication
            }
        }
    }
}
