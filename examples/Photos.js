var package = "javaxt.photos";
var models = {

  //**************************************************************************
  //** Contact
  //**************************************************************************
  /** Used to represent an individual or company.
   */
    Contact: {
        fields: [
            {name: 'id',           type: 'int'},
            {name: 'firstName',    type: 'string'},
            {name: 'lastName',     type: 'string'},
            {name: 'fullName',     type: 'string'},
            {name: 'gender',       type: 'string'},
            {name: 'dob',          type: 'string'},
            {name: 'lastModified', type: 'date'}
        ],
        hasMany: [
            {model: 'Phone',     name: 'phoneNumbers'},
            {model: 'Email',     name: 'emails'},
            {model: 'Address',   name: 'addresses'},
            {model: 'Alias',     name: 'aliases'}
        ]
    },


  //**************************************************************************
  //** Phone
  //**************************************************************************
  /** Used to represent a contact's phone number.
   */
    Phone: {
        fields: [
            {name: 'number',    type: 'string'},
            {name: 'type',      type: 'string'}, //home, mobile, work
            {name: 'primary',   type: 'boolean'} //is primary phone?
        ]
    },


  //**************************************************************************
  //** Physical Address
  //**************************************************************************
  /** Used to represent a contact's physical address.
   */
    Address: {
        fields: [
            {name: 'type',          type: 'string'}, //home, work, etc
            {name: 'street',        type: 'string'},
            {name: 'city',          type: 'string'},
            {name: 'state',         type: 'string'},
            {name: 'postalCode',    type: 'string'},
            {name: 'coordinates',   type: 'geo'},
            {name: 'primary',       type: 'boolean'} //is principle address?
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
    },


  //**************************************************************************
  //** Alias
  //**************************************************************************
  /** Used to represent an alias associated with a contact.
   */
    Alias: {
        fields: [
            {name: 'name',   type: 'string'}
        ],
        constraints: [
            {name: 'name',  required: true,  length: 255,  unique: true}
        ]
    },


  //**************************************************************************
  //** UserAccount
  //**************************************************************************
    UserAccount: {
        fields: [
            {name: 'username',    type: 'string'},
            {name: 'password',    type: 'password'},
            {name: 'accessLevel', type: 'int'},
            {name: 'active',      type: 'boolean'},
            {name: 'contact',     type: 'Contact'},
            {name: 'auth',        type: 'json'}
        ],
        constraints: [
            {name: 'username',   required: true,  length: 255,  unique: true},
            {name: 'password',   required: true},
            {name: 'active',     required: true}
        ],
        defaults: [
            {name: 'active',    value: true}
        ]
    },


  //**************************************************************************
  //** Path
  //**************************************************************************
    Path: {
        fields: [
            {name: 'dir',           type: 'string'},
            {name: 'host',          type: 'Host'},
            {name: 'lastIndexed',   type: 'date'}
        ],
        constraints: [
            {name: 'dir',    required: true}
        ]
    },


  //**************************************************************************
  //** Host
  //**************************************************************************
    Host: {
        fields: [
            {name: 'name',         type: 'string'},
            {name: 'description',  type: 'string'},
            {name: 'metadata',     type: 'json'}
        ]
    },


  //**************************************************************************
  //** File
  //**************************************************************************
    File: {
        fields: [
            {name: 'name',         type: 'string'},
            {name: 'description',  type: 'string'},
            {name: 'path',         type: 'Path'},
            {name: 'type',         type: 'string'}, //photo, video, etc
            {name: 'date',         type: 'date'},
            {name: 'size',         type: 'long'},
            {name: 'location',     type: 'geo'},
            {name: 'metadata',     type: 'json'}
        ],
        hasMany: [
            {model: 'Rating',     name: 'ratings'},
            {model: 'Keyword',    name: 'keywords'},
            {model: 'Face',       name: 'faces'},
            {model: 'Access',     name: 'accesses'}
        ],
        constraints: [
            {name: 'name',    required: true},
            {name: 'path',    required: true}
        ]
    },


  //**************************************************************************
  //** Rating
  //**************************************************************************
    Rating: {
        fields: [
            {name: 'user',     type: 'UserAccount'},
            {name: 'rating',   type: 'int'},
            {name: 'comment',  type: 'string'},
            {name: 'date',     type: 'date'}
        ]
    },


  //**************************************************************************
  //** Keyword
  //**************************************************************************
    Keyword: {
        fields: [
            {name: 'keyword',   type: 'string'},
            {name: 'category',  type: 'string'}
        ]
    },


  //**************************************************************************
  //** Face
  //**************************************************************************
  /** Used to represent a face extracted from a photo.
   */
    Face: {
        fields: [
            {name: 'coordinates',   type: 'string'},
            {name: 'contact',       type: 'Contact'},
            {name: 'metadata',      type: 'json'}
        ]
    },


  //**************************************************************************
  //** Access
  //**************************************************************************
  /** Used to log access to individual files.
   */
    Access: {
        fields: [
            {name: 'type',      type: 'string'}, //view, download, share, etc
            {name: 'user',      type: 'UserAccount'},
            {name: 'date',      type: 'date'},
            {name: 'metadata',  type: 'json'}
        ]
    },


  //**************************************************************************
  //** Place
  //**************************************************************************
  /** Used to represent a location on the earth. Can be used to represent a
   *  descrete location such as a GPS point, a route, an area (e.g. park,
   *  country, state, etc).
   */
    Place: {
        fields: [
            {name: 'name',         type: 'string'},
            {name: 'description',  type: 'string'},
            {name: 'location',     type: 'geo'}
        ]
    },


  //**************************************************************************
  //** Album
  //**************************************************************************
  /** A collection of photos, videos, documents, etc.
   */
    Album: {
        fields: [
            {name: 'name',         type: 'string'},
            {name: 'description',  type: 'string'},
            {name: 'place',        type: 'Place'},
            {name: 'parent',       type: 'Album'}
        ],
        hasMany: [
            {model: 'Item',       name: 'items'},
            {model: 'Privilege',  name: 'users'}
        ]
    },


  //**************************************************************************
  //** Item
  //**************************************************************************
  /** Used to represent an individual item in an Album.
   */
    Item: {
        fields: [
            {name: 'file',     type: 'File'},
            {name: 'index',    type: 'long'}
        ]
    },


  //**************************************************************************
  //** Privilege
  //**************************************************************************
    Privilege: {
        fields: [
            {name: 'user',     type: 'UserAccount'},
            {name: 'edit',     type: 'boolean'},
            {name: 'share',    type: 'boolean'}
        ]
    }

};