use esa
db.createUser(
  {
    user: "serviceUser",
    pwd: "changeTh1sDefaultPasswrd",
    roles: [
       { role: "read", db: "readonlydb" },
       { role: "readWrite", db: "users" },
       { role: "readWrite", db: "roles" }
    ]
  }
)