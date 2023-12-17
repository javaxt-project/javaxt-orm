package javaxt.orm;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.tools.*;
import java.nio.charset.Charset;

import static javaxt.utils.Console.console;

//******************************************************************************
//**  Compiler
//******************************************************************************
/**
 *   Used to compile ORM models into Java classes. The classes are ephemeral
 *   meaning they are not saved anywhere on disk (no class files or jar files).
 *   Instead the classes are stored in memory and are tied to the lifecycle of
 *   the JVM.
 *
 ******************************************************************************/

public class Compiler {

    private Class[] classes;
    private HashMap<String, SimpleJavaFileObject> outputFiles;
    private URLClassLoader urlClassLoader;
    private DiagnosticListener<JavaFileObject> listener = null;
    private Locale locale = null;
    private Charset charset = Charset.defaultCharset();


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public Compiler(Model[] models) throws Exception {

        if (models==null || models.length==0){
            classes = new Class[0];
        }
        else{

            outputFiles = new HashMap<>();
            urlClassLoader = getClassLoader();



          //Convert models into an ArrayList
            ArrayList<Model> arr = new ArrayList<>();
            for (Model model : models){
                arr.add(model);
            }


          //Compile classes
            ArrayList<Class> classes = new ArrayList<>();
            int numErrors = 0;
            while (classes.size()<models.length){
                try{
                    Model model = arr.get(0);
                    classes.add(compile(model));
                    arr.remove(0);
                }
                catch(Exception e){
                    numErrors++;

                  //The following logic assumes that the compile error is due
                  //to an ordering issue where one class depends on another
                  //but the dependency hasn't been compiled yet. So we'll
                  //shuffle the list of models and try again.

                    if (arr.size()==1) throw e;
                    String firstClass = getClassName(arr.get(0));
                    while (true){
                        Collections.shuffle(arr);
                        String className = getClassName(arr.get(0));
                        if (!className.equals(firstClass)) break;
                    }
                }


              //Safety switch
                if (numErrors>(models.length*models.length)){
                    throw new Exception("Failed to compile " + getClassName(arr.get(0)));
                }
            }


          //Convert the class list into an array. Sorts the classes to match
          //the order of the input models.
            this.classes = new Class[classes.size()];
            for (int i=0; i<models.length; i++){
                String className = getClassName(models[i]);
                for (Class c : classes){
                    String name = c.getPackageName() + "." + c.getSimpleName();
                    if (name.equals(className)){
                        this.classes[i] = c;
                        break;
                    }
                }
            }
        }
    }


  //**************************************************************************
  //** getClasses
  //**************************************************************************
  /** Returns classes generated by the compiler for each model
   */
    public Class[] getClasses(){
        return classes;
    }



  //**************************************************************************
  //** compile
  //**************************************************************************
  /** Used to compile a model and return a class
   */
    private Class compile(Model model) throws Exception {

        JavaCompiler c = ToolProvider.getSystemJavaCompiler();
        String className = getClassName(model);


      //Create input file object
        SimpleJavaFileObject src = new SimpleJavaFileObject(
        URI.create("string:///" + model.getName() + ".java"),
        JavaFileObject.Kind.SOURCE) {
            public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                return model.getJavaCode();
            }
        };



      //Create output file object
        SimpleJavaFileObject cls = new SimpleJavaFileObject(
        URI.create("runtime:///" + model.getName() + ".class"),
        JavaFileObject.Kind.CLASS) {
            private ByteArrayOutputStream out = new ByteArrayOutputStream();
            public OutputStream openOutputStream() throws IOException {
                //console.log("openOutputStream");
                return out;
            }
            public InputStream openInputStream() throws IOException {
                //console.log("openInputStream");
                return new ByteArrayInputStream(out.toByteArray());
            }
        };
        outputFiles.put(className, cls);



      //Create in-memory file manager
        StandardJavaFileManager fm = c.getStandardFileManager(listener, locale, charset);
        JavaFileManager fileManager = new ForwardingJavaFileManager(fm) {
            public JavaFileObject getJavaFileForOutput(
                JavaFileManager.Location location, String className,
                JavaFileObject.Kind kind, FileObject sibling) throws IOException {
                return outputFiles.get(className);
            }


            public Iterable list(JavaFileManager.Location location,
                String packageName, Set kinds, boolean recurse) throws IOException {

                if (location==StandardLocation.CLASS_PATH){
                    ArrayList<SimpleJavaFileObject> arr = new ArrayList<>();
                    Iterator<String> it = outputFiles.keySet().iterator();
                    while (it.hasNext()){
                        String name = it.next();
                        if (name.startsWith(packageName + ".")){
                            if (!name.equals(className)){ //don't include the current file since it hasn't been compiled yet
                                arr.add(outputFiles.get(name));
                            }
                        }
                    }
                    if (!arr.isEmpty()) return arr;
                }

                return super.list(location, packageName, kinds, recurse);
            }



            public String inferBinaryName(JavaFileManager.Location location, JavaFileObject file) {

                if (location==StandardLocation.CLASS_PATH){
                    Iterator<String> it = outputFiles.keySet().iterator();
                    while (it.hasNext()){
                        String name = it.next();
                        SimpleJavaFileObject f = outputFiles.get(name);
                        if (file==f) return name;
                    }
                }

                return super.inferBinaryName(location, file);
            }
        };




      //Compile class
        JavaCompiler.CompilationTask task = c.getTask(
            null,
            fileManager,
            listener,
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.singleton(src)
        );

        if (task.call()) {
            return urlClassLoader.loadClass(className);
        }
        else{
            throw new Exception("Failed to compile " + className);
        }
    }


  //**************************************************************************
  //** getClassLoader
  //**************************************************************************
  /** Returns a custom class loader used to find classes created by this class
   */
    private URLClassLoader getClassLoader(){
        return new URLClassLoader(new URL[0]){
            protected Class<?> findClass(final String name) throws ClassNotFoundException {


                SimpleJavaFileObject f = outputFiles.get(name);
                if (f!=null){
                    try (InputStream is = f.openInputStream()) {


                        ByteArrayOutputStream out = new ByteArrayOutputStream();

                        int x;
                        byte[] buffer = new byte[256];
                        while ((x = is.read(buffer, 0, buffer.length)) != -1) {
                            out.write(buffer, 0, x);
                        }

                        out.flush();
                        byte[] classBytes = out.toByteArray();
                        return defineClass(name, classBytes, 0, classBytes.length);

                    }
                    catch(Exception e){
                        e.printStackTrace();
                    }
                }

                return super.findClass(name);
            }
        };
    }


  //**************************************************************************
  //** getClassName
  //**************************************************************************
    private String getClassName(Model model){
        return model.getPackageName() + "." + model.getName();
    }


}