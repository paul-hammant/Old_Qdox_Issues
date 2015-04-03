
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Map;

import org.junit.Test;

import com.thoughtworks.qdox.JavaDocBuilder;
import com.thoughtworks.qdox.model.Annotation;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaField;

/** */
public class QDoxFailure {


	// Change as needed
	private static final String SRC_FILE = "SRC-test/Instruction.java";

	/** @throws Exception a */
	@SuppressWarnings("unchecked")
	@Test
	public void testAnnotationMap() throws Exception{
		JavaDocBuilder javaDocBuilder = new JavaDocBuilder();
		javaDocBuilder.addSource(new File(SRC_FILE));
		JavaClass classByName = javaDocBuilder.getClassByName("Instruction");
		JavaField fieldByName = classByName.getFieldByName("testfield");
		Annotation[] annotations = fieldByName.getAnnotations();
		// Now we do have the annotation "JoinColumn" in annotations[0]
		Map<String,String> propertyMap = annotations[0].getNamedParameterMap();
		// This one works
		assertEquals("\"hi\"", propertyMap.get("bla"));
		String string = propertyMap.get("name");
		// This one does not work
		assertEquals("\"test\"", string);
	}

}

