package javaxt.orm;

public class Main {

    public static void main(String[] args) throws Exception {
        
      //Parse inputs
        javaxt.io.File inputFile = new javaxt.io.File(args[0]);
        javaxt.io.Directory outputDirectory = new javaxt.io.Directory(args[1]);
        
        
      //Validate inputs
        if (!inputFile.exists()) throw new IllegalArgumentException("Input file not found");
        

      //Get models
        Model[] models = new Parser(inputFile.getText()).getModels();

        
      //Run tests
        //testContact(models);
        //testUser(models);
        new Writer(models).write(outputDirectory);
    }
    
    
    private static void testContact(Model[] models){
        for (Model model : models){
            
            String modelName = model.getName();
            if (modelName.equals("Contact") || modelName.equals("Phone") || modelName.equals("Email")){} 
            else continue;
            
            
          //Create class
            if (modelName.equals("Contact")) System.out.println(model.getJavaCode());
            
        
          //Create DDL
            System.out.println(model.getTableSQL());
            System.out.println(model.getForeignKeySQL());
        }
    }
    
    
    private static void testUser(Model[] models){
        for (Model model : models){


            
            String modelName = model.getName();
            if (!modelName.equals("User")) continue;
            
            
            System.out.println(model.getJavaCode());
            System.out.println(model.getTableSQL());
            System.out.println(model.getForeignKeySQL());
        }
    }
}