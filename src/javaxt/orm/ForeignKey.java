package javaxt.orm;

public class ForeignKey {

    private String foreignKey;
    private String foreignTable;
    private String onDelete = "NO ACTION"; //CASCADE

    protected ForeignKey(String columnName, String type){
        foreignKey = columnName;
        foreignTable = Utils.camelCaseToUnderScore(type);
    }

    public String getColumnName(){
        return foreignKey;
    }

    public String getForeignTable(){
        return foreignTable;
    }

    public String onDelete(){
        return onDelete;
    }

    protected void onDelete(String action){
        onDelete = action.toUpperCase();
    }
}