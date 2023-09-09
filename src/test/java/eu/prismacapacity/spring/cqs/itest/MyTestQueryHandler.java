/*
 * Copyright © 2023 PRISMA European Capacity Platform GmbH 
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
package eu.prismacapacity.spring.cqs.itest;

import eu.prismacapacity.spring.cqs.query.QueryHandler;
import eu.prismacapacity.spring.cqs.query.QueryHandlingException;
import eu.prismacapacity.spring.cqs.query.QueryTimeoutException;
import eu.prismacapacity.spring.cqs.query.QueryVerificationException;
import lombok.NonNull;
import org.springframework.stereotype.Component;

@Component
class MyTestQueryHandler implements QueryHandler<MyTestQuery, Boolean> {
  @Override
  public void verify(@NonNull MyTestQuery query) throws QueryVerificationException {}

  @Override
  public @NonNull Boolean handle(@NonNull MyTestQuery query)
      throws QueryHandlingException, QueryTimeoutException {
    return true;
  }
}
