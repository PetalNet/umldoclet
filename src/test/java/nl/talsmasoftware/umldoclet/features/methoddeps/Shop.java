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
package nl.talsmasoftware.umldoclet.features.methoddeps;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/// Uses other types in its method signatures.
public class Shop {
    /// Field association: method parameters of this type must not add a dependency.
    protected Product featured;

    /// Default constructor.
    public Shop() {
    }

    /// Parameter (unwrapped from a List) and return types become dependencies.
    ///
    /// @param order  The order.
    /// @param extras Extra products.
    /// @return The invoice.
    public Invoice checkout(Order order, List<Product> extras) {
        return null;
    }

    /// Second use of Invoice: must not add a second dependency.
    ///
    /// @param id The id.
    /// @return The invoice, if found.
    public Optional<Invoice> findInvoice(String id) {
        return Optional.empty();
    }

    /// Map key types are unwrapped too.
    ///
    /// @param discounts Discounts by coupon.
    public void applyCoupons(Map<Coupon, Integer> discounts) {
    }

    /// Getter that is rendered as association: must not add a dependency.
    ///
    /// @return The voucher.
    public Voucher getVoucher() {
        return null;
    }

    /// Self references never become a dependency.
    ///
    /// @return This shop.
    public Shop self() {
        return this;
    }

    /// Private methods are not visible by default and must not add a dependency.
    private Receipt internalReceipt() {
        return null;
    }
}
