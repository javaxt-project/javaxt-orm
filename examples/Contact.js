var package = "com.example.models";
var models = {

  //**************************************************************************
  //** Person
  //**************************************************************************
  /** Used to represent an individual.
   */
    Person: {
        fields: [
            {name: 'gender',     type: 'string'}, //M,F
            {name: 'birthday',   type: 'int'}, //yyyymmdd
            {name: 'info',       type: 'json'}
        ],
        hasMany: [
            {model: 'Name',      name: 'names'},
            {model: 'Phone',     name: 'phoneNumbers'},
            {model: 'Email',     name: 'emails'},
            {model: 'Address',   name: 'addresses'}
        ],
        constraints: [
            {name: 'names',         required: true},
            {name: 'gender',        length: 1}
        ]
    },


  //**************************************************************************
  //** Name
  //**************************************************************************
  /** Used to represent a name of a person or place
   */
    Name: {
        fields: [
            {name: 'name',      type: 'string'},
            {name: 'primary',   type: 'boolean'}
        ],
        constraints: [
            {name: 'name',  required: true,  length: 75}
        ]
    },


  //**************************************************************************
  //** Phone
  //**************************************************************************
  /** Used to represent a phone number.
   */
    Phone: {
        fields: [
            {name: 'number',    type: 'string'},
            {name: 'type',      type: 'string'}, //home, mobile, work
            {name: 'primary',   type: 'boolean'} //is primary phone?
        ],
        constraints: [
            {name: 'number',  required: true,  length: 20},
            {name: 'type',  length: 15}
        ]
    },


  //**************************************************************************
  //** Physical Address
  //**************************************************************************
  /** Used to represent a physical address.
   */
    Address: {
        fields: [
            {name: 'type',          type: 'string'}, //home, work, etc
            {name: 'street',        type: 'string'},
            {name: 'city',          type: 'string'},
            {name: 'state',         type: 'string'},
            {name: 'postalCode',    type: 'string'},
            {name: 'coordinates',   type: 'geo'},
            {name: 'primary',       type: 'boolean'}
        ]
    },


  //**************************************************************************
  //** Email Address
  //**************************************************************************
  /** Used to represent an email address.
   */
    Email: {
        fields: [
            {name: 'type',      type: 'string'}, //personal, work, etc.
            {name: 'address',   type: 'string'}, //name@server.com
            {name: 'primary',   type: 'boolean'} //is primary email?
        ],
        constraints: [
            {name: 'address',    required: true}
        ]
    }

};