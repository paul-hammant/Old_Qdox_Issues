package oqube.patchwork.report;

import java.io.InputStreamReader;

import com.thoughtworks.qdox.JavaDocBuilder;

import junit.framework.TestCase;

public class QDoxTest extends TestCase {

  public void testFailedParsing() {
    JavaDocBuilder b = new JavaDocBuilder();
    b.addSource(new InputStreamReader(getClass().getResourceAsStream("/dfs.j")));
    b.getSources();
  }
}
