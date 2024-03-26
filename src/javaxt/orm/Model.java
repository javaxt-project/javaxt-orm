package javaxt.orm;
import javaxt.json.*;
import java.util.*;
import java.util.stream.Collectors;

//******************************************************************************
//**  Model Class
//******************************************************************************
/**
 *   Used to parse a model and generate code (e.g. Java and DDL).
 *
 ******************************************************************************/

public class Model {

    private String name;
    private TreeSet<String> implementations;
    private ArrayList<Field> fields;
    private static final String template = getTemplate();
    private String tableName;
    private String escapedTableName;
    private String packageName;
    private String schemaName;
    private String escapedSchemaName;
    private HashMap<String, String> options;


  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class.
   */
    protected Model(String modelName, JSONObject modelInfo, String packageName, HashMap<String, String> options){
        this.name = modelName;
        this.implementations = new TreeSet<>();
        this.fields = new ArrayList<>();
        this.options = options;
        this.packageName = packageName;
        this.tableName = Utils.camelCaseToUnderScore(name).toLowerCase();
        this.schemaName = options.get("schema");


        if (schemaName==null){
            escapedTableName = escapeTableName(tableName);
        }
        else{
            escapedTableName = tableName.toUpperCase();
            escapedSchemaName = escapeTableName(schemaName);
            escapedTableName = escapedSchemaName + "." + escapedTableName;
        }


      //Get implementation classes
        JSONArray implementations = modelInfo.get("implements").toJSONArray();
        if (implementations!=null){
            for (JSONValue i : implementations){
                this.implementations.add(i.toString());
            }
        }


      //Parse fields
        JSONArray arr = modelInfo.get("fields").toJSONArray();
        if (arr!=null){
            for (JSONValue f : arr){
                String name = f.get("name").toString();
                String type = f.get("type").toString();

              //Don't add ID field. It is added automatically.
                if (name.equalsIgnoreCase("id")) continue;

              //Create field and update the fields array
                Field field = new Field(name, type);
                addConstraints(field, f.toJSONObject());
                this.fields.add(field);
            }
        }


      //Parse hasMany
        JSONArray hasMany = modelInfo.get("hasMany").toJSONArray();
        if (hasMany!=null)
        for (int i=0; i<hasMany.length(); i++){
            JSONObject json = hasMany.get(i).toJSONObject();
            Field field = new Field(json.get("name").toString(), json.get("model").toString()+"[]");
            fields.add(field);
        }


      //Parse constraints
        JSONArray constraints = modelInfo.get("constraints").toJSONArray();
        if (constraints!=null){
            for (JSONValue constraint : constraints){
                String fieldName = constraint.get("name").toString();

                for (Field field : fields){
                    if (field.getName().equals(fieldName)){
                        addConstraints(field, constraint.toJSONObject());
                        break;
                    }
                }
            }
        }


      //Parse default values
        JSONArray defaultValues = modelInfo.get("defaults").toJSONArray();
        if (defaultValues!=null){
            for (JSONValue d : defaultValues){
                String fieldName = d.get("name").toString();

                for (Field field : fields){
                    if (field.getName().equals(fieldName)){
                        field.setDefaultValue(d.get("value").toObject());
                        break;
                    }
                }
            }
        }
    }


