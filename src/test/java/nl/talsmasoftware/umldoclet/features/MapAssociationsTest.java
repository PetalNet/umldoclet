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
import nl.talsmasoftware.umldoclet.features.maps.GameHub;
import nl.talsmasoftware.umldoclet.util.TestUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.spi.ToolProvider;

import static org.assertj.core.api.Assertions.assertThat;

/// Test that fields and properties of `java.util.Map` types (and subtypes) render associations.
class MapAssociationsTest {
    static final File outputdir = new File("target/features/map-associations");
    static final String PKG = GameHub.class.getPackageName() + "::";
    static String packageUml;

    @BeforeAll
    static void generateJavadoc() {
        assertThat(ToolProvider.findFirst("javadoc").get().run(
                System.out, System.err,
                "-d", outputdir.getPath(),
                "-sourcepath", "src/test/java",
                "-doclet", UMLDoclet.class.getName(),
                "-quiet", "-createPumlFiles", "-private",
                GameHub.class.getPackageName()
        )).as("Javadoc result").isZero();
        packageUml = TestUtil.read(new File(outputdir, GameHub.class.getPackageName().replace('.', '/') + "/package.puml"));
    }

    @Test
    void mapFieldsRenderAssociationToValueType() {
        assertThat(packageUml).contains(
                PKG + "GameHub --> \"*\" " + PKG + "Customer: customers",
                PKG + "GameHub --> \"*\" " + PKG + "Purchasable: catalog");
        // The fields are represented by the associations.
        assertThat(packageUml).doesNotContain("-customers: HashMap", "-catalog: Map");
    }

    @Test
    void mapSubtypesRenderAssociationToValueType() {
        assertThat(packageUml).contains(
                PKG + "MapSubtypes --> \"*\" " + PKG + "Customer: sorted\\nregistry",
                PKG + "MapSubtypes --> \"*\" " + PKG + "Purchasable: concurrent");
    }

    @Test
    void mapGetterRendersAssociationToValueType() {
        assertThat(packageUml).contains(PKG + "MapGetter --> \"*\" " + PKG + "Purchasable: products");
    }

    @Test
    void mapKeyInNamespaceRendersKeyAssociation() {
        assertThat(packageUml).contains(
                PKG + "KeyedHub --> \"*\" " + PKG + "Customer: byId",
                PKG + "KeyedHub --> \"*\" " + PKG + "CustomerId: byId key");
    }

    @Test
    void mapKeyOutsideNamespaceRendersNoKeyAssociation() {
        assertThat(packageUml).doesNotContain("customers key", "catalog key", "java.lang::Integer", "--> \"*\" Integer");
    }

    @Test
    void mapWithValueOutsideNamespaceRendersNoAssociation() {
        assertThat(packageUml)
                .doesNotContain(PKG + "ForeignValues -->")
                .contains("-names: Map<Integer, String>");
    }

    @Test
    void nestedContainerInMapValueIsUnwrapped() {
        assertThat(packageUml).contains(PKG + "NestedValues --> \"*\" " + PKG + "Customer: byRegion");
    }

    @Test
    void mapInsideOptionalIsUnwrapped() {
        assertThat(packageUml).contains(
                PKG + "OptionalMap --> \"*\" " + PKG + "Purchasable: maybeCatalog",
                PKG + "OptionalMap --> \"*\" " + PKG + "CustomerId: maybeCatalog key");
    }

    @Test
    void arrayOfMapsIsUnwrapped() {
        assertThat(packageUml).contains(
                PKG + "MapArray --> \"*\" " + PKG + "Customer: shards",
                PKG + "MapArray --> \"*\" " + PKG + "CustomerId: shards key");
    }

    @Test
    void boundedWildcardMapValueIsUnwrapped() {
        assertThat(packageUml).contains(PKG + "WildcardMap --> \"*\" " + PKG + "Purchasable: offers");
    }

    @Test
    void rawMapIsNotUnwrapped() {
        assertThat(packageUml)
                .doesNotContain(PKG + "RawMap -->")
                .contains("-raw: Map");
    }

    @Test
    void mapTakesPriorityOverIterable() {
        assertThat(packageUml)
                .contains(PKG + "MapIterableHolder --> \"*\" " + PKG + "Customer: both")
                .doesNotContain(PKG + "MapIterableHolder --> \"*\" " + PKG + "Purchasable");
    }

    @Test
    void directIterableOptionalAndArrayBehaviourIsUnchanged() {
        assertThat(packageUml).contains(
                PKG + "DirectContainers --> \"*\" " + PKG + "Customer: list",
                PKG + "DirectContainers --> \"0..1\" " + PKG + "Purchasable: maybe",
                PKG + "DirectContainers --> \"*\" " + PKG + "CustomerId: ids");
    }

    @Test
    void containerElementInNamespaceIsReferencedDirectly() {
        assertThat(packageUml)
                .contains(PKG + "GroupHolder --> \"*\" " + PKG + "CustomerGroup: groups")
                .doesNotContain(PKG + "GroupHolder --> \"*\" " + PKG + "Customer:");
    }
}
