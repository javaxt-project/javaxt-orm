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
    private boolean unique = false;
    private Object defaultValue = null;
    private Integer length;
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
        else if (type.equalsIgnoreCase("double") || type.equalsIgnoreCase("float")){
            type = "Double";
            columnType = "double precision";
        }
        else if (type.equalsIgnoreCase("decimal") || type.equalsIgnoreCase("numeric")){ 
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
            type = "Geometry";
            columnType = "geometry(Geometry,4326)"; //PostgreSQL specific
        }
        else if (type.equalsIgnoreCase("password")){
            //type = "String";
            columnType = "text";
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
    
   
    
  /** Returns true if a value is required for this field. Default is false (nullable). 
   */
    public boolean isRequired(){
        return required;
    }
    
    public void isRequired(boolean required){
        this.required = required;
    }
    
    
  /** Returns true if the field value must be unique. Default is false. 
   */
    public boolean isUnique(){
        return unique;
    }
    
    public void isUnique(boolean unique){
        this.unique = unique;
    }
    
  /** Returns true if the field has a default value. 
   */
    public boolean hasDefaultValue(){
        return defaultValue!=null;
    }
   
    
  /** Returns the default value assigned to this field. Returns null if there  
   *  is no default value. 
   */
    public Object getDefaultValue(){
        return defaultValue;
    }
    
    public void setDefaultValue(Object defaultValue){
        this.defaultValue = defaultValue;
    }
 

    
    public void setLength(int length){
        if (this.columnType.equalsIgnoreCase("text")){
            this.columnType = "VARCHAR(" + length + ")";
        }
    }
}