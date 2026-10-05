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
import nl.talsmasoftware.umldoclet.features.methoddeps.Shop;
import nl.talsmasoftware.umldoclet.util.TestUtil;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.spi.ToolProvider;

import static org.assertj.core.api.Assertions.assertThat;

/// Test the `--uml-method-dependencies` option.
class MethodDependenciesTest {
    static final String PKG = Shop.class.getPackageName() + "::";

    static String generatePackageUml(String outputDir, String... extraOptions) {
        List<String> args = new ArrayList<>(List.of(
                "-d", outputDir,
                "-sourcepath", "src/test/java",
                "-doclet", UMLDoclet.class.getName(),
                "-quiet", "-createPumlFiles"));
        args.addAll(List.of(extraOptions));
        args.add(Shop.class.getPackageName());
        assertThat(ToolProvider.findFirst("javadoc").get().run(System.out, System.err, args.toArray(new String[0])))
                .as("Javadoc result").isZero();
        return TestUtil.read(new File(outputDir, Shop.class.getPackageName().replace('.', '/') + "/package.puml"));
    }

    @Test
    void noMethodDependenciesByDefault() {
        String uml = generatePackageUml("target/features/method-dependencies/off");
        assertThat(uml).doesNotContain("..>");
        // Associations are unaffected.
        assertThat(uml).contains(PKG + "Shop --> " + PKG + "Product: featured");
    }

    @Test
    void methodDependenciesWithOption() {
        String uml = generatePackageUml("target/features/method-dependencies/on", "--uml-method-dependencies");
        assertThat(uml).contains(
                PKG + "Shop ..> " + PKG + "Order",
                PKG + "Shop ..> " + PKG + "Invoice",
                PKG + "Shop ..> " + PKG + "Coupon");
    }

    @Test
    void methodDependenciesWithLegacyOptionName() {
        String uml = generatePackageUml("target/features/method-dependencies/legacy", "-umlMethodDependencies");
        assertThat(uml).contains(PKG + "Shop ..> " + PKG + "Order");
    }

    @Test
    void methodDependenciesAreDeduplicatedAndSkipAssociations() {
        String uml = generatePackageUml("target/features/method-dependencies/dedup", "--uml-method-dependencies");
        // Invoice is used twice, but rendered once.
        assertThat(uml.split(java.util.regex.Pattern.quote(PKG + "Shop ..> " + PKG + "Invoice"), -1)).hasSize(2);
        // Product already has a field association, Voucher a getter association.
        assertThat(uml)
                .contains(PKG + "Shop --> " + PKG + "Product: featured", PKG + "Shop --> " + PKG + "Voucher: voucher")
                .doesNotContain(PKG + "Shop ..> " + PKG + "Product", PKG + "Shop ..> " + PKG + "Voucher");
        // No self-dependency, and the private method is not visible.
        assertThat(uml).doesNotContain(PKG + "Shop ..> " + PKG + "Shop", PKG + "Shop ..> " + PKG + "Receipt");
    }
}
