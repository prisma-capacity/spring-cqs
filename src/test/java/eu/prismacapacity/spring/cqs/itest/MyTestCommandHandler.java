/*
 * Copyright © 2023-2024 PRISMA European Capacity Platform GmbH 
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

import eu.prismacapacity.spring.cqs.cmd.CommandHandler;
import eu.prismacapacity.spring.cqs.cmd.CommandHandlingException;
import eu.prismacapacity.spring.cqs.cmd.CommandVerificationException;
import lombok.NonNull;
import org.springframework.stereotype.Component;

@Component
class MyTestCommandHandler implements CommandHandler<MyTestCommand> {
  @Override
  public void verify(@NonNull MyTestCommand query) throws CommandVerificationException {
    if (query.getAge() == 118) throw new RuntimeException("age=118 does not verify");
  }

  @Override
  public void handle(@NonNull MyTestCommand query) throws CommandHandlingException {
    if (query.getAge() == 119) throw new RuntimeException("age=119 fails to execute");
  }
}
