
import com.thoughtworks.qdox.JavaDocBuilder;
import com.thoughtworks.qdox.model.DefaultDocletTagFactory;
import com.thoughtworks.qdox.model.DocletTag;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaSource;
import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;

/**
 *
 * @dummytag dummy-parameter="this is a first line
 *      and this is a second one"
 * @author greg
 * @author $Author: $ (last edit)
 * @version $Revision:  $
 */
public class MultineLineAttributeValuesWithQDoxTestCase extends TestCase {
    public void testMultineLineAttributeValuesWorksWithQDox() throws IOException {
        JavaDocBuilder b = new JavaDocBuilder(new DefaultDocletTagFactory());
        JavaSource src = b.addSource(new File("/Users/greg/dev/projects/xdoclet-plugins/xdoclet-plugins/plugin-web/src/test/java/MultineLineAttributeValuesWithQDoxTestCase.java"));
        JavaClass c = src.getClasses()[0];
        DocletTag tag = c.getTagByName("dummytag");
        String value = tag.getNamedParameter("dummy-parameter");
        assertEquals(value, "this is a first line\nand this is a second one");
    }
}
