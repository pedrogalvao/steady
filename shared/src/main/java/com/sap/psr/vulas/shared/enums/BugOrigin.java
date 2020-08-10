/**
 * This file is part of Eclipse Steady.
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
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (c) 2018 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sap.psr.vulas.shared.enums;

import com.sap.psr.vulas.shared.json.model.Bug;

/**
 * Defines the origin of a given {@link Bug} in the sense of the type of information used for creating it.
 *
 * {@link BugOrigin#PUBLIC} means that the bug is publicly disclosed and a description is provided in a databases and/or issue tracker.
 * {@link BugOrigin#MCHLRN} means that the bug has been found by analyzing commits (using Machine Learning).
 * {@link BugOrigin#SRCSCN} means that the bug has been found by static source code scans.
 */
public enum BugOrigin {
    PUBLIC((byte) 1),
    MCHLRN((byte) 2),
    SRCSCN((byte) 4),
    PRIVAT((byte) 8);
    private byte value;

    private BugOrigin(byte _value) {
        this.value = _value;
    }
    /**
     * <p>toString.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String toString() {
        if (this.value == 1) return "PUBLIC";
        else if (this.value == 2) return "MCHLRN";
        else if (this.value == 4) return "SRCSCN";
        else if (this.value == 8) return "PRIVAT";
        else throw new IllegalArgumentException("[" + this.value + "] is not a valid origin");
    }
}
