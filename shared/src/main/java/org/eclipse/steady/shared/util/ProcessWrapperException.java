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
 * SPDX-FileCopyrightText: Copyright (c) 2018-2020 SAP SE or an SAP affiliate company and Eclipse Steady contributors
 */
package org.eclipse.steady.shared.util;

/**
 * Thrown to indicate a problem when calling OS-level services.
 */
public class ProcessWrapperException extends Exception {

  private static final long serialVersionUID = 1L;

  /**
   * <p>Constructor for ProcessWrapperException.</p>
   *
   * @param _message a {@link java.lang.String} object.
   */
  public ProcessWrapperException(String _message) {
    super(_message);
  }

  /**
   * <p>Constructor for ProcessWrapperException.</p>
   *
   * @param _message a {@link java.lang.String} object.
   * @param _cause a {@link java.lang.Throwable} object.
   */
  public ProcessWrapperException(String _message, Throwable _cause) {
    super(_message, _cause);
  }
}
