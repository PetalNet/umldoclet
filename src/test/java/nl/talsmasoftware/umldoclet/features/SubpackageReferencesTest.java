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
package nl.talsmasoftware.umldoclet.features;

import nl.talsmasoftware.umldoclet.UMLDoclet;
import nl.talsmasoftware.umldoclet.features.subpkg.Owner;
import nl.talsmasoftware.umldoclet.features.subpkg.sub.Region;
import nl.talsmasoftware.umldoclet.util.TestUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.spi.ToolProvider;

import static org.assertj.core.api.Assertions.assertThat;

/// Map associations and method dependencies must only point to types rendered in the same package diagram,
/// never into a subpackage (both packages are documented in this test).
class SubpackageReferencesTest {
    static final File outputdir = new File("target/features/subpackage-references");
    static final String PKG = Owner.class.getPackageName() + "::";
    static final String REGION = Region.class.getPackageName() + "::Region";
    static String packageUml;

    @BeforeAll
    static void generateJavadoc() {
        assertThat(ToolProvider.findFirst("javadoc").get().run(
                System.out, System.err,
                "-d", outputdir.getPath(),
                "-sourcepath", "src/test/java",
                "-doclet", UMLDoclet.class.getName(),
                "-quiet", "-createPumlFiles", "-private", "--uml-method-dependencies",
                Owner.class.getPackageName(), Region.class.getPackageName()
        )).as("Javadoc result").isZero();
        packageUml = TestUtil.read(new File(outputdir, Owner.class.getPackageName().replace('.', '/') + "/package.puml"));
    }

    /// Unchanged upstream behaviour: Namespace.contains also matches subpackages for plain fields.
    @Test
    void plainFieldToSubpackageKeepsUpstreamBehaviour() {
        assertThat(packageUml).contains(PKG + "PlainOwner --> " + REGION + ": region");
    }

    @Test
    void mapAndNestedFieldsDoNotPointIntoSubpackage() {
        assertThat(packageUml)
                .doesNotContain(PKG + "Owner --> ")
                .contains("-regions: Map<String, Region>", "-nested: List<Map<String, Region>>");
    }

    @Test
    void methodDependenciesDoNotPointIntoSubpackage() {
        assertThat(packageUml)
                .contains(PKG + "Owner ..> " + PKG + "Local")
                .doesNotContain(PKG + "Owner ..> " + REGION);
    }
}
