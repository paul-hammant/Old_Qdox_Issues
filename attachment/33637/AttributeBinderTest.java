package org.carrot2.util.attribute;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.carrot2.util.attribute.constraint.*;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings(value =
{
    "unchecked", "unused"
})
public class AttributeBinderTest
{
    private Map<String, Object> attributes;

    @Bindable
    public static class SingleClass
    {
        @TestInit
        @Input
        @Attribute
        private int initInput = 5;

        @TestInit
        @Output
        @Attribute
        private int initOutput = 10;

        @TestProcessing
        @Input
        @Attribute
        private int processingInput = 5;

        @TestProcessing
        @Output
        @Attribute
        private int processingOutput = 10;
    }

    @Bindable
    public static class SuperClass
    {
        @TestProcessing
        @Input
        @Attribute
        private int processingInput = 5;

        @TestProcessing
        @Output
        @Attribute
        private int processingOutput = 9;
    }

    @Bindable
    public static class SubClass extends SuperClass
    {
        @TestProcessing
        @Input
        @Attribute
        private int processingInput = 5;

        @TestProcessing
        @Output
        @Attribute
        private int processingOutput = 5;
    }

    @Bindable
    public static class InputReferenceContainerWithNoImplementingClass
    {
        @TestProcessing
        @Input
        @Attribute
        private BindableReference bindableReference;
    }

    @Bindable
    public static class OutputReferenceContainerWithNoImplementingClass
    {
        @TestProcessing
        @Output
        @Attribute
        private BindableReference bindableReference;
    }

    @Bindable
    public static class EnumAttributeContainer
    {
        @Input
        @Attribute
        private TestEnum enumInput;
    }

    public static enum TestEnum
    {
        VALUE1, VALUE2
    }

    @Bindable
    public static class BindableReferenceContainer
    {
        private BindableReference bindableReference = new BindableReference();

        private NotBindable notBindableReference = new NotBindable();
    }

    @Bindable
    public static class BindableReference
    {
        @TestProcessing
        @Input
        @Attribute
        private int processingInput = 5;

        @TestProcessing
        @Output
        @Attribute
        private int processingOutput = 5;
    }

    @Bindable
    public static class SimpleConstraint
    {
        @TestProcessing
        @Input
        @Attribute
        @IntRange(min = 0, max = 10)
        private int processingInput = 5;
    }

    @Bindable
    public static class CompoundConstraint
    {
        @TestProcessing
        @Input
        @Attribute
        @IntRange(min = 0, max = 10)
        @IntModulo(modulo = 3)
        private int processingInput = 3;
    }

    @Bindable
    public static class CoercedReferenceContainer
    {
        @Input
        @TestInit
        @Attribute
        @ImplementingClasses(classes =
        {
            CoercedInterfaceImpl.class
        })
        private CoercedInterface coerced = null;
    }

    @Bindable
    public static class CircularReferenceContainer
    {
        @TestProcessing
        @Input
        @Output
        @Attribute
        @ImplementingClasses(classes =
        {
            CircularReferenceContainer.class
        })
        private CircularReferenceContainer circular;
    }

    public static interface CoercedInterface
    {
    }

    @Bindable
    public static class CoercedInterfaceImpl implements CoercedInterface
    {
        @TestInit
        @Input
        @Attribute
        private int initInput = 5;
    }

    @Bindable(prefix = "Prefix")
    public static class ClassWithPrefix
    {
        @TestInit
        @Input
        @Attribute(key = "init")
        private int initInput = 5;

        @TestProcessing
        @Input
        @Attribute
        private int processingInput = 10;
    }

    @Bindable
    public static class NullReferenceContainer
    {
        @TestProcessing
        @Input
        @Attribute
        @ImplementingClasses(classes =
        {
            BindableReference.class
        })
        private BindableReference processingInput = null;
    }

    public static class NotBindable
    {
        @TestProcessing
        @Input
        @Attribute
        private int processingInput = 5;
    }

    @Bindable
    public static class OnlyBindingDirectionAnnotationProvided
    {
        @Input
        private int initInput;
    }

    @Bindable
    public static class OnlyBindingTimeAnnotationProvided
    {
        @TestInit
        private int initInput;
    }

