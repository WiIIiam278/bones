/*
 * MIT License
 *
 * Copyright (c) 2024 William278
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.william278.backend.service;

import lombok.extern.slf4j.Slf4j;
import net.william278.backend.configuration.AppConfiguration;
import net.william278.backend.database.model.Project;
import net.william278.backend.database.repository.ProjectRepository;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class ProjectDocsService {

    private final Map<String, GitWiki> wikis;
    private final ProjectRepository projects;
    private final AppConfiguration config;

    public static final String PATTERN = "[a-zA-Z0-9-_.]+";

    public ProjectDocsService(ProjectRepository projects, AppConfiguration config) {
        this.wikis = new HashMap<>();
        this.projects = projects;
        this.config = config;

        this.cloneWikis();
    }

    public Optional<String> getPage(@NotNull Project project, @NotNull String langCode,
                                    @NotNull String pageSlug) {
        if (!this.wikis.containsKey(project.getSlug())) {
            return Optional.empty();
        }

        // Validate slug
        if (!pageSlug.matches(PATTERN) || pageSlug.startsWith(".")) {
            return Optional.empty();
        }
        final String slug = pageSlug.endsWith(".md") ? pageSlug.substring(0, pageSlug.length() - 3) : pageSlug;
        final String locale = langCode.equalsIgnoreCase(config.getDefaultDocLocale()) ? "" : langCode.toLowerCase();

        // Find a file ignoring case in the project's docs directory at pageSlug
        final File[] file = getProjectPath(project).toFile().listFiles
                ((dir, name) -> name.equalsIgnoreCase(String.format("%s%s.md", slug, locale)));
        if (file == null || file.length == 0) {
            return Optional.empty();
        }

        // Read the file
        try (InputStream stream = new FileInputStream(file[0])) {
            return Optional.of(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            log.warn("Failed to read docs page \"{}\" for project \"{}\"", pageSlug, project.getSlug(), e);
            return Optional.empty();
        }
    }

    @NotNull
    private Path getProjectPath(@NotNull Project project) {
        return config.getDocsPath().resolve(project.getSlug());
    }

    private void cloneWikis() {
        log.info("Initializing project docs service");

        // Make directories
        if (config.getDocsPath().toFile().mkdirs()) {
            log.info("Created docs directory at {}", config.getDocsPath());
        }

        // Clone & pull wikis
        this.projects.findAll().stream().filter(p -> p.getMetadata().isDocumentation()).forEach(proj -> {
            try {
                this.wikis.put(proj.getSlug(), new GitWiki(proj.getMetadata().getGithub(), getProjectPath(proj)));
            } catch (GitAPIException | IOException e) {
                log.error("Failed to initialize wiki for project {}", proj.getSlug(), e);
            }
        });
    }

    public boolean updateWiki(@NotNull Project project) {
        if (this.wikis.containsKey(project.getSlug())) {
            return this.wikis.get(project.getSlug()).update();
        }
        return false;
    }

    public static final class GitWiki {

        private final Git git;

        private GitWiki(@NotNull String github, @NotNull Path cloneDest) throws GitAPIException, IOException {
            if (!cloneDest.toFile().exists()) {
                this.git = Git.cloneRepository()
                        .setURI(String.format("%s.wiki.git", github))
                        .setDirectory(cloneDest.toFile())
                        .call();
            } else {
                this.git = Git.open(cloneDest.toFile());
            }
        }

        public boolean update() {
            try {
                this.git.pull().call();
                return true;
            } catch (GitAPIException e) {
                log.error("Failed to update wiki", e);
                return false;
            }
        }

    }

}
