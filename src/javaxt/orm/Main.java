package javaxt.orm;
import java.util.*;
import javaxt.io.Jar;
import static javaxt.utils.Console.console;

//******************************************************************************
//**  Main
//******************************************************************************
/**
 *  Command line interface used to parse a model file and generate Java and SQL
 *  output files.
 *
 ******************************************************************************/

public class Main {


  //**************************************************************************
  //** main
  //**************************************************************************
  /** Entry point for the application
   *  @param arguments Command line arguments
   */
    public static void main(String[] arguments) throws Exception {
        HashMap<String, String> args = console.parseArgs(arguments);


      //Print version as needed
        if (args.containsKey("-version")){
            Jar jar = new Jar(Main.class);
            javaxt.io.File jarFile = new javaxt.io.File(jar.getFile());
            String version = jar.getVersion();
            if (version==null) version = "Unknown";
            System.out.println(jarFile.getName(false) + " version \"" + version + "\"");
            return;
        }


      //Parse inputs
        String input = args.get("-input");
        String output = args.get("-output");
        if (input==null){
            input = arguments[0];
            if (arguments.length>1) output = arguments[1];
        }
        javaxt.io.File inputFile = new javaxt.io.File(input);
        if (!inputFile.exists()) throw new IllegalArgumentException("Input file not found");
        javaxt.io.Directory outputDirectory = output==null ?
                inputFile.getDirectory() : new javaxt.io.Directory(output);


      //Create models
        Model[] models = new Parser(inputFile.getText()).getModels();


      //Create files
        Writer.write(models, outputDirectory);
    }
}