package javaxt.orm;
import javaxt.json.*;

//******************************************************************************
//**  Model Class
//******************************************************************************
/**
 *   Used to parse a model and generate code (e.g. Java and DDL).
 *
 ******************************************************************************/

public class Model {
    
    private String name;
    private java.util.ArrayList<Field> fields;
    private static final String template = getTemplate();
    private String packageName;
    
  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class.
   */
    protected Model(String modelName, String packageName, JSONObject modelInfo, JSONObject otherModels){
        this.name = modelName;
        this.fields = new java.util.ArrayList<Field>();
        this.packageName = packageName;
        JSONArray arr = modelInfo.get("fields").toJSONArray();
        for (int i=0; i<arr.length(); i++){
            JSONObject json = arr.get(i).toJSONObject();
            Field field = new Field(json.get("name").toString(), json.get("type").toString());
            if (!field.getName().equalsIgnoreCase("id")){
                fields.add(field);
            }
        }
    }
    
    
  //**************************************************************************
  //** getName
  //**************************************************************************
  /** Returns the name of this model.
   */
    public String getName(){
        return name;
    }
    
    
  //**************************************************************************
  //** getFields
  //**************************************************************************
  /** Returns an array of all the fields found in this model.
   */
    public Field[] getFields(){
        return fields.toArray(new Field[fields.size()]);
    }
    
    
  //**************************************************************************
  //** createClass
  //**************************************************************************
  /** Used to generate Java code for the model.
   */
    public String createClass(){
        String str = template.replace("${modelName}", name);
        str = str.replace("${package}", packageName);
        str = str.replace("${tableName}", Utils.camelCaseToUnderScore(name));
        
        StringBuilder privateFields = new StringBuilder();
        StringBuilder publicMembers = new StringBuilder();
        StringBuilder getValues = new StringBuilder();
        StringBuilder setValues = new StringBuilder();
        StringBuilder getJson = new StringBuilder();
        StringBuilder toJson = new StringBuilder();
        String getLastModified = "";
        
        
        for (Field field : fields){
            String fieldName = field.getName();
            String fieldType = field.getType();
            String methodName = Utils.capitalize(fieldName);
            String columnName = field.getColumnName();
            
          //Append private field
            privateFields.append("    private ");
            privateFields.append(fieldType);
            privateFields.append(" ");
            privateFields.append(fieldName);
            privateFields.append(";\r\n");
            
            
          //Append public get method
            publicMembers.append("    public ");
            publicMembers.append(fieldType);
            publicMembers.append(" get");
            publicMembers.append(methodName);
            publicMembers.append("(){\r\n");
            publicMembers.append("        return ");
            publicMembers.append(fieldName);
            publicMembers.append(";\r\n");
            publicMembers.append("    }\r\n\r\n");
            
            
          //Append public set method
            if (!field.isLastModifiedDate()){
                publicMembers.append("    public void set");
                publicMembers.append(methodName);
                publicMembers.append("(");
                publicMembers.append(fieldType);
                publicMembers.append(" ");
                publicMembers.append(fieldName);
                publicMembers.append("){\r\n");
                publicMembers.append("        this.");
                publicMembers.append(fieldName);
                publicMembers.append(" = ");
                publicMembers.append(fieldName);
                publicMembers.append(";\r\n");
                publicMembers.append("    }\r\n\r\n");
            }
            
            
          //Update database constructor
            getValues.append("        this.");
            getValues.append(fieldName);
            getValues.append(" = rs.getValue(\"");
            getValues.append(columnName);
            getValues.append("\").to");
            getValues.append(fieldType);
            getValues.append("();\r\n");
            
            
          //Update json constructor
            getJson.append("        this.");
            getJson.append(fieldName);
            getJson.append(" = json.get(\"");
            getJson.append(columnName);
            getJson.append("\").to");
            getJson.append(fieldType);
            getJson.append("();\r\n");
            
            
          //Update save method
            if (!field.isLastModifiedDate()){
                setValues.append("        rs.setValue(\"");
                setValues.append(columnName);
                setValues.append("\", ");
                setValues.append(fieldName);
                setValues.append(");\r\n");
            }
            else{
                getLastModified = "lastModified = rs.getValue(\"last_modified\").toDate();";
            }

            
          //Update toJson method
            toJson.append("        json.set(\"");
            toJson.append(fieldName);
            toJson.append("\", ");
            toJson.append(fieldName);
            toJson.append(");\r\n");
        }
        
        str = str.replace("${privateFields}", privateFields.toString().trim());
        str = str.replace("${publicMembers}", publicMembers.toString().trim());
        str = str.replace("${getValues}", getValues.toString().trim());
        str = str.replace("${setValues}", setValues.toString().trim());
        str = str.replace("${getJson}", getJson.toString().trim());
        str = str.replace("${toJson}", toJson.toString().trim());
        
        
        
      //Special case for Models with a lastModified field
        str = str.replace("${getLastModified}", getLastModified);

        
        
        return str;
    }
    
    
  //**************************************************************************
  //** createDDL
  //**************************************************************************
  /** Used to generate DDL script for the model. The schema is targeted for
   *  a PostgreSQL database.
   */
    public String createDDL(){
        String tableName = Utils.camelCaseToUnderScore(name).toUpperCase();
        
        StringBuilder str = new StringBuilder();
        str.append("CREATE TABLE ");
        str.append(tableName);
        str.append(" {\r\n");
        str.append("    ID BIGSERIAL NOT NULL,\r\n"); 
        
        boolean addLastModifiedTrigger = false;
        java.util.ArrayList<String> foreignKeys = new java.util.ArrayList<String>();
        java.util.Iterator<Field> it = fields.iterator();
        while (it.hasNext()){
            Field field = it.next();
            str.append("    ");
            str.append(field.getColumnName().toUpperCase());
            str.append(" ");
            str.append(field.getColumnType());
            str.append(",");
            str.append("\r\n");
            
            String foreignKey = field.getForeignKey();
            if (foreignKey!=null) foreignKeys.add(foreignKey);
            if (field.isLastModifiedDate()) addLastModifiedTrigger = true;
        }


        str.append("    CONSTRAINT PK_");
        str.append(tableName);
        str.append(" PRIMARY KEY (ID)"); 
        
        if (!foreignKeys.isEmpty()){
            str.append(",\r\n");
            for (int i=0; i<foreignKeys.size(); i++){
                String foreignKey = foreignKeys.get(i).toUpperCase();
                String foreignTable = foreignKey.toUpperCase(); 
                String foreignColumn = "ID"; 
                
                str.append("    CONSTRAINT FK_");
                str.append(foreignKey);
                str.append(" FOREIGN KEY (");
                str.append(foreignKey);
                str.append(") REFERENCES ");
                str.append(foreignTable);
                str.append("(");
                str.append(foreignColumn);
                str.append(")\r\n");
                str.append("        ON DELETE CASCADE ON UPDATE NO ACTION");
                
                if (i<foreignKeys.size()-1) str.append(",");
                str.append("\r\n");
                //FOREIGN KEY (ARTICLE_ID) REFERENCES NEWS_ARTICLES(ARTICLE_ID)
                
            }
        }
        str.append("\r\n};\r\n\r\n");
        
        
      //Add last modified trigger as needed
        if (addLastModifiedTrigger){
            str.append("\r\nCREATE TRIGGER TGR_");
            str.append(tableName);
            str.append("_UPDATE BEFORE INSERT OR UPDATE ON ");
            str.append(tableName);
            str.append("\r\n    FOR EACH ROW EXECUTE PROCEDURE last_modified();\r\n\r\n");
        }
        /*
        CREATE FUNCTION last_modified() RETURNS trigger AS $last_modified$
            BEGIN
                NEW.LAST_MODIFIED := current_timestamp;
                RETURN NEW;
            END;
        $last_modified$ LANGUAGE plpgsql;
        */
        
        
        return str.toString();
    }
    
    
    
  //**************************************************************************
  //** toString
  //**************************************************************************
  /** Returns the name of this model.
   */
    public String toString(){
        return getName();
    }
    
    

  //**************************************************************************
  //** getTemplate
  //**************************************************************************
  /** Static method used to extract the class template (Class.txt) found in 
   *  this package.
   */
    private static String getTemplate(){
        javaxt.io.Jar jar = new javaxt.io.Jar(javaxt.orm.Model.class);
        javaxt.io.Jar.Entry entry = jar.getEntry("javaxt.orm", "Class.txt");
        return entry.getText();
    }
}