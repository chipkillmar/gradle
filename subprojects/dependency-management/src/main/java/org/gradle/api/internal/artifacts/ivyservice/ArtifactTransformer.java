/*
 * Copyright 2016 the original author or authors.
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

package org.gradle.api.internal.artifacts.ivyservice;

import com.google.common.io.Files;
import org.gradle.api.Attribute;
import org.gradle.api.AttributeContainer;
import org.gradle.api.Nullable;
import org.gradle.api.Transformer;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.attributes.ArtifactExtension;
import org.gradle.api.artifacts.attributes.ArtifactName;
import org.gradle.api.artifacts.attributes.ArtifactType;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.internal.DefaultAttributeContainer;
import org.gradle.api.internal.artifacts.DefaultResolvedArtifact;
import org.gradle.api.internal.artifacts.configurations.ResolutionStrategyInternal;
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.artifact.ArtifactVisitor;
import org.gradle.internal.Factory;
import org.gradle.internal.component.model.DefaultIvyArtifactName;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArtifactTransformer {
    private final String format;
    private final ResolutionStrategyInternal resolutionStrategy;
    private final Map<File, File> transformed = new HashMap<File, File>();

    public ArtifactTransformer(@Nullable String format, ResolutionStrategyInternal resolutionStrategy) {
        this.format = format;
        this.resolutionStrategy = resolutionStrategy;
    }

    public ArtifactVisitor visitor(final ArtifactVisitor visitor) {
        if (format == null) {
            return visitor;
        }
        return new ArtifactVisitor() {
            @Override
            public void visitArtifact(final ResolvedArtifact artifact) {
                if (artifact.getType().equals(format)) {
                    visitor.visitArtifact(artifact);
                    return;
                }
                final Transformer<File, File> transform = resolutionStrategy.getTransform(artifact.getType(), format);
                if (transform == null) {
                    return;
                }
                visitor.visitArtifact(new DefaultResolvedArtifact(artifact.getModuleVersion(), new DefaultIvyArtifactName(artifact.getName(), format, artifact.getExtension()), artifact.getId(), new Factory<File>() {
                    @Override
                    public File create() {
                        File file = artifact.getFile();
                        File transformedFile = transformed.get(file);
                        if (transformedFile == null) {
                            transformedFile = transform.transform(file);
                            transformed.put(file, transformedFile);
                        }
                        return transformedFile;
                    }
                }));
            }

            @Override
            public boolean includeFiles() {
                return visitor.includeFiles();
            }

            @Override
            public void visitFiles(@Nullable ComponentIdentifier componentIdentifier, Iterable<File> files) {
                List<File> result = new ArrayList<File>();
                for (File file : files) {
                    String fileFormat = Files.getFileExtension(file.getName());
                    if (format.equals(fileFormat)) {
                    AttributeContainer attributeContainer = defaultFileAttributes(file);
                        result.add(file);
                        continue;
                    }
                    File transformedFile = transformed.get(file);
                    if (transformedFile != null) {
                        result.add(transformedFile);
                        continue;
                    }
                    Transformer<File, File> transform = resolutionStrategy.getTransform(fileFormat, format);
                    if (transform == null) {
                        continue;
                    }
                    transformedFile = transform.transform(file);
                    transformed.put(file, transformedFile);
                    result.add(transformedFile);
                }
                if (!result.isEmpty()) {
                    visitor.visitFiles(componentIdentifier, result);
                }
            }

            private AttributeContainer defaultFileAttributes(File file) {
                DefaultAttributeContainer attributes = new DefaultAttributeContainer();
                attributes.attribute(Attribute.of(ArtifactName.class), new ArtifactName(file.getName()));
                String fileExtension = Files.getFileExtension(file.getName());
                if (!"".equals(fileExtension)) {
                    attributes.attribute(Attribute.of(ArtifactType.class), new ArtifactType(fileExtension));
                    attributes.attribute(Attribute.of(ArtifactExtension.class), new ArtifactExtension(fileExtension));
                }
                return attributes;
            }
        };
    }
}
