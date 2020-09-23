/*
 * Copyright © 2020 PRISMA European Capacity Platform GmbH
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

import lombok.NonNull;

/**
 * happened during execution of the verify method of a
 * ({@link RespondingCommandHandler} or {@link CommandHandler}
 */

public class CommandVerificationException extends CommandHandlingException {

	public CommandVerificationException(@NonNull Throwable e) {
		super(e);
	}

	public CommandVerificationException(@NonNull String msg, @NonNull Throwable e) {
		super(msg, e);
	}

	public CommandVerificationException(@NonNull String msg) {
		super(msg);
	}
}
