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
Requires Java 8 for javascript parsing. Also requires javaxt-core.jar
