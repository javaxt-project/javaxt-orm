package javaxt.orm;
import javaxt.json.*;

public class Model {
    
    private String name;
    private java.util.ArrayList<Field> fields;
    private static final String template = getTemplate();
    
    
    private Model(String modelName, JSONObject modelInfo, JSONObject otherModels){
        this.name = modelName;
        this.fields = new java.util.ArrayList<Field>();
        JSONArray arr = modelInfo.get("fields").toJSONArray();
        for (int i=0; i<arr.length(); i++){
            JSONObject json = arr.get(i).toJSONObject();
            Field field = new Field(json.get("name").toString(), json.get("type").toString());
            if (!field.getName().equalsIgnoreCase("id")){
                fields.add(field);
            }
        }
    }
    
    
    public String getName(){
        return name;
    }
    
    
    public Field[] getFields(){
        return fields.toArray(new Field[fields.size()]);
    }
    
    
    public String createClass(){
        String str = template.replace("${modelName}", name);
        str = str.replace("${tableName}", Utils.camelCaseToUnderScore(name));
        
        StringBuilder privateFields = new StringBuilder();
        StringBuilder publicMembers = new StringBuilder();
        StringBuilder getValues = new StringBuilder();
        StringBuilder setValues = new StringBuilder();
        StringBuilder getJson = new StringBuilder();
        StringBuilder toJson = new StringBuilder();
        
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
            setValues.append("        rs.setValue(\"");
            setValues.append(columnName);
            setValues.append("\", ");
            setValues.append(fieldName);
            setValues.append(");\r\n");

            
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
        
        return str;
    }
    
    
    
    public String toString(){
        return getName();
    }
    
    
    
    public static Model[] getModels(JSONObject json){
        
        java.util.ArrayList<Model> models = new java.util.ArrayList<Model>();
        for (String modelName : json.keySet()){
            Model model = new Model(modelName, json.get(modelName).toJSONObject(), json);
            models.add(model);
        }
        
        
        return models.toArray(new Model[models.size()]);
    }
    
    
    
    private static String getTemplate(){
        javaxt.io.Jar jar = new javaxt.io.Jar(javaxt.orm.Model.class);
        javaxt.io.Jar.Entry entry = jar.getEntry("javaxt.orm", "Class.txt");
        return entry.getText();
    }
}