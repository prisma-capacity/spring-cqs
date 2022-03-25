/*
 * Copyright © 2022 PRISMA European Capacity Platform GmbH 
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
package eu.prismacapacity.spring.cqs.retry;

import eu.prismacapacity.spring.cqs.cmd.CommandValidationException;
import eu.prismacapacity.spring.cqs.query.QueryValidationException;
import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Inherited
public @interface RetryConfiguration {
  /** Limits maximum number of attempts to provided value. */
  int maxAttempts() default 3;

  /**
   * Either the interval for a fixed backoff or the start interval if exponential backoff is
   * configured.
   */
  long interval() default 20;

  /** Enables exponential backoff by setting the max interval. */
  long exponentialBackoffMaxInterval() default 0;

  /** Configures a list of throwables where no retry will be executed. */
  Class<? extends Throwable>[] notRetryOn() default {
    CommandValidationException.class, QueryValidationException.class
  };
}
