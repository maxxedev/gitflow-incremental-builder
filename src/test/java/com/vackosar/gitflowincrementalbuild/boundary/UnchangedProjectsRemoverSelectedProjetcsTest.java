package com.vackosar.gitflowincrementalbuild.boundary;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.vackosar.gitflowincrementalbuild.control.Property;

/**
 * Tests {@link UnchangedProjectsRemover} with Mockito mocks when "selected projects" are present ({@code mvn -pl ...}).
 *
 * @author famod
 */
public class UnchangedProjectsRemoverSelectedProjetcsTest extends BaseUnchangedProjectsRemoverTest {

    @Override
    @Before
    public void before() throws GitAPIException, IOException {
        super.before();
        projectProperties.put(Property.skipTestsForUpstreamModules.fullName(), "true");
    }

    // mvn -pl module-B
    @Test
    public void nothingChanged() throws GitAPIException, IOException {
        MavenProject moduleB = addModuleMock(AID_MODULE_B, false);
        selectProjects(moduleB);

        underTest.act();

        assertEquals("Unexpected goals", Collections.emptyList(), mavenSessionMock.getGoals());

        verify(mavenSessionMock, never()).setProjects(anyList());

        assertProjectPropertiesEqual(moduleB, Collections.emptyMap());
    }

    // mvn -pl module-B -am
    @Test
    public void nothingChanged_makeUpstream() throws GitAPIException, IOException {
        MavenProject moduleB = addModuleMock(AID_MODULE_B, false);
        selectProjects(moduleB);
        projects.add(0, moduleA);  // add back module-A (was wiped by selectProjects())
        when(mavenExecutionRequestMock.getMakeBehavior()).thenReturn(MavenExecutionRequest.REACTOR_MAKE_UPSTREAM);

        underTest.act();

        assertEquals("Unexpected goals", Collections.emptyList(), mavenSessionMock.getGoals());

        verify(mavenSessionMock).setProjects(Collections.singletonList(moduleB));

        assertProjectPropertiesEqual(moduleB, Collections.emptyMap());
    }

    // mvn -pl module-B -amd
    @Test
    public void nothingChanged_makeDownstream() throws GitAPIException, IOException {
        MavenProject moduleB = addModuleMock(AID_MODULE_B, false);
        selectProjects(moduleB);
        MavenProject moduleC = addModuleMock(AID_MODULE_C, false);
        setUpstreamProjects(moduleC, moduleB, moduleA);
        setDownstreamProjects(moduleB, moduleC);
        setDownstreamProjects(moduleA, moduleB, moduleC);

        when(mavenExecutionRequestMock.getMakeBehavior()).thenReturn(MavenExecutionRequest.REACTOR_MAKE_DOWNSTREAM);

        underTest.act();

        assertEquals("Unexpected goals", Collections.emptyList(), mavenSessionMock.getGoals());

        verify(mavenSessionMock).setProjects(Arrays.asList(moduleB, moduleC));

        assertProjectPropertiesEqual(moduleB, Collections.emptyMap());
        assertProjectPropertiesEqual(moduleC, Collections.emptyMap());
    }

    // mvn -pl module-B -amd
    @Test
    public void nothingChanged_makeDownstream_buildDownstreamDisabled() throws GitAPIException, IOException {
        MavenProject moduleB = addModuleMock(AID_MODULE_B, false);
        selectProjects(moduleB);
        MavenProject moduleC = addModuleMock(AID_MODULE_C, false);
        setUpstreamProjects(moduleC, moduleB, moduleA);
        setDownstreamProjects(moduleB, moduleC);
        setDownstreamProjects(moduleA, moduleB, moduleC);

        when(mavenExecutionRequestMock.getMakeBehavior()).thenReturn(MavenExecutionRequest.REACTOR_MAKE_DOWNSTREAM);

        projectProperties.put(Property.buildDownstream.fullName(), "false");

        underTest.act();

        assertEquals("Unexpected goals", Collections.emptyList(), mavenSessionMock.getGoals());

        verify(mavenSessionMock).setProjects(Collections.singletonList(moduleB));

        assertProjectPropertiesEqual(moduleB, Collections.emptyMap());
    }

