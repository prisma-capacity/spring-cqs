package %1$s;

import java.util.Objects;
import java.util.Set;
import javax.annotation.Generated;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import eu.prismacapacity.spring.cqs.metrics.CommandMetrics;
import eu.prismacapacity.spring.cqs.cmd.CommandHandlingException;
import eu.prismacapacity.spring.cqs.cmd.CommandValidationException;
import eu.prismacapacity.spring.cqs.cmd.CommandVerificationException;

@Generated(value = "%6$s")
@Component
@Primary
public class %2$s extends %3$s {
    private final Validator validator;
    private final CommandMetrics metrics;

    public %2$s(Validator validator, CommandMetrics metrics%8$s) {
        super(%9$s);
        this.validator = Objects.requireNonNull(validator);
        this.metrics = Objects.requireNonNull(metrics);
    }

    public %5$s handle(%4$s command) {

        return metrics.timedCommand(getClass().getCanonicalName(), () -> {

            // validator based validate
            Set<ConstraintViolation<%4$s>> violations = validator.validate(command);
            if (!violations.isEmpty()) {
                throw new CommandValidationException(violations);
            }

            // custom validate
            try {
                validate(command);
            } catch (CommandValidationException e) {
                throw e;
            } catch (Throwable e) {
                throw new CommandValidationException(e);
            }

            // verification
            try {
                verify(command);
            } catch (CommandVerificationException e) {
                throw e;
            } catch (Throwable e) {
                throw new CommandVerificationException(e);
            }

            // execution
            try {
                %5$s result = super.handle(command);
                if (result == null) {
                    throw new CommandHandlingException("Response must not be null");
                }
                return result;
            } catch (CommandHandlingException e) {
                throw e;
            } catch (Throwable e) {
                throw new CommandHandlingException(e);
            }
        });
    }
}
