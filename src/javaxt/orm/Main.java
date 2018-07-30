package javaxt.orm;
import javax.script.*;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import javaxt.json.*;


public class Main {

    public static void main(String[] args) throws Exception {
        
        
        javaxt.io.File inputFile = new javaxt.io.File(args[0]);
        javaxt.io.Directory outputDirectory = new javaxt.io.Directory(args[1]);
        
        
      //Get models
        JSONObject models;
        String ext = inputFile.getExtension().toLowerCase();
        if (ext.equals("js")){
            models = getModels(inputFile.getText());
        }
        else{
            models = new JSONObject(inputFile.getText());
        }
        //System.out.println(models.toString(4));
        

        
      //Iterate through all the models
        for (Model model : Model.getModels(models)){
            System.out.println(model);

            
            if (model.getName().equals("Contact")){
                System.out.println(model.createClass());
                break;
            }
        }
        
        
        //For each model...
        
        //Create class
        
        //Create DDL
        
        
        
    }
    
    
    
    /** Evaluate model using nashorn */
    private static JSONObject getModels(String js) throws Exception {
        
        
      //Instantiate ScriptEngine
        ScriptEngineManager factory = new ScriptEngineManager();
        ScriptEngine engine = factory.getEngineByName("nashorn");
        
        
      //Get models
        ScriptContext ctx = new SimpleScriptContext();
        ctx.setBindings(engine.createBindings(), ScriptContext.ENGINE_SCOPE);
        engine.eval(js, ctx);
        Object models = ctx.getAttribute("models");
        
        
      //Stringify models
        ScriptObjectMirror json = (ScriptObjectMirror) engine.eval("JSON");
        String str = json.callMember("stringify", models).toString();

      //Return JSON
        return new JSONObject(str);
    }
    
}