    // mvn -pl module-B -am -amd
    @Test
    public void nothingChanged_makeBoth() throws GitAPIException, IOException {
        MavenProject moduleB = addModuleMock(AID_MODULE_B, false);
        selectProjects(moduleB);
        projects.add(0, moduleA);  // add back module-A (was wiped by selectProjects())
        MavenProject moduleC = addModuleMock(AID_MODULE_C, false);
        setUpstreamProjects(moduleC, moduleB, moduleA);
        setDownstreamProjects(moduleB, moduleC);
        setDownstreamProjects(moduleA, moduleB, moduleC);

        when(mavenExecutionRequestMock.getMakeBehavior()).thenReturn(MavenExecutionRequest.REACTOR_MAKE_BOTH);

        underTest.act();

        assertEquals("Unexpected goals", Collections.emptyList(), mavenSessionMock.getGoals());

        verify(mavenSessionMock).setProjects(Arrays.asList(moduleB, moduleC));

        assertProjectPropertiesEqual(moduleB, Collections.emptyMap());
        assertProjectPropertiesEqual(moduleC, Collections.emptyMap());
    }

    // mvn -pl module-B
    @Test
    public void moduleAChanged() throws GitAPIException, IOException {
        MavenProject moduleB = addModuleMock(AID_MODULE_B, false);
        selectProjects(moduleB);

        changedProjects.add(moduleA);

        underTest.act();

        verify(mavenSessionMock, never()).setProjects(anyList());
    }

    // mvn -pl module-B -am
    @Test
    public void moduleAChanged_makeUpstream() throws GitAPIException, IOException {
        MavenProject moduleB = addModuleMock(AID_MODULE_B, false);
        selectProjects(moduleB);
        projects.add(0, moduleA);  // add back module-A (was wiped by selectProjects())
        when(mavenExecutionRequestMock.getMakeBehavior()).thenReturn(MavenExecutionRequest.REACTOR_MAKE_UPSTREAM);

        changedProjects.add(moduleA);

        underTest.act();

        verify(mavenSessionMock).setProjects(Arrays.asList(moduleA, moduleB));

        assertProjectPropertiesEqual(moduleA, ImmutableMap.of("maven.test.skip", "true"));
        assertProjectPropertiesEqual(moduleB, Collections.emptyMap());
    }

    // mvn -pl module-B -am
    @Test
    public void moduleAChanged_makeUpstream_buildUpstreamDisabled() throws GitAPIException, IOException {
        MavenProject moduleB = addModuleMock(AID_MODULE_B, false);
        selectProjects(moduleB);
        projects.add(0, moduleA);  // add back module-A (was wiped by selectProjects())
        when(mavenExecutionRequestMock.getMakeBehavior()).thenReturn(MavenExecutionRequest.REACTOR_MAKE_UPSTREAM);

        changedProjects.add(moduleA);

        projectProperties.put(Property.buildUpstream.fullName(), "false");

        underTest.act();

        verify(mavenSessionMock).setProjects(Collections.singletonList(moduleB));

        assertProjectPropertiesEqual(moduleB, Collections.emptyMap());
    }

    // mvn -pl module-B -am
    @Test
    public void moduleAChanged_makeUpstream_moduleCSelected() throws GitAPIException, IOException {
        MavenProject moduleB = addModuleMock(AID_MODULE_B, false);
        MavenProject moduleC = addModuleMock(AID_MODULE_C, false);
        selectProjects(moduleC);
        projects.addAll(0, Arrays.asList(moduleA, moduleB));  // add back module-A & B (which were wiped by selectProjects())
        setDownstreamProjects(moduleB, moduleC);
        setUpstreamProjects(moduleC, moduleA, moduleB);

        when(mavenExecutionRequestMock.getMakeBehavior()).thenReturn(MavenExecutionRequest.REACTOR_MAKE_UPSTREAM);

        changedProjects.add(moduleA);

        underTest.act();

        // upstream B was not changed but its upstream A, so B must be built as well
        verify(mavenSessionMock).setProjects(Arrays.asList(moduleA, moduleB, moduleC));

        assertProjectPropertiesEqual(moduleA, ImmutableMap.of("maven.test.skip", "true"));
        assertProjectPropertiesEqual(moduleB, ImmutableMap.of("maven.test.skip", "true"));
        assertProjectPropertiesEqual(moduleC, Collections.emptyMap());
    }

