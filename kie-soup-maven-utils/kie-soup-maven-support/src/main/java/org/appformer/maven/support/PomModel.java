/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.appformer.maven.support;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface PomModel {

    String NATIVE_MAVEN_PARSER_CLASS = "org.appformer.maven.integration.MavenPomModelGenerator";

    AFReleaseId getReleaseId();

    AFReleaseId getParentReleaseId();

    Collection<AFReleaseId> getDependencies();
    Collection<AFReleaseId> getDependencies(DependencyFilter filter);

    class InternalModel implements PomModel {
        private AFReleaseId releaseId;
        private AFReleaseId parentReleaseId;
        private final Map<String, Set<AFReleaseId>> dependencies = new HashMap<String, Set<AFReleaseId>>();

        @Override
        public AFReleaseId getReleaseId() {
            return releaseId;
        }

        public void setReleaseId(AFReleaseId releaseId) {
            this.releaseId = releaseId;
        }

        @Override
        public AFReleaseId getParentReleaseId() {
            return parentReleaseId;
        }

        public void setParentReleaseId(AFReleaseId parentReleaseId) {
            this.parentReleaseId = parentReleaseId;
        }

        @Override
        public Collection<AFReleaseId> getDependencies() {
            return getDependencies(DependencyFilter.TAKE_ALL_FILTER);
        }

        @Override
        public Collection<AFReleaseId> getDependencies(DependencyFilter filter ) {
            Set<AFReleaseId> depSet = new HashSet<AFReleaseId>();
            for (Map.Entry<String, Set<AFReleaseId>> entry : dependencies.entrySet()) {
                for (AFReleaseId releaseId : entry.getValue()) {
                    if (filter.accept( releaseId, entry.getKey() )) {
                        depSet.add(releaseId);
                    }
                }
            }
            return depSet;
        }

        protected void addDependency(AFReleaseId dependency, String scope) {
            Set<AFReleaseId> depsByScope = dependencies.get(scope);
            if (depsByScope == null) {
                depsByScope = new HashSet<AFReleaseId>();
                dependencies.put( scope, depsByScope );
            }
            depsByScope.add( dependency );
        }
    }

    class Parser {

        private static final Logger log = LoggerFactory.getLogger(PomModel.class);

        private static class PomModelGeneratorHolder {
            private static PomModelGenerator pomModelGenerator;

            static {
                try {
                    pomModelGenerator = (PomModelGenerator) Class.forName(NATIVE_MAVEN_PARSER_CLASS).newInstance();
                } catch (Exception e) {
                    pomModelGenerator = new DefaultPomModelGenerator();
                }
            }
        }

        public static PomModel parse(String path, InputStream is) {
            try {
                return PomModelGeneratorHolder.pomModelGenerator.parse(path, is);
            } catch (Exception e) {
                if (PomModelGeneratorHolder.pomModelGenerator.getClass().getName().equals(NATIVE_MAVEN_PARSER_CLASS) && isOpen(is)) {
                    log.warn("Error generated by the maven pom parser, falling back to the internal one", e);
                    return MinimalPomParser.parse(path, is);
                }
                if (e instanceof RuntimeException) {
                    throw (RuntimeException)e;
                } else {
                    throw new RuntimeException(e);
                }
            }
        }

        private static boolean isOpen(InputStream is) {
            try {
                return is.available() > 0;
            } catch (IOException ioe) {
                return false;
            }
        }
    }

    class DefaultPomModelGenerator implements PomModelGenerator {
        @Override
        public PomModel parse(String path, InputStream is) {
            return MinimalPomParser.parse(path, is);
        }
    }
}
