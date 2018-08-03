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
    private String tableName;
    private String packageName;
    
  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class.
   */
    protected Model(String modelName, String packageName, JSONObject modelInfo){
        this.name = modelName;
        this.fields = new java.util.ArrayList<Field>();
        this.tableName = Utils.camelCaseToUnderScore(name).toUpperCase();
        this.packageName = packageName;

        
      //Parse fields
        JSONArray arr = modelInfo.get("fields").toJSONArray();
        if (arr!=null)
        for (int i=0; i<arr.length(); i++){
            JSONObject json = arr.get(i).toJSONObject();
            Field field = new Field(json.get("name").toString(), json.get("type").toString());
            
            
          //Don't add ID field. It is added automatically.
            if (field.getName().equalsIgnoreCase("id")) continue;
            
            
            fields.add(field);
        }
        
        
      //Parse hasMany
        JSONArray hasMany = modelInfo.get("hasMany").toJSONArray();
        if (hasMany!=null)
        for (int i=0; i<hasMany.length(); i++){
            JSONObject json = hasMany.get(i).toJSONObject();
            Field field = new Field(json.get("name").toString(), json.get("model").toString()+"[]");
            fields.add(field);
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
  //** getTableName
  //**************************************************************************
  /** Returns the name of the table associated with this model.
   */
    public String getTableName(){
        return tableName;
    }
    
    
  //**************************************************************************
  //** getJavaCode
  //**************************************************************************
  /** Used to generate Java code for the model.
   */
    public String getJavaCode(){
        String str = template.replace("${modelName}", name);
        str = str.replace("${package}", packageName);
        str = str.replace("${tableName}", Utils.camelCaseToUnderScore(name));
        
        StringBuilder privateFields = new StringBuilder();
        StringBuilder publicMembers = new StringBuilder();
        StringBuilder getModels = new StringBuilder();
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
            String modelName = null;
            if (field.isArray()){
                modelName = fieldType.substring(10, fieldType.length()-1);
            }
            
            
          //Append private field
            privateFields.append("    private ");
            privateFields.append(fieldType);
            privateFields.append(" ");
            privateFields.append(fieldName);
            privateFields.append(";\r\n");
            
            
          //Append public get method
            if (!field.isArray()){
                publicMembers.append("    public ");
                publicMembers.append(fieldType);
                publicMembers.append(" get");
                publicMembers.append(methodName);
                publicMembers.append("(){\r\n");
                publicMembers.append("        return ");
                publicMembers.append(fieldName);
                publicMembers.append(";\r\n");
                publicMembers.append("    }\r\n\r\n");
            }
            else{
                
                publicMembers.append("    public ");
                publicMembers.append(modelName);
                publicMembers.append("[] get");
                publicMembers.append(methodName);
                publicMembers.append("(){\r\n");
                publicMembers.append("        return ");
                publicMembers.append(fieldName);
                publicMembers.append(".toArray(new "); 
                publicMembers.append(modelName);
                publicMembers.append("[");
                publicMembers.append(fieldName);
                publicMembers.append(".size()]);\r\n");
                publicMembers.append("    }\r\n\r\n");
            }
            
            
          //Append public set method
            if (!field.isLastModifiedDate()){
                if (!field.isArray()){
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
                else{
                    publicMembers.append("    public void set");
                    publicMembers.append(methodName);
                    publicMembers.append("(");
                    publicMembers.append(modelName);
                    publicMembers.append("[] arr){\r\n");
                    publicMembers.append("        " + fieldName + " = new " + fieldType + "();\r\n");
                    publicMembers.append("        for (int i=0; i<arr.length; i++){\r\n");
                    publicMembers.append("            ");
                    publicMembers.append(fieldName);
                    publicMembers.append(".add(arr[i]);\r\n");
                    publicMembers.append("        }\r\n");
                    publicMembers.append("    }\r\n\r\n");
                    
                    
                    publicMembers.append("    public void add");
                    publicMembers.append(modelName);
                    publicMembers.append("(");
                    String paramName = modelName.substring(0, 1).toLowerCase() + modelName.substring(1);
                    publicMembers.append(modelName + " " + paramName);
                    publicMembers.append("){\r\n");
                    publicMembers.append("        " + fieldName + ".add(" + paramName + ");\r\n");
                    publicMembers.append("    }\r\n\r\n");
                }
            }
            
            
          //Update database constructor
            if (!field.isArray()){
                getValues.append("        this.");
                getValues.append(fieldName);
                getValues.append(" = rs.getValue(\"");
                getValues.append(columnName);
                getValues.append("\").to");
                getValues.append(fieldType);
                getValues.append("();\r\n");
            }
            else{
                String leftTable = this.tableName;
                String leftColumn = leftTable + "_ID";
                String rightTable = Utils.camelCaseToUnderScore(modelName).toUpperCase();
                String rightColumn = rightTable + "_ID";
                String tableName = leftTable + "_" + rightTable;
                
                String idArray = modelName + "IDs";
                getModels.append("\r\n\r\n");
                getModels.append("      //Set " + fieldName + "\r\n");
                getModels.append("        " + fieldName + " = new " + fieldType + "();\r\n");
                getModels.append("        ArrayList<Integer> " + idArray + " = new ArrayList<Integer>();\r\n");
                getModels.append("        for (Recordset row : conn.getRecordset(\"select " + rightColumn + " from " + tableName + " where " + leftColumn + "=\"+id)){\r\n");
                getModels.append("            " + idArray + ".add(row.getValue(0).toLong());\r\n");
                getModels.append("        }\r\n");
                getModels.append("        for (int id : " + idArray + "){\r\n");
                getModels.append("            " + fieldName + ".add(new " + modelName + "(id, conn));\r\n");
                getModels.append("        }\r\n\r\n");
            }
            
            
          //Update json constructor
            if (!field.isArray()){
                getJson.append("        this.");
                getJson.append(fieldName);
                getJson.append(" = json.get(\"");
                getJson.append(fieldName);
                getJson.append("\").to");
                getJson.append(fieldType);
                getJson.append("();\r\n");
            }
            else{
                getJson.append("\r\n");
                getJson.append("      //Set " + fieldName + "\r\n");
                getJson.append("        " + fieldName + " = new " + fieldType + "();\r\n");
                getJson.append("        if (json.has(\"");
                getJson.append(fieldName);
                getJson.append("\")){\r\n");
                getJson.append("            JSONArray _");
                getJson.append(fieldName);
                getJson.append(" = json.get(\"");
                getJson.append(fieldName);
                getJson.append("\").toJSONArray();\r\n");
                getJson.append("            for (int i=0; i<_" + fieldName + ".length(); i++){\r\n");
                getJson.append("                ");
                getJson.append(fieldName);
                getJson.append(".add(new " + modelName + "(_" + fieldName + ".get(i).toJSONObject()));\r\n");
                getJson.append("            }\r\n");
                getJson.append("        }\r\n\r\n");
            }
            
            
          //Update save method
            if (!field.isLastModifiedDate()){
                if (!field.isArray()){
                    setValues.append("        rs.setValue(\"");
                    setValues.append(columnName);
                    setValues.append("\", ");
                    setValues.append(fieldName);
                    setValues.append(");\r\n");
                }
            }
            else{
                getLastModified = "lastModified = rs.getValue(\"last_modified\").toDate();";
            }

            
          //Update toJson method
            if (!field.isArray()){
                toJson.append("        json.set(\"");
                toJson.append(fieldName);
                toJson.append("\", ");
                toJson.append(fieldName);
                toJson.append(");\r\n");
            }
            else{
                toJson.append("\r\n");
                toJson.append("        JSONArray _");
                toJson.append(fieldName);
                toJson.append(" = new JSONArray();\r\n");
                toJson.append("        for (int i=0; i<" + fieldName + ".size(); i++){\r\n");
                toJson.append("            _" + fieldName + ".add(" + fieldName + ".get(i).toJson());\r\n");
                toJson.append("        }\r\n");
                toJson.append("        json.set(\"");
                toJson.append(fieldName);
                toJson.append("\", _");
                toJson.append(fieldName);
                toJson.append(");\r\n\r\n");
            }
        }
        
        str = str.replace("${privateFields}", privateFields.toString().trim());
        str = str.replace("${publicMembers}", publicMembers.toString().trim());
        str = str.replace("${getModels}", getModels.toString());
        str = str.replace("${getValues}", getValues.toString());
        str = str.replace("${setValues}", setValues.toString().trim());
        str = str.replace("${getJson}", getJson.toString().trim());
        str = str.replace("${toJson}", toJson.toString().trim());
        
        
        
      //Special case for Models with a lastModified field
        str = str.replace("${getLastModified}", getLastModified);

        
        
        return str;
    }
    
    
  //**************************************************************************
  //** getTableSQL
  //**************************************************************************
  /** Returns an SQL script used to create a table for the model. The schema 
   *  is targeted for a PostgreSQL database.
   */
    public String getTableSQL(){

      //Begin create table script
        StringBuilder str = new StringBuilder();
        str.append("CREATE TABLE ");
        str.append(tableName);
        str.append(" {\r\n");
        str.append("    ID BIGSERIAL NOT NULL,\r\n"); 
        
        
      //Add fields
        boolean addLastModifiedTrigger = false;
        java.util.ArrayList<String> foreignKeys = new java.util.ArrayList<String>();
        java.util.Iterator<Field> it = fields.iterator();
        while (it.hasNext()){
            Field field = it.next();
            if (field.isArray()) continue;
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

      //Add primary key constraint
        str.append("    CONSTRAINT PK_");
        str.append(tableName);
        str.append(" PRIMARY KEY (ID)"); 
        
        
      //End create table script
        str.append("\r\n};\r\n\r\n");
        
        
      //Add last modified trigger as needed. See Writer.write() for the 
      //last_modified() implementation.
        if (addLastModifiedTrigger){
            str.append("\r\nCREATE TRIGGER TGR_");
            str.append(tableName);
            str.append("_UPDATE BEFORE INSERT OR UPDATE ON ");
            str.append(tableName);
            str.append("\r\n    FOR EACH ROW EXECUTE PROCEDURE last_modified();\r\n\r\n");
        }
        
        
      //Add diamond tables
        it = fields.iterator();
        while (it.hasNext()){
            Field field = it.next();
            if (!field.isArray()) continue;
            
            String modelName = field.getType().substring(10);
            modelName = modelName.substring(0, modelName.length()-1);
            
            String leftTable = this.tableName;
            String leftColumn = leftTable + "_ID";
            
            String rightTable = Utils.camelCaseToUnderScore(modelName).toUpperCase();
            String rightColumn = rightTable + "_ID";

            String tableName = leftTable + "_" + rightTable;
            
            str.append("CREATE TABLE ");
            str.append(tableName);
            str.append(" {\r\n    ");
            str.append(leftColumn);
            str.append(" BIGINT NOT NULL,\r\n    "); 
            str.append(rightColumn);
            str.append(" BIGINT NOT NULL,\r\n"); 
            str.append("    CONSTRAINT FK_"); 
            str.append(tableName);
            str.append(" FOREIGN KEY (");
            str.append(leftColumn); 
            str.append(") REFERENCES "); 
            str.append(leftTable);
            str.append("(ID)\r\n");
            str.append("        ON DELETE CASCADE ON UPDATE NO ACTION");
            str.append("};\r\n\r\n");
        }


        return str.toString();
    }
    
    
  //**************************************************************************
  //** getForeignKeySQL
  //**************************************************************************
  /** Returns an SQL script used to add foreign keys to a table associated 
   *  with the model. Ordinarily, the foreign keys are defined inline with the
   *  create table script. However, that would require creating tables in a
   *  proper sequence which is difficult to do. Instead, it is easier to 
   *  create all the tables first, then update the tables by adding a foreign 
   *  key constraint.
   */
    public String getForeignKeySQL(){
        StringBuilder str = new StringBuilder();
        java.util.Iterator<Field> it = fields.iterator();
        while (it.hasNext()){
            Field field = it.next();
            if (!field.isArray()){
                String foreignKey = field.getForeignKey();
                if (foreignKey!=null){       
                    foreignKey = foreignKey.toUpperCase();
                    String foreignTable = field.getForeignTable().toUpperCase(); 
                    String foreignColumn = "ID";

                    str.append("ALTER TABLE ");
                    str.append(tableName);
                    str.append(" ADD FOREIGN KEY (");
                    str.append(foreignKey);
                    str.append(") REFERENCES ");
                    str.append(foreignTable);
                    str.append("(");
                    str.append(foreignColumn);
                    str.append(")\r\n");
                    str.append("    ON DELETE NO ACTION ON UPDATE NO ACTION;\r\n\r\n");  //ON DELETE CASCADE?
                }
            }
            else{
            
                
                String modelName = field.getType().substring(10);
                modelName = modelName.substring(0, modelName.length()-1);

                String leftTable = this.tableName;
                String leftColumn = leftTable + "_ID";

                String rightTable = Utils.camelCaseToUnderScore(modelName).toUpperCase();
                String rightColumn = rightTable + "_ID";

                String tableName = leftTable + "_" + rightTable;
                
                
                str.append("ALTER TABLE ");
                str.append(tableName);
                str.append(" ADD FOREIGN KEY (");
                str.append(rightColumn);
                str.append(") REFERENCES ");
                str.append(rightTable);
                str.append("(ID)\r\n");
                str.append("    ON DELETE CASCADE ON UPDATE NO ACTION;\r\n\r\n");
            }
        }
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