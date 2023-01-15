package javaxt.orm;

//******************************************************************************
//**  Writer Class
//******************************************************************************
/**
 *   Used to export models to a given directory.
 *
 ******************************************************************************/

public class Writer {


  //**************************************************************************
  //** write
  //**************************************************************************
  /** Used to output Java classes and SQL script to a given directory.
   */
    public static void write(Model[] models, javaxt.io.Directory output){

      //Create Java classes
        for (Model model : models){
            javaxt.io.File file = new javaxt.io.File(output, model.getName() + ".java");
            file.write(model.getJavaCode());
        }


      //Create SQL script
        javaxt.io.File file = new javaxt.io.File(output, "Schema.sql");
        file.write(new Schema(models).getSQLScript());
    }
}