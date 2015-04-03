

import java.io.StringReader;

import com.thoughtworks.qdox.JavaDocBuilder;
import com.thoughtworks.qdox.model.JavaClass;

import junit.framework.TestCase;


public class ImportedEnumTest extends TestCase {

    public void testImportedEnum() {
    
        String source = ""
            + "import org.apache.commons.lang.enum.Enum;" +
                "public enum Enum1 { a, b }"
            + "class X { "
            + "  enum Enum2 { c, /** some doc */ d } "
            + "  int someField; "
            + "}";
        JavaDocBuilder javaDocBuilder = new JavaDocBuilder();
        javaDocBuilder.addSource(new StringReader(source));
        JavaClass cls = javaDocBuilder.getClassByName("X");
        assertEquals( "int" , cls.getFieldByName("someField").getType().getValue() ) ;
    }
}
