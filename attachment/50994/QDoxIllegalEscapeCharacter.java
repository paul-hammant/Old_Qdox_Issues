import com.thoughtworks.qdox.JavaDocBuilder;
import java.io.StringReader;

public class QDoxIllegalEscapeCharacter {

  public static void main(String[] args) throws Exception {
	
	JavaDocBuilder builder = new JavaDocBuilder();
	String source = "public class Foo { @SuppressWarnings({\"a\", \"abc\\d\"})\nprivate void bar() { } }";
	builder.addSource( new StringReader(source) );
  }
}