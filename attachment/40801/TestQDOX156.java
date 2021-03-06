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
import com.thoughtworks.qdox.model.JavaField;

/**
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id: $
 */
public class TestQDOX156
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

            if ( !clazz.getName().equals( "TestQDOX156" ) )
            {
                continue;
            }

            System.out.println( "Looking the class: " + clazz.getFullyQualifiedName() );

            for ( int j = 0; j < clazz.getFields().length; j++ )
            {
                JavaField field = clazz.getFields()[j];
                System.out.println( field.getName() + "=" + field.getInitializationExpression() );
            }
        }
    }

    public static final String TEST1 = "test1";

    // TODO: blabla
    public static final String TEST2 = "test2";

    public static final String TEST3 = "test3"; public static final String TEST4 = "test4";
}
