package com.vackosar.gitflowincrementalbuild.mocks;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;

import com.vackosar.gitflowincrementalbuild.mocks.server.TestServerType;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

public class LocalRepoMock implements AutoCloseable {

    static {
        JGitIsolation.ensureIsolatedFromSystemAndUserConfig();
    }

    private File baseFolder;
    private File templateProjectZip = new File(getClass().getClassLoader().getResource("template.zip").getFile());
    private RemoteRepoMock remoteRepo;
    private Git git;

    public LocalRepoMock(File baseFolder, TestServerType remoteRepoServerType) throws IOException, URISyntaxException, GitAPIException {
        this.baseFolder = new File(baseFolder.getAbsolutePath(), "tmp/repo/");
        new UnZipper().act(templateProjectZip, this.baseFolder);

        remoteRepo = remoteRepoServerType != null ? new RemoteRepoMock(baseFolder, remoteRepoServerType) : null;
        git = new Git(new FileRepository(new File(this.baseFolder, ".git")));

        if (remoteRepoServerType != null) {
            try {
                configureRemote(git, remoteRepo.repoUri.toString());
            } catch (IOException | URISyntaxException | GitAPIException | RuntimeException e) {
                close();
                throw e;
            }
        }
    }

    public Git getGit() {
        return git;
    }

    private void configureRemote(Git git, String repoUrl) throws URISyntaxException, IOException, GitAPIException {
        StoredConfig config = git.getRepository().getConfig();
        config.clear();
        config.setString("remote", "origin" ,"fetch", "+refs/heads/*:refs/remotes/origin/*");
        config.setString("remote", "origin" ,"push", "+refs/heads/*:refs/remotes/origin/*");
        config.setString("branch", "master", "remote", "origin");
        config.setString("baseBranch", "master", "merge", "refs/heads/master");
        config.setString("push", null, "default", "current");

        // disable all gc
        // http://download.eclipse.org/jgit/site/5.2.1.201812262042-r/apidocs/org/eclipse/jgit/internal/storage/file/GC.html#setAuto-boolean-
        config.setString("gc", null, "auto", "0");
        config.setString("gc", null, "autoPackLimit", "0");
        config.setBoolean("receive", null, "autogc", false);

        RemoteConfig remoteConfig = new RemoteConfig(config, "origin");
        URIish uri = new URIish(repoUrl);
        remoteConfig.addURI(uri);
        remoteConfig.addFetchRefSpec(new RefSpec("refs/heads/master:refs/heads/master"));
        remoteConfig.addPushRefSpec(new RefSpec("refs/heads/master:refs/heads/master"));
        remoteConfig.update(config);

        config.save();
    }

    @Override
    public void close() {
        if (remoteRepo != null) {
            remoteRepo.close();
        }
        if (git != null) {
            git.getRepository().close();
            git.close();
        }
    }

    public RemoteRepoMock getRemoteRepo() {
        return remoteRepo;
    }

    public File getBaseCanonicalBaseFolder() throws IOException {
        return baseFolder.getCanonicalFile();
    }
}
