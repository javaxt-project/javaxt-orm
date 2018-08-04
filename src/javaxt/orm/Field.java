package javaxt.orm;

//******************************************************************************
//**  Field Class
//******************************************************************************
/**
 *   Used to represent an individual field in a model.
 *
 ******************************************************************************/

public class Field {

    private String name;
    private String type;
    private String columnName;
    private String columnType;
    private boolean required = false;
    private String foreignKey;
    private String foreignTable;
    
  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class.
   */
    protected Field(String name, String type){
        this.name = name;
        this.columnName = Utils.camelCaseToUnderScore(name);
        
        if (type.equalsIgnoreCase("int")){ 
            type = "integer";
        }
        else if (type.equalsIgnoreCase("long")){ 
            columnType = "bigint";
        }
        else if (type.equalsIgnoreCase("float")){
            type = "Double";
            columnType = "double precision";
        }
        else if (type.equalsIgnoreCase("decimal")){ 
            type = "BigDecimal";
            columnType = "numeric";
        }
        else if (type.equalsIgnoreCase("text") || type.equalsIgnoreCase("string")){ 
            type = "string";
            columnType = "text";
        }
        else if (type.equalsIgnoreCase("boolean")){ 
            //do nothing (type and columnType are all set)
        }
        else if (type.equalsIgnoreCase("date")){
            columnType = "TIMESTAMP with time zone"; //PostgreSQL specific
        }
        else if (type.equalsIgnoreCase("binary")){
            type = "byte[]";
            columnType = "bytea"; //PostgreSQL specific
        }
        else if (type.equalsIgnoreCase("json")){
            type = "JSONObject";
            columnType = "jsonb"; //PostgreSQL specific
        }
        else if (type.equalsIgnoreCase("geo")){
            type = "object";
            columnType = "geometry(Geometry,4326)"; //PostgreSQL specific
        }
        else{ //Model?
            
            
            if (type.endsWith("[]")){ //Array of models
                
                String modelName = type.substring(0, type.length()-2);
                type = "ArrayList<" + modelName + ">";
                
            }
            else{ //Single model
                
                columnName = columnName + "_id";
                foreignKey = columnName;  
                foreignTable = Utils.camelCaseToUnderScore(type);
                columnType = "bigint";
            }

        }
        
        this.type = Utils.capitalize(type);
        if (columnType==null) columnType = type;
    }   
    
    public String getName(){
        return name;
    }
    
    public String getType(){
        return type;
    }
    
    public String getColumnName(){
        return columnName;
    }
    
    public String getColumnType(){
        return columnType;
    }
    
    public String getForeignKey(){
        return foreignKey;
    }
    
    public String getForeignTable(){
        return foreignTable;
    }
    
    public boolean isLastModifiedDate(){
        return name.equalsIgnoreCase("lastModified") && type.equalsIgnoreCase("date");
    }
    
    public boolean isModel(){
        return foreignKey!=null;
    }
    
    public boolean isArray(){
        return type.startsWith("ArrayList<");
    }
    
}