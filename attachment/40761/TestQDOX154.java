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

import com.thoughtworks.qdox.JavaDocBuilder;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaMethod;

/**
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id: $
 */
public class TestQDOX154
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

            if ( !clazz.getName().equals( "TestQDOX154" ) )
            {
                continue;
            }

            System.out.println( "Looking the class: " + clazz.getFullyQualifiedName() );

            for ( int j = 0; j < clazz.getMethods().length; j++ )
            {
                JavaMethod javaMethod = clazz.getMethods()[j];

                if ( !javaMethod.getName().equals( "getSize" ) && !javaMethod.getName().equals( "getSize2" ) )
                {
                    continue;
                }

                System.out.println( "Looking the method: " + javaMethod.getName() );

                System.out.println( "javaMethod.getComment()=" + javaMethod.getComment() );

                System.out.println( "javaMethod.getTagByName( \"return\" )="
                    + javaMethod.getTagByName( "return" ).getValue() );

            }
        }
    }

    /**
     * A Javadoc sample.
     *
     * @return The size.
     */
    public long getSize()
    {
        return 0;
    }

    /**
     * @return The size.
     *
     * A Javadoc sample.
     */
    public long getSize2()
    {
        return 0;
    }
}
