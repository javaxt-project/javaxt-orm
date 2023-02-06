# JavaXT ORM (Object Relational Mapping)
Command line utility for generating Java code and DDL from a set of models defined in a javascript or json document. 


This is not intended to be a full fledged ORM framework. Instead, the goal is to help jumpstart new projects by
providing a simple utility for stubbing out code and SQL.

## Command Line Interface
The javaxt-orm library provides a command line interface that can be used to generate Java classes 
and schema. All you need to do is provide a input model and an output directory. Example:
```
java -jar javaxt-orm.jar /path/to/model.js /output
```

## Model Input
Below is a simple example of an input Javascript file with an Address model. 
```javascript
var package = "com.example.models";
var models = {
    Address: {
        fields: [
            {name: 'street',        type: 'string'},
            {name: 'city',          type: 'string'},
            {name: 'state',         type: 'string'},
            {name: 'postalCode',    type: 'string'},
            {name: 'coordinates',   type: 'geo'}
        ]
    }
}
```

The examples folder contains a few sample models that you can use as reference.


## Model mapping and supported types


Field Type	| Java Type		| Database Type   | Comments
------------|-------------|-----------------|----------------------------
int		| Integer		| integer |
long		| Long			| bigint |
float		| Double		| double precision |
double		| Double		| double precision |
decimal		| BigDecimal		| numeric |
numeric		| BigDecimal		| numeric |
text		| String		| text or varchar | varchar if there is a length constraint
string		| String		| text or varchar | varchar if there is a length constraint
char		| String		| char(1) |
boolean		| Boolean		| Boolean |
date		| Date			| timestamp with time zone |
binary		| byte[]		| bytea |
json		| JSONObject		| jasonb |
geo		| Geometry		| geometry(Geometry,4326) | For lat/lon geographic data
geometry		| Geometry		| geometry(GeometryZ) | For x,y,z data
password	| String | text | Stores bcrypt hash

In addition to these standard field types, you can specify a model as a `type`. Example:

```javascript
{
  Contact: {
     fields: [
        {name: 'name', type: 'string'},
        {name: 'address', type: 'Address'}
     ]
  },
  Address: {
      ...
  }  
}
```


## Supported field constraints

Constraint Key	| Type		| Comments
----------------|---------|----------------------------------------------
required	| boolean	| If true, adds a "NOT NULL" constraint to the field. Can also use "nullable" keyword.
length		| int		| Only applies to "text" field types. When specified, reassigns the database type to varchar.
unique		| boolean	| Should not be applied to "text" fields without a length constraint.
onDelete    | string    | Only applies to fields with models. Options include "cascade" and "no action" (default).

## Misc

IDs are automatically added so you don't have to explicitly define one in the model.

Models can be assigned to fields. A foreign key will be created in the database.

lastUpdate fields in the model are automatically assigned a trigger in the database.


## Dependencies
The javaxt-orm library requires Java 8 or higher for Javascript parsing and javaxt-core.jar for JSON and basic file IO.


## Generated Code Dependencies
The javaxt-orm library generates Java code that extends/implements javaxt.sql.Model class. 
It also calls java.util.Map.ofEntries which was introduced in Java 9. 
Therefore, you will need both javaxt-core and Java 9 (or higher) to use the generated code
in your project. In addition, you will need JTS if you include a `geo` or `geometry` type.
Last but not least, you will need to include a JDBC driver in your project for persistance.


## How to Use With Java 15 and Up
The javaxt-orm library relies on Nashorn for parsing input Javascript files.
Nashorn used to be bundled with Java between versions 8 to 14. Unfortunately, starting with Java 15
Nashorn is no longer included in the standard distribution and needs to be downloaded as a seperate JAR via OpenJDK.
More info <a href="https://gist.github.com/pborissow/a1d8a7721d131b773235cca88dc0b88c">here</a>.
