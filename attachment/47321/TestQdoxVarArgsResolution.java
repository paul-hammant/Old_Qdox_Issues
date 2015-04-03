package ch.kwsoft.meta.spec;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.thoughtworks.qdox.model.*;

/**
 * Unit-Test illustrating the issue QDOX-201.
 * 
 * The method {@link JavaParameter#getType()} gives incorrect results for
 * vararg-parameters, resulting in {@link JavaClass#getMethodBySignature(String, Type[])}
 * failing to correctly lookup existing methods.
 * <p/>
 * {@link JavaParameter#getType()} should return an array-Type of dimension 1
 * for vararg-parameters (instead of just the basic type), as this is what
 * java compilers do when generating byte-code for vararg-methods
 * 
 * @author Samuel Bernet <samuel.bernet@kwsoft.ch>
 */
public class TestQdoxVarArgsResolution {

	private static JavaClass testClass;

	/**
	 * Initial Test setup: Programmatically creates a class syntactically
	 * identical to {@link TestClass} (except for visibility and nesting)
	 * @see TestClass
	 */
	@Before
	public void setUp() {
		testClass = new JavaClass("ch.kwsoft.qdox.TestClass");
		JavaMethod varArgsMethod = new JavaMethod("method");
		JavaParameter varArgsParam = new JavaParameter(new Type(String.class.getName()), "param", true);
		varArgsMethod.addParameter(varArgsParam);
		testClass.addMethod(varArgsMethod);

		JavaMethod simpleMethod = new JavaMethod("method");
		JavaParameter simpleParam = new JavaParameter(new Type(String.class.getName()), "param");
		simpleMethod.addParameter(simpleParam);
		testClass.addMethod(simpleMethod);
	}

	/**
	 * Test retrieving correct method with one vararg-parameter of type 'String...'
	 */
	@Test
	public void testGetMethodBySignatureVarArgParam() {
		// Try to find varargs-Method by signature
		JavaMethod varArgsMethod = testClass.getMethodBySignature("method",
				new Type[] { new Type(String.class.getName(), 1) });
		assertNotNull("Varargs method must be found with Type 'String[]' which equals 'String...'", varArgsMethod);
		JavaParameter varArgsParam = varArgsMethod.getParameterByName("param");
		assertNotNull("Correct method must be found", varArgsParam);
		assertTrue("Correct method must be found", varArgsParam.isVarArgs());
	}

	/**
	 * Test retrieving correct method with one parameter of simple type 'String'
	 */
	@Test
	public void testGetMethodBySignatureNonVarArgParam() {
		JavaMethod simpleMethod = testClass.getMethodBySignature("method",
				new Type[] { new Type(String.class.getName()) });
		assertNotNull("Simple method must be found with Type 'String'", simpleMethod);
		JavaParameter simpleParam = simpleMethod.getParameterByName("param");
		assertNotNull("Correct method must be found", simpleParam);
		assertFalse("Correct method must be found", simpleParam.isVarArgs());
	}

	@SuppressWarnings("unused")
	private class TestClass {
		private void method(String... param) {}
		private void method(String param) {}
	}

}
