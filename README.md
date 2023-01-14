# JavaXT ORM (Object Relational Mapping)
Command line utility for generating Java code and DDL from a set of models defined in a javascript or json document. 


This is not intended to be a full fledged ORM framework. Instead, the goal is to help jumpstart new projects by
providing a simple utility for stubbing out code and SQL.



## Model mapping and supported types


Field Type	| Java Type		| Database Type
------------|-------------|----------------------------------------------
text		| String		| text (or varchar if there is a length constraint)
int		| Integer		| integer
long		| Long			| bigint
float		| Double		| double precision
decimal		| BigDecimal		| numeric
date		| Date			| timestamp with time zone
boolean		| Boolean		| Boolean
binary		| byte[]		| bytea
json		| JSONObject		| jasonb
geo		| Object		| geometry(Geometry,4326)
password	| String (bcrypt hash)	| text (bcrypt hash)




## Supported field constraints

Constraint Key	| Type		| Comments
----------------|---------|----------------------------------------------
required	| boolean	| Can also use "nullable" keyword.
length		| int		| Only applies to "text" field types. Reassigns the database type to varchar.
unique		| boolean	| Should not be applied to "text" fields without a length constraint.


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
in your project.


## How to Use With Java 15 and Up
The javaxt-orm library relies on Nashorn for parsing input Javascript files.
Nashorn used to be bundled with Java between versions 8 to 14. Unfortunately, starting with Java 15
Nashorn is no longer included in the standard distribution and needs to be downloaded as a seperate JAR via OpenJDK.
More info <a href="https://gist.github.com/pborissow/a1d8a7721d131b773235cca88dc0b88c">here</a>.
