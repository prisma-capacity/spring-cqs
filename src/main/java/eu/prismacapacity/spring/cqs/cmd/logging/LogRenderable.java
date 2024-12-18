/*
 * Copyright © 2024 PRISMA European Capacity Platform GmbH 
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
package eu.prismacapacity.spring.cqs.cmd.logging;

import eu.prismacapacity.spring.cqs.cmd.LogRenderer;

public interface LogRenderable {
  /**
   * This method is supposed to return a human readable display of the command's type and its
   * values. The values might be masked or filtered as necessary. The expected format is
   * 'Type(property=value,property2,value)'.
   *
   * <p>There is no need for escaping as the output will not be parsed anywhere.
   *
   * @return the string to log
   */
  default String toLogString() {
    return LogRenderer.renderDefault(this);
  }
}
