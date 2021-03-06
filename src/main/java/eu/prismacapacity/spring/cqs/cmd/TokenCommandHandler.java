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
 * A command handler that returns just a token as a response. Should be used if
 * you need an indication token, only.
 */
public interface TokenCommandHandler<C extends Command> extends ICommandHandler<C> {

	@NonNull
	CommandTokenResponse handle(@NonNull C cmd) throws CommandHandlingException;
}
