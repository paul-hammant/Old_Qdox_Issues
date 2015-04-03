package test;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.thoughtworks.qdox.JavaDocBuilder;
import com.thoughtworks.qdox.model.DocletTag;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaMethod;

/**
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id: $
 */
public class TestQDOX173
{
    public static void main( String[] args )
        throws Exception
    {
        String basedir = new File( "" ).getAbsolutePath();
        File src = new File( basedir, "src/main/java" ); // a la Maven
        JavaDocBuilder builder = new JavaDocBuilder();
        builder.setEncoding( "UTF-8" );
        builder.addSourceTree( src );

        JavaClass[] classes = builder.getClasses();
        for ( int i = 0; i < classes.length; i++ )
        {
            JavaClass clazz = classes[i];

            if ( !clazz.getName().equals( "TestQDOX173" ) )
            {
                continue;
            }

            System.out.println( "Looking the class: " + clazz.getFullyQualifiedName() );

            for ( int j = 0; j < clazz.getMethods().length; j++ )
            {
                JavaMethod javaMethod = clazz.getMethods()[j];

                if ( !javaMethod.getName().equals( "dummyMethod" ) )
                {
                    continue;
                }

                System.out.println( "Looking the method: " + javaMethod.getName() );
                System.out.println( "javaMethod.toString()=" + javaMethod.toString() );
                for ( int k = 0; k < javaMethod.getTags().length; k++ )
                {
                    DocletTag docletTag = javaMethod.getTags()[k];

                    String[] params = docletTag.getParameters();
                    System.out.println( "docletTag.getParameters()=" + Arrays.toString( params ) );
                    if ( docletTag.getParameters()[0].equals( "<" ) )
                    {
                        String param = params[1];
                        List l = new ArrayList( Arrays.asList( docletTag.getParameters() ) );
                        l.set( 1, "<" + param + ">" );
                        l.remove( 0 );
                        l.remove( 1 );
                        System.out.println( "\tWRONG should be the array: "
                            + Arrays.toString( l.toArray( new String[0] ) ) );
                    }
                }
            }
        }
    }

    /**
     * Dummy method.
     *
     * @param <K>  The Key type for the method
     * @param <V>  The Value type for the method
     * @param name The name.
     * @return A map configured.
     */
    public <K, V> java.util.Map<K, V> dummyMethod( String name )
    {
        return null;
    }
}