    // mvn -pl module-B -am -amd
    @Test
    public void moduleAChanged_makeBoth() throws GitAPIException, IOException {
        MavenProject moduleB = addModuleMock(AID_MODULE_B, false);
        selectProjects(moduleB);
        projects.add(0, moduleA);  // add back module-A (was wiped by selectProjects())
        MavenProject moduleC = addModuleMock(AID_MODULE_C, false);
        setUpstreamProjects(moduleC, moduleB, moduleA);
        setDownstreamProjects(moduleB, moduleC);
        setDownstreamProjects(moduleA, moduleB, moduleC);

        when(mavenExecutionRequestMock.getMakeBehavior()).thenReturn(MavenExecutionRequest.REACTOR_MAKE_BOTH);

        changedProjects.add(moduleA);

        underTest.act();

        verify(mavenSessionMock).setProjects(Arrays.asList(moduleA, moduleB, moduleC));

        assertProjectPropertiesEqual(moduleA, ImmutableMap.of("maven.test.skip", "true"));
        assertProjectPropertiesEqual(moduleB, Collections.emptyMap());
        assertProjectPropertiesEqual(moduleC, Collections.emptyMap());
    }

    // mvn -pl module-B -am -amd
    @Test
    public void moduleAChanged_makeBoth_buildUpstreamDisabled() throws GitAPIException, IOException {
        MavenProject moduleB = addModuleMock(AID_MODULE_B, false);
        selectProjects(moduleB);
        projects.add(0, moduleA);  // add back module-A (was wiped by selectProjects())
        MavenProject moduleC = addModuleMock(AID_MODULE_C, false);
        setUpstreamProjects(moduleC, moduleB, moduleA);
        setDownstreamProjects(moduleB, moduleC);
        setDownstreamProjects(moduleA, moduleB, moduleC);

        when(mavenExecutionRequestMock.getMakeBehavior()).thenReturn(MavenExecutionRequest.REACTOR_MAKE_BOTH);

        projectProperties.put(Property.buildUpstream.fullName(), "false");

        changedProjects.add(moduleA);

        underTest.act();

        verify(mavenSessionMock).setProjects(Arrays.asList(moduleB, moduleC));

        assertProjectPropertiesEqual(moduleB, Collections.emptyMap());
        assertProjectPropertiesEqual(moduleC, Collections.emptyMap());
    }

    // mvn -pl module-B -am -amd
    @Test
    public void moduleAChanged_makeBoth_buildDownstreamDisabled() throws GitAPIException, IOException {
        MavenProject moduleB = addModuleMock(AID_MODULE_B, false);
        selectProjects(moduleB);
        projects.add(0, moduleA);  // add back module-A (was wiped by selectProjects())
        MavenProject moduleC = addModuleMock(AID_MODULE_C, false);
        setUpstreamProjects(moduleC, moduleB, moduleA);
        setDownstreamProjects(moduleB, moduleC);
        setDownstreamProjects(moduleA, moduleB, moduleC);

        when(mavenExecutionRequestMock.getMakeBehavior()).thenReturn(MavenExecutionRequest.REACTOR_MAKE_BOTH);

        projectProperties.put(Property.buildDownstream.fullName(), "false");

        changedProjects.add(moduleA);

        underTest.act();

        verify(mavenSessionMock).setProjects(Arrays.asList(moduleA, moduleB));

        assertProjectPropertiesEqual(moduleA, ImmutableMap.of("maven.test.skip", "true"));
        assertProjectPropertiesEqual(moduleB, Collections.emptyMap());
    }

    // mvn -pl module-B -am -amd
    @Test
    public void moduleAChanged_makeDownstream_buildDownstreamDisabled() throws GitAPIException, IOException {
        MavenProject moduleB = addModuleMock(AID_MODULE_B, false);
        selectProjects(moduleB);
        projects.add(0, moduleA);  // add back module-A (was wiped by selectProjects())
        MavenProject moduleC = addModuleMock(AID_MODULE_C, false);
        setUpstreamProjects(moduleC, moduleB, moduleA);
        setDownstreamProjects(moduleB, moduleC);
        setDownstreamProjects(moduleA, moduleB, moduleC);

        when(mavenExecutionRequestMock.getMakeBehavior()).thenReturn(MavenExecutionRequest.REACTOR_MAKE_DOWNSTREAM);

        projectProperties.put(Property.buildDownstream.fullName(), "false");

        changedProjects.add(moduleA);

        underTest.act();

        verify(mavenSessionMock).setProjects(Collections.singletonList(moduleB));

        assertProjectPropertiesEqual(moduleB, Collections.emptyMap());
    }

