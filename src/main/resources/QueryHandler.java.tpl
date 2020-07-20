package %1$s;

import java.util.Objects;
import java.util.Set;
import javax.annotation.Generated;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import eu.prismacapacity.spring.cqs.metrics.QueryMetrics;
import eu.prismacapacity.spring.cqs.query.QueryHandlingException;
import eu.prismacapacity.spring.cqs.query.QueryValidationException;
import eu.prismacapacity.spring.cqs.query.QueryVerificationException;

@Generated(value = "%6$s")
@Component
@Primary
public class %2$s extends %3$s {
    private final Validator validator;
    private final QueryMetrics metrics;

    public %2$s(Validator validator, QueryMetrics metrics%8$s) {
        super(%9$s);
        this.validator = Objects.requireNonNull(validator);
        this.metrics = Objects.requireNonNull(metrics);
    }

    public %5$s handle(%4$s query) {

        return metrics.timedQuery(getClass().getCanonicalName(), () -> {

            // validator based validate
            Set<ConstraintViolation<%4$s>> violations = validator.validate(query);
            if (!violations.isEmpty()) {
                throw new QueryValidationException(violations);
            }

            // custom validate
            try {
                validate(query);
            } catch (QueryValidationException e) {
                throw e;
            } catch (Throwable e) {
                throw new QueryValidationException(e);
            }

            // verification
            try {
                verify(query);
            } catch (QueryVerificationException e) {
                throw e;
            } catch (Throwable e) {
                throw new QueryVerificationException(e);
            }

            // execution
            try {
                %5$s result = super.handle(query);
                if (result == null) {
                    throw new QueryHandlingException("Response must not be null");
                }
                return result;
            } catch (QueryHandlingException e) {
                throw e;
            } catch (Throwable e) {
                throw new QueryHandlingException(e);
            }
        });
    }
}
