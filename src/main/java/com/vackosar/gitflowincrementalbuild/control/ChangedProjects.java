package com.vackosar.gitflowincrementalbuild.control;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
@Named
public class ChangedProjects {

    private Logger logger = LoggerFactory.getLogger(ChangedProjects.class);

    @Inject private DifferentFiles differentFiles;
    @Inject private MavenSession mavenSession;
    @Inject private Modules modules;

    public Set<MavenProject> get() throws GitAPIException, IOException {
        Map<Path, MavenProject> modulesPathMap = modules.createPathMap(mavenSession);
        return differentFiles.get().stream()
                .map(path -> findProject(path, modulesPathMap))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private MavenProject findProject(Path diffPath, Map<Path, MavenProject> modulesPathMap) {
        Path path = diffPath;
        // Files.exist() to spot changes in non-reactor module (path will then yield a null changedReactorProject).
        // Without this check, the changed would be wrongly mapped to the "closest" reactor module (which hasn't changed at all!).
        while (path != null && !modulesPathMap.containsKey(path) && !Files.exists(path.resolve("pom.xml"))) {
            path = path.getParent();
        }
        if (path == null) {
            logger.warn("Ignoring changed file outside build project: {}", diffPath);
            return null;
        }
        MavenProject changedReactorProject = modulesPathMap.get(path);
        if (changedReactorProject == null) {
            logger.warn("Ignoring changed file in non-reactor module: {}", diffPath);
            return null;
        }
        logger.debug("Changed file: {}", diffPath);
        return changedReactorProject;
    }
}
