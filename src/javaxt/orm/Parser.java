package javaxt.orm;
import javax.script.*;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import javaxt.json.*;

//******************************************************************************
//**  Parser Class
//******************************************************************************
/**
 *   Used to extract models defined in a javascript or json document.
 *
 ******************************************************************************/

public class Parser {

    private String packageName;
    private Model[] models;
    
    
  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a string representing either
   *  a json document or a javascript.
   */
    public Parser(String input) throws Exception {
        if (input==null || input.trim().isEmpty()){ 
            throw new IllegalArgumentException("Parser input is empty");
        }
        
        try{
            init(new JSONObject(input));
        }
        catch(JSONException e){
            init(parseJavaScript(input));
        }
    }
    
    
  //**************************************************************************
  //** Constructor
  //**************************************************************************
  /** Creates a new instance of this class using a json document.
   */
    public Parser(JSONObject json){
        init(json);
    }

    
  //**************************************************************************
  //** getModels
  //**************************************************************************
  /** Returns all the models found in the input document.
   */
    public Model[] getModels(){
        return models;
    }
    
    
    
  //**************************************************************************
  //** init
  //**************************************************************************
  /** Used to parse a json document containing models and a package name.
   */
    private void init(JSONObject json){
        packageName = json.get("package").toString();
        JSONObject models = json.get("models").toJSONObject();
        
        java.util.ArrayList<Model> arr = new java.util.ArrayList<Model>();
        for (String modelName : models.keySet()){
            Model model = new Model(modelName, packageName, models.get(modelName).toJSONObject());
            arr.add(model);
        }
        
        
        this.models = arr.toArray(new Model[arr.size()]);
    }

    
  //**************************************************************************
  //** parseJavaScript
  //**************************************************************************
  /** Used to parse a javascript containing models and a package name.
   */
    private JSONObject parseJavaScript(String js) throws Exception {
        JSONObject output = new JSONObject();
        
        
      //Instantiate ScriptEngine
        ScriptEngineManager factory = new ScriptEngineManager();
        ScriptEngine engine = factory.getEngineByName("nashorn");
        
        
      //Extract variables
        ScriptContext ctx = new SimpleScriptContext();
        ctx.setBindings(engine.createBindings(), ScriptContext.ENGINE_SCOPE);
        engine.eval(js, ctx);
        Object packageName = ctx.getAttribute("package");
        Object models = ctx.getAttribute("models");
        
        
      //Add package to output
        output.set("package", packageName.toString());
        
        
      //Stringify models and convert to json
        ScriptObjectMirror json = (ScriptObjectMirror) engine.eval("JSON");
        String str = json.callMember("stringify", models).toString();
        output.set("models", new JSONObject(str));
        
        
      //Return JSON
        return output;
    }
}