/*
 * Copyright 2016-2026 Talsma ICT
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.talsmasoftware.umldoclet.features.subpkg;

import nl.talsmasoftware.umldoclet.features.subpkg.sub.Region;

import java.util.List;
import java.util.Map;

/// New paths (Map, nested, method dependencies) must not point into a subpackage.
public class Owner {
    private Map<String, Region> regions;
    private List<Map<String, Region>> nested;
    private Map<Region, String> byRegion;

    /// Default constructor.
    public Owner() {
    }

    /// Returns a subpackage type.
    ///
    /// @param name The name.
    /// @return The region.
    public Region locate(String name) {
        return null;
    }

    /// Returns a same-package type.
    ///
    /// @return The local.
    public Local local() {
        return null;
    }
}
