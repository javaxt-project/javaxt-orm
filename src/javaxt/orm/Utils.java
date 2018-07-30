package javaxt.orm;

public class Utils {
    
    private Utils(){}
    
    public static String camelCaseToUnderScore(String input) {
        StringBuffer result = new StringBuffer();
        boolean begin = true;
        boolean lastUppercase = false;
        for( int i=0; i < input.length(); i++ ) {
            char ch = input.charAt(i);
            if( Character.isUpperCase(ch) ) {
                // is start?
                if( begin ) {
                    result.append(ch);
                } else {
                    if( lastUppercase ) {
                        // test if end of acronym
                        if( i+1<input.length() ) {
                            char next = input.charAt(i+1);
                            if( Character.isUpperCase(next) ) {
                                // acronym continues
                                result.append(ch);
                            } else {
                                // end of acronym
                                result.append('_').append(ch);
                            }
                        } else {
                            // acronym continues
                            result.append(ch);
                        }
                    } else {
                        // last was lowercase, insert _
                        result.append('_').append(ch);
                    }
                }
                lastUppercase=true;
            } else {
                result.append(Character.toUpperCase(ch));
                lastUppercase=false;
            }
            begin=false;
        }
        return result.toString().toLowerCase();
    }    
    
    
    public static String underscoreToCamelCase(String input){
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("_(.)");
        java.util.regex.Matcher m = p.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, m.group(1).toUpperCase());
        }
        m.appendTail(sb);
        
        String str = sb.toString();
        if (str.endsWith("Id") && input.toLowerCase().endsWith("_id")){
            str = str.substring(0, str.length()-2) + "ID";
        }
        return str;
    }
    
    
    /** Used to capitalize the first letter in the given string. */
    public static String capitalize(String fieldName){
        return fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
    }

}