package javaxt.orm;

//******************************************************************************
//**  Writer Class
//******************************************************************************
/**
 *   Used to export models to a given directory.
 *
 ******************************************************************************/

public class Writer {

    private Model[] models;
    
  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class.
   */
    public Writer(Model[] models){
        this.models = models;
    }
    
    
  //**************************************************************************
  //** write
  //**************************************************************************
  /** Used to output Java classes and DDL script to a given directory.
   */
    public void write(javaxt.io.Directory output){
        javaxt.io.Jar jar = new javaxt.io.Jar(this);
        
        
      //Export java classes
        boolean copyBCrypt = false;
        boolean copyConfig = true;
        for (Model model : models){
            javaxt.io.File file = new javaxt.io.File(output, model.getName() + ".java");
            file.write(model.getJavaCode());
            
            
            if (!copyBCrypt){
                for (Field field : model.getFields()){
                    if (field.getType().equals("Password")){
                        copyBCrypt = true;
                        break;
                    }
                }
            }
            
            if (model.getName().equals("Config")){
                copyConfig = false;
            }
        }
        
        
        
      //Export BCrypt as needed
        if (copyBCrypt){
            javaxt.io.Jar.Entry entry = jar.getEntry("javaxt.orm", "BCrypt.txt");
            String bcryptCode = entry.getText();
            String packageName = models[0].getPackageName();
            bcryptCode = bcryptCode.replace("${package}", packageName);
            javaxt.io.File file = new javaxt.io.File(output, "BCrypt.java");
            file.write(bcryptCode);
        }
        
        
      //Export Config as needed
        if (copyConfig){
            javaxt.io.Jar.Entry entry = jar.getEntry("javaxt.orm", "Config.txt");
            String configCode = entry.getText();
            String packageName = models[0].getPackageName();
            configCode = configCode.replace("${package}", packageName);
            javaxt.io.File file = new javaxt.io.File(output, "Config.java");
            if (!file.exists()) file.write(configCode);
        }
        
        
      //Generate SQL script
        StringBuilder sql = new StringBuilder();
        sql.append(
        "CREATE FUNCTION last_modified() RETURNS trigger AS $last_modified$\r\n" +
        "    BEGIN\r\n" +
        "        NEW.LAST_MODIFIED := current_timestamp;\r\n" +
        "        RETURN NEW;\r\n" +
        "    END;\r\n" +
        "$last_modified$ LANGUAGE plpgsql;\r\n\r\n");
        for (Model model : models){
            sql.append(model.getTableSQL());
        }
        for (Model model : models){
            sql.append(model.getForeignKeySQL());
        }
        for (Model model : models){
            sql.append(model.getIndexSQL());
        }
        
      //Export SQL script
        javaxt.io.File file = new javaxt.io.File(output, "Database.sql");
        file.write(sql.toString());
    }
}