    @Bindable
    public static class AttributeAnnotationWithoutBindingDirection
    {
        @TestInit
        @Attribute
        private int initInput;
    }

    @Bindable
    public static class RequiredInputAttributes
    {
        @TestInit
        @Input
        @Required
        @Attribute
        private int initInputInt;

        @TestInit
        @Input
        @Required
        @Attribute
        private String initInputString;
    }

    @Bindable
    public static class ClassValuedAttribute
    {
        @TestInit
        @Input
        @Output
        @Required
        @Attribute
        private Class initInputOutputClass;
    }

    @Bindable
    public static class NotBindableCoercedAttribute
    {
        @TestInit
        @Input
        @Required
        @Attribute
        @ImplementingClasses(classes =
        {
            NotBindable.class
        })
        private NotBindable initInputReference;
    }

    @Bindable
    public static class RequiredInitProcessingAttributes
    {
        @TestInit
        @TestProcessing
        @Input
        @Required
        @Attribute
        private String initProcessingInputString;
    }

    @Before
    public void initAttributes()
    {
        attributes = new HashMap<String, Object>();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalBindingDirection() throws InstantiationException
    {
        SingleClass instance = new SingleClass();
        AttributeBinder.bind(instance, attributes, Attribute.class, TestInit.class);
    }

    @Test
    public void testSingleClassInput() throws InstantiationException
    {
        SingleClass instance;

        addAttribute(SingleClass.class, "initInput", 6);
        addAttribute(SingleClass.class, "processingInput", 6);

        instance = new SingleClass();
        AttributeBinder.bind(instance, attributes, Input.class, TestInit.class);
        checkFieldValues(instance, new Object []
        {
            "initInput", 6, "processingInput", 5, "initOutput", 10, "processingOutput",
            10
        });

        instance = new SingleClass();
        AttributeBinder.bind(instance, attributes, Input.class, TestProcessing.class);
        checkFieldValues(instance, new Object []
        {
            "initInput", 5, "processingInput", 6, "initOutput", 10, "processingOutput",
            10
        });
    }

    @Test
    public void testSingleClassOutput() throws InstantiationException
    {
        final SingleClass instance = new SingleClass();

        AttributeBinder.bind(instance, attributes, Output.class, TestInit.class);
        checkAttributeValues(instance.getClass(), new Object []
        {
            "initOutput", 10
        });

        attributes.clear();
        AttributeBinder.bind(instance, attributes, Output.class, TestProcessing.class);
        checkFieldValues(instance, new Object []
        {
            "processingOutput", 10
        });
    }

    @Test
    public void testBindableHierarchyInput() throws InstantiationException
    {
        final SubClass instance = new SubClass();

        addAttribute(SubClass.class, "processingInput", 6);
        addAttribute(SuperClass.class, "processingInput", 7);

        AttributeBinder.bind(instance, attributes, Input.class, TestProcessing.class);
        checkFieldValues(instance, new Object []
        {
            "processingInput", 6
        });
        checkFieldValues(instance, SuperClass.class, new Object []
        {
            "processingInput", 7
        });
    }

    @Test
    public void testBindableHierarchyOutput() throws InstantiationException
    {
        final SubClass instance = new SubClass();

        AttributeBinder.bind(instance, attributes, Output.class, TestProcessing.class);
        checkAttributeValues(SubClass.class, new Object []
        {
            "processingOutput", 5
        });
        checkAttributeValues(SuperClass.class, new Object []
        {
            "processingOutput", 9
        });
    }

    @Test
    public void testReferenceInput() throws InstantiationException
    {
        final BindableReferenceContainer instance = new BindableReferenceContainer();
        addAttribute(BindableReference.class, "processingInput", 6);
        addAttribute(NotBindable.class, "processingInput", 7);

        AttributeBinder.bind(instance, attributes, Input.class, TestProcessing.class);

        checkFieldValues(instance.bindableReference, new Object []
        {
            "processingInput", 6
        });

        // Not bindable field must not change
        checkFieldValues(instance.notBindableReference, new Object []
        {
            "processingInput", 5
        });
    }

    @Test
    public void testReferenceOutput() throws InstantiationException
    {
        final BindableReferenceContainer instance = new BindableReferenceContainer();

        AttributeBinder.bind(instance, attributes, Output.class, TestProcessing.class);
        checkAttributeValues(BindableReference.class, new Object []
        {
            "processingOutput", 5
        });

        // Not bindable fields must not be collected
        assertFalse("Fields from not bindables not collected", attributes
            .containsKey(getKey(NotBindable.class, "processingInput")));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testCircularReferences() throws InstantiationException
    {
        final CircularReferenceContainer instance = new CircularReferenceContainer();
        instance.circular = instance;

        addAttribute(CircularReferenceContainer.class, "circular", instance);

        AttributeBinder.bind(instance, attributes, Input.class, TestProcessing.class);
    }

    @Test
    public void testSimpleConstraints() throws InstantiationException
    {
        final SimpleConstraint instance = new SimpleConstraint();

        addAttribute(SimpleConstraint.class, "processingInput", 2);
        AttributeBinder.bind(instance, attributes, Input.class, TestProcessing.class);
        checkFieldValues(instance, new Object []
        {
            "processingInput", 2
        });

        addAttribute(SimpleConstraint.class, "processingInput", 12);
        try
        {
            AttributeBinder.bind(instance, attributes, Input.class, TestProcessing.class);
            fail();
        }
        catch (final ConstraintViolationException e)
        {
            assertEquals(12, e.offendingValue);
        }
        checkFieldValues(instance, new Object []
        {
            "processingInput", 2
        });
    }

    @Test
    public void testCompoundConstraints() throws InstantiationException
    {
        final CompoundConstraint instance = new CompoundConstraint();

        addAttribute(CompoundConstraint.class, "processingInput", 9);
        AttributeBinder.bind(instance, attributes, Input.class, TestProcessing.class);
        checkFieldValues(instance, new Object []
        {
            "processingInput", 9
        });

        addAttribute(CompoundConstraint.class, "processingInput", 8);
        try
        {
            AttributeBinder.bind(instance, attributes, Input.class, TestProcessing.class);
            fail();
        }
        catch (final ConstraintViolationException e)
        {
            assertEquals(8, e.offendingValue);
        }
        checkFieldValues(instance, new Object []
        {
            "processingInput", 9
        });

        addAttribute(CompoundConstraint.class, "processingInput", 12);
        try
        {
            AttributeBinder.bind(instance, attributes, Input.class, TestProcessing.class);
            fail();
        }
        catch (final ConstraintViolationException e)
        {
            assertEquals(12, e.offendingValue);
        }
        checkFieldValues(instance, new Object []
        {
            "processingInput", 9
        });
    }

    @Test
    public void testClassCoercion() throws InstantiationException
    {
        final CoercedReferenceContainer instance = new CoercedReferenceContainer();

        addAttribute(CoercedReferenceContainer.class, "coerced",
            CoercedInterfaceImpl.class);
        addAttribute(CoercedInterfaceImpl.class, "initInput", 7);

        AttributeBinder.bind(instance, attributes, Input.class, TestInit.class);
        assertNotNull(instance.coerced);
        assertEquals(instance.coerced.getClass(), CoercedInterfaceImpl.class);
        checkFieldValues(instance.coerced, new Object []
        {
            "initInput", 7
        });
    }

    @Test
    public void testPrefixing() throws InstantiationException
    {
        final ClassWithPrefix instance = new ClassWithPrefix();

        attributes.put("init", 7);
        attributes.put("Prefix.procesingField", 6);

        AttributeBinder.bind(instance, attributes, Input.class, TestInit.class);
        AttributeBinder.bind(instance, attributes, Input.class, TestProcessing.class);

        checkFieldValues(instance, new Object []
        {
            "initInput", 7, "processingInput", 6
        });
    }

    @Test
    public void testNullReference() throws InstantiationException
    {
        final NullReferenceContainer instance = new NullReferenceContainer();

        addAttribute(BindableReference.class, "processingInput", 10);

        // Neither @Input nor @Output binding can fail
        AttributeBinder.bind(instance, attributes, Input.class, TestProcessing.class);
        AttributeBinder.bind(instance, attributes, Output.class, TestProcessing.class);
    }

    @Test
    public void testNullInputAttributes() throws InstantiationException
    {
        final NullReferenceContainer instance = new NullReferenceContainer();
        instance.processingInput = new BindableReference();

        addAttribute(NullReferenceContainer.class, "processingInput", null);

        AttributeBinder.bind(instance, attributes, Input.class, TestProcessing.class);

        assertNull(instance.processingInput);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNotBindable() throws InstantiationException
    {
        final NotBindable instance = new NotBindable();
        AttributeBinder.bind(instance, attributes, Input.class, TestInit.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testOnlyBindingDirectionAnnotationProvided()
        throws InstantiationException
    {
        final OnlyBindingDirectionAnnotationProvided instance = new OnlyBindingDirectionAnnotationProvided();
        AttributeBinder.bind(instance, attributes, Input.class, TestInit.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testOnlyBindingTimeAnnotationProvided() throws InstantiationException
    {
        final OnlyBindingTimeAnnotationProvided instance = new OnlyBindingTimeAnnotationProvided();
        AttributeBinder.bind(instance, attributes, Input.class, TestInit.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAttributeAnnotationWithoutBindingDirection()
        throws InstantiationException
    {
        final AttributeAnnotationWithoutBindingDirection instance = new AttributeAnnotationWithoutBindingDirection();
        AttributeBinder.bind(instance, attributes, Input.class, TestInit.class);
    }

    @Test(expected = AttributeBindingException.class)
    public void testRequiredInputAttributeNotProvided() throws InstantiationException
    {
        RequiredInputAttributes instance;
        instance = new RequiredInputAttributes();

        // Attribute value missing
        addAttribute(RequiredInputAttributes.class, "initInputInt", 6);

        AttributeBinder.bind(instance, attributes, Input.class, TestInit.class);
    }

    @Test(expected = AttributeBindingException.class)
    public void testRequiredInputAttributeIsNull() throws InstantiationException
    {
        RequiredInputAttributes instance;
        instance = new RequiredInputAttributes();

        // Attribute value missing
        addAttribute(RequiredInputAttributes.class, "initInputInt", 6);
        addAttribute(RequiredInputAttributes.class, "initInputString", null);

        AttributeBinder.bind(instance, attributes, Input.class, TestInit.class);
    }

    @Test
    public void testClassValuedAttribute() throws InstantiationException
    {
        ClassValuedAttribute instance;

        addAttribute(ClassValuedAttribute.class, "initInputOutputClass",
            CoercedInterfaceImpl.class);

        instance = new ClassValuedAttribute();
        AttributeBinder.bind(instance, attributes, Input.class, TestInit.class);
        checkFieldValues(instance, new Object []
        {
            "initInputOutputClass", CoercedInterfaceImpl.class
        });

        instance.initInputOutputClass = String.class;
        AttributeBinder.bind(instance, attributes, Output.class, TestInit.class);
        checkAttributeValues(ClassValuedAttribute.class, new Object []
        {
            "initInputOutputClass", String.class
        });
    }

    @Test
    public void testNotBindableCoercedAttribute() throws InstantiationException
    {
        NotBindableCoercedAttribute instance;

        addAttribute(NotBindableCoercedAttribute.class, "initInputReference",
            NotBindable.class);

        instance = new NotBindableCoercedAttribute();
        AttributeBinder.bind(instance, attributes, Input.class, TestInit.class);
        assertNotNull(instance.initInputReference);
    }

    @Test
    public void testRequiredAttributeAlreadyBound() throws InstantiationException
    {
        RequiredInitProcessingAttributes instance;
        instance = new RequiredInitProcessingAttributes();
        instance.initProcessingInputString = "test";

        // Attribute value missing, but should not cause exception
        AttributeBinder.bind(instance, attributes, Input.class, TestProcessing.class);
    }

    @Test(expected = AttributeBindingException.class)
    public void testRequiredAttributeNullAttempt() throws InstantiationException
    {
        RequiredInitProcessingAttributes instance;
        instance = new RequiredInitProcessingAttributes();
        instance.initProcessingInputString = "test";

        // An attempt to set the required attribute to null
        addAttribute(RequiredInitProcessingAttributes.class, "initProcessingInputString",
            null);

        AttributeBinder.bind(instance, attributes, Input.class, TestProcessing.class);
    }

    @Test
    public void testRequiredAttributeNullAttemptWithRequiredCheckingDisabled()
        throws InstantiationException
    {
        RequiredInitProcessingAttributes instance;
        instance = new RequiredInitProcessingAttributes();
        instance.initProcessingInputString = "test";

        // An attempt to set the required attribute to null
        addAttribute(RequiredInitProcessingAttributes.class, "initProcessingInputString",
            null);

        AttributeBinder.bind(instance, new AttributeBinder.AttributeBinderAction []
        {
            new AttributeBinder.AttributeBinderActionBind(Input.class, attributes, false)
        }, Input.class, TestProcessing.class);
    }

    @Test
    public void testSingleClassUnbind() throws InstantiationException
    {
        final SingleClass instance = new SingleClass();
        instance.processingInput = 6;

        AttributeBinder.unbind(instance, attributes, Input.class);
        checkAttributeValues(instance.getClass(), new Object []
        {
            "initInput", 5, "processingInput", 6
        });
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInputReferenceAttributeWithoutImplementingClass()
        throws AttributeBindingException, InstantiationException
    {
        final InputReferenceContainerWithNoImplementingClass instance = new InputReferenceContainerWithNoImplementingClass();

        AttributeBinder.bind(instance, attributes, Input.class);
    }

    @Test
    public void testOutputReferenceAttributeWithoutImplementingClass()
        throws AttributeBindingException, InstantiationException
    {
        final OutputReferenceContainerWithNoImplementingClass instance = new OutputReferenceContainerWithNoImplementingClass();

        AttributeBinder.bind(instance, attributes, Output.class);
    }

    @Test
    public void testEnumInput() throws InstantiationException
    {
        EnumAttributeContainer instance = new EnumAttributeContainer();

        addAttribute(EnumAttributeContainer.class, "enumInput", TestEnum.VALUE1);
        AttributeBinder.bind(instance, attributes, Input.class);
        checkFieldValues(instance, new Object []
        {
            "enumInput", TestEnum.VALUE1
        });
    }

    private void addAttribute(Class<?> clazz, String field, Object value)
    {
        attributes.put(getKey(clazz, field), value);
    }

    private String getKey(Class<?> clazz, String fieldName)
    {
        if (clazz.getAnnotation(Bindable.class) != null)
        {
            return AttributeUtils.getKey(clazz, fieldName);
        }
        else
        {
            return clazz.getName() + "." + fieldName;
        }
    }

    private void checkFieldValues(Object instance, Object [] fieldNamesValues)
    {
        checkFieldValues(instance, instance.getClass(), fieldNamesValues);
    }

    private void checkFieldValues(Object instance, Class<?> clazz,
        Object [] fieldNamesValues)
    {
        assertTrue(fieldNamesValues.length % 2 == 0);
        for (int i = 0; i < fieldNamesValues.length / 2; i += 2)
        {
            final String fieldName = (String) fieldNamesValues[i];
            final Object expectedFieldValue = fieldNamesValues[i + 1];

            Object actualFieldValue = null;
            try
            {
                final Field declaredField = clazz.getDeclaredField(fieldName);
                declaredField.setAccessible(true);

                actualFieldValue = declaredField.get(instance);
            }
            catch (final Exception e)
            {
                throw new RuntimeException(e);
            }

            assertEquals("Value of " + clazz.getName() + "#" + fieldName,
                expectedFieldValue, actualFieldValue);
        }
    }

    private void checkAttributeValues(Class<?> clazz, Object [] keysValues)
    {
        assertTrue(keysValues.length % 2 == 0);

        for (int i = 0; i < keysValues.length / 2; i += 2)
        {
            final String key = clazz.getName() + "." + (String) keysValues[i];
            final Object expectedValue = keysValues[i + 1];
            final Object actualValue = attributes.get(key);

            assertEquals("Value of " + clazz.getName() + "#" + keysValues[i],
                expectedValue, actualValue);
        }
    }
}
