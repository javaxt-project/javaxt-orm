package javaxt.orm;

//******************************************************************************
//**  Schema Class
//******************************************************************************
/**
 *   Used to generate an SQL schema for a given set of Models
 *
 ******************************************************************************/

public class Schema {

    private Model[] models;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
    public Schema(Model[] models){
        this.models = models;
    }


  //**************************************************************************
  //** getSQL
  //**************************************************************************
  /** Returns a SQL script used to generate tables and indexes in a relational
   *  database.
   */
    public String getSQLScript(){
        boolean hasGeometry = false;
        boolean hasLastModifiedField = false;
        java.util.HashSet<String> schemas = new java.util.HashSet<String>();
        for (Model model : models){


          //Check whether any of the fields are geospatial/geometry types
            if (!hasGeometry){
                for (Field field : model.getFields()){
                    if (field.getColumnType().startsWith("geometry")){
                        hasGeometry = true;
                        break;
                    }
                }
            }

            if (model.hasLastModifiedField()){
                hasLastModifiedField = true;
            }


            String schemaName = model.getSchemaName();
            if (schemaName!=null){
                schemas.add(model.escapeTableName(schemaName));
            }
        }


      //Generate SQL script
        StringBuilder sql = new StringBuilder();

        for (String schemaName : schemas){
            sql.append("CREATE SCHEMA IF NOT EXISTS ");
            sql.append(schemaName);
            sql.append(";\r\n");
        }

        if (hasGeometry){
            sql.append("CREATE EXTENSION IF NOT EXISTS postgis;\r\n");
        }


        for (Model model : models){
            sql.append("\r\n");
            sql.append(model.getTableSQL());
        }
        for (Model model : models){
            String str = model.getDiamondTableSQL();
            if (!str.isEmpty()){
                sql.append("\r\n");
                sql.append(str);
            }
        }

        sql.append("\r\n\r\n");
        for (Model model : models){
            sql.append(model.getForeignKeySQL());
        }

        sql.append("\r\n\r\n");
        for (Model model : models){
            sql.append(model.getIndexSQL());
        }

        if (hasLastModifiedField){
            sql.append("\r\n\r\n\r\n");
            sql.append(
                "CREATE OR REPLACE FUNCTION last_modified() RETURNS trigger AS $last_modified$\r\n" +
                "    BEGIN\r\n" +
                "        NEW.LAST_MODIFIED := current_timestamp;\r\n" +
                "        RETURN NEW;\r\n" +
                "    END;\r\n" +
                "$last_modified$ LANGUAGE plpgsql;\r\n\r\n"
            );
            sql.append("\r\n\r\n");
        }

        for (Model model : models){
            sql.append(model.getTriggerSQL());
        }

        return sql.toString();
    }
}