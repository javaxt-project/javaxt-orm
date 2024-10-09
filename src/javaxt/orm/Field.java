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
    private boolean isModel = false;
    private ForeignKey foreignKey;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class.
   */
    protected Field(String name, String type, Model model){
        this.name = name;
        this.columnName = Utils.camelCaseToUnderScore(name);

      //Set columnType and tweak type as needed
        if (type.equalsIgnoreCase("int")){
            type = "Integer";
            columnType = "integer";
        }
        else if (type.equalsIgnoreCase("int[]")){
            type = "Integer[]";
            columnType = "integer array";
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
        else if (type.equalsIgnoreCase("text") || type.equalsIgnoreCase("string") || type.equalsIgnoreCase("password")){
            type = "String";
            columnType = "varchar"; //varchar without the length specifier and text are equivalent
        }
        else if (type.equalsIgnoreCase("text[]") || type.equalsIgnoreCase("string[]")){
            type = "String[]";
            columnType = "varchar array"; //same as "text array"
        }
        else if (type.equalsIgnoreCase("char")){
            type = "string";
            columnType = "char(1)";
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
            columnType = "geometry(Geometry,4326)";
        }
        else if (type.equalsIgnoreCase("geometry")){
            type = "Geometry";
            columnType = "geometry(GeometryZ)";
        }
        else{ //Model?


            if (type.endsWith("[]")){ //Array of models

                String modelName = type.substring(0, type.length()-2);
                type = "ArrayList<" + Utils.capitalize(modelName) + ">";

            }
            else{ //Single model

                isModel = true;
                columnName = columnName + "_id";
                columnType = "bigint";

              //Typically, when we have a model field we want to create a
              //foreign key to tie the field to the model. The only exception
              //is if the model field references the parent model - which is
              //very rare (most models don't have a model field with the same
              //type).
                if (!type.equals(model.getName())){
                    foreignKey = new ForeignKey(columnName, type);
                }

            }

        }

        if (type.equals("byte[]")) this.type = type;
        else this.type = Utils.capitalize(type);

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

    public ForeignKey getForeignKey(){
        return foreignKey;
    }

    public boolean isLastModifiedDate(){
        return name.equalsIgnoreCase("lastModified") && type.equalsIgnoreCase("date");
    }

    public boolean isModel(){
        return isModel;
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
        if (columnType.equalsIgnoreCase("text") ||
            columnType.equalsIgnoreCase("varchar")){
            columnType = "VARCHAR(" + length + ")";
            this.length = length;
        }
        else{
            if (columnType.toLowerCase().startsWith("char(")){
                columnType = "CHAR(" + length + ")";
                this.length = length;
            }
        }
    }


    public void setSRID(Integer srid){
        if (type.equalsIgnoreCase("Geometry")){
            boolean hasZ = columnType.contains("Z");
            columnType = "geometry(Geometry";
            if (hasZ) columnType+="Z";
            if (srid!=null) columnType+=","+srid;
            columnType+=")";
        }
    }
}