package javaxt.orm;

public class Field {

    private String name;
    private String type;
    private boolean required = false;
    
    protected Field(String name, String type){
        this.name = name;
        if (type.equalsIgnoreCase("int")) type = "integer";
        if (type.equalsIgnoreCase("text")){ 
            type = "string";
            //columnType = "text"
        }
        this.type = Utils.capitalize(type);
    }
    
    public String getName(){
        return name;
    }
    
    public String getType(){
        return type;
    }
    
    public String getColumnName(){
        return Utils.camelCaseToUnderScore(name);
    }
    
}