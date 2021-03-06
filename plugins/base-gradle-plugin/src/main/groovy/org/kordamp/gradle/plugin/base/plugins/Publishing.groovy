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
package org.kordamp.gradle.plugin.base.plugins

import groovy.transform.Canonical
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.gradle.api.Project

/**
 * @author Andres Almiray
 * @since 0.8.0
 */
@CompileStatic
@Canonical
class Publishing extends AbstractFeature {
    String releasesRepository
    String snapshotsRepository
    boolean signing = false

    private boolean signingSet

    Publishing(Project project) {
        super(project)
    }

    @Override
    String toString() {
        toMap().toString()
    }

    @Override
    @CompileDynamic
    Map<String, Map<String, Object>> toMap() {
        Map map = [enabled: enabled]

        if (enabled) {
            map.signing = signing
            map.releasesRepository = releasesRepository
            map.snapshotsRepository = snapshotsRepository
        }

        ['publishing': map]
    }

    void setSigning(boolean signing) {
        this.signing = signing
        this.signingSet = true
    }

    boolean isSigningSet() {
        this.signingSet
    }

    void copyInto(Publishing copy) {
        super.copyInto(copy)

        copy.@releasesRepository = releasesRepository
        copy.@snapshotsRepository = snapshotsRepository
        copy.@signing = this.signing
        copy.@signingSet = this.signingSet
    }

    static void merge(Publishing o1, Publishing o2) {
        AbstractFeature.merge(o1, o2)
        o1.releasesRepository = o1.@releasesRepository ?: o2.@releasesRepository
        o1.snapshotsRepository = o1.@snapshotsRepository ?: o2.@snapshotsRepository
        o1.setSigning((boolean) (o1.signingSet ? o1.signing : o2.signing))
    }
}
