package ninja.cero.sqltemplate.core.parameter;

import ninja.cero.sqltemplate.core.util.BeanFields;
import ninja.cero.sqltemplate.core.util.Jsr310JdbcUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.jdbc.core.namedparam.AbstractSqlParameterSource;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * {@link org.springframework.jdbc.core.namedparam.SqlParameterSource} implementation that obtains parameter values
 * from public fields of a given value object.
 * Supports {@link java.time.LocalDateTime} and {@link java.time.LocalDate} of JSR-310
 */
public class BeanParameter extends AbstractSqlParameterSource {
    /** the value object for parameters */
    protected Object entity;

    /** BeanWrapper for beans with private fields and accessor methods. */
    protected BeanWrapper beanWrapper;

    /** Set of the fields for beans with private fields. */
    protected Set<String> privateFields = new HashSet<>();


    /** Map of the fields for beans with public fields. */
    protected Map<String, Field> publicFeilds = new HashMap<>();

    /** ZoneId for OffsetDateTime and ZonedDateTime */
    protected ZoneId zoneId;

    /**
     * Create a new BeanParameter for the given value object.
     *
     * @param entity the value object for parameters
     * @param zoneId zoneId
     */
    public BeanParameter(Object entity, ZoneId zoneId) {
        init(entity);
        this.zoneId = zoneId;
    }

    protected void init(Object entity) {
        this.entity = entity;

        beanWrapper = PropertyAccessorFactory.forBeanPropertyAccess(entity);
        PropertyDescriptor[] descriptors = beanWrapper.getPropertyDescriptors();
        if (descriptors.length > 1) {
            for (PropertyDescriptor descriptor : descriptors) {
                if (beanWrapper.isReadableProperty(descriptor.getName())) {
                    privateFields.add(descriptor.getName());
                }
            }
        }

        Field[] fields = BeanFields.get(entity.getClass());
        if (fields != null) {
            for (Field field : fields) {
                publicFeilds.put(field.getName(), field);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasValue(String paramName) {
        return privateFields.contains(paramName) || publicFeilds.containsKey(paramName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getValue(String paramName) {
        Object value = null;
        if (privateFields.contains(paramName)) {
            value = beanWrapper.getPropertyValue(paramName);
        } else if (publicFeilds.containsKey(paramName)) {
            Field field = publicFeilds.get(paramName);
            try {
                value = field.get(entity);
            } catch (IllegalAccessException e) {
                throw new IllegalArgumentException(e);
            }
        }

        if (value == null) {
            return null;
        }

        return Jsr310JdbcUtils.convertIfNecessary(value, zoneId);
    }

    // TODO: Override getSqlType
}