    // mvn -pl module-B
    @Test
    public void moduleBChanged() throws GitAPIException, IOException {
        MavenProject moduleB = addModuleMock(AID_MODULE_B, true);
        selectProjects(moduleB);

        underTest.act();

        verify(mavenSessionMock, never()).setProjects(anyList());
    }

    // mvn -pl module-B -am
    @Test
    public void moduleBChanged_makeUpstream() throws GitAPIException, IOException {
        MavenProject moduleB = addModuleMock(AID_MODULE_B, true);
        selectProjects(moduleB);
        projects.add(0, moduleA);  // add back module-A (was wiped by selectProjects())
        when(mavenExecutionRequestMock.getMakeBehavior()).thenReturn(MavenExecutionRequest.REACTOR_MAKE_UPSTREAM);

        underTest.act();

        verify(mavenSessionMock).setProjects(Collections.singletonList(moduleB));

        assertProjectPropertiesEqual(moduleB, Collections.emptyMap());
    }

    // mvn -pl module-B -am
    @Test
    public void moduleBChanged_makeUpstream_buildAll() throws GitAPIException, IOException {
        MavenProject moduleB = addModuleMock(AID_MODULE_B, true);
        selectProjects(moduleB);
        projects.add(0, moduleA);  // add back module-A (was wiped by selectProjects())
        when(mavenExecutionRequestMock.getMakeBehavior()).thenReturn(MavenExecutionRequest.REACTOR_MAKE_UPSTREAM);

        projectProperties.put(Property.buildAll.fullName(), "true");

        underTest.act();

        verify(mavenSessionMock, never()).setProjects(anyList());

        assertProjectPropertiesEqual(moduleA, ImmutableMap.of("maven.test.skip", "true"));
        assertProjectPropertiesEqual(moduleB, Collections.emptyMap());
    }

    // mvn -pl module-B -am
    @Test
    public void moduleBChanged_makeUpstream_forceBuildA() throws GitAPIException, IOException {
        MavenProject moduleB = addModuleMock(AID_MODULE_B, true);
        selectProjects(moduleB);
        projects.add(0, moduleA);  // add back module-A (was wiped by selectProjects())
        when(mavenExecutionRequestMock.getMakeBehavior()).thenReturn(MavenExecutionRequest.REACTOR_MAKE_UPSTREAM);

        projectProperties.put(Property.forceBuildModules.fullName(), "module-A");

        underTest.act();

        verify(mavenSessionMock).setProjects(Arrays.asList(moduleA, moduleB));

        assertProjectPropertiesEqual(moduleA, ImmutableMap.of("maven.test.skip", "true"));
        assertProjectPropertiesEqual(moduleB, Collections.emptyMap());
    }

    // mvn -pl module-B -am -amd
    @Test
    public void moduleBChanged_makeBoth() throws GitAPIException, IOException {
        MavenProject moduleB = addModuleMock(AID_MODULE_B, true);
        selectProjects(moduleB);
        projects.add(0, moduleA);  // add back module-A (was wiped by selectProjects())
        MavenProject moduleC = addModuleMock(AID_MODULE_C, false);
        setUpstreamProjects(moduleC, moduleB, moduleA);
        setDownstreamProjects(moduleB, moduleC);
        setDownstreamProjects(moduleA, moduleB, moduleC);

        when(mavenExecutionRequestMock.getMakeBehavior()).thenReturn(MavenExecutionRequest.REACTOR_MAKE_BOTH);

        underTest.act();

        verify(mavenSessionMock).setProjects(Arrays.asList(moduleB, moduleC));

        assertProjectPropertiesEqual(moduleB, Collections.emptyMap());
        assertProjectPropertiesEqual(moduleC, Collections.emptyMap());
    }

    // mimics "-pl :...,:..."
    private void selectProjects(MavenProject... projectsToSelect) {
        List<String> selection = Arrays.stream(projectsToSelect).map(p -> ":" + p.getArtifactId()).collect(Collectors.toList());
        when(mavenExecutionRequestMock.getSelectedProjects()).thenReturn(selection);
        projects.clear();
        projects.addAll(Arrays.asList(projectsToSelect));
    }
}