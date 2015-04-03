package org.carrot2.util.attribute;

import org.carrot2.util.attribute.constraint.ImplementingClasses;

/**
 *
 */
public class AnnotationTest
{
    static class X
    {
        @ImplementingClasses(classes = {})
        private Object test;
    }
}