  //**************************************************************************
  //** addConstraints
  //**************************************************************************
  /** Used to add constraints to a given field. Constraints can be defined in
   *  a "constraints" array or inline with the field definition. If both are
   *  present, the constraints in the "constraints" array may override
   *  constraints defined inline.
   */
    private void addConstraints(Field field, JSONObject constraint){

        Boolean isRequired = constraint.get("required").toBoolean();
        if (isRequired==null) isRequired = constraint.get("nullable").toBoolean();
        Boolean isUnique = constraint.get("unique").toBoolean();
        Integer length = constraint.get("length").toInteger();
        if (length==null) length = constraint.get("size").toInteger();
        Integer srid = constraint.get("srid").toInteger();


        if (isRequired!=null) field.isRequired(isRequired);
        if (isUnique!=null) field.isUnique(isUnique);
        if (length!=null) field.setLength(length);
        if (srid!=null) field.setSRID(srid);


        ForeignKey foreignKey = field.getForeignKey();
        if (foreignKey!=null){
            String onDelete = constraint.get("onDelete").toString();

          //Special case for models. Cascade delete by default
            if (onDelete==null && field.isModel()){
                onDelete = "cascade";
            }

            if (onDelete!=null){
                foreignKey.onDelete(onDelete);
            }
        }


        if (constraint.has("default")){
            Object defaultValue = constraint.get("default").toObject();
            field.setDefaultValue(defaultValue);
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
  //** getPackageName
  //**************************************************************************
    public String getPackageName(){
        return packageName;
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
  //** getSchemaName
  //**************************************************************************
    public String getSchemaName(){
        return schemaName;
    }


  //**************************************************************************
  //** getJavaCode
  //**************************************************************************
  /** Used to generate Java code for the model.
   */
    public String getJavaCode(){
        String str = template.replace("${modelName}", name);
        str = str.replace("${package}", packageName);
        str = str.replace("${tableName}", schemaName==null ? tableName : (schemaName + "." + tableName));
        str = str.replace("${implements}", implementations.isEmpty() ? "" : "\r\n    implements " +
        implementations.stream().map(Object::toString).collect(Collectors.joining(", ")) + " ");


        StringBuilder fieldMap = new StringBuilder("\r\n");
        StringBuilder privateFields = new StringBuilder();
        StringBuilder publicMembers = new StringBuilder();
        StringBuilder getModels = new StringBuilder();
        StringBuilder saveModels = new StringBuilder();
        StringBuilder getValues = new StringBuilder();
        StringBuilder getJson = new StringBuilder();
        StringBuilder toJson = new StringBuilder();
        StringBuilder hasMany = new StringBuilder();
        StringBuilder initArrays = new StringBuilder();
        String getLastModified = "";
        TreeSet<String> includes = new TreeSet<>();



      //Special case for models that implement java.security.Principal
        boolean hasNameField = false;
        String getSecurityPrincipalName = "";
        if (implementations.contains("java.security.Principal")){

          //Check if fields have a name field
            for (Field field : fields){
                if (field.getName().equalsIgnoreCase("name")){
                    hasNameField = true;
                    break;
                }
            }

          //If no name field is found, we'll need to add a public getName()
            if (!hasNameField){
                getSecurityPrincipalName += "    public String getName(){\r\n";
                getSecurityPrincipalName += "        return null;\r\n";
                getSecurityPrincipalName += "    }\r\n\r\n";
            }
        }



      //Loop through all the fields and generate java code
        for (int i=0; i<fields.size(); i++){
            Field field = fields.get(i);
            String fieldName = Utils.underscoreToCamelCase(field.getName());
            String fieldType = field.getType();
            String methodName = Utils.capitalize(fieldName);
            String columnName = field.getColumnName();
            String modelName = null;
            if (field.isArray()){
                modelName = fieldType.substring(10, fieldType.length()-1);
                includes.add("java.util.ArrayList");
            }
            boolean password = fieldType.equals("Password");
            if (password) includes.add("javaxt.encryption.BCrypt");
            if (fieldType.equals("Date")) includes.add("javaxt.utils.Date");
            if (fieldType.equals("BigDecimal")) includes.add("java.math.BigDecimal");
            if (fieldType.equals("Geometry")){
                String jts = options.get("jts");
                if (jts==null) jts = "org.locationtech.jts"; //vs "com.vividsolutions.jts";
                includes.add(jts+".geom.Geometry");
                includes.add(jts+".io.WKTReader");
            }


            /* For Java 8 and below
          //Append field to the fieldMap
            fieldMap.append("            put(\"");
            fieldMap.append(fieldName);
            fieldMap.append("\", \"");
            fieldMap.append(columnName);
            fieldMap.append("\");\r\n");
            */



          //Append field to the fieldMap
            fieldMap.append("            java.util.Map.entry(\"");
            fieldMap.append(fieldName);
            fieldMap.append("\", \"");
            fieldMap.append(columnName);
            fieldMap.append("\")");
            if (i<fields.size()-1) fieldMap.append(",");
            fieldMap.append("\r\n");




          //Append private field
            if (!password){
                privateFields.append("    private ");
                privateFields.append(fieldType);
                privateFields.append(" ");
                privateFields.append(fieldName);
                privateFields.append(";\r\n");
            }
            else{
                privateFields.append("    private String ");
                privateFields.append(fieldName);
                privateFields.append("; //bcrypt hash\r\n");
            }

          //Append public get method
            if (!field.isArray()){

                if (password){
                    publicMembers.append("    /** Returns a BCrypt encrypted password */\r\n");
                }

                publicMembers.append("    public ");
                if (password){
                    publicMembers.append("String");
                }
                else{
                    publicMembers.append(fieldType);
                }
                publicMembers.append(" get");
                publicMembers.append(methodName);
                publicMembers.append("(){\r\n");
                publicMembers.append("        return ");
                publicMembers.append(fieldName);
                publicMembers.append(";\r\n");
                publicMembers.append("    }\r\n\r\n");


              //Add extra method if password
                if (password){

                  //Add an authenticate method for password fields
                    publicMembers.append("    public boolean authenticate(String ");
                    publicMembers.append(fieldName);
                    publicMembers.append("){\r\n");
                    publicMembers.append("        return BCrypt.checkpw(");
                    publicMembers.append(fieldName);
                    publicMembers.append(", this.");
                    publicMembers.append(fieldName);
                    publicMembers.append(");\r\n");
                    publicMembers.append("    }\r\n\r\n");
                }


              //Special case for models that implement java.security.Principal
              //If there's no name field, use the username as a return value
              //for the getName() method
                if (fieldName.equals("username") && fieldType.equals("String") &&
                    implementations.contains("java.security.Principal") &&
                    !hasNameField){
                    getSecurityPrincipalName = getSecurityPrincipalName.replace("null", fieldName);
                }

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
                    publicMembers.append(password ? "String" : fieldType);
                    publicMembers.append(" ");
                    publicMembers.append(fieldName);
                    publicMembers.append("){\r\n");

                    if (password){
                        publicMembers.append("        if (BCrypt.hasSalt(");
                        publicMembers.append(fieldName);
                        publicMembers.append(")) this.");
                        publicMembers.append(fieldName);
                        publicMembers.append(" = ");
                        publicMembers.append(fieldName);
                        publicMembers.append(";\r\n");
                        publicMembers.append("        else this.");
                    }
                    else{
                        publicMembers.append("        this.");
                    }

                    publicMembers.append(fieldName);
                    publicMembers.append(" = ");
                    if (password){
                        publicMembers.append("BCrypt.hashpw(" + fieldName + ", BCrypt.gensalt())");
                    }
                    else{
                        publicMembers.append(fieldName);
                    }
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
                    publicMembers.append("        this." + fieldName + ".add(" + paramName + ");\r\n");
                    publicMembers.append("    }\r\n\r\n");
                }
            }


          //Update database constructor
            if (!field.isArray()){
                if (!field.isModel()){
                    if (fieldType.equals("JSONObject")){
                        getValues.append("            this.");
                        getValues.append(fieldName);
                        getValues.append(" = new JSONObject(getValue(rs, \"");
                        getValues.append(columnName);
                        getValues.append("\").toString());\r\n");
                    }
                    else if (fieldType.equals("byte[]")){
                        getValues.append("            this.");
                        getValues.append(fieldName);
                        getValues.append(" = getValue(rs, \"");
                        getValues.append(columnName);
                        getValues.append("\").toByteArray();\r\n");
                    }
                    else if (fieldType.equals("Geometry")){
                        getValues.append("            try{this.");
                        getValues.append(fieldName);
                        getValues.append(" = new WKTReader().read(getValue(rs, \"");
                        getValues.append(columnName);
                        getValues.append("\").toString());}catch(Exception e){}\r\n");
                    }
                    else{

                        if (fieldType.endsWith("[]")){ //e.g. String[]
                            getValues.append("            {");

                            //Object[] v = (Object[])getValue(rs, "recent_customers").toArray();
                            getValues.append("Object[] v = (Object[]) getValue(rs, \"");
                            getValues.append(columnName);
                            getValues.append("\").toArray();\r\n");

                            //this.recentCustomers = java.util.Arrays.copyOf(v, v.length, String[].class);
                            getValues.append("            this.");
                            getValues.append(fieldName);
                            getValues.append(" = v==null ? null : java.util.Arrays.copyOf(v, v.length, ");
                            getValues.append(fieldType);
                            getValues.append(".class);");

                            getValues.append("}\r\n");
                        }
                        else{
                            getValues.append("            this.");
                            getValues.append(fieldName);
                            getValues.append(" = getValue(rs, \"");
                            getValues.append(columnName);
                            getValues.append("\").to");
                            getValues.append(password ? "String" : fieldType);
                            getValues.append("();\r\n");
                        }
                    }
                }
                else{
                    String id = Utils.underscoreToCamelCase(fieldName) + "ID";
                    getValues.append("            Long ");
                    getValues.append(id);
                    getValues.append(" = getValue(rs, \"");
                    getValues.append(columnName);
                    getValues.append("\").toLong();\r\n");

                    getModels.append("\r\n\r\n");
                    getModels.append("          //Set " + fieldName + "\r\n");
                    getModels.append("            if (" + id + "!=null) ");
                    getModels.append(fieldName + " = new " + fieldType + "(" + id + ");\r\n");
                }
            }
            else{
                String leftTable = this.tableName;
                String leftColumn = leftTable + "_id";
                String rightTable = Utils.camelCaseToUnderScore(modelName).toLowerCase();
                String rightColumn = rightTable + "_id";

              //Special case for when a model hasMany of itself
                if (leftTable.equals(rightTable))  rightColumn = rightTable + "_id2";

                String tableName = leftTable + "_" + rightTable;
                if (schemaName!=null) tableName = escapedSchemaName + "." + tableName;
                tableName = tableName.toLowerCase();



                String idArray = modelName + "IDs";
                idArray = idArray.substring(0, 1).toLowerCase() + idArray.substring(1);

                String id = modelName + "ID";
                id = id.substring(0, 1).toLowerCase() + id.substring(1);


              //Update get models (see database constructor)
                hasMany.append("\r\n\r\n");
                hasMany.append("              //Set " + fieldName + "\r\n");
                hasMany.append("                for (javaxt.sql.Record record : conn.getRecords(\r\n");
                hasMany.append("                    \"select " + rightColumn + " from " + tableName + " where " + leftColumn + "=\"+id)){\r\n");
                hasMany.append("                    " + fieldName + ".add(new " + modelName + "(record.get(0).toLong()));\r\n");
                hasMany.append("                }\r\n");

                initArrays.append("        " + fieldName + " = new " + fieldType + "();\r\n");


              //Update save models (see save method)
                saveModels.append("\r\n");
                saveModels.append("          //Save " + fieldName + "\r\n");
                saveModels.append("            ArrayList<Long> " + idArray + " = new ArrayList<>();\r\n");
                saveModels.append("            for (" + modelName + " obj : this." + fieldName + "){\r\n");
                saveModels.append("                obj.save();\r\n");
                saveModels.append("                " + idArray + ".add(obj.getID());\r\n");
                saveModels.append("            }\r\n");


                saveModels.append("\r\n\r\n");
                saveModels.append("          //Link " + fieldName + " to this " + this.name + "\r\n");
                saveModels.append("            target = \"" + tableName + " where " + leftColumn + "=\" + this.id;\r\n");
                saveModels.append("            conn.execute(\"delete from \" + target);\r\n");
                saveModels.append("            try (javaxt.sql.Recordset rs = conn.getRecordset(\"select * from \" + target, false)){\r\n");
                saveModels.append("                for (long " + id + " : " + idArray + "){\r\n");
                saveModels.append("                    rs.addNew();\r\n");
                saveModels.append("                    rs.setValue(\"" + leftColumn + "\", this.id);\r\n");
                saveModels.append("                    rs.setValue(\"" + rightColumn + "\", " + id + ");\r\n");
                saveModels.append("                    rs.update();\r\n");
                saveModels.append("                }\r\n");
                saveModels.append("            }\r\n");
            }


          //Update json constructor
            if (!field.isArray()){
                if (!field.isModel()){
                    if (!password){


                        if (fieldType.equals("Geometry")){
                            getJson.append("        try {\r\n");
                            getJson.append("            this.");
                            getJson.append(fieldName);
                            getJson.append(" = new WKTReader().read(json.get(\"");
                            getJson.append(columnName);
                            getJson.append("\").toString());\r\n");
                            getJson.append("        }\r\n");
                            getJson.append("        catch(Exception e) {}\r\n");
                        }
                        else if (fieldType.equals("byte[]")){
                            getJson.append("        this.");
                            getJson.append(fieldName);
                            getJson.append(" = json.get(\"");
                            getJson.append(fieldName);
                            getJson.append("\").toByteArray();\r\n");
                        }
                        else{

                            if (fieldType.endsWith("[]")){ //e.g. String[]
                                getJson.append("        {");

                                //Object[] v = json.has("recentCustomers") ? json.get("recentCustomers").toJSONArray().toArray() : null;
                                getJson.append("Object[] v = json.has(\"");
                                getJson.append(fieldName);
                                getJson.append("\") ? json.get(\"");
                                getJson.append(fieldName);
                                getJson.append("\").toJSONArray().toArray() : null;\r\n");

                                //this.recentCustomers = java.util.Arrays.copyOf(v, v.length, String[].class);
                                getJson.append("        this.");
                                getJson.append(fieldName);
                                getJson.append(" = v==null ? null : java.util.Arrays.copyOf(v, v.length, ");
                                getJson.append(fieldType);
                                getJson.append(".class);");

                                getJson.append("}\r\n");
                            }
                            else{
                                getJson.append("        this.");
                                getJson.append(fieldName);
                                getJson.append(" = json.get(\"");
                                getJson.append(fieldName);
                                getJson.append("\").to");
                                getJson.append(fieldType);
                                getJson.append("();\r\n");
                            }
                        }
                    }
                    else{
                        getJson.append("        this.set");
                        getJson.append(methodName);
                        getJson.append("(json.get(\"");
                        getJson.append(columnName);
                        getJson.append("\").toString());\r\n");
                    }
                }
                else{ //Model

                    //if (json.has("account")){...
                    getJson.append("        if (json.has(\"");
                    getJson.append(fieldName);
                    getJson.append("\")){\r\n            ");
                    getJson.append(fieldName);
                    getJson.append(" = new ");
                    getJson.append(fieldType);
                    getJson.append("(json.get(\"");
                    getJson.append(fieldName);
                    getJson.append("\").toJSONObject());\r\n");
                    getJson.append("        }\r\n");


                    //if (json.has("accountID")){...
                    getJson.append("        else if (json.has(\"");
                    getJson.append(fieldName);
                    getJson.append("ID\")){\r\n");
                    getJson.append("            ");
                    getJson.append("try{\r\n");
                    getJson.append("                ");
                    getJson.append(fieldName);
                    getJson.append(" = new ");
                    getJson.append(fieldType);
                    getJson.append("(json.get(\"");
                    getJson.append(fieldName);
                    getJson.append("ID\").toLong());\r\n");
                    getJson.append("            ");
                    getJson.append("}\r\n");
                    getJson.append("            ");
                    getJson.append("catch(Exception e){}\r\n");
                    getJson.append("        }\r\n");
                }
            }
            else{
                getJson.append("\r\n");
                getJson.append("      //Set " + fieldName + "\r\n");
                getJson.append("        if (json.has(\"");
                getJson.append(fieldName);
                getJson.append("\")){\r\n");
                getJson.append("            for (JSONValue _");
                getJson.append(fieldName);
                getJson.append(" : json.get(\"");
                getJson.append(fieldName);
                getJson.append("\").toJSONArray()){\r\n");
                getJson.append("                ");
                getJson.append(fieldName);
                getJson.append(".add(new " + modelName + "(_" + fieldName + ".toJSONObject()));\r\n");
                getJson.append("            }\r\n");
                getJson.append("        }\r\n\r\n");
            }




          //Update toJson method
            if (!field.isArray()){
                if (password){
                    if (toJson.length()==0) toJson.append("\r\n");
                    toJson.append("        json.set(\"");
                    toJson.append(fieldName);
                    toJson.append("\", null");
                    toJson.append(");\r\n");
                }
            }

        }


      //Update the database constructor with hasMany variables
        if (hasMany.length()>0){
            getValues.append("\r\n\r\n");
            getValues.append("            try (javaxt.sql.Connection conn = getConnection(this.getClass())) {\r\n");
            getValues.append(hasMany);
            getValues.append("            }\r\n");
        }


      //Generate list of field names used in the init() method
        StringBuilder fieldNames = new StringBuilder("\r\n            \"id\"");
        for (Field field : fields){
            String fieldType = field.getType();
            String columnName = field.getColumnName();
            fieldNames.append(", ");
            fieldNames.append("\r\n            ");
            fieldNames.append("\"");
            if (fieldType.equals("Geometry")){
                fieldNames.append("ST_AsText(" + columnName + ") as ");
            }
            fieldNames.append(columnName);
            fieldNames.append("\"");
        }


      //Replace keys in the class template
        str = str.replace("${fieldMap}", fieldMap.toString());
        str = str.replace("${fieldNames}", fieldNames.toString());
        str = str.replace("${field[0]}", fields.get(0).getColumnName());
        str = str.replace("${initArrays}", initArrays.toString().trim());
        str = str.replace("${privateFields}", privateFields.toString().trim());
        str = str.replace("${publicMembers}", (getSecurityPrincipalName + publicMembers.toString()).trim());
        str = str.replace("${getModels}", getModels.toString());
        str = str.replace("${getValues}", getValues.toString());
        str = str.replace("${getJson}", getJson.toString().trim());


      //Add custom save method as needed
        if (saveModels.length()>0){

            String fn =
            "\r\n" +
            "  //**************************************************************************\r\n" +
            "  //** save\r\n" +
            "  //**************************************************************************\r\n" +
            "  /** Used to save a " + name + " in the database.\r\n" +
            "   */\r\n" +
            "    public void save() throws SQLException {\r\n\r\n" +

            "      //Update record in the " + this.tableName + " table\r\n" +
            "        super.save();\r\n\r\n\r\n" +

            "      //Save models\r\n" +
            "        try (javaxt.sql.Connection conn = getConnection(this.getClass())) {\r\n" +
            "            String target;\r\n" +
            "            " + saveModels +
            "        }\r\n" +
            "    }\r\n";

            str = str.replace("${saveModel}", fn);
        }
        else{
            str = str.replace("${saveModel}", "");
        }


      //Add custom toJson method as needed
        if (toJson.length()>0){
            String fn =
            "\r\n" +
            "  //**************************************************************************\r\n" +
            "  //** toJson\r\n" +
            "  //**************************************************************************\r\n" +
            "  /** Returns a string representation of the " + name + " in JSON notation.\r\n" +
            "   */\r\n" +
            "    public JSONObject toJson(){\r\n" +
            "        JSONObject json = super.toJson();\r\n" +
            "            " + toJson + "\r\n" +
            "        return json;\r\n" +
            "    }\r\n";

            str = str.replace("${toJson}", fn);
        }
        else{
            str = str.replace("${toJson}", "");
        }


      //Add includes
        if (includes.isEmpty()){
            str = str.replace("${includes}", "");
        }
        else{
            StringBuilder s = new StringBuilder();
            for (String include : includes){
                s.append("import ");
                s.append(include);
                s.append(";\r\n");
            }
            str = str.replace("${includes}", s.toString().trim());
        }



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
        str.append(escapedTableName);
        str.append(" (\r\n");
        str.append("    ID BIGSERIAL NOT NULL,\r\n");


      //Add fields
        ArrayList<String> foreignKeys = new ArrayList<>();
        Iterator<Field> it = fields.iterator();
        while (it.hasNext()){
            Field field = it.next();
            if (field.isArray()) continue;
            str.append("    ");
            str.append(field.getColumnName().toUpperCase());
            str.append(" ");
            str.append(field.getColumnType());


            if (field.isRequired()) str.append(" NOT NULL");

            if (field.hasDefaultValue()){
                Object defaultValue = field.getDefaultValue();
                str.append(" DEFAULT ");
                if (defaultValue instanceof String){
                    String val = (String) defaultValue;
                    if (val.contains("(") && val.endsWith(")")){
                        str.append(val);
                    }
                    else{
                        str.append("'" + val.replace("'", "''") + "'");
                    }
                }
                else{
                    str.append(defaultValue.toString());
                }
            }


            if (field.isUnique()) str.append(" UNIQUE");



            str.append(",");
            str.append("\r\n");

            ForeignKey foreignKey = field.getForeignKey();
            if (foreignKey!=null) foreignKeys.add(foreignKey.getColumnName());
        }


      //Add primary key constraint
        str.append("    CONSTRAINT PK_");
        str.append(this.tableName.toUpperCase());
        str.append(" PRIMARY KEY (ID)");


      //End create table script
        str.append("\r\n);\r\n\r\n");


        return str.toString();
    }


  //**************************************************************************
  //** getDiamondTableSQL
  //**************************************************************************
  /** Returns an SQL script used to generate diamond tables and indexes
   */
    public String getDiamondTableSQL(){
        StringBuilder str = new StringBuilder();
        Iterator<Field> it = fields.iterator();
        while (it.hasNext()){
            Field field = it.next();
            if (field.isArray()){



                String modelName = field.getType().substring(10);
                modelName = modelName.substring(0, modelName.length()-1);

                String leftTable = this.tableName.toUpperCase();
                String leftColumn = leftTable + "_ID";

                String rightTable = Utils.camelCaseToUnderScore(modelName).toUpperCase();
                String rightColumn = rightTable + "_ID";

                //Special case for when a model hasMany of itself
                if (leftTable.equals(rightTable)) rightColumn = rightTable + "_ID2";


                String tableName = leftTable + "_" + rightTable;
                //String tableName = leftTable + "_" + field.getColumnName().toUpperCase();


                String foreignKey = "FK_" + tableName;
                String indexPrefix = "IDX_" + tableName.toUpperCase()+ "_";
                if (schemaName!=null) tableName = escapedSchemaName + "." + tableName;

                leftTable = escapeTableName(leftTable);
                if (schemaName!=null) leftTable = escapedSchemaName + "." + leftTable;

                rightTable = escapeTableName(rightTable);
                if (schemaName!=null) rightTable = escapedSchemaName + "." + rightTable;


                str.append("CREATE TABLE ");
                str.append(tableName);
                str.append(" (\r\n    ");
                str.append(leftColumn);
                str.append(" BIGINT NOT NULL,\r\n    ");
                str.append(rightColumn);
                str.append(" BIGINT NOT NULL,\r\n");
                str.append("    CONSTRAINT ");
                str.append(foreignKey);
                str.append(" FOREIGN KEY (");
                str.append(leftColumn);
                str.append(") REFERENCES ");
                str.append(leftTable);
                str.append("(ID)\r\n");
                str.append("        ON DELETE CASCADE ON UPDATE NO ACTION\r\n");
                str.append(");\r\n\r\n");


                str.append("ALTER TABLE ");
                str.append(tableName);
                str.append(" ADD FOREIGN KEY (");
                str.append(rightColumn);
                str.append(") REFERENCES ");
                str.append(rightTable);
                str.append("(ID)\r\n");
                str.append("    ON DELETE CASCADE ON UPDATE NO ACTION;\r\n\r\n");


                str.append("CREATE INDEX ");
                str.append(indexPrefix);
                str.append(leftColumn);
                str.append(" ON ");
                str.append(tableName);
                str.append("(");
                str.append(leftColumn);
                str.append(");\r\n");


                str.append("CREATE INDEX ");
                str.append(indexPrefix);
                str.append(rightColumn);
                str.append(" ON ");
                str.append(tableName);
                str.append("(");
                str.append(rightColumn);
                str.append(");\r\n");


                str.append("\r\n");
            }
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
        Iterator<Field> it = fields.iterator();
        while (it.hasNext()){
            Field field = it.next();
            if (!field.isArray()){
                ForeignKey foreignKey = field.getForeignKey();
                if (foreignKey!=null){
                    String columnName = foreignKey.getColumnName().toUpperCase();
                    String foreignTable = escapeTableName(foreignKey.getForeignTable().toUpperCase());
                    if (schemaName!=null) foreignTable = escapedSchemaName + "." + foreignTable;
                    String foreignColumn = "ID";

                    str.append("ALTER TABLE ");
                    str.append(escapedTableName);
                    str.append(" ADD FOREIGN KEY (");
                    str.append(columnName);
                    str.append(") REFERENCES ");
                    str.append(foreignTable);
                    str.append("(");
                    str.append(foreignColumn);
                    str.append(")\r\n");
                    str.append("    ON DELETE ");
                    str.append(foreignKey.onDelete());
                    str.append(" ON UPDATE NO ACTION;\r\n\r\n");
                }
            }
            else{
                //Handled by getDiamondTableSQL()
            }
        }
        return str.toString();
    }


  //**************************************************************************
  //** getIndexSQL
  //**************************************************************************
  /** Returns an SQL script used to add indexes to the table associated with
   *  the model.
   */
    public String getIndexSQL(){
        StringBuilder str = new StringBuilder();
        String indexPrefix = "IDX_" + tableName.toUpperCase()+ "_";
        Iterator<Field> it = fields.iterator();
        while (it.hasNext()){
            Field field = it.next();
            if (!field.isArray()){
                ForeignKey foreignKey = field.getForeignKey();
                if (foreignKey!=null){
                    String columnName = foreignKey.getColumnName().toUpperCase();
                    String foreignTable = foreignKey.getForeignTable().toUpperCase();
                    str.append("CREATE INDEX ");
                    str.append(indexPrefix);
                    str.append(foreignTable);
                    str.append(" ON ");
                    str.append(escapedTableName);
                    str.append("(");
                    str.append(columnName);
                    str.append(");\r\n");
                }
                else{
                    String columnName = field.getColumnName().toUpperCase();
                    if (field.getColumnType().startsWith("geo")){
                        str.append("CREATE INDEX ");
                        str.append(indexPrefix);
                        str.append(columnName);
                        str.append(" ON ");
                        str.append(escapedTableName);
                        str.append(" USING GIST(");
                        str.append(columnName);
                        str.append(");\r\n");
                    }
                }
            }
            else{
                //Handled by getDiamondTableSQL()
            }
        }
        return str.toString();
    }


  //**************************************************************************
  //** getTriggerSQL
  //**************************************************************************
  /** Returns an SQL script used to generate triggers
   */
    public String getTriggerSQL(){
        StringBuilder str = new StringBuilder();


      //Add last modified trigger as needed. See Writer.write() for the
      //last_modified() implementation.
        if (hasLastModifiedField()){
            str.append("CREATE TRIGGER TGR_");
            str.append(this.tableName.toUpperCase());
            str.append("_UPDATE BEFORE INSERT OR UPDATE ON ");
            str.append(escapedTableName);
            str.append("\r\n    FOR EACH ROW EXECUTE PROCEDURE last_modified();\r\n\r\n");
        }


        return str.toString();
    }


  //**************************************************************************
  //** hasLastModifiedField
  //**************************************************************************
  /** Returns true if the model contains a lastModified date field.
   */
    protected boolean hasLastModifiedField(){
        Iterator<Field> it = fields.iterator();
        while (it.hasNext()){
            Field field = it.next();
            if (field.isArray()) continue;
            if (field.isLastModifiedDate()) return true;
        }
        return false;
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
  //** escapeTableName
  //**************************************************************************
  /** Returns a sql compatible table name
   */
    protected String escapeTableName(String tableName){
        if (tableName==null) return null;
        if (tableName.equalsIgnoreCase("user") && schemaName==null){ //and db is postgres
            return "\"" + tableName.toLowerCase() + "\"";
        }
        else{
            return tableName.toUpperCase();
        }
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