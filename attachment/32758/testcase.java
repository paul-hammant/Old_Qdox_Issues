public void testAnnotationInMethodParamList() {
	String source = ""
	    + "class Foo {\n"
	    + "    @X()\n"
	    + "    public String xyz(@Y(1) int blah) {\n"
	    + "    }\n"
	    + "}\n";

	builder.addSource(new StringReader(source));
	JavaClass clazz = builder.getClassByName("Foo");
	JavaMethod mth = clazz.getMethods()[0];
	assertEquals("Foo", clazz.getName());
	assertEquals("X", mth.getAnnotations()[0].getType().getJavaClass().getName());
}
