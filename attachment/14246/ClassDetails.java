import com.thoughtworks.qdox.*;
import com.thoughtworks.qdox.model.JavaSource;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaMethod;

import java.io.FileReader;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: nir.melamoud
 * Date: Apr 11, 2005
 * Time: 2:48:00 PM
 * To change this template use File | Settings | File Templates.
 */
public class ClassDetails {
    public static void main(String[] args) {
        if(args.length < 1) {
                System.out.println("Usage : qdox FileName.java");
                System.exit(-1);
            }
            String filename = args[0];
            JavaDocBuilder builder = new JavaDocBuilder();
            FileReader srcFile = null;
            try {
                srcFile = new FileReader(filename);
                builder.addSource(srcFile);
                JavaSource src[] = builder.getSources();
                System.out.println("! " + filename);
                for (int i=0;i< src.length;i++) {
                    //String packageName = src[i].getPackage();
                    //if (!packageName.trim().equals(""))
                    //    packageName = packageName +".";
                    JavaClass[] classes = src[i].getClasses();
                    for (int t=0;t<classes.length;t++) {
                        String className = classes[t].getFullyQualifiedName();
                        System.out.println("# " + className);
                        JavaMethod[] methods = classes[t].getMethods();
                        for (int x=0;x<methods.length;x++) {
                            String methodName = methods[x].getName();
                            int lineNumber = methods[x].getLineNumber();
                            System.out.println("- " + lineNumber + "\t" + methodName);
                        }
                    }

                }

            }
        catch (IOException exp) {
                System.err.println("Exception " + exp);
            }
        finally {
                try {
                if (srcFile != null)
                    srcFile.close();
                }
                catch (IOException e) {}
            }

    }
}
