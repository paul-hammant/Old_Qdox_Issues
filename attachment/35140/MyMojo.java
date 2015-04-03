package it;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * IT MOJO. Some Non-ASCIIs: \u00E4 \u00F6 \u00FC \u00DF
 * 
 * @goal it
 */
public class MyMojo extends AbstractMojo {

  /**
   * @parameter
   */
  private String paramWithNonAsciis\u00E4\u00F6\u00FC\u00DF;

	public void execute() throws MojoExecutionException, MojoFailureException {
	}

}
