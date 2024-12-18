/*
 * Copyright © 2020-2024 PRISMA European Capacity Platform GmbH 
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
package eu.prismacapacity.spring.cqs.cmd;

import eu.prismacapacity.spring.cqs.StateToken;
import eu.prismacapacity.spring.cqs.cmd.logging.LogRenderable;
import lombok.Value;

/** Both token and value can be null. */
@Value(staticConstructor = "of")
public class CommandValueResponse<T> implements LogRenderable {

  StateToken token;
  T value;

  public static <T> CommandValueResponse<T> empty() {
    return of(null, null);
  }
}
