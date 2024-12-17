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

import eu.prismacapacity.spring.cqs.Violations;
import jakarta.validation.ConstraintViolation;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NonNull;

/**
 * happened while bean validation of the incoming command, or during execution of the validate
 * method of a ({@link RespondingCommandHandler} or {@link TokenCommandHandler}
 */
public class CommandValidationException extends CommandHandlingException {
  @Getter
  private Set<? extends ConstraintViolation<? extends Command>> violations = new HashSet<>();

  public CommandValidationException(
      @NonNull Set<? extends ConstraintViolation<? extends Command>> violations) {
    super(Violations.render(violations));
    this.violations = violations;
  }

  public CommandValidationException(@NonNull String msg, @NonNull Throwable e) {
    super(msg, e);
  }

  public CommandValidationException(@NonNull Throwable e) {
    super(e);
  }

  public static CommandValidationException wrap(@NonNull Throwable e) {
    if (e instanceof CommandValidationException) return (CommandValidationException) e;
    else return new CommandValidationException(e);
  }
}
