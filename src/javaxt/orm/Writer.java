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
        
      //Check whether any of the fields are geospatial/geometry types
        boolean hasGeometry = false;
        for (Model model : models){
            javaxt.io.File file = new javaxt.io.File(output, model.getName() + ".java");
            file.write(model.getJavaCode());


            if (!hasGeometry){
                for (Field field : model.getFields()){
                    if (field.getColumnType().startsWith("geometry")){
                        hasGeometry = true;
                        break;
                    }
                }
            }
        }
        
        
      //Generate SQL script
        StringBuilder sql = new StringBuilder();
        if (hasGeometry) sql.append("CREATE EXTENSION postgis;\r\n\r\n");
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