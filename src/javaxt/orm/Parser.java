package javaxt.orm;

import java.util.*;
import javaxt.json.*;

//Scripting includes
import javax.script.*;
import org.openjdk.nashorn.api.scripting.ScriptObjectMirror;
import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory;
import org.openjdk.nashorn.api.scripting.NashornScriptEngine;


//******************************************************************************
//**  Parser Class
//******************************************************************************
/**
 *   Used to extract models defined in a javascript or json document.
 *
 ******************************************************************************/

public class Parser {

    private Model[] models;
    private static String[] optionalVars = new String[]{"schema", "jts"};


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
        String packageName = json.get("package").toString();
        JSONObject models = json.get("models").toJSONObject();
        HashMap<String, String> options = new HashMap<>();
        for (String key : optionalVars){
            String val = json.get(key).toString();
            if (val!=null) options.put(key, val);
        }

        ArrayList<Model> arr = new ArrayList<>();
        for (String modelName : models.keySet()){
            Model model = new Model(modelName, models.get(modelName).toJSONObject(), packageName, options);
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
        String[] options = new String[] { "--language=es6" };
        NashornScriptEngineFactory factory = new NashornScriptEngineFactory();
        NashornScriptEngine engine = (NashornScriptEngine) factory.getScriptEngine(options);


      //Extract variables
        ScriptContext ctx = new SimpleScriptContext();
        ctx.setBindings(engine.createBindings(), ScriptContext.ENGINE_SCOPE);
        engine.eval(js, ctx);


      //Add package name to output
        Object packageName = ctx.getAttribute("package");
        output.set("package", packageName.toString());


      //Add options to output
        for (String key : optionalVars){
            Object schemaName = ctx.getAttribute(key);
            if (schemaName!=null) output.set(key, schemaName.toString());
        }


      //Stringify models and convert to json
        Object models = ctx.getAttribute("models");
        ScriptObjectMirror json = (ScriptObjectMirror) engine.eval("JSON");
        String str = json.callMember("stringify", models).toString();
        output.set("models", new JSONObject(str));



      //Return JSON
        return output;
    }